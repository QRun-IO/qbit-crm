/*******************************************************************************
 ** Unit tests for CrmPipelineSummaryWidget.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the CRM Pipeline Summary widget renderer.
 *******************************************************************************/
class CrmPipelineSummaryWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Create deals in various stages, render, verify counts and amounts.
    *******************************************************************************/
   @Test
   void testRenderPipelineSummary() throws QException
   {
      ///////////////////////////////////////////
      // set up pipeline and stages            //
      ///////////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales Pipeline");
      Integer stage1Id   = insertStage(pipelineId, "Qualification", 1, 10);
      Integer stage2Id   = insertStage(pipelineId, "Proposal", 2, 50);
      Integer stage3Id   = insertStage(pipelineId, "Closed Won", 3, 100);

      ///////////////////////////////////////////
      // insert deals in various stages        //
      ///////////////////////////////////////////
      insertDeal("Deal A", pipelineId, stage1Id, new BigDecimal("10000"), new BigDecimal("1000"));
      insertDeal("Deal B", pipelineId, stage1Id, new BigDecimal("5000"), new BigDecimal("500"));
      insertDeal("Deal C", pipelineId, stage2Id, new BigDecimal("20000"), new BigDecimal("10000"));

      ///////////////////////////////////////////
      // render the widget                     //
      ///////////////////////////////////////////
      CrmPipelineSummaryRenderer renderer = new CrmPipelineSummaryRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmPipelineSummaryWidgetProducer().produce(null));
      widgetInput.setQueryParams(new HashMap<>(Map.of("pipelineId", String.valueOf(pipelineId))));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();

      ///////////////////////////////////////////
      // three stages should appear            //
      ///////////////////////////////////////////
      assertEquals(3, rows.size());

      ///////////////////////////////////////////
      // verify stage 1 counts                 //
      ///////////////////////////////////////////
      Map<String, Object> qualRow = rows.get(0);
      assertEquals("Qualification", qualRow.get("stageName"));
      assertEquals(2, qualRow.get("dealCount"));

      ///////////////////////////////////////////
      // verify stage 2 counts                 //
      ///////////////////////////////////////////
      Map<String, Object> propRow = rows.get(1);
      assertEquals("Proposal", propRow.get("stageName"));
      assertEquals(1, propRow.get("dealCount"));

      ///////////////////////////////////////////
      // verify stage 3 has zero deals         //
      ///////////////////////////////////////////
      Map<String, Object> wonRow = rows.get(2);
      assertEquals("Closed Won", wonRow.get("stageName"));
      assertEquals(0, wonRow.get("dealCount"));
   }



   /*******************************************************************************
    ** Render widget with no deals and verify empty stage rows still appear.
    *******************************************************************************/
   @Test
   void testRenderEmptyPipeline() throws QException
   {
      Integer pipelineId = insertPipeline("Empty Pipeline");
      insertStage(pipelineId, "Stage A", 1, 10);
      insertStage(pipelineId, "Stage B", 2, 50);

      CrmPipelineSummaryRenderer renderer = new CrmPipelineSummaryRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmPipelineSummaryWidgetProducer().produce(null));
      widgetInput.setQueryParams(new HashMap<>(Map.of("pipelineId", String.valueOf(pipelineId))));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      assertEquals(2, tableData.getRows().size());

      ///////////////////////////////////////////
      // both stages should have zero deals    //
      ///////////////////////////////////////////
      assertEquals(0, tableData.getRows().get(0).get("dealCount"));
      assertEquals(0, tableData.getRows().get(1).get("dealCount"));
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
    ** Helper: insert a Deal.
    ***************************************************************************/
   private void insertDeal(String name, Integer pipelineId, Integer stageId,
                           BigDecimal amount, BigDecimal weightedAmount) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withWeightedAmount(weightedAmount)
               .withOwnerUserId("user-001")
               .withStageEnteredDate(Instant.now())));
   }

}
