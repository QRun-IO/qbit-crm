/*******************************************************************************
 ** Unit tests for the DealStageHistory entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for DealStageHistory append-only insert behavior and field
 ** persistence.
 *******************************************************************************/
class DealStageHistoryTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the deal stage history table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(DealStageHistory.TABLE_NAME);
      assertNotNull(table);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("dealId");
      assertThat(table.getFields()).containsKey("fromStageId");
      assertThat(table.getFields()).containsKey("toStageId");
      assertThat(table.getFields()).containsKey("userId");
      assertThat(table.getFields()).containsKey("transitionDate");
      assertThat(table.getFields()).containsKey("durationInFromStageDays");
      assertThat(table.getFields()).containsKey("createDate");

      /////////////////////////////////////////////
      // append-only: no modifyDate field        //
      /////////////////////////////////////////////
      assertThat(table.getFields()).doesNotContainKey("modifyDate");

      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("history");
   }



   /*******************************************************************************
    ** Insert an initial stage history entry (fromStageId is null) and verify.
    *******************************************************************************/
   @Test
   void testInsertInitialStageEntry() throws QException
   {
      Integer pipelineId = insertPipeline("Pipeline");
      Integer stage1Id = insertPipelineStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("New Deal", pipelineId, stage1Id);

      Instant now = Instant.now();

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(DealStageHistory.TABLE_NAME)
            .withRecordEntity(new DealStageHistory()
               .withDealId(dealId)
               .withFromStageId(null)
               .withToStageId(stage1Id)
               .withUserId("user-001")
               .withTransitionDate(now)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME));
      assertEquals(1, queryOutput.getRecords().size());

      DealStageHistory fetched = new DealStageHistory(queryOutput.getRecords().get(0));
      assertEquals(dealId, fetched.getDealId());
      assertNull(fetched.getFromStageId());
      assertEquals(stage1Id, fetched.getToStageId());
      assertEquals("user-001", fetched.getUserId());
      assertNotNull(fetched.getTransitionDate());
   }



   /*******************************************************************************
    ** Insert multiple stage transitions and verify all persist (append-only).
    *******************************************************************************/
   @Test
   void testMultipleTransitions() throws QException
   {
      Integer pipelineId = insertPipeline("Pipeline");
      Integer stage1Id = insertPipelineStage(pipelineId, "Prospecting", 1, 10);
      Integer stage2Id = insertPipelineStage(pipelineId, "Qualification", 2, 25);
      Integer stage3Id = insertPipelineStage(pipelineId, "Proposal", 3, 50);
      Integer dealId = insertDeal("Progressive Deal", pipelineId, stage1Id);

      Instant now = Instant.now();

      //////////////////////////////////////
      // initial entry: null -> stage 1   //
      //////////////////////////////////////
      new InsertAction().execute(new InsertInput(DealStageHistory.TABLE_NAME)
         .withRecordEntity(new DealStageHistory()
            .withDealId(dealId)
            .withFromStageId(null)
            .withToStageId(stage1Id)
            .withUserId("user-001")
            .withTransitionDate(now)));

      //////////////////////////////////////
      // transition: stage 1 -> stage 2   //
      //////////////////////////////////////
      new InsertAction().execute(new InsertInput(DealStageHistory.TABLE_NAME)
         .withRecordEntity(new DealStageHistory()
            .withDealId(dealId)
            .withFromStageId(stage1Id)
            .withToStageId(stage2Id)
            .withUserId("user-001")
            .withTransitionDate(now.plusSeconds(86400))
            .withDurationInFromStageDays(3)));

      //////////////////////////////////////
      // transition: stage 2 -> stage 3   //
      //////////////////////////////////////
      new InsertAction().execute(new InsertInput(DealStageHistory.TABLE_NAME)
         .withRecordEntity(new DealStageHistory()
            .withDealId(dealId)
            .withFromStageId(stage2Id)
            .withToStageId(stage3Id)
            .withUserId("user-002")
            .withTransitionDate(now.plusSeconds(172800))
            .withDurationInFromStageDays(5)));

      //////////////////////////////////////////////
      // verify all three entries are persisted   //
      //////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME));
      assertEquals(3, queryOutput.getRecords().size());

      //////////////////////////////////////////////
      // verify the latest transition details     //
      //////////////////////////////////////////////
      DealStageHistory latest = new DealStageHistory(queryOutput.getRecords().stream()
         .filter(r -> stage3Id.equals(r.getValueInteger("toStageId")))
         .findFirst()
         .orElseThrow());
      assertEquals(stage2Id, latest.getFromStageId());
      assertEquals(stage3Id, latest.getToStageId());
      assertEquals("user-002", latest.getUserId());
      assertEquals(5, latest.getDurationInFromStageDays());
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
   private Integer insertPipelineStage(Integer pipelineId, String name, Integer sortOrder, Integer probabilityPct) throws QException
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
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
