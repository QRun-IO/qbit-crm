/*******************************************************************************
 ** Unit tests for LogActivityProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests that LogActivityProcess creates an activity record with the correct
 ** fields and sets ownerUserId from the session.
 *******************************************************************************/
class LogActivityProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Insert an activity via the process and verify the record was created.
    *******************************************************************************/
   @Test
   void testLogActivity() throws QException
   {
      ///////////////////////////////////////
      // set up prerequisite: activity type //
      ///////////////////////////////////////
      Integer activityTypeId = insertActivityType("Call");

      ////////////////////////////////////
      // run the LogActivity process    //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(LogActivityProcess.NAME);
      processInput.addValue("activityTypeId", activityTypeId);
      processInput.addValue("subject", "Test call with client");
      processInput.addValue("description", "Discussed new contract terms");
      processInput.addValue("contactId", 42);
      processInput.addValue("durationMinutes", 15);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      ////////////////////////////////////////////
      // verify the activity record was created //
      ////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      Activity activity = new Activity(queryOutput.getRecords().get(0));
      assertEquals(activityTypeId, activity.getActivityTypeId());
      assertEquals("Test call with client", activity.getSubject());
      assertEquals("Discussed new contract terms", activity.getDescription());
      assertEquals(42, activity.getContactId());
      assertEquals(15, activity.getDurationMinutes());
      assertNotNull(activity.getOwnerUserId());
   }



   /*******************************************************************************
    ** Test that the process can create a minimal activity with just required fields.
    *******************************************************************************/
   @Test
   void testLogActivityMinimalFields() throws QException
   {
      Integer activityTypeId = insertActivityType("Note");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(LogActivityProcess.NAME);
      processInput.addValue("activityTypeId", activityTypeId);
      processInput.addValue("subject", "Quick note");

      new RunProcessAction().execute(processInput);

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());
      assertEquals("Quick note", queryOutput.getRecords().get(0).getValueString("subject"));
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

}
