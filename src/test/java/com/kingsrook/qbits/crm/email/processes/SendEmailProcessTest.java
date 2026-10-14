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
 ** Unit tests for SendEmailProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.email.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.email.model.EmailMessage;
import com.kingsrook.qbits.crm.email.model.EmailTemplate;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for sending emails: activity + message creation, doNotEmail rejection,
 ** and template-based sends.
 *******************************************************************************/
class SendEmailProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Send an email and verify activity + email message records created.
    *******************************************************************************/
   @Test
   void testSendEmail() throws QException
   {
      ///////////////////////////////////////
      // set up prerequisites              //
      ///////////////////////////////////////
      insertActivityType("Email");
      Integer contactId = insertContact("jane@example.com", "Jane", "Doe", false);

      ////////////////////////////////////
      // run the SendEmail process      //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(SendEmailProcess.NAME);
      processInput.addValue("contactId", contactId);
      processInput.addValue("subject", "Test Subject");
      processInput.addValue("bodyHtml", "<p>Hello</p>");
      processInput.addValue("bodyText", "Hello");

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      ////////////////////////////////////////////
      // verify the activity record was created //
      ////////////////////////////////////////////
      QueryOutput activityQuery = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));
      assertEquals(1, activityQuery.getRecords().size());

      Activity activity = new Activity(activityQuery.getRecords().get(0));
      assertEquals("Test Subject", activity.getSubject());
      assertEquals(contactId, activity.getContactId());
      assertTrue(activity.getIsCompleted());

      ////////////////////////////////////////////////
      // verify the email message record was created //
      ////////////////////////////////////////////////
      QueryOutput emailQuery = new QueryAction().execute(
         new QueryInput(EmailMessage.TABLE_NAME));
      assertEquals(1, emailQuery.getRecords().size());

      EmailMessage emailMsg = new EmailMessage(emailQuery.getRecords().get(0));
      assertEquals("Test Subject", emailMsg.getSubject());
      assertEquals("<p>Hello</p>", emailMsg.getBodyHtml());
      assertEquals("jane@example.com", emailMsg.getToAddresses());
      assertNotNull(emailMsg.getSentDate());
      assertTrue(emailMsg.getIsTracked());
      assertEquals(0, emailMsg.getOpenCount());

      assertNotNull(processOutput.getValues().get("emailMessageId"));
   }



   /*******************************************************************************
    ** Verify that sending to a doNotEmail contact throws QUserFacingException.
    *******************************************************************************/
   @Test
   void testSendEmailDoNotEmail()
   {
      assertThrows(Exception.class, () ->
      {
         insertActivityType("Email");
         Integer contactId = insertContact("blocked@example.com", "Blocked", "User", true);

         RunProcessInput processInput = new RunProcessInput();
         processInput.setProcessName(SendEmailProcess.NAME);
         processInput.addValue("contactId", contactId);
         processInput.addValue("subject", "Test");
         processInput.addValue("bodyHtml", "<p>Test</p>");

         new RunProcessAction().execute(processInput);
      });
   }



   /*******************************************************************************
    ** Send email with template and verify template fields are used.
    *******************************************************************************/
   @Test
   void testSendEmailWithTemplate() throws QException
   {
      insertActivityType("Email");
      Integer contactId = insertContact("temp@example.com", "Temp", "User", false);
      Integer templateId = insertEmailTemplate("Welcome", "Welcome Subject", "<h1>Welcome</h1>", "Welcome text");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(SendEmailProcess.NAME);
      processInput.addValue("contactId", contactId);
      processInput.addValue("emailTemplateId", templateId);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      QueryOutput emailQuery = new QueryAction().execute(
         new QueryInput(EmailMessage.TABLE_NAME));
      assertEquals(1, emailQuery.getRecords().size());

      EmailMessage emailMsg = new EmailMessage(emailQuery.getRecords().get(0));
      assertEquals("Welcome Subject", emailMsg.getSubject());
      assertEquals("<h1>Welcome</h1>", emailMsg.getBodyHtml());
   }



   /***************************************************************************
    ** Helper: insert an ActivityType and return its id.
    ***************************************************************************/
   private void insertActivityType(String name) throws QException
   {
      new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName("email")
               .withIsSystem(false)
               .withSortOrder(1)
               .withIsActive(true)));
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String email, String firstName, String lastName, boolean doNotEmail) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail(email)
               .withFirstName(firstName)
               .withLastName(lastName)
               .withDoNotEmail(doNotEmail)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an EmailTemplate and return its id.
    ***************************************************************************/
   private Integer insertEmailTemplate(String name, String subject, String bodyHtml, String bodyText) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailTemplate.TABLE_NAME).withRecordEntity(
            new EmailTemplate()
               .withName(name)
               .withSubject(subject)
               .withBodyHtml(bodyHtml)
               .withBodyText(bodyText)
               .withOwnerUserId("user-001")
               .withIsShared(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
