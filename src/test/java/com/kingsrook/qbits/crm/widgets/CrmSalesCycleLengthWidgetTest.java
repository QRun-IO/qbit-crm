/*******************************************************************************
 ** Unit tests for CrmSalesCycleLengthRenderer.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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


/*******************************************************************************
 ** Tests for the Sales Cycle Length widget: with data and empty results.
 *******************************************************************************/
class CrmSalesCycleLengthWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Test render with closed-won deals computes cycle length.
    *******************************************************************************/
   @Test
   void testRenderWithData() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer wonStageId = insertPipelineStage(pipelineId, "Won", 3, true, false);

      insertDeal("Deal A", pipelineId, wonStageId, LocalDate.now().minusDays(30));

      CrmSalesCycleLengthRenderer renderer = new CrmSalesCycleLengthRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmSalesCycleLengthWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);
      TableData tableData = (TableData) output.getWidgetData();

      assertThat(tableData.getRows()).hasSize(2);
      assertThat(tableData.getRows().get(0).get("metric")).isEqualTo("Avg Sales Cycle (days)");
      assertThat(tableData.getRows().get(1).get("value")).isEqualTo("1");
   }



   /*******************************************************************************
    ** Test render with no data shows zero.
    *******************************************************************************/
   @Test
   void testRenderEmpty() throws QException
   {
      CrmSalesCycleLengthRenderer renderer = new CrmSalesCycleLengthRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmSalesCycleLengthWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);
      TableData tableData = (TableData) output.getWidgetData();

      assertThat(tableData.getRows()).hasSize(2);
      assertThat(tableData.getRows().get(0).get("value")).isEqualTo("0");
      assertThat(tableData.getRows().get(1).get("value")).isEqualTo("0");
   }



   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName(name).withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder,
                                        boolean isClosedWon, boolean isClosedLost) throws QException
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



   private void insertDeal(String name, Integer pipelineId, Integer stageId,
                            LocalDate closeDate) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(new BigDecimal("10000"))
               .withActualCloseDate(closeDate)
               .withOwnerUserId("user-001")));
   }

}
