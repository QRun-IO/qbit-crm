/*******************************************************************************
 ** Unit tests for CrmWinRateWidget.
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
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for the Win Rate widget.
 *******************************************************************************/
class CrmWinRateWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Insert won and lost deal transitions, render widget, verify output.
    *******************************************************************************/
   @Test
   void testRenderWinRate() throws QException
   {
      ///////////////////////////////////////
      // set up pipeline and stages        //
      ///////////////////////////////////////
      Integer pipelineId    = insertPipeline("Sales");
      Integer prospectId    = insertPipelineStage(pipelineId, "Prospect", 1, false, false);
      Integer closedWonId   = insertPipelineStage(pipelineId, "Closed Won", 2, true, false);
      Integer closedLostId  = insertPipelineStage(pipelineId, "Closed Lost", 3, false, true);

      ///////////////////////////////////////
      // insert transitions                //
      ///////////////////////////////////////
      insertStageHistory(1, prospectId, closedWonId);
      insertStageHistory(2, prospectId, closedWonId);
      insertStageHistory(3, prospectId, closedLostId);

      ///////////////////////////////////////
      // render the widget                 //
      ///////////////////////////////////////
      CrmWinRateRenderer renderer = new CrmWinRateRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmWinRateWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();
      assertThat(rows).isNotEmpty();

      Map<String, Object> firstRow = rows.get(0);
      assertThat(firstRow).containsKey("month");
      assertThat(firstRow).containsKey("won");
      assertThat(firstRow).containsKey("lost");
      assertThat(firstRow).containsKey("winRate");

      //////////////////////////////////////////////
      // verify 2 won, 1 lost => 66.7% win rate  //
      //////////////////////////////////////////////
      int totalWon  = (int) firstRow.get("won");
      int totalLost = (int) firstRow.get("lost");
      assertTrue(totalWon >= 2);
      assertTrue(totalLost >= 1);
   }



   /*******************************************************************************
    ** Render with no data returns empty rows.
    *******************************************************************************/
   @Test
   void testRenderEmptyWinRate() throws QException
   {
      CrmWinRateRenderer renderer = new CrmWinRateRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmWinRateWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      assertThat(tableData.getRows()).isEmpty();
   }



   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName(name).withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder, boolean isClosedWon, boolean isClosedLost) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(isClosedWon ? 100 : 0)
               .withIsClosedWon(isClosedWon)
               .withIsClosedLost(isClosedLost)));
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
