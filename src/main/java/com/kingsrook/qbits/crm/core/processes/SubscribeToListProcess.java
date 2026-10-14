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
 ** Process to subscribe an email address to an email list. Handles
 ** resubscription after unsubscribe and links to an existing contact.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions to subscribe an email address to a list.
 ** Handles: new subscription, resubscription after unsub, and no-op for
 ** existing active subscribers.
 *******************************************************************************/
public class SubscribeToListProcess implements BackendStep
{
   public static final String NAME = "crm_subscribe_to_list";



   /*******************************************************************************
    ** Execute the subscription logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String  email              = input.getValueString("email");
      Integer emailListId        = input.getValueInteger("emailListId");
      String  firstName          = input.getValueString("firstName");
      String  subscriptionSource = input.getValueString("subscriptionSource");

      ////////////////////////////////////////////////////
      // check if already subscribed to this list       //
      ////////////////////////////////////////////////////
      QueryOutput existingMembers = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("emailListId", QCriteriaOperator.EQUALS, emailListId))
               .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email))));

      if(!existingMembers.getRecords().isEmpty())
      {
         QRecord existingRecord = existingMembers.getRecords().get(0);
         Integer currentStatus = existingRecord.getValueInteger("status");

         if(CrmListMemberStatus.UNSUBSCRIBED.getPossibleValueId().equals(currentStatus))
         {
            ///////////////////////////////////////////////
            // resubscribe: update status and date       //
            ///////////////////////////////////////////////
            QRecord updateRecord = new QRecord()
               .withValue("id", existingRecord.getValueInteger("id"))
               .withValue("status", CrmListMemberStatus.SUBSCRIBED.getPossibleValueId())
               .withValue("subscribedDate", Instant.now())
               .withValue("unsubscribedDate", null);

            new UpdateAction().execute(
               new UpdateInput(EmailListMember.TABLE_NAME).withRecord(updateRecord));

            output.addValue("action", "RESUBSCRIBED");
            output.addValue("emailListMemberId", existingRecord.getValueInteger("id"));
         }
         else
         {
            /////////////////////////
            // already subscribed  //
            /////////////////////////
            output.addValue("action", "NOOP");
            output.addValue("emailListMemberId", existingRecord.getValueInteger("id"));
         }

         return;
      }

      ///////////////////////////////////////////////
      // look up contact by email for linking      //
      ///////////////////////////////////////////////
      Integer contactId = lookupContactByEmail(email);

      ///////////////////////////////////////////////
      // insert new EmailListMember               //
      ///////////////////////////////////////////////
      EmailListMember member = new EmailListMember()
         .withEmailListId(emailListId)
         .withEmail(email)
         .withFirstName(firstName)
         .withContactId(contactId)
         .withStatus(CrmListMemberStatus.SUBSCRIBED.getPossibleValueId())
         .withSubscribedDate(Instant.now())
         .withSubscriptionSource(subscriptionSource);

      new InsertAction().execute(
         new InsertInput(EmailListMember.TABLE_NAME).withRecordEntity(member));

      output.addValue("action", "SUBSCRIBED");
   }



   /***************************************************************************
    ** Look up a contact id by email address. Returns null if not found.
    ***************************************************************************/
   private Integer lookupContactByEmail(String email) throws QException
   {
      if(!StringUtils.hasContent(email))
      {
         return (null);
      }

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }

}
