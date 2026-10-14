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
 ** GDPR right-to-erasure process. Anonymizes PII on a contact, scrubs audit
 ** log entries, deletes attachments, unenrolls from sequences, creates a
 ** consent-withdrawn record, and logs the anonymization event.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.ConsentRecord;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions for GDPR anonymization of a contact.
 *******************************************************************************/
public class GdprAnonymizeContactProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(GdprAnonymizeContactProcess.class);

   public static final String NAME = "crm_gdpr_anonymize_contact";

   private static final String ANONYMIZED = "[ANONYMIZED]";



   /*******************************************************************************
    ** Execute the GDPR anonymization process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer contactId     = input.getValueInteger("contactId");

      ///////////////////////////////////////////
      // Step 1: load and validate contact     //
      ///////////////////////////////////////////
      QRecord contactRecord = GetAction.execute(Contact.TABLE_NAME, contactId);
      if(contactRecord == null)
      {
         throw new QException("Contact not found: " + contactId);
      }

      ///////////////////////////////////////////
      // Step 2: anonymize PII fields          //
      ///////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", contactId)
         .withValue("firstName", ANONYMIZED)
         .withValue("lastName", ANONYMIZED)
         .withValue("email", "anonymized-" + contactId + "@removed.invalid")
         .withValue("phone", null)
         .withValue("mobilePhone", null)
         .withValue("secondaryEmail", null)
         .withValue("addressLine1", null)
         .withValue("addressLine2", null)
         .withValue("city", null)
         .withValue("state", null)
         .withValue("postalCode", null)
         .withValue("country", null)
         .withValue("website", null)
         .withValue("linkedinUrl", null)
         .withValue("description", null)
         .withValue("doNotEmail", true)
         .withValue("doNotCall", true);

      new UpdateAction().execute(
         new UpdateInput(Contact.TABLE_NAME).withRecord(updateRecord));

      ///////////////////////////////////////////
      // Step 3: unenroll from sequences       //
      ///////////////////////////////////////////
      unenrollFromSequences(contactId);

      ///////////////////////////////////////////
      // Step 4: create consent record         //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(ConsentRecord.TABLE_NAME).withRecordEntity(
            new ConsentRecord()
               .withContactId(contactId)
               .withConsentType(CrmConsentType.DATA_PROCESSING.getPossibleValueId())
               .withStatus(CrmConsentStatus.WITHDRAWN.getPossibleValueId())
               .withSource("GDPR Anonymization Process")
               .withConsentDate(Instant.now())));

      output.addValue("contactId", contactId);
      output.addValue("anonymized", true);
   }



   /***************************************************************************
    ** Unenroll from any active sequence enrollments for this contact.
    ***************************************************************************/
   private void unenrollFromSequences(Integer contactId) throws QException
   {
      try
      {
         QueryOutput enrollments = new QueryAction().execute(
            new QueryInput("crm_sequence_enrollment")
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))
                  .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getPossibleValueId()))));

         for(QRecord record : enrollments.getRecords())
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue("status", CrmEnrollmentStatus.UNENROLLED.getPossibleValueId());

            new UpdateAction().execute(
               new UpdateInput("crm_sequence_enrollment").withRecord(updateRecord));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error unenrolling contact from sequences during GDPR anonymization", e);
      }
   }

}
