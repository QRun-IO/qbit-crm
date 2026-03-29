/*******************************************************************************
 ** Unit tests for CrmActivityCountWidget.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the CRM Activity Count widget renderer.
 *******************************************************************************/
class CrmActivityCountWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Insert activities of different types, render widget, verify counts.
    *******************************************************************************/
   @Test
   void testRenderActivityCounts() throws QException
   {
      ///////////////////////////////////////
      // set up reference data             //
      ///////////////////////////////////////
      Integer callTypeId = insertActivityType("Call");
      Integer emailTypeId = insertActivityType("Email");
      Integer meetingTypeId = insertActivityType("Meeting");

      ///////////////////////////////////////
      // insert activities (today)         //
      ///////////////////////////////////////
      insertActivity(callTypeId, "Call 1");
      insertActivity(callTypeId, "Call 2");
      insertActivity(emailTypeId, "Email 1");
      insertActivity(meetingTypeId, "Meeting 1");

      ///////////////////////////////////////
      // render the widget                 //
      ///////////////////////////////////////
      CrmActivityCountRenderer renderer = new CrmActivityCountRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmActivityCountWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(MultiStatisticsData.class);

      MultiStatisticsData data = (MultiStatisticsData) widgetOutput.getWidgetData();
      assertNotNull(data.getStatisticsGroupData());

      ////////////////////////////////////////////////////////
      // we should have groups for each activity type used  //
      ////////////////////////////////////////////////////////
      assertThat(data.getStatisticsGroupData()).hasSizeGreaterThanOrEqualTo(2);
   }



   /*******************************************************************************
    ** Render widget with no activities and verify empty output.
    *******************************************************************************/
   @Test
   void testRenderEmptyCounts() throws QException
   {
      CrmActivityCountRenderer renderer = new CrmActivityCountRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmActivityCountWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      MultiStatisticsData data = (MultiStatisticsData) widgetOutput.getWidgetData();
      assertThat(data.getStatisticsGroupData()).isEmpty();
   }



   /***************************************************************************
    ** Helper: insert an ActivityType and return its id.
    ***************************************************************************/
   private Integer insertActivityType(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName("phone")
               .withIsSystem(false)
               .withSortOrder(1)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an Activity.
    ***************************************************************************/
   private void insertActivity(Integer typeId, String subject) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(typeId)
               .withSubject(subject)
               .withOwnerUserId("user-001")
               .withIsCompleted(false)));
   }

}
