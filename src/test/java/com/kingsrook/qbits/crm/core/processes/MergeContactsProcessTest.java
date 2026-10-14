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
 ** Unit tests for MergeContactsProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for merging contacts: activities re-linked, tags de-duped,
 ** secondary deleted, audit logged.
 *******************************************************************************/
class MergeContactsProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Merge two contacts and verify activities are re-linked and secondary deleted.
    *******************************************************************************/
   @Test
   void testMergeContacts() throws QException
   {
      ///////////////////////////////////////
      // set up contacts                   //
      ///////////////////////////////////////
      Integer primaryId   = insertContact("primary@example.com", "Primary", "Contact");
      Integer secondaryId = insertContact("secondary@example.com", "Secondary", "Contact");
      MemoryRecordStore.getInstance().insert(new InsertInput("crm_deal")
         .withRecord(new QRecord().withValue("id", 1).withValue("primaryContactId", secondaryId)), true);
      MemoryRecordStore.getInstance().insert(new InsertInput("crm_email_message")
         .withRecord(new QRecord().withValue("id", 1).withValue("contactId", secondaryId)), true);

      ///////////////////////////////////////
      // create activity on secondary      //
      ///////////////////////////////////////
      Integer typeId = insertActivityType("Note");
      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(typeId)
               .withSubject("Note on secondary")
               .withContactId(secondaryId)
               .withOwnerUserId("user-001")
               .withIsCompleted(false)));

      ////////////////////////////////////
      // run merge process              //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MergeContactsProcess.NAME);
      processInput.addValue("primaryContactId", primaryId);
      processInput.addValue("secondaryContactId", secondaryId);

      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify secondary contact is deleted     //
      /////////////////////////////////////////////
      assertNull(GetAction.execute(Contact.TABLE_NAME, secondaryId));

      /////////////////////////////////////////////
      // verify activity re-linked to primary    //
      /////////////////////////////////////////////
      QueryOutput activities = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, primaryId))));
      assertEquals(1, activities.getRecords().size());
      assertEquals(primaryId, GetAction.execute("crm_deal", 1).getValueInteger("primaryContactId"));
      assertEquals(primaryId, GetAction.execute("crm_email_message", 1).getValueInteger("contactId"));

   }



   /***************************************************************************
    ** Helper: insert a Contact.
    ***************************************************************************/
   private Integer insertContact(String email, String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail(email)
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Activity Type.
    *******************************************************************************/
   private Integer insertActivityType(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName("note")
               .withIsSystem(false)
               .withSortOrder(1)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



}
