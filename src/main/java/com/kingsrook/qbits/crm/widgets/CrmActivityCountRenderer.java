/*******************************************************************************
 ** Renderer for the CRM Activity Count widget.
 **
 ** Queries activities created today and groups them by activityTypeId to
 ** produce counts. Returns a MultiStatisticsData with one entry per known
 ** type: Call, Email, Meeting.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
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
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Renders activity counts by type for today as a multi-statistics widget.
 *******************************************************************************/
public class CrmActivityCountRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ////////////////////////////////////////////////////////
      // determine start-of-day in the system's time zone   //
      ////////////////////////////////////////////////////////
      ZoneId zoneId = ZoneId.systemDefault();
      ZonedDateTime startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId);
      Instant todayStart = startOfDay.toInstant();

      ////////////////////////////////////////////////////////////////
      // query all activity types for labeling                      //
      ////////////////////////////////////////////////////////////////
      Map<Integer, String> typeNameMap = new HashMap<>();
      QueryOutput typeOutput = new QueryAction().execute(
         new QueryInput(ActivityType.TABLE_NAME));
      for(QRecord typeRecord : typeOutput.getRecords())
      {
         typeNameMap.put(typeRecord.getValueInteger("id"), typeRecord.getValueString("name"));
      }

      ////////////////////////////////////////////////////////////
      // query activities created today                         //
      ////////////////////////////////////////////////////////////
      QueryOutput activityOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, todayStart))));

      ////////////////////////////////////////////////////////////
      // count by activityTypeId                                //
      ////////////////////////////////////////////////////////////
      Map<Integer, Integer> countByType = new HashMap<>();
      for(QRecord record : activityOutput.getRecords())
      {
         Integer typeId = record.getValueInteger("activityTypeId");
         if(typeId != null)
         {
            countByType.merge(typeId, 1, Integer::sum);
         }
      }

      ////////////////////////////////////////////////////////////
      // build statistics group data                            //
      ////////////////////////////////////////////////////////////
      List<MultiStatisticsData.StatisticsGroupData> groups = new ArrayList<>();
      for(Map.Entry<Integer, Integer> entry : countByType.entrySet())
      {
         String typeName = typeNameMap.getOrDefault(entry.getKey(), "Type " + entry.getKey());
         groups.add(new MultiStatisticsData.StatisticsGroupData()
            .withHeader(typeName)
            .withSubheader("today")
            .withStatisticList(List.of(
               new MultiStatisticsData.StatisticsGroupData.Statistic(typeName, entry.getValue(), null)
            )));
      }

      MultiStatisticsData data = new MultiStatisticsData("Activity Counts (Today)", groups);

      return (new RenderWidgetOutput(data));
   }

}
