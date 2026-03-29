/*******************************************************************************
 ** Unit tests for CrmPipelineConversionWidget.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the Pipeline Conversion widget.
 *******************************************************************************/
class CrmPipelineConversionWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Insert stage history data and render widget.
    *******************************************************************************/
   @Test
   void testRenderPipelineConversion() throws QException
   {
      ///////////////////////////////////////
      // set up pipeline and stages        //
      ///////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id   = insertPipelineStage(pipelineId, "Prospect", 1);
      Integer stage2Id   = insertPipelineStage(pipelineId, "Qualified", 2);
      Integer stage3Id   = insertPipelineStage(pipelineId, "Closed", 3);

      ///////////////////////////////////////
      // insert stage transitions          //
      ///////////////////////////////////////
      insertStageHistory(1, null, stage1Id);
      insertStageHistory(1, stage1Id, stage2Id);
      insertStageHistory(2, null, stage1Id);
      insertStageHistory(2, stage1Id, stage2Id);
      insertStageHistory(2, stage2Id, stage3Id);

      ///////////////////////////////////////
      // render the widget                 //
      ///////////////////////////////////////
      CrmPipelineConversionRenderer renderer = new CrmPipelineConversionRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmPipelineConversionWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();
      assertThat(rows).isNotEmpty();

      Map<String, Object> firstRow = rows.get(0);
      assertThat(firstRow).containsKey("stageName");
      assertThat(firstRow).containsKey("entered");
      assertThat(firstRow).containsKey("progressed");
      assertThat(firstRow).containsKey("conversionRate");
   }



   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName(name).withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(sortOrder * 25)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private void insertStageHistory(Integer dealId, Integer fromStageId, Integer toStageId) throws QException
   {
      new InsertAction().execute(
         new InsertInput(DealStageHistory.TABLE_NAME).withRecordEntity(
            new DealStageHistory()
               .withDealId(dealId)
               .withFromStageId(fromStageId)
               .withToStageId(toStageId)
               .withUserId("user-001")
               .withTransitionDate(Instant.now())));
   }

}
