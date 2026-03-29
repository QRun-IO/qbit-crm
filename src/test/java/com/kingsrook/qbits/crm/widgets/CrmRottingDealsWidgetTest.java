/*******************************************************************************
 ** Unit tests for CrmRottingDealsWidget.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 ** Tests for the CRM Rotting Deals widget renderer.
 *******************************************************************************/
class CrmRottingDealsWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Create deals past rot threshold and verify they appear in the widget.
    *******************************************************************************/
   @Test
   void testRenderRottingDeals() throws QException
   {
      ///////////////////////////////////////////
      // set up pipeline with rot days         //
      ///////////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales Pipeline");
      Integer stageId    = insertStage(pipelineId, "Qualification", 1, 10, 7);

      /////////////////////////////////////////////////////////
      // insert a deal that entered stage 15 days ago (rot=7) //
      /////////////////////////////////////////////////////////
      insertDealInStage("Stale Deal", pipelineId, stageId,
         new BigDecimal("10000"), Instant.now().minus(15, ChronoUnit.DAYS));

      /////////////////////////////////////////////////////////
      // insert a deal that entered stage 3 days ago (ok)     //
      /////////////////////////////////////////////////////////
      insertDealInStage("Fresh Deal", pipelineId, stageId,
         new BigDecimal("5000"), Instant.now().minus(3, ChronoUnit.DAYS));

      ///////////////////////////////////////////
      // render the widget                     //
      ///////////////////////////////////////////
      CrmRottingDealsRenderer renderer = new CrmRottingDealsRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmRottingDealsWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();

      ///////////////////////////////////////////
      // only the stale deal should appear     //
      ///////////////////////////////////////////
      assertEquals(1, rows.size());
      assertEquals("Stale Deal", rows.get(0).get("dealName"));
      assertEquals("Qualification", rows.get(0).get("stageName"));
      assertEquals(7, rows.get(0).get("rotDays"));

      int daysInStage = (int) rows.get(0).get("daysInStage");
      assertThat(daysInStage).isGreaterThanOrEqualTo(15);
   }



   /*******************************************************************************
    ** Deals in stages without rotDays should not appear.
    *******************************************************************************/
   @Test
   void testStagesWithoutRotDaysAreExcluded() throws QException
   {
      Integer pipelineId = insertPipeline("No Rot Pipeline");
      Integer stageId    = insertStageNoRot(pipelineId, "Open", 1, 10);

      insertDealInStage("No Rot Deal", pipelineId, stageId,
         new BigDecimal("1000"), Instant.now().minus(100, ChronoUnit.DAYS));

      CrmRottingDealsRenderer renderer = new CrmRottingDealsRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmRottingDealsWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      assertEquals(0, tableData.getRows().size());
   }



   /*******************************************************************************
    ** Render with no deals and verify empty output.
    *******************************************************************************/
   @Test
   void testRenderEmptyRottingDeals() throws QException
   {
      CrmRottingDealsRenderer renderer = new CrmRottingDealsRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmRottingDealsWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      assertEquals(0, tableData.getRows().size());
   }



   /*******************************************************************************
    ** Deals with multiple rot stages: only past-threshold ones appear.
    *******************************************************************************/
   @Test
   void testMultipleStagesWithDifferentRotDays() throws QException
   {
      Integer pipelineId = insertPipeline("Multi Rot");
      Integer stage1Id   = insertStage(pipelineId, "Stage 5 Day Rot", 1, 10, 5);
      Integer stage2Id   = insertStage(pipelineId, "Stage 30 Day Rot", 2, 50, 30);

      ///////////////////////////////////////////
      // deal in stage1, 10 days old (rot=5)   //
      ///////////////////////////////////////////
      insertDealInStage("Rotting 1", pipelineId, stage1Id,
         new BigDecimal("8000"), Instant.now().minus(10, ChronoUnit.DAYS));

      ///////////////////////////////////////////
      // deal in stage2, 10 days old (rot=30)  //
      ///////////////////////////////////////////
      insertDealInStage("Fresh 2", pipelineId, stage2Id,
         new BigDecimal("12000"), Instant.now().minus(10, ChronoUnit.DAYS));

      CrmRottingDealsRenderer renderer = new CrmRottingDealsRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmRottingDealsWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();

      ///////////////////////////////////////////
      // only Rotting 1 should appear          //
      ///////////////////////////////////////////
      assertEquals(1, rows.size());
      assertEquals("Rotting 1", rows.get(0).get("dealName"));
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
    ** Helper: insert a PipelineStage with rotDays.
    ***************************************************************************/
   private Integer insertStage(Integer pipelineId, String name, Integer sortOrder,
                               Integer probabilityPct, Integer rotDays) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(false)
               .withIsClosedLost(false)
               .withRotDays(rotDays)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage without rotDays.
    ***************************************************************************/
   private Integer insertStageNoRot(Integer pipelineId, String name, Integer sortOrder,
                                    Integer probabilityPct) throws QException
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
    ** Helper: insert a Deal with a specific stageEnteredDate.
    ***************************************************************************/
   private void insertDealInStage(String name, Integer pipelineId, Integer stageId,
                                  BigDecimal amount, Instant stageEnteredDate) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withOwnerUserId("user-001")
               .withStageEnteredDate(stageEnteredDate)));
   }

}
