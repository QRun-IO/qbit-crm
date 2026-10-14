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
 ** Unit tests for SubscribeToListProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.EmailList;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
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
 ** Tests for SubscribeToListProcess: new subscription, resubscription after
 ** unsub, and no-op for already subscribed.
 *******************************************************************************/
class SubscribeToListProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Subscribe a new email and verify the member is created.
    *******************************************************************************/
   @Test
   void testNewSubscription() throws QException
   {
      Integer listId = insertEmailList("Newsletter");

      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(SubscribeToListProcess.NAME);
      processInput.addValue("email", "new@example.com");
      processInput.addValue("emailListId", listId);
      processInput.addValue("firstName", "Alice");
      processInput.addValue("subscriptionSource", "website");

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

      assertEquals("SUBSCRIBED", processOutput.getValues().get("action"));

      /////////////////////////////////////////////
      // verify member record was created        //
      /////////////////////////////////////////////
      QueryOutput memberQuery = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME));

      assertEquals(1, memberQuery.getRecords().size());
      EmailListMember member = new EmailListMember(memberQuery.getRecords().get(0));
      assertEquals("new@example.com", member.getEmail());
      assertEquals("Alice", member.getFirstName());
      assertEquals(CrmListMemberStatus.SUBSCRIBED.getId(), member.getStatus());
      assertNotNull(member.getSubscribedDate());
      assertEquals("website", member.getSubscriptionSource());
   }



   /*******************************************************************************
    ** Resubscribe after unsubscription and verify status is updated.
    *******************************************************************************/
   @Test
   void testResubscribeAfterUnsub() throws QException
   {
      Integer listId = insertEmailList("Newsletter");

      ///////////////////////////////////////////
      // insert an unsubscribed member record  //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(EmailListMember.TABLE_NAME).withRecordEntity(
            new EmailListMember()
               .withEmailListId(listId)
               .withEmail("unsub@example.com")
               .withStatus(CrmListMemberStatus.UNSUBSCRIBED.getId())
               .withSubscribedDate(java.time.Instant.now().minusSeconds(86400))));

      ///////////////////////////////////////////
      // run subscribe process                 //
      ///////////////////////////////////////////
      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(SubscribeToListProcess.NAME);
      processInput.addValue("email", "unsub@example.com");
      processInput.addValue("emailListId", listId);

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

      assertEquals("RESUBSCRIBED", processOutput.getValues().get("action"));

      ///////////////////////////////////////////
      // verify status was updated             //
      ///////////////////////////////////////////
      QueryOutput memberQuery = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME));

      assertEquals(1, memberQuery.getRecords().size());
      EmailListMember member = new EmailListMember(memberQuery.getRecords().get(0));
      assertEquals(CrmListMemberStatus.SUBSCRIBED.getId(), member.getStatus());
   }



   /*******************************************************************************
    ** No-op when already subscribed.
    *******************************************************************************/
   @Test
   void testNoopWhenAlreadySubscribed() throws QException
   {
      Integer listId = insertEmailList("Newsletter");

      ///////////////////////////////////////////
      // insert an already subscribed member   //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(EmailListMember.TABLE_NAME).withRecordEntity(
            new EmailListMember()
               .withEmailListId(listId)
               .withEmail("active@example.com")
               .withStatus(CrmListMemberStatus.SUBSCRIBED.getId())
               .withSubscribedDate(java.time.Instant.now())));

      ///////////////////////////////////////////
      // run subscribe process                 //
      ///////////////////////////////////////////
      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(SubscribeToListProcess.NAME);
      processInput.addValue("email", "active@example.com");
      processInput.addValue("emailListId", listId);

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

      assertEquals("NOOP", processOutput.getValues().get("action"));

      ///////////////////////////////////////////
      // verify only 1 member record exists    //
      ///////////////////////////////////////////
      QueryOutput memberQuery = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME));
      assertEquals(1, memberQuery.getRecords().size());
   }



   /*******************************************************************************
    ** Test that contactId is linked when a matching contact exists.
    *******************************************************************************/
   @Test
   void testLinksContactByEmail() throws QException
   {
      Integer listId = insertEmailList("Newsletter");

      //////////////////////////////////////////
      // create a contact with matching email //
      //////////////////////////////////////////
      InsertOutput contactInsert = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail("linked@example.com")
               .withFirstName("Linked")
               .withLastName("User")
               .withOwnerUserId("user-001")));
      Integer contactId = contactInsert.getRecords().get(0).getValueInteger("id");

      ///////////////////////////////////////////
      // subscribe with the same email         //
      ///////////////////////////////////////////
      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(SubscribeToListProcess.NAME);
      processInput.addValue("email", "linked@example.com");
      processInput.addValue("emailListId", listId);

      runBusinessStep(processInput);

      ///////////////////////////////////////////
      // verify contactId is set               //
      ///////////////////////////////////////////
      QueryOutput memberQuery = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME));

      assertEquals(1, memberQuery.getRecords().size());
      assertEquals(contactId, memberQuery.getRecords().get(0).getValueInteger("contactId"));
   }



   /***************************************************************************
    ** Helper: insert an EmailList and return its id.
    ***************************************************************************/
   private Integer insertEmailList(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailList.TABLE_NAME).withRecordEntity(
            new EmailList()
               .withName(name)
               .withListKey(name.toLowerCase()).withSlug(name.toLowerCase())
               .withIsActive(true)
               .withIsPublic(true)
               .withSortOrder(1)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
