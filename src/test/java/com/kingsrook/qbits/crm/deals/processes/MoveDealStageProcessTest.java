/*******************************************************************************
 ** Unit tests for MoveDealStageProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
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
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for moving deals through pipeline stages: basic moves, stage history,
 ** weighted amount recalculation, close-date stamping, required field gating,
 ** and cross-pipeline rejection.
 *******************************************************************************/
class MoveDealStageProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Move a deal from stage 1 to stage 2 and verify stage history, weighted
    ** amount, and audit log entry.
    *******************************************************************************/
   @Test
   void testMoveDealToNextStage() throws QException
   {
      ///////////////////////////////////////////
      // set up pipeline with two stages       //
      ///////////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales Pipeline");
      Integer stage1Id   = insertStage(pipelineId, "Qualification", 1, 10, false, false, null, null);
      Integer stage2Id   = insertStage(pipelineId, "Proposal", 2, 50, false, false, null, null);

      ///////////////////////////////////////////
      // create a deal in stage 1              //
      ///////////////////////////////////////////
      Integer dealId = insertDeal("Big Deal", pipelineId, stage1Id, new BigDecimal("10000"));

      ///////////////////////////////////////////
      // move to stage 2                       //
      ///////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", stage2Id);

      new RunProcessAction().execute(processInput);

      ///////////////////////////////////////////
      // verify deal was updated               //
      ///////////////////////////////////////////
      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(stage2Id, updatedDeal.getPipelineStageId());
      assertNotNull(updatedDeal.getStageEnteredDate());
      assertNull(updatedDeal.getActualCloseDate());

      ///////////////////////////////////////////
      // verify weighted amount = 10000 * 50%  //
      ///////////////////////////////////////////
      assertEquals(0, new BigDecimal("5000.00").compareTo(updatedDeal.getWeightedAmount()));

      ///////////////////////////////////////////
      // verify stage history was created      //
      ///////////////////////////////////////////
      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

      assertEquals(1, historyOutput.getRecords().size());
      DealStageHistory history = new DealStageHistory(historyOutput.getRecords().get(0));
      assertEquals(stage1Id, history.getFromStageId());
      assertEquals(stage2Id, history.getToStageId());
      assertNotNull(history.getTransitionDate());
   }



   /*******************************************************************************
    ** Move a deal to a Closed Won stage and verify actualCloseDate is set.
    *******************************************************************************/
   @Test
   void testMoveDealToClosedWon() throws QException
   {
      Integer pipelineId = insertPipeline("Sales Pipeline");
      Integer stage1Id   = insertStage(pipelineId, "Qualification", 1, 10, false, false, null, null);
      Integer wonStageId = insertStage(pipelineId, "Closed Won", 2, 100, true, false, null, null);

      Integer dealId = insertDeal("Win Deal", pipelineId, stage1Id, new BigDecimal("5000"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", wonStageId);

      new RunProcessAction().execute(processInput);

      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(wonStageId, updatedDeal.getPipelineStageId());
      assertNotNull(updatedDeal.getActualCloseDate());

      ///////////////////////////////////////////
      // verify weighted amount = 5000 * 100%  //
      ///////////////////////////////////////////
      assertEquals(0, new BigDecimal("5000.00").compareTo(updatedDeal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Move a deal to a Closed Lost stage and verify actualCloseDate is set.
    *******************************************************************************/
   @Test
   void testMoveDealToClosedLost() throws QException
   {
      Integer pipelineId  = insertPipeline("Sales Pipeline");
      Integer stage1Id    = insertStage(pipelineId, "Qualification", 1, 10, false, false, null, null);
      Integer lostStageId = insertStage(pipelineId, "Closed Lost", 2, 0, false, true, null, null);

      Integer dealId = insertDeal("Lost Deal", pipelineId, stage1Id, new BigDecimal("8000"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", lostStageId);

      new RunProcessAction().execute(processInput);

      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(updatedDeal.getActualCloseDate());
      assertEquals(0, new BigDecimal("0.00").compareTo(updatedDeal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Verify that moving to a stage in a different pipeline throws an error.
    *******************************************************************************/
   @Test
   void testRejectsCrossPipelineMove() throws QException
   {
      Integer pipeline1Id = insertPipeline("Pipeline A");
      Integer stage1Id    = insertStage(pipeline1Id, "Stage A1", 1, 10, false, false, null, null);

      Integer pipeline2Id = insertPipeline("Pipeline B");
      Integer stage2Id    = insertStage(pipeline2Id, "Stage B1", 1, 20, false, false, null, null);

      Integer dealId = insertDeal("Cross Pipeline Deal", pipeline1Id, stage1Id, new BigDecimal("1000"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", stage2Id);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



   /*******************************************************************************
    ** Verify that required field gating blocks the move when field is null.
    *******************************************************************************/
   @Test
   void testRequiredFieldGatingBlocks() throws QException
   {
      Integer pipelineId = insertPipeline("Gated Pipeline");
      Integer stage1Id   = insertStage(pipelineId, "Open", 1, 10, false, false, null, null);
      Integer stage2Id   = insertStage(pipelineId, "Needs Amount", 2, 50, false, false, null, "[\"expectedCloseDate\"]");

      //////////////////////////////////////////////////////////////
      // create deal without expectedCloseDate                    //
      //////////////////////////////////////////////////////////////
      Integer dealId = insertDeal("Gated Deal", pipelineId, stage1Id, new BigDecimal("1000"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", stage2Id);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



   /*******************************************************************************
    ** Verify that probabilityOverridePct is used when set on the deal.
    *******************************************************************************/
   @Test
   void testProbabilityOverrideUsed() throws QException
   {
      Integer pipelineId = insertPipeline("Override Pipeline");
      Integer stage1Id   = insertStage(pipelineId, "Open", 1, 10, false, false, null, null);
      Integer stage2Id   = insertStage(pipelineId, "Nego", 2, 50, false, false, null, null);

      /////////////////////////////////////////////////////
      // create deal with probabilityOverridePct = 75    //
      /////////////////////////////////////////////////////
      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName("Override Deal")
               .withPipelineId(pipelineId)
               .withPipelineStageId(stage1Id)
               .withAmount(new BigDecimal("10000"))
               .withProbabilityOverridePct(75)
               .withOwnerUserId("user-001")
               .withStageEnteredDate(Instant.now().minus(2, ChronoUnit.DAYS))));
      Integer dealId = dealInsert.getRecords().get(0).getValueInteger("id");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", stage2Id);

      new RunProcessAction().execute(processInput);

      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));

      //////////////////////////////////////////////////////////
      // weighted = 10000 * 75 / 100 = 7500.00               //
      //////////////////////////////////////////////////////////
      assertEquals(0, new BigDecimal("7500.00").compareTo(updatedDeal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Verify that an audit log entry with STAGE_CHANGED is created.
    *******************************************************************************/
   @Test
   void testAuditLogCreated() throws QException
   {
      Integer pipelineId = insertPipeline("Audit Pipeline");
      Integer stage1Id   = insertStage(pipelineId, "Step 1", 1, 10, false, false, null, null);
      Integer stage2Id   = insertStage(pipelineId, "Step 2", 2, 50, false, false, null, null);

      Integer dealId = insertDeal("Audit Deal", pipelineId, stage1Id, new BigDecimal("3000"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MoveDealStageProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("targetStageId", stage2Id);

      new RunProcessAction().execute(processInput);

      ///////////////////////////////////////////
      // verify audit log entry                //
      ///////////////////////////////////////////
      QueryOutput auditOutput = new QueryAction().execute(
         new QueryInput(AuditLog.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("entityId", QCriteriaOperator.EQUALS, dealId))
               .withCriteria(new QFilterCriteria("action", QCriteriaOperator.EQUALS, CrmAuditAction.STAGE_CHANGED.getId()))));

      assertTrue(auditOutput.getRecords().size() >= 1);
      AuditLog audit = new AuditLog(auditOutput.getRecords().get(0));
      assertEquals("pipelineStageId", audit.getFieldName());
      assertEquals(String.valueOf(stage1Id), audit.getOldValue());
      assertEquals(String.valueOf(stage2Id), audit.getNewValue());
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
                               Integer probabilityPct, Boolean isClosedWon, Boolean isClosedLost,
                               Integer rotDays, String requiredFieldsJson) throws QException
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
               .withRotDays(rotDays)
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
               .withOwnerUserId("user-001")
               .withStageEnteredDate(Instant.now().minus(5, ChronoUnit.DAYS))));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
