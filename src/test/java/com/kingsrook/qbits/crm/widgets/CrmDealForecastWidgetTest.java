/*******************************************************************************
 ** Unit tests for CrmDealForecastWidget.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the CRM Deal Forecast widget renderer.
 *******************************************************************************/
class CrmDealForecastWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Create deals with different expectedCloseDates, render, verify grouping.
    *******************************************************************************/
   @Test
   void testRenderDealForecast() throws QException
   {
      ///////////////////////////////////////////
      // set up pipeline with open and closed  //
      ///////////////////////////////////////////
      Integer pipelineId   = insertPipeline("Sales Pipeline");
      Integer openStageId  = insertStage(pipelineId, "Qualification", 1, 10, false, false);
      Integer wonStageId   = insertStage(pipelineId, "Closed Won", 2, 100, true, false);

      ///////////////////////////////////////////
      // insert open deals in different months //
      ///////////////////////////////////////////
      insertDealWithExpectedClose("Deal Jan", pipelineId, openStageId,
         new BigDecimal("10000"), new BigDecimal("1000"), LocalDate.of(2026, 1, 15));
      insertDealWithExpectedClose("Deal Jan 2", pipelineId, openStageId,
         new BigDecimal("5000"), new BigDecimal("500"), LocalDate.of(2026, 1, 20));
      insertDealWithExpectedClose("Deal Mar", pipelineId, openStageId,
         new BigDecimal("20000"), new BigDecimal("10000"), LocalDate.of(2026, 3, 1));

      ////////////////////////////////////////////////////////////
      // insert a closed deal -- should NOT appear in forecast  //
      ////////////////////////////////////////////////////////////
      insertDealWithExpectedClose("Closed Deal", pipelineId, wonStageId,
         new BigDecimal("15000"), new BigDecimal("15000"), LocalDate.of(2026, 2, 1));

      ///////////////////////////////////////////
      // render the widget                     //
      ///////////////////////////////////////////
      CrmDealForecastRenderer renderer = new CrmDealForecastRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmDealForecastWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();

      //////////////////////////////////////////////////////////////
      // should have 2 months (Jan and Mar), not Feb (closed)     //
      //////////////////////////////////////////////////////////////
      assertEquals(2, rows.size());

      ///////////////////////////////////////////
      // verify Jan grouping                   //
      ///////////////////////////////////////////
      Map<String, Object> janRow = rows.get(0);
      assertEquals("2026-01", janRow.get("month"));
      assertEquals(2, janRow.get("dealCount"));

      ///////////////////////////////////////////
      // verify Mar grouping                   //
      ///////////////////////////////////////////
      Map<String, Object> marRow = rows.get(1);
      assertEquals("2026-03", marRow.get("month"));
      assertEquals(1, marRow.get("dealCount"));
   }



   /*******************************************************************************
    ** Render forecast with no open deals.
    *******************************************************************************/
   @Test
   void testRenderEmptyForecast() throws QException
   {
      CrmDealForecastRenderer renderer = new CrmDealForecastRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmDealForecastWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      assertEquals(0, tableData.getRows().size());
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
    ** Helper: insert a Deal with expectedCloseDate.
    ***************************************************************************/
   private void insertDealWithExpectedClose(String name, Integer pipelineId, Integer stageId,
                                            BigDecimal amount, BigDecimal weightedAmount,
                                            LocalDate expectedCloseDate) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withWeightedAmount(weightedAmount)
               .withExpectedCloseDate(expectedCloseDate)
               .withOwnerUserId("user-001")
               .withStageEnteredDate(Instant.now())));
   }

}
