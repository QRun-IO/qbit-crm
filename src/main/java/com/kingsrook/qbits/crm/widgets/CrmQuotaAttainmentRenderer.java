/*******************************************************************************
 ** Renderer for the CRM Quota Attainment widget.
 **
 ** Queries the SalesGoal for the current user and current period, then queries
 ** won deals in that period to compute attainment percentage.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.scoring.model.SalesGoal;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Renders quota attainment as multi-statistics showing target, actual, and
 ** attainment percentage.
 *******************************************************************************/
public class CrmQuotaAttainmentRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      String currentUserId = QContext.getQSession() != null
         ? QContext.getQSession().getIdReference() : null;

      LocalDate today = LocalDate.now();

      ///////////////////////////////////////////////
      // query SalesGoal for current user/period   //
      ///////////////////////////////////////////////
      QueryOutput goalQuery = new QueryAction().execute(
         new QueryInput(SalesGoal.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, currentUserId))
               .withCriteria(new QFilterCriteria("periodStart", QCriteriaOperator.LESS_THAN_OR_EQUALS, today))
               .withCriteria(new QFilterCriteria("periodEnd", QCriteriaOperator.GREATER_THAN_OR_EQUALS, today))));

      List<MultiStatisticsData.StatisticsGroupData> groups = new ArrayList<>();

      if(goalQuery.getRecords().isEmpty())
      {
         ///////////////////////////////////////////////
         // no goal found -- show empty state         //
         ///////////////////////////////////////////////
         groups.add(new MultiStatisticsData.StatisticsGroupData()
            .withHeader("No Goal Set")
            .withSubheader("current period")
            .withStatisticList(List.of(
               new MultiStatisticsData.StatisticsGroupData.Statistic("Attainment", 0, "%")
            )));
      }
      else
      {
         SalesGoal goal = new SalesGoal(goalQuery.getRecords().get(0));

         ///////////////////////////////////////////////
         // find closed-won stage ids                 //
         ///////////////////////////////////////////////
         QueryOutput stageQuery = new QueryAction().execute(
            new QueryInput(PipelineStage.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("isClosedWon", QCriteriaOperator.EQUALS, true))));

         List<Integer> closedWonStageIds = stageQuery.getRecords().stream()
            .map(r -> r.getValueInteger("id"))
            .toList();

         ///////////////////////////////////////////////
         // query won deals in the period             //
         ///////////////////////////////////////////////
         BigDecimal totalWon = BigDecimal.ZERO;

         if(!closedWonStageIds.isEmpty())
         {
            QQueryFilter dealFilter = new QQueryFilter()
               .withCriteria(new QFilterCriteria("ownerUserId", QCriteriaOperator.EQUALS, currentUserId))
               .withCriteria(new QFilterCriteria("pipelineStageId", QCriteriaOperator.IN, closedWonStageIds))
               .withCriteria(new QFilterCriteria("actualCloseDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, goal.getPeriodStart()))
               .withCriteria(new QFilterCriteria("actualCloseDate", QCriteriaOperator.LESS_THAN_OR_EQUALS, goal.getPeriodEnd()));

            if(goal.getPipelineId() != null)
            {
               dealFilter.withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, goal.getPipelineId()));
            }

            QueryOutput dealQuery = new QueryAction().execute(
               new QueryInput(Deal.TABLE_NAME).withFilter(dealFilter));

            for(QRecord dealRecord : dealQuery.getRecords())
            {
               BigDecimal amount = dealRecord.getValueBigDecimal("amountInBaseCurrency");
               if(amount != null)
               {
                  totalWon = totalWon.add(amount);
               }
            }
         }

         ///////////////////////////////////////////////
         // compute attainment percentage             //
         ///////////////////////////////////////////////
         int attainmentPct = 0;
         if(goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0)
         {
            attainmentPct = totalWon
               .multiply(new BigDecimal(100))
               .divide(goal.getTargetAmount(), 0, RoundingMode.HALF_UP)
               .intValue();
         }

         groups.add(new MultiStatisticsData.StatisticsGroupData()
            .withHeader("Quota Attainment")
            .withSubheader(goal.getPeriodStart() + " to " + goal.getPeriodEnd())
            .withStatisticList(List.of(
               new MultiStatisticsData.StatisticsGroupData.Statistic("Target", goal.getTargetAmount().intValue(), null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Actual", totalWon.intValue(), null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Attainment", attainmentPct, "%")
            )));
      }

      MultiStatisticsData data = new MultiStatisticsData("Quota Attainment", groups);

      return (new RenderWidgetOutput(data));
   }

}
