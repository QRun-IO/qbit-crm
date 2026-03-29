/*******************************************************************************
 ** Unit tests for DealInitializationCustomizer -- verifies that inserting a
 ** deal sets stageEnteredDate, weightedAmount, and creates the initial
 ** DealStageHistory row.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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


/*******************************************************************************
 ** Tests for DealInitializationCustomizer: verifies computed field
 ** initialization and initial stage history creation on deal insert.
 *******************************************************************************/
class DealInitializationCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Insert a deal via InsertAction. Get it back. Verify stageEnteredDate is set.
    *******************************************************************************/
   @Test
   void testNewDealGetsStageEnteredDate() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("New Deal", pipelineId, stageId, new BigDecimal("5000"));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getStageEnteredDate());
   }



   /*******************************************************************************
    ** Insert deal with amount=1000 at a stage with probabilityPct=50. Verify
    ** weightedAmount=500.
    *******************************************************************************/
   @Test
   void testNewDealGetsWeightedAmount() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Qualified", 1, 50);
      Integer dealId = insertDeal("Weighted Deal", pipelineId, stageId, new BigDecimal("1000"));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getWeightedAmount());
      assertEquals(0, new BigDecimal("500.00").compareTo(deal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Insert deal. Query DealStageHistory for that dealId. Verify one row with
    ** fromStageId=null and toStageId matching the deal's stage.
    *******************************************************************************/
   @Test
   void testNewDealCreatesInitialHistory() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("History Deal", pipelineId, stageId, new BigDecimal("3000"));

      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

      assertEquals(1, historyOutput.getRecords().size());

      DealStageHistory history = new DealStageHistory(historyOutput.getRecords().get(0));
      assertNull(history.getFromStageId());
      assertEquals(stageId, history.getToStageId());
      assertNotNull(history.getTransitionDate());
      assertEquals(dealId, history.getDealId());
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
   private Integer insertStage(Integer pipelineId, String name, Integer sortOrder, Integer probabilityPct) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(false)
               .withIsClosedLost(false)));
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
