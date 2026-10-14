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
 ** Process to merge two contacts. Re-links activities, deal contacts,
 ** and email list memberships from the secondary to the primary, then deletes
 ** the secondary contact with core table auditing.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions for merging two contacts into one.
 *******************************************************************************/
public class MergeContactsProcess implements BackendStep
{
   public static final String NAME = "crm_merge_contacts";



   /*******************************************************************************
    ** Execute the merge contacts process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer primaryContactId   = input.getValueInteger("primaryContactId");
      Integer secondaryContactId = input.getValueInteger("secondaryContactId");
      String  sessionUserId      = CrmSessionUtils.getCurrentUserId();

      /////////////////////////////////////////
      // validate both contacts exist        //
      /////////////////////////////////////////
      QRecord primaryRecord = GetAction.execute(Contact.TABLE_NAME, primaryContactId);
      QRecord secondaryRecord = GetAction.execute(Contact.TABLE_NAME, secondaryContactId);

      if(primaryRecord == null)
      {
         throw new QException("Primary contact not found: " + primaryContactId);
      }
      if(secondaryRecord == null)
      {
         throw new QException("Secondary contact not found: " + secondaryContactId);
      }

      ///////////////////////////////////////////////
      // Re-link activities from secondary         //
      ///////////////////////////////////////////////
      relinkRecords(Activity.TABLE_NAME, "contactId", secondaryContactId, primaryContactId);
      relinkRecords("crm_email_message", "contactId", secondaryContactId, primaryContactId);
      relinkRecords("crm_deal", "primaryContactId", secondaryContactId, primaryContactId);

      ///////////////////////////////////////////////
      // Re-link DealContact rows                  //
      ///////////////////////////////////////////////
      relinkDealContacts(primaryContactId, secondaryContactId);


      ///////////////////////////////////////////////
      // Re-link EmailListMember rows              //
      ///////////////////////////////////////////////
      relinkJunctionRecords(EmailListMember.TABLE_NAME, "contactId", "emailListId", primaryContactId, secondaryContactId);

      ///////////////////////////////////////////////
      // Delete the secondary contact              //
      ///////////////////////////////////////////////
      new DeleteAction().execute(
         new DeleteInput(Contact.TABLE_NAME).withPrimaryKeys(List.of(secondaryContactId)));

      output.addValue("primaryContactId", primaryContactId);
   }



   /***************************************************************************
    ** Re-link simple FK records from secondary to primary.
    ***************************************************************************/
   private void relinkRecords(String tableName, String fkField, Integer fromId, Integer toId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fkField, QCriteriaOperator.EQUALS, fromId))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue(fkField, toId);

         new UpdateAction().execute(
            new UpdateInput(tableName).withRecord(updateRecord));
      }
   }



   /***************************************************************************
    ** Re-link DealContact rows: if same deal exists for both, delete secondary;
    ** otherwise update contactId.
    ***************************************************************************/
   private void relinkDealContacts(Integer primaryContactId, Integer secondaryContactId) throws QException
   {
      //////////////////////////////////////////////////////////
      // find deals already linked to primary                 //
      //////////////////////////////////////////////////////////
      QueryOutput primaryDeals = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, primaryContactId))));

      Set<Integer> primaryDealIds = new HashSet<>();
      for(QRecord record : primaryDeals.getRecords())
      {
         primaryDealIds.add(record.getValueInteger("dealId"));
      }

      //////////////////////////////////////////////////////////
      // process secondary deal contacts                      //
      //////////////////////////////////////////////////////////
      QueryOutput secondaryDeals = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, secondaryContactId))));

      for(QRecord record : secondaryDeals.getRecords())
      {
         Integer dealId = record.getValueInteger("dealId");
         if(primaryDealIds.contains(dealId))
         {
            new DeleteAction().execute(
               new DeleteInput(DealContact.TABLE_NAME).withPrimaryKeys(List.of(record.getValueInteger("id"))));
         }
         else
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue("contactId", primaryContactId);
            new UpdateAction().execute(
               new UpdateInput(DealContact.TABLE_NAME).withRecord(updateRecord));
         }
      }
   }



   /***************************************************************************
    ** Re-link junction table records: if same FK2 exists for both, delete
    ** secondary; otherwise update FK1 (contactId).
    ***************************************************************************/
   private void relinkJunctionRecords(String tableName, String fk1Field, String fk2Field, Integer primaryId, Integer secondaryId) throws QException
   {
      //////////////////////////////////////////////////////////
      // find FK2 values already linked to primary            //
      //////////////////////////////////////////////////////////
      QueryOutput primaryRecords = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fk1Field, QCriteriaOperator.EQUALS, primaryId))));

      Set<Integer> primaryFk2Ids = new HashSet<>();
      for(QRecord record : primaryRecords.getRecords())
      {
         primaryFk2Ids.add(record.getValueInteger(fk2Field));
      }

      //////////////////////////////////////////////////////////
      // process secondary records                            //
      //////////////////////////////////////////////////////////
      QueryOutput secondaryRecords = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fk1Field, QCriteriaOperator.EQUALS, secondaryId))));

      for(QRecord record : secondaryRecords.getRecords())
      {
         Integer fk2Value = record.getValueInteger(fk2Field);
         if(primaryFk2Ids.contains(fk2Value))
         {
            new DeleteAction().execute(
               new DeleteInput(tableName).withPrimaryKeys(List.of(record.getValueInteger("id"))));
         }
         else
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue(fk1Field, primaryId);
            new UpdateAction().execute(
               new UpdateInput(tableName).withRecord(updateRecord));
         }
      }
   }

}
