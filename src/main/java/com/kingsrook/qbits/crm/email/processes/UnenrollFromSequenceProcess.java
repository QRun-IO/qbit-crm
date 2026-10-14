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
 ** Process to unenroll a contact from an email sequence.
 *******************************************************************************/

package com.kingsrook.qbits.crm.email.processes;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions to unenroll a contact from an email sequence.
 ** Sets status to UNENROLLED and records the unenrolled date.
 *******************************************************************************/
public class UnenrollFromSequenceProcess implements BackendStep
{
   public static final String NAME = "crm_unenroll_from_sequence";



   /*******************************************************************************
    ** Execute the unenrollment logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer enrollmentId = input.getValueInteger("enrollmentId");

      /////////////////////////////////////////////
      // validate enrollment exists              //
      /////////////////////////////////////////////
      GetOutput enrollmentGet = new GetAction().execute(
         new GetInput(SequenceEnrollment.TABLE_NAME).withPrimaryKey(enrollmentId));

      if(enrollmentGet.getRecord() == null)
      {
         throw (new QUserFacingException("Enrollment not found"));
      }

      /////////////////////////////////////////////
      // set status=UNENROLLED, unenrolledDate   //
      /////////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", enrollmentId)
         .withValue("status", CrmEnrollmentStatus.UNENROLLED.getPossibleValueId())
         .withValue("unenrolledDate", Instant.now())
         .withValue("nextStepDate", null);

      new UpdateAction().execute(
         new UpdateInput(SequenceEnrollment.TABLE_NAME).withRecord(updateRecord));

      output.addValue("action", "UNENROLLED");
      output.addValue("enrollmentId", enrollmentId);
   }

}
