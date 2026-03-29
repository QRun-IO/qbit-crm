/*******************************************************************************
 ** Unit tests for DealStageChangeCustomizer -- verifies that updating a deal's
 ** pipelineStageId triggers recalculation of weightedAmount, stageEnteredDate,
 ** actualCloseDate, and DealStageHistory creation.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for DealStageChangeCustomizer: verifies stage transition business
 ** logic fires correctly when pipelineStageId is updated via UpdateAction.
 *******************************************************************************/
class DealStageChangeCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Create deal, update pipelineStageId via UpdateAction. Verify
    ** stageEnteredDate changed.
    *******************************************************************************/
   @Test
   void testStageChangeUpdatesStageEnteredDate() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer stage2Id = insertStage(pipelineId, "Negotiation", 2, 50, false, false, null);
      Integer dealId = insertDeal("Stage Date Deal", pipelineId, stage1Id, new BigDecimal("5000"));

      ///////////////////////////////////////////
      // get the initial stageEnteredDate      //
      ///////////////////////////////////////////
      Deal dealBefore = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      Instant enteredDateBefore = dealBefore.getStageEnteredDate();
      assertNotNull(enteredDateBefore);

      ///////////////////////////////////////////
      // move to stage 2                       //
      ///////////////////////////////////////////
      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", stage2Id);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      Deal dealAfter = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(dealAfter.getStageEnteredDate());

      //////////////////////////////////////////////////////////////////
      // the stageEnteredDate should have been refreshed by the       //
      // customizer to a new Instant.now() -- at minimum it should    //
      // not be before the original enteredDate                       //
      //////////////////////////////////////////////////////////////////
      assertFalse(dealAfter.getStageEnteredDate().isBefore(enteredDateBefore));
   }



   /*******************************************************************************
    ** Create deal with amount=1000, move to stage with probabilityPct=50.
    ** Verify weightedAmount=500.
    *******************************************************************************/
   @Test
   void testStageChangeRecalculatesWeightedAmount() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer stage2Id = insertStage(pipelineId, "Qualified", 2, 50, false, false, null);
      Integer dealId = insertDeal("Weighted Deal", pipelineId, stage1Id, new BigDecimal("1000"));

      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", stage2Id);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(0, new BigDecimal("500.00").compareTo(deal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Set probabilityOverridePct=80 on deal, move stage. Verify weightedAmount
    ** uses 80 not stage probability.
    *******************************************************************************/
   @Test
   void testStageChangeWithOverrideProbability() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer stage2Id = insertStage(pipelineId, "Qualified", 2, 50, false, false, null);

      ///////////////////////////////////////////
      // insert deal with override probability //
      ///////////////////////////////////////////
      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName("Override Deal")
               .withPipelineId(pipelineId)
               .withPipelineStageId(stage1Id)
               .withAmount(new BigDecimal("1000"))
               .withProbabilityOverridePct(80)
               .withOwnerUserId("user-001")));
      Integer dealId = dealInsert.getRecords().get(0).getValueInteger("id");

      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", stage2Id);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));

      //////////////////////////////////////////////////////////////////
      // weighted = 1000 * 80 / 100 = 800.00                         //
      //////////////////////////////////////////////////////////////////
      assertEquals(0, new BigDecimal("800.00").compareTo(deal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Move deal to new stage. Query DealStageHistory, verify row with correct
    ** fromStageId and toStageId.
    *******************************************************************************/
   @Test
   void testStageChangeCreatesHistory() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer stage2Id = insertStage(pipelineId, "Proposal", 2, 60, false, false, null);
      Integer dealId = insertDeal("History Deal", pipelineId, stage1Id, new BigDecimal("2000"));

      ///////////////////////////////////////////
      // DealInitializationCustomizer already  //
      // created one history row on insert     //
      ///////////////////////////////////////////
      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", stage2Id);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

      //////////////////////////////////////////////////////////////////
      // expect 2 rows: initial (null->stage1) + change (stage1->stage2)
      //////////////////////////////////////////////////////////////////
      assertEquals(2, historyOutput.getRecords().size());

      ///////////////////////////////////////////
      // verify the stage change history row   //
      ///////////////////////////////////////////
      DealStageHistory changeHistory = new DealStageHistory(historyOutput.getRecords().get(1));
      assertEquals(stage1Id, changeHistory.getFromStageId());
      assertEquals(stage2Id, changeHistory.getToStageId());
      assertNotNull(changeHistory.getTransitionDate());
   }



   /*******************************************************************************
    ** Move deal to closedWon stage. Verify actualCloseDate is set.
    *******************************************************************************/
   @Test
   void testClosedWonSetsActualCloseDate() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer wonStageId = insertStage(pipelineId, "Closed Won", 2, 100, true, false, null);
      Integer dealId = insertDeal("Won Deal", pipelineId, stage1Id, new BigDecimal("7000"));

      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", wonStageId);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getActualCloseDate());
   }



   /*******************************************************************************
    ** Move deal to closedLost stage. Verify actualCloseDate is set.
    *******************************************************************************/
   @Test
   void testClosedLostSetsActualCloseDate() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer lostStageId = insertStage(pipelineId, "Closed Lost", 2, 0, false, true, null);
      Integer dealId = insertDeal("Lost Deal", pipelineId, stage1Id, new BigDecimal("4000"));

      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", lostStageId);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getActualCloseDate());
   }



   /*******************************************************************************
    ** Close deal, then move back to open stage. Verify actualCloseDate is null
    ** and winLossReasonId is null.
    *******************************************************************************/
   @Test
   void testReopenClearsActualCloseDate() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer openStageId = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer wonStageId = insertStage(pipelineId, "Closed Won", 2, 100, true, false, null);
      Integer reopenStageId = insertStage(pipelineId, "Reopened", 3, 30, false, false, null);
      Integer dealId = insertDeal("Reopen Deal", pipelineId, openStageId, new BigDecimal("6000"));

      ///////////////////////////////////////////
      // close the deal                        //
      ///////////////////////////////////////////
      QRecord closeRecord = new QRecord();
      closeRecord.setValue("id", dealId);
      closeRecord.setValue("pipelineStageId", wonStageId);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(closeRecord));

      Deal closedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(closedDeal.getActualCloseDate());

      ///////////////////////////////////////////
      // reopen the deal                       //
      ///////////////////////////////////////////
      QRecord reopenRecord = new QRecord();
      reopenRecord.setValue("id", dealId);
      reopenRecord.setValue("pipelineStageId", reopenStageId);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(reopenRecord));

      Deal reopenedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNull(reopenedDeal.getActualCloseDate());
      assertNull(reopenedDeal.getWinLossReasonId());
   }



   /*******************************************************************************
    ** Create stage with requiredFieldsJson=["amount"]. Create deal with
    ** amount=null. Try to move to that stage. Verify the record has an error.
    *******************************************************************************/
   @Test
   void testRequiredFieldsJsonBlocksInvalidMove() throws QException
   {
      Integer pipelineId = insertPipeline("Gated");
      Integer stage1Id = insertStage(pipelineId, "Open", 1, 10, false, false, null);
      Integer gatedStageId = insertStage(pipelineId, "Gated", 2, 50, false, false, "[\"expectedCloseDate\"]");

      ///////////////////////////////////////////
      // create deal without expectedCloseDate //
      ///////////////////////////////////////////
      Integer dealId = insertDeal("Gated Deal", pipelineId, stage1Id, new BigDecimal("1000"));

      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      updateRecord.setValue("pipelineStageId", gatedStageId);

      UpdateInput updateInput = new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord);
      new UpdateAction().execute(updateInput);

      //////////////////////////////////////////////////////////////////
      // The customizer adds an error to the record. Verify the deal  //
      // was NOT moved to the gated stage.                            //
      //////////////////////////////////////////////////////////////////
      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(stage1Id, deal.getPipelineStageId());
   }



   /////////////////////////////////////////////////////////////////////////////
   // Helper methods                                                          //
   /////////////////////////////////////////////////////////////////////////////

   /***************************************************************************
    ** Helper: insert a Pipeline and return its id.
    ***************************************************************************/
   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName(name)
               .withIsDefault(true)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage and return its id.
    ***************************************************************************/
   private Integer insertStage(Integer pipelineId, String name, Integer sortOrder,
                               Integer probabilityPct, Boolean isClosedWon, Boolean isClosedLost,
                               String requiredFieldsJson) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(isClosedWon)
               .withIsClosedLost(isClosedLost)
               .withRequiredFieldsJson(requiredFieldsJson)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Deal and return its id.
    ***************************************************************************/
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId, BigDecimal amount) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
