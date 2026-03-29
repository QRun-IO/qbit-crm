/*******************************************************************************
 ** Unit tests for ProcessOverdueTasksProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.processes;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for processing overdue tasks.
 *******************************************************************************/
class ProcessOverdueTasksProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Create an overdue task, run process, verify it was marked.
    *******************************************************************************/
   @Test
   void testProcessOverdueTasks() throws QException
   {
      ///////////////////////////////////////
      // set up prerequisites              //
      ///////////////////////////////////////
      Integer taskTypeId = insertActivityType("Task");

      ///////////////////////////////////////
      // create an overdue incomplete task //
      ///////////////////////////////////////
      InsertOutput activityInsert = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(taskTypeId)
               .withSubject("Overdue task")
               .withDueDate(Instant.now().minus(2, ChronoUnit.DAYS))
               .withIsCompleted(false)
               .withOwnerUserId("user-001")));
      Integer activityId = activityInsert.getRecords().get(0).getValueInteger("id");

      ////////////////////////////////////
      // run the process                //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ProcessOverdueTasksProcess.NAME);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("processedCount"));

      /////////////////////////////////////////////
      // verify the task description was updated //
      /////////////////////////////////////////////
      Activity updated = new Activity(GetAction.execute(Activity.TABLE_NAME, activityId));
      assertNotNull(updated.getDescription());
      assertTrue(updated.getDescription().contains("[OVERDUE]"));
   }



   /*******************************************************************************
    ** Non-overdue tasks should not be processed.
    *******************************************************************************/
   @Test
   void testNoOverdueTasks() throws QException
   {
      Integer taskTypeId = insertActivityType("Task");

      ///////////////////////////////////////
      // create a future task              //
      ///////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(taskTypeId)
               .withSubject("Future task")
               .withDueDate(Instant.now().plus(5, ChronoUnit.DAYS))
               .withIsCompleted(false)
               .withOwnerUserId("user-001")));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ProcessOverdueTasksProcess.NAME);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(0, processOutput.getValues().get("processedCount"));
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
               .withIconName("task")
               .withIsSystem(false)
               .withSortOrder(1)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
