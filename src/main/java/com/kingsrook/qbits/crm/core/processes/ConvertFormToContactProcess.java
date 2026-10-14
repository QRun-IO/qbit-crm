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
 ** Process to convert a form submission into a CRM contact. Handles duplicate
 ** detection by email, creates the contact if needed, updates the form
 ** submission record, and logs a Note activity on the contact.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.time.Instant;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.core.model.LeadSource;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions for converting a form submission into a
 ** contact record. Deduplicates on email, updates the form with conversion
 ** data, and creates a Note activity on the contact.
 *******************************************************************************/
public class ConvertFormToContactProcess implements BackendStep
{
   public static final String NAME = "crm_convert_form_to_contact";



   /*******************************************************************************
    ** Execute the conversion process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer formSubmissionId = input.getValueInteger("formSubmissionId");
      String  sessionUserId    = QContext.getQSession().getIdReference();

      ///////////////////////////////////////
      // Step 1: load the form submission  //
      ///////////////////////////////////////
      QRecord formRecord = GetAction.execute(FormSubmission.TABLE_NAME, formSubmissionId);
      if(formRecord == null)
      {
         throw new QException("Form submission not found: " + formSubmissionId);
      }

      FormSubmission form = new FormSubmission(formRecord);

      //////////////////////////////////////////////////////////////
      // Step 2: check if a contact exists with the same email    //
      //////////////////////////////////////////////////////////////
      Integer contactId = null;
      boolean isNewContact = false;

      if(StringUtils.hasContent(form.getEmail()))
      {
         QueryOutput existingContacts = new QueryAction().execute(
            new QueryInput(Contact.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, form.getEmail()))));

         if(!existingContacts.getRecords().isEmpty())
         {
            /////////////////////////////////////////////
            // Step 4: existing contact found, use it  //
            /////////////////////////////////////////////
            contactId = existingContacts.getRecords().get(0).getValueInteger("id");
         }
      }

      if(contactId == null)
      {
         //////////////////////////////////////////////////////////
         // Step 3: no existing contact, create one               //
         //////////////////////////////////////////////////////////
         Integer leadSourceId = lookupWebsiteLeadSourceId();

         Contact contact = new Contact()
            .withEmail(form.getEmail())
            .withFirstName(form.getFirstName())
            .withLastName(StringUtils.hasContent(form.getLastName()) ? form.getLastName() : "Unknown")
            .withPhone(form.getPhone())
            .withLeadSourceId(leadSourceId)
            .withOwnerUserId(sessionUserId);

         InsertOutput insertOutput = new InsertAction().execute(
            new InsertInput(Contact.TABLE_NAME).withRecordEntity(contact));

         contactId = insertOutput.getRecords().get(0).getValueInteger("id");
         isNewContact = true;
      }

      ///////////////////////////////////////////////////////////
      // Step 5: update the form submission with conversion    //
      ///////////////////////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", formSubmissionId)
         .withValue("contactId", contactId)
         .withValue("conversionStatus", CrmFormConversionStatus.CONVERTED_TO_CONTACT.getPossibleValueId())
         .withValue("convertedDate", Instant.now())
         .withValue("convertedByUserId", sessionUserId);

      new UpdateAction().execute(
         new UpdateInput(FormSubmission.TABLE_NAME).withRecord(updateRecord));

      /////////////////////////////////////////////////////////////
      // Step 6: create a Note activity on the contact           //
      /////////////////////////////////////////////////////////////
      Integer noteTypeId = lookupNoteActivityTypeId();
      if(noteTypeId != null)
      {
         String noteDescription = "Form submission converted to contact.";
         if(StringUtils.hasContent(form.getMessage()))
         {
            noteDescription = form.getMessage();
         }

         Activity noteActivity = new Activity()
            .withActivityTypeId(noteTypeId)
            .withSubject("Form Submission Converted")
            .withDescription(noteDescription)
            .withContactId(contactId)
            .withOwnerUserId(sessionUserId)
            .withIsCompleted(true);

         new InsertAction().execute(
            new InsertInput(Activity.TABLE_NAME).withRecordEntity(noteActivity));
      }

      output.addValue("contactId", contactId);
      output.addValue("isNewContact", isNewContact);
   }



   /***************************************************************************
    ** Look up the "Website" lead source id. Returns null if not found.
    ***************************************************************************/
   private Integer lookupWebsiteLeadSourceId() throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(LeadSource.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Website"))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }



   /***************************************************************************
    ** Look up the "Note" activity type id. Returns null if not found.
    ***************************************************************************/
   private Integer lookupNoteActivityTypeId() throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityType.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Note"))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }

}
