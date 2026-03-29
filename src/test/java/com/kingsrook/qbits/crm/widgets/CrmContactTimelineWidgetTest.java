/*******************************************************************************
 ** Unit tests for CrmContactTimelineWidget.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.util.Map;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StepperData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the CRM Contact Timeline widget renderer.
 *******************************************************************************/
class CrmContactTimelineWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Insert activities for a contact, render widget, verify timeline steps.
    *******************************************************************************/
   @Test
   void testRenderContactTimeline() throws QException
   {
      ///////////////////////////////////////
      // set up reference data             //
      ///////////////////////////////////////
      Integer activityTypeId = insertActivityType("Call");
      Integer contactId = insertContact("Alice", "A");

      ///////////////////////////////////////
      // insert activities for the contact //
      ///////////////////////////////////////
      insertActivity(activityTypeId, "First call", contactId, "Initial outreach");
      insertActivity(activityTypeId, "Second call", contactId, "Follow-up discussion");
      insertActivity(activityTypeId, "Third call", contactId, "Closing discussion");

      ///////////////////////////////////////
      // render the widget                 //
      ///////////////////////////////////////
      CrmContactTimelineRenderer renderer = new CrmContactTimelineRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmContactTimelineWidgetProducer().produce(null));
      widgetInput.setQueryParams(Map.of("contactId", String.valueOf(contactId)));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(StepperData.class);

      StepperData stepperData = (StepperData) widgetOutput.getWidgetData();
      assertEquals(3, stepperData.getSteps().size());
      assertEquals("First call", stepperData.getSteps().get(0).getLabel());
      assertEquals("Second call", stepperData.getSteps().get(1).getLabel());
      assertEquals("Third call", stepperData.getSteps().get(2).getLabel());

      ///////////////////////////////////////////////////////////
      // activeStep should be the last (most recent) activity  //
      ///////////////////////////////////////////////////////////
      assertEquals(2, stepperData.getActiveStep());
   }



   /*******************************************************************************
    ** Render widget with no contactId and verify empty timeline.
    *******************************************************************************/
   @Test
   void testRenderEmptyTimeline() throws QException
   {
      CrmContactTimelineRenderer renderer = new CrmContactTimelineRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmContactTimelineWidgetProducer().produce(null));
      widgetInput.setQueryParams(Map.of());

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      StepperData stepperData = (StepperData) widgetOutput.getWidgetData();
      assertEquals(0, stepperData.getSteps().size());
   }



   /*******************************************************************************
    ** Verify that activities for a different contact are not included.
    *******************************************************************************/
   @Test
   void testTimelineOnlyShowsTargetContact() throws QException
   {
      Integer activityTypeId = insertActivityType("Note");
      Integer contactId1 = insertContact("Alice", "A");
      Integer contactId2 = insertContact("Bob", "B");

      insertActivity(activityTypeId, "Alice's note", contactId1, "For Alice");
      insertActivity(activityTypeId, "Bob's note", contactId2, "For Bob");
      insertActivity(activityTypeId, "Another Alice note", contactId1, "Also for Alice");

      CrmContactTimelineRenderer renderer = new CrmContactTimelineRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmContactTimelineWidgetProducer().produce(null));
      widgetInput.setQueryParams(Map.of("contactId", String.valueOf(contactId1)));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      StepperData stepperData = (StepperData) widgetOutput.getWidgetData();
      assertEquals(2, stepperData.getSteps().size());
      assertEquals("Alice's note", stepperData.getSteps().get(0).getLabel());
      assertEquals("Another Alice note", stepperData.getSteps().get(1).getLabel());
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
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an Activity linked to a contact.
    ***************************************************************************/
   private void insertActivity(Integer typeId, String subject, Integer contactId, String description) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(typeId)
               .withSubject(subject)
               .withContactId(contactId)
               .withDescription(description)
               .withOwnerUserId("user-001")
               .withIsCompleted(false)));
   }

}
