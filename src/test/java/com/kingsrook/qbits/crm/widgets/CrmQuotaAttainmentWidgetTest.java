/*******************************************************************************
 ** Unit tests for CrmQuotaAttainmentRenderer.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.LocalDate;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmGoalPeriod;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.scoring.model.SalesGoal;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for the Quota Attainment widget: no goal, with goal, with goal and deals.
 *******************************************************************************/
class CrmQuotaAttainmentWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Test render with no goal shows empty state.
    *******************************************************************************/
   @Test
   void testRenderNoGoal() throws QException
   {
      CrmQuotaAttainmentRenderer renderer = new CrmQuotaAttainmentRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmQuotaAttainmentWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      assertThat(output.getWidgetData()).isInstanceOf(MultiStatisticsData.class);
      MultiStatisticsData data = (MultiStatisticsData) output.getWidgetData();
      assertThat(data.getStatisticsGroupData()).isNotEmpty();
      assertThat(data.getStatisticsGroupData().get(0).getHeader()).isEqualTo("No Goal Set");
   }



   /*******************************************************************************
    ** Test render with goal but no deals.
    *******************************************************************************/
   @Test
   void testRenderWithGoalNoDeal() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      insertPipelineStage(pipelineId, "Won", 3, true, false);

      insertSalesGoal(TEST_USER_ID, pipelineId, new BigDecimal("100000"));

      CrmQuotaAttainmentRenderer renderer = new CrmQuotaAttainmentRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmQuotaAttainmentWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      MultiStatisticsData data = (MultiStatisticsData) output.getWidgetData();
      assertThat(data.getStatisticsGroupData()).isNotEmpty();
      assertThat(data.getStatisticsGroupData().get(0).getHeader()).isEqualTo("Quota Attainment");
   }



   /*******************************************************************************
    ** Test render with goal and deals.
    *******************************************************************************/
   @Test
   void testRenderWithGoalAndDeals() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer wonStageId = insertPipelineStage(pipelineId, "Won", 3, true, false);

      insertSalesGoal(TEST_USER_ID, pipelineId, new BigDecimal("100000"));

      insertDeal("Deal A", pipelineId, wonStageId, new BigDecimal("25000"));
      insertDeal("Deal B", pipelineId, wonStageId, new BigDecimal("25000"));

      CrmQuotaAttainmentRenderer renderer = new CrmQuotaAttainmentRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmQuotaAttainmentWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      MultiStatisticsData data = (MultiStatisticsData) output.getWidgetData();
      assertThat(data.getStatisticsGroupData()).isNotEmpty();
      assertThat(data.getStatisticsGroupData().get(0).getHeader()).isEqualTo("Quota Attainment");
      // 50000 / 100000 = 50%
      assertThat(data.getStatisticsGroupData().get(0).getStatisticList()).hasSize(3);
   }



   /*******************************************************************************
    ** Test render with goal without pipeline filter.
    *******************************************************************************/
   @Test
   void testRenderWithGoalNoPipelineFilter() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer wonStageId = insertPipelineStage(pipelineId, "Won", 3, true, false);

      insertSalesGoalNoPipeline(TEST_USER_ID, new BigDecimal("50000"));

      insertDeal("Deal C", pipelineId, wonStageId, new BigDecimal("10000"));

      CrmQuotaAttainmentRenderer renderer = new CrmQuotaAttainmentRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmQuotaAttainmentWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      MultiStatisticsData data = (MultiStatisticsData) output.getWidgetData();
      assertThat(data.getStatisticsGroupData()).isNotEmpty();
      assertThat(data.getStatisticsGroupData().get(0).getHeader()).isEqualTo("Quota Attainment");
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



   private void insertSalesGoal(String userId, Integer pipelineId, BigDecimal target) throws QException
   {
      new InsertAction().execute(
         new InsertInput(SalesGoal.TABLE_NAME).withRecordEntity(
            new SalesGoal()
               .withUserId(userId)
               .withPipelineId(pipelineId)
               .withPeriodType(CrmGoalPeriod.MONTHLY.getId())
               .withPeriodStart(LocalDate.now().withDayOfMonth(1))
               .withPeriodEnd(LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1))
               .withTargetAmount(target)
               .withCurrencyCode("USD")));
   }



   private void insertSalesGoalNoPipeline(String userId, BigDecimal target) throws QException
   {
      new InsertAction().execute(
         new InsertInput(SalesGoal.TABLE_NAME).withRecordEntity(
            new SalesGoal()
               .withUserId(userId)
               .withPeriodType(CrmGoalPeriod.MONTHLY.getId())
               .withPeriodStart(LocalDate.now().withDayOfMonth(1))
               .withPeriodEnd(LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1))
               .withTargetAmount(target)
               .withCurrencyCode("USD")));
   }



   private void insertDeal(String name, Integer pipelineId, Integer stageId,
                            BigDecimal amountInBase) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amountInBase)
               .withAmountInBaseCurrency(amountInBase)
               .withActualCloseDate(LocalDate.now())
               .withOwnerUserId(TEST_USER_ID)));
   }

}
