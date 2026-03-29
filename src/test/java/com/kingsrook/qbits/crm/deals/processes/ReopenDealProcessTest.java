/*******************************************************************************
 ** Unit tests for ReopenDealProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Tests for reopening closed deals: verifies actualCloseDate is cleared,
 ** winLossReasonId is cleared, weighted amount is recalculated, stage history
 ** is logged, and validation rejects reopening non-closed deals or targeting
 ** closed stages.
 *******************************************************************************/
class ReopenDealProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Close a deal (Closed Won), then reopen to an active stage. Verify
    ** actualCloseDate and winLossReasonId are cleared.
    *******************************************************************************/
   @Test
   void testReopenClosedWonDeal() throws QException
   {
      ///////////////////////////////////////////
      // set up pipeline with stages           //
      ///////////////////////////////////////////
      Integer pipelineId  = insertPipeline("Sales Pipeline");
      Integer activeStage = insertStage(pipelineId, "Negotiation", 1, 60, false, false);
      Integer wonStage    = insertStage(pipelineId, "Closed Won", 2, 100, true, false);

      ///////////////////////////////////////////
      // create a deal already in Closed Won   //
      ///////////////////////////////////////////
      Integer dealId = insertClosedDeal("Won Deal", pipelineId, wonStage, new BigDecimal("10000"), 42);

      ///////////////////////////////////////////
      // reopen the deal to Negotiation        //
      ///////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ReopenDealProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", activeStage);

      new RunProcessAction().execute(processInput);

      ///////////////////////////////////////////
      // verify deal was updated               //
      ///////////////////////////////////////////
      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(activeStage, updatedDeal.getPipelineStageId());
      assertNull(updatedDeal.getActualCloseDate());
      assertNull(updatedDeal.getWinLossReasonId());
      assertNotNull(updatedDeal.getStageEnteredDate());

      ///////////////////////////////////////////
      // verify weighted amount recalculated   //
      ///////////////////////////////////////////
      assertEquals(0, new BigDecimal("6000.00").compareTo(updatedDeal.getWeightedAmount()));

      ///////////////////////////////////////////
      // verify stage history was created      //
      ///////////////////////////////////////////
      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

      assertEquals(1, historyOutput.getRecords().size());
      DealStageHistory history = new DealStageHistory(historyOutput.getRecords().get(0));
      assertEquals(wonStage, history.getFromStageId());
      assertEquals(activeStage, history.getToStageId());
   }



   /*******************************************************************************
    ** Verify that reopening a deal that is NOT closed throws an error.
    *******************************************************************************/
   @Test
   void testRejectsReopenNonClosedDeal() throws QException
   {
      Integer pipelineId  = insertPipeline("Pipeline");
      Integer activeStage = insertStage(pipelineId, "Active", 1, 30, false, false);
      Integer otherStage  = insertStage(pipelineId, "Other Active", 2, 50, false, false);

      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName("Open Deal")
               .withPipelineId(pipelineId)
               .withPipelineStageId(activeStage)
               .withAmount(new BigDecimal("5000"))
               .withOwnerUserId("user-001")
               .withStageEnteredDate(Instant.now())));
      Integer dealId = dealInsert.getRecords().get(0).getValueInteger("id");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ReopenDealProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", otherStage);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



   /*******************************************************************************
    ** Verify that targeting a closed stage when reopening throws an error.
    *******************************************************************************/
   @Test
   void testRejectsReopenToClosedStage() throws QException
   {
      Integer pipelineId = insertPipeline("Pipeline");
      Integer wonStage   = insertStage(pipelineId, "Closed Won", 1, 100, true, false);
      Integer lostStage  = insertStage(pipelineId, "Closed Lost", 2, 0, false, true);

      Integer dealId = insertClosedDeal("Closed Deal", pipelineId, wonStage, new BigDecimal("5000"), null);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ReopenDealProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", lostStage);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



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
                               Integer probabilityPct, Boolean isClosedWon, Boolean isClosedLost) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(isClosedWon)
               .withIsClosedLost(isClosedLost)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a closed deal and return its id.
    ***************************************************************************/
   private Integer insertClosedDeal(String name, Integer pipelineId, Integer stageId,
                                    BigDecimal amount, Integer winLossReasonId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withOwnerUserId("user-001")
               .withActualCloseDate(LocalDate.now())
               .withWinLossReasonId(winLossReasonId)
               .withStageEnteredDate(Instant.now().minus(3, ChronoUnit.DAYS))));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
