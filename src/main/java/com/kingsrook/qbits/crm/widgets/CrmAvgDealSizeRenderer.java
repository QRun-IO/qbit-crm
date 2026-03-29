/*******************************************************************************
 ** Renderer for the Average Deal Size widget.
 **
 ** Queries deals where actualCloseDate is set and stage is closedWon.
 ** Computes average amountInBaseCurrency and returns as a single-stat table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.math.RoundingMode;
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
 ** Renders average deal size as a single-stat table widget.
 *******************************************************************************/
public class CrmAvgDealSizeRenderer extends AbstractWidgetRenderer
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
      // compute average for closedWon deals                   //
      ///////////////////////////////////////////////////////////
      BigDecimal totalAmount = BigDecimal.ZERO;
      int count = 0;

      for(QRecord record : dealOutput.getRecords())
      {
         Integer stageId = record.getValueInteger("pipelineStageId");
         if(closedWonStageIds.contains(stageId))
         {
            BigDecimal amount = record.getValueBigDecimal("amountInBaseCurrency");
            if(amount == null)
            {
               amount = record.getValueBigDecimal("amount");
            }
            if(amount != null)
            {
               totalAmount = totalAmount.add(amount);
               count++;
            }
         }
      }

      BigDecimal avgDealSize = count > 0
         ? totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
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
         "metric", "Average Deal Size",
         "value", "$" + avgDealSize.toPlainString()
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
