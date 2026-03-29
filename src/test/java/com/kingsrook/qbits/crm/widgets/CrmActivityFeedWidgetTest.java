/*******************************************************************************
 ** Unit tests for CrmActivityFeedWidget.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the CRM Activity Feed widget renderer.
 *******************************************************************************/
class CrmActivityFeedWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Insert activities, render widget, verify output contains correct rows.
    *******************************************************************************/
   @Test
   void testRenderActivityFeed() throws QException
   {
      ///////////////////////////////////////
      // set up reference data             //
      ///////////////////////////////////////
      Integer callTypeId = insertActivityType("Call");
      Integer emailTypeId = insertActivityType("Email");

      ///////////////////////////////////////
      // insert activities                 //
      ///////////////////////////////////////
      insertActivity(callTypeId, "Client call", 1, null);
      insertActivity(emailTypeId, "Follow-up email", 2, null);
      insertActivity(callTypeId, "Team standup", null, 100);

      ///////////////////////////////////////
      // render the widget                 //
      ///////////////////////////////////////
      CrmActivityFeedRenderer renderer = new CrmActivityFeedRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmActivityFeedWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();

      assertEquals(3, rows.size());

      /////////////////////////////////////////////////////////////
      // verify columns are present in at least the first row    //
      /////////////////////////////////////////////////////////////
      Map<String, Object> firstRow = rows.get(0);
      assertThat(firstRow).containsKey("subject");
      assertThat(firstRow).containsKey("activityType");
      assertThat(firstRow).containsKey("contact");
      assertThat(firstRow).containsKey("company");
      assertThat(firstRow).containsKey("date");
   }



   /*******************************************************************************
    ** Render widget with no activities and verify empty output.
    *******************************************************************************/
   @Test
   void testRenderEmptyFeed() throws QException
   {
      CrmActivityFeedRenderer renderer = new CrmActivityFeedRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmActivityFeedWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      assertEquals(0, tableData.getRows().size());
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
   private void insertActivity(Integer typeId, String subject, Integer contactId, Integer companyId) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(typeId)
               .withSubject(subject)
               .withContactId(contactId)
               .withCompanyId(companyId)
               .withOwnerUserId("user-001")
               .withIsCompleted(false)));
   }

}
