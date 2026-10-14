/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*******************************************************************************
 ** Unit tests for LogActivityProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.activities.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
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
      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(LogActivityProcess.NAME);
      processInput.addValue("activityTypeId", activityTypeId);
      processInput.addValue("subject", "Test call with client");
      processInput.addValue("description", "Discussed new contract terms");
      processInput.addValue("contactId", 42);
      processInput.addValue("durationMinutes", 15);

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

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

      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(LogActivityProcess.NAME);
      processInput.addValue("activityTypeId", activityTypeId);
      processInput.addValue("subject", "Quick note");

      runBusinessStep(processInput);

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
