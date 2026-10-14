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
 ** Unit tests for FormSubmissionAutoLinkCustomizer -- verifies that inserting a
 ** FormSubmission auto-links contactId when a matching contact email exists.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.customizers;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for FormSubmissionAutoLinkCustomizer: verifies that form submissions
 ** are auto-linked to existing contacts by matching email address.
 *******************************************************************************/
class FormSubmissionAutoLinkCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Insert a contact with email, then insert a form submission with the same
    ** email. Verify contactId is auto-linked on the form submission.
    *******************************************************************************/
   @Test
   void testFormWithMatchingContactEmailAutoLinks() throws QException
   {
      Integer contactId = insertContact("Test", "User", "test@example.com");
      Integer formId = insertFormSubmission("test@example.com", null);

      FormSubmission form = new FormSubmission(GetAction.execute(FormSubmission.TABLE_NAME, formId));
      assertThat(form.getContactId()).isEqualTo(contactId);
   }



   /*******************************************************************************
    ** Insert a form submission with an email that does not match any contact.
    ** Verify contactId remains null.
    *******************************************************************************/
   @Test
   void testFormWithNoMatchingEmailDoesNotLink() throws QException
   {
      Integer formId = insertFormSubmission("unknown@example.com", null);

      FormSubmission form = new FormSubmission(GetAction.execute(FormSubmission.TABLE_NAME, formId));
      assertThat(form.getContactId()).isNull();
   }



   /*******************************************************************************
    ** Insert contact A and contact B. Insert form with contactId=A and email
    ** matching B. Verify contactId stays as A (not overwritten to B).
    *******************************************************************************/
   @Test
   void testFormWithExistingContactIdNotOverwritten() throws QException
   {
      Integer contactAId = insertContact("Alice", "Alpha", "alice@example.com");
      Integer contactBId = insertContact("Bob", "Beta", "bob@example.com");

      Integer formId = insertFormSubmission("bob@example.com", contactAId);

      FormSubmission form = new FormSubmission(GetAction.execute(FormSubmission.TABLE_NAME, formId));
      assertThat(form.getContactId()).isEqualTo(contactAId);
   }



   /////////////////////////////////////////////////////////////////////////////
   // Helper methods                                                          //
   /////////////////////////////////////////////////////////////////////////////

   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName, String email) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(email)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a FormSubmission and return its id.
    ***************************************************************************/
   private Integer insertFormSubmission(String email, Integer contactId) throws QException
   {
      FormSubmission form = new FormSubmission()
         .withEmail(email)
         .withFormKey("test-form").withFormType(CrmFormType.CONTACT.getPossibleValueId());

      if(contactId != null)
      {
         form.withContactId(contactId);
      }

      InsertOutput output = new InsertAction().execute(
         new InsertInput(FormSubmission.TABLE_NAME)
            .withRecordEntity(form));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
