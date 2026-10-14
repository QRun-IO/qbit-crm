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
 ** Process to log a CRM activity. Creates a single crm_activity record with
 ** the provided fields and sets ownerUserId from the current session user.
 *******************************************************************************/

package com.kingsrook.qbits.crm.activities.processes;


import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions that creates a crm_activity record from
 ** the provided input fields. Sets ownerUserId from the current session.
 *******************************************************************************/
public class LogActivityProcess implements BackendStep
{
   public static final String NAME = "crm_log_activity";



   /*******************************************************************************
    ** Execute the process: build an Activity entity from inputs and insert it.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String ownerUserId = QContext.getQSession().getIdReference();

      Activity activity = new Activity()
         .withActivityTypeId(input.getValueInteger("activityTypeId"))
         .withSubject(input.getValueString("subject"))
         .withDescription(input.getValueString("description"))
         .withContactId(input.getValueInteger("contactId"))
         .withCompanyId(input.getValueInteger("companyId"))
         .withDealId(input.getValueInteger("dealId"))
         .withDirection(input.getValueInteger("direction"))
         .withDurationMinutes(input.getValueInteger("durationMinutes"))
         .withOutcomeId(input.getValueInteger("outcomeId"))
         .withLocation(input.getValueString("location"))
         .withConferenceLink(input.getValueString("conferenceLink"))
         .withStartDate(input.getValueInstant("startDate"))
         .withEndDate(input.getValueInstant("endDate"))
         .withOwnerUserId(ownerUserId)
         .withIsCompleted(false);

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(activity));

      Integer activityId = insertOutput.getRecords().get(0).getValueInteger("id");
      output.addValue("activityId", activityId);
   }

}
