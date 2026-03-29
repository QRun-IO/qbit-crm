/*******************************************************************************
 ** Renderer for the Sales Cycle Length widget.
 **
 ** Queries closedWon deals and computes the average number of days from
 ** createDate to actualCloseDate.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Renders sales cycle length as a single-stat table widget.
 *******************************************************************************/
public class CrmSalesCycleLengthRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // find closedWon stage IDs                              //
      ///////////////////////////////////////////////////////////
      Set<Integer> closedWonStageIds = new HashSet<>();
      QueryOutput stageOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("isClosedWon", QCriteriaOperator.EQUALS, true))));

      for(QRecord record : stageOutput.getRecords())
      {
         closedWonStageIds.add(record.getValueInteger("id"));
      }

      ///////////////////////////////////////////////////////////
      // query deals with actualCloseDate set                  //
      ///////////////////////////////////////////////////////////
      QueryOutput dealOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("actualCloseDate", QCriteriaOperator.IS_NOT_BLANK))));

      ///////////////////////////////////////////////////////////
      // compute average days for closedWon deals              //
      ///////////////////////////////////////////////////////////
      long totalDays = 0;
      int count = 0;

      for(QRecord record : dealOutput.getRecords())
      {
         Integer stageId = record.getValueInteger("pipelineStageId");
         if(closedWonStageIds.contains(stageId))
         {
            Instant   createDate     = record.getValueInstant("createDate");
            LocalDate actualCloseDate = record.getValueLocalDate("actualCloseDate");

            if(createDate != null && actualCloseDate != null)
            {
               LocalDate createLocalDate = createDate.atZone(ZoneId.systemDefault()).toLocalDate();
               long days = ChronoUnit.DAYS.between(createLocalDate, actualCloseDate);
               totalDays += days;
               count++;
            }
         }
      }

      BigDecimal avgCycleLength = count > 0
         ? BigDecimal.valueOf(totalDays).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP)
         : BigDecimal.ZERO;

      ///////////////////////////
      // build table output    //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Metric", "metric", "1fr", null),
         new TableData.Column("html", "Value", "value", "1fr", "right")
      );

      List<Map<String, Object>> rows = new ArrayList<>();
      rows.add(MapBuilder.of(
         "metric", "Avg Sales Cycle (days)",
         "value", avgCycleLength.toPlainString()
      ));
      rows.add(MapBuilder.of(
         "metric", "Closed Won Deals",
         "value", String.valueOf(count)
      ));

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(10)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
