/*******************************************************************************
 ** Renderer for the Recent Deals widget.
 **
 ** Queries deals with actualCloseDate set, ordered by actualCloseDate DESC,
 ** limited to 20 rows. Returns table with name, amount, owner, closedDate,
 ** winLossReason.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Renders recent deals as a table widget.
 *******************************************************************************/
public class CrmRecentDealsRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // query recent deals with actualCloseDate set           //
      ///////////////////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("actualCloseDate", QCriteriaOperator.IS_NOT_BLANK))
               .withOrderBy(new QFilterOrderBy("actualCloseDate", false))
               .withLimit(20)));

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Name", "name", "2fr", null),
         new TableData.Column("html", "Amount", "amount", "1fr", "right"),
         new TableData.Column("html", "Owner", "ownerUserId", "1fr", null),
         new TableData.Column("html", "Closed Date", "closedDate", "1fr", "right"),
         new TableData.Column("html", "Win/Loss Reason", "winLossReason", "1fr", null)
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String name = record.getValueString("name");
         if(name == null)
         {
            name = "";
         }

         BigDecimal amount = record.getValueBigDecimal("amount");
         String amountDisplay = amount != null ? "$" + amount.toPlainString() : "-";

         String ownerUserId = record.getValueString("ownerUserId");
         if(ownerUserId == null)
         {
            ownerUserId = "-";
         }

         LocalDate closedDate = record.getValueLocalDate("actualCloseDate");
         String closedDateDisplay = closedDate != null ? closedDate.toString() : "-";

         Integer winLossReasonId = record.getValueInteger("winLossReasonId");
         String winLossDisplay = winLossReasonId != null ? String.valueOf(winLossReasonId) : "-";

         rows.add(MapBuilder.of(
            "name", name,
            "amount", amountDisplay,
            "ownerUserId", ownerUserId,
            "closedDate", closedDateDisplay,
            "winLossReason", winLossDisplay
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(20)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
