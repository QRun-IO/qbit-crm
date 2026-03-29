/*******************************************************************************
 ** Renderer for the CRM Deal Forecast widget.
 **
 ** Queries open deals (not in closed stages) and groups them by expected close
 ** month. Returns a table with columns: month, dealCount, totalAmount,
 ** totalWeightedAmount.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
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
 ** Renders the deal forecast as a table widget showing open deal aggregates
 ** grouped by expected close month.
 *******************************************************************************/
public class CrmDealForecastRenderer extends AbstractWidgetRenderer
{
   private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////////
      // find all closed stage IDs to exclude                      //
      ///////////////////////////////////////////////////////////////
      Set<Integer> closedStageIds = findClosedStageIds();

      ///////////////////////////////////////////////////////////////
      // query all deals that are NOT in closed stages             //
      ///////////////////////////////////////////////////////////////
      QQueryFilter filter = new QQueryFilter();
      if(!closedStageIds.isEmpty())
      {
         filter.withCriteria(new QFilterCriteria("pipelineStageId", QCriteriaOperator.NOT_IN, new ArrayList<>(closedStageIds)));
      }

      QueryOutput dealOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME).withFilter(filter));

      //////////////////////////////////////////////////////////
      // aggregate deals by expected close month              //
      //////////////////////////////////////////////////////////
      Map<YearMonth, Integer> countByMonth          = new TreeMap<>();
      Map<YearMonth, BigDecimal> amountByMonth      = new TreeMap<>();
      Map<YearMonth, BigDecimal> weightedByMonth    = new TreeMap<>();

      for(QRecord record : dealOutput.getRecords())
      {
         LocalDate expectedCloseDate = record.getValueLocalDate("expectedCloseDate");
         if(expectedCloseDate == null)
         {
            continue;
         }

         YearMonth yearMonth = YearMonth.from(expectedCloseDate);

         countByMonth.merge(yearMonth, 1, Integer::sum);

         BigDecimal amount = record.getValueBigDecimal("amount");
         if(amount != null)
         {
            amountByMonth.merge(yearMonth, amount, BigDecimal::add);
         }

         BigDecimal weighted = record.getValueBigDecimal("weightedAmount");
         if(weighted != null)
         {
            weightedByMonth.merge(yearMonth, weighted, BigDecimal::add);
         }
      }

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Month", "month", "1fr", null),
         new TableData.Column("html", "Deal Count", "dealCount", "1fr", "right"),
         new TableData.Column("html", "Total Amount", "totalAmount", "1fr", "right"),
         new TableData.Column("html", "Weighted Amount", "totalWeightedAmount", "1fr", "right")
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(YearMonth yearMonth : countByMonth.keySet())
      {
         rows.add(MapBuilder.of(
            "month", yearMonth.format(MONTH_FORMAT),
            "dealCount", countByMonth.getOrDefault(yearMonth, 0),
            "totalAmount", amountByMonth.getOrDefault(yearMonth, BigDecimal.ZERO).toPlainString(),
            "totalWeightedAmount", weightedByMonth.getOrDefault(yearMonth, BigDecimal.ZERO).toPlainString()
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(50)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }



   /***************************************************************************
    ** Find all pipeline stage IDs that are closed (won or lost).
    ***************************************************************************/
   private Set<Integer> findClosedStageIds() throws QException
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("isClosedWon", QCriteriaOperator.EQUALS, true));

      QQueryFilter lostFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("isClosedLost", QCriteriaOperator.EQUALS, true));

      ///////////////////////////////////////////////////////////////////////////
      // query stages that are closed won OR closed lost                       //
      ///////////////////////////////////////////////////////////////////////////
      QueryOutput wonOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME).withFilter(filter));

      QueryOutput lostOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME).withFilter(lostFilter));

      Set<Integer> closedIds = wonOutput.getRecords().stream()
         .map(r -> r.getValueInteger("id"))
         .collect(Collectors.toSet());

      lostOutput.getRecords().stream()
         .map(r -> r.getValueInteger("id"))
         .forEach(closedIds::add);

      return (closedIds);
   }

}
