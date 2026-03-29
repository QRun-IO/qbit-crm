/*******************************************************************************
 ** Renderer for the CRM Activity Feed widget.
 **
 ** Queries the most recent 50 activities (by createDate descending) and returns
 ** a table with columns: subject, activityType, contact, company, date.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
 ** Renders the activity feed as a table widget showing recent activities.
 *******************************************************************************/
public class CrmActivityFeedRenderer extends AbstractWidgetRenderer
{
   private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm")
      .withZone(ZoneId.systemDefault());



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // query recent activities, ordered by createDate desc   //
      ///////////////////////////////////////////////////////////
      QueryInput queryInput = new QueryInput(Activity.TABLE_NAME);
      queryInput.setFilter(new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("createDate", false))
         .withLimit(50));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Subject", "subject", "2fr", null),
         new TableData.Column("html", "Activity Type", "activityType", "1fr", null),
         new TableData.Column("html", "Contact", "contact", "1fr", null),
         new TableData.Column("html", "Company", "company", "1fr", null),
         new TableData.Column("html", "Date", "date", "1fr", "right")
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String subject = record.getValueString("subject");
         if(subject == null)
         {
            subject = "";
         }

         Integer activityTypeId = record.getValueInteger("activityTypeId");
         String activityTypeDisplay = activityTypeId != null ? String.valueOf(activityTypeId) : "-";

         Integer contactId = record.getValueInteger("contactId");
         String contactDisplay = contactId != null ? String.valueOf(contactId) : "-";

         Integer companyId = record.getValueInteger("companyId");
         String companyDisplay = companyId != null ? String.valueOf(companyId) : "-";

         Instant createDate = record.getValueInstant("createDate");
         String dateDisplay = createDate != null ? DATE_FORMATTER.format(createDate) : "-";

         rows.add(MapBuilder.of(
            "subject", subject,
            "activityType", activityTypeDisplay,
            "contact", contactDisplay,
            "company", companyDisplay,
            "date", dateDisplay
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(50)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
