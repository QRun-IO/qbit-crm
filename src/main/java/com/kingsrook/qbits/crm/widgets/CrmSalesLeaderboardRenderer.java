/*******************************************************************************
 ** Renderer for the CRM Sales Leaderboard widget.
 **
 ** Queries deals in closed-won stages, groups by ownerUserId, sums
 ** amountInBaseCurrency, and returns a ranked table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 ** Renders the sales leaderboard as a table widget, ranking reps by closed-won
 ** deal revenue.
 *******************************************************************************/
public class CrmSalesLeaderboardRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // find closed-won stage ids                             //
      ///////////////////////////////////////////////////////////
      QueryOutput stageQuery = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("isClosedWon", QCriteriaOperator.EQUALS, true))));

      List<Integer> closedWonStageIds = stageQuery.getRecords().stream()
         .map(r -> r.getValueInteger("id"))
         .toList();

      ///////////////////////////////////////////////////////////
      // query deals in closed-won stages                      //
      ///////////////////////////////////////////////////////////
      Map<String, BigDecimal> revenueByOwner = new LinkedHashMap<>();

      if(!closedWonStageIds.isEmpty())
      {
         QueryOutput dealQuery = new QueryAction().execute(
            new QueryInput(Deal.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("pipelineStageId", QCriteriaOperator.IN, closedWonStageIds))));

         for(QRecord dealRecord : dealQuery.getRecords())
         {
            String ownerUserId = dealRecord.getValueString("ownerUserId");
            BigDecimal amount = dealRecord.getValueBigDecimal("amountInBaseCurrency");
            if(ownerUserId != null && amount != null)
            {
               revenueByOwner.merge(ownerUserId, amount, BigDecimal::add);
            }
         }
      }

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Rank", "rank", "0.5fr", null),
         new TableData.Column("html", "Owner", "owner", "2fr", null),
         new TableData.Column("html", "Revenue", "revenue", "1fr", "right")
      );

      //////////////////////////
      // build ranked rows    //
      //////////////////////////
      List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(revenueByOwner.entrySet());
      sorted.sort(Comparator.comparing(Map.Entry<String, BigDecimal>::getValue).reversed());

      List<Map<String, Object>> rows = new ArrayList<>();
      int rank = 1;
      for(Map.Entry<String, BigDecimal> entry : sorted)
      {
         rows.add(MapBuilder.of(
            "rank", String.valueOf(rank),
            "owner", entry.getKey(),
            "revenue", entry.getValue().toPlainString()
         ));
         rank++;
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(25)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
