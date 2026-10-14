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
 ** Simplified import process for contacts. Takes a list of QRecords,
 ** validates required fields, checks for duplicates by email, inserts new
 ** contacts, with core table auditing.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for importing contacts from QRecords.
 *******************************************************************************/
public class ImportContactsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_import_contacts";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Import Contacts")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("upload"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ImportContactsProcess.class))
         ));
   }



   /*******************************************************************************
    ** Execute the import contacts process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      List<QRecord> inputRecords = input.getRecords();
      String sessionUserId = QContext.getQSession().getIdReference();

      ////////////////////////////////////////////////////////////
      // build set of existing emails for duplicate detection   //
      ////////////////////////////////////////////////////////////
      Set<String> existingEmails = new HashSet<>();
      QueryOutput existingContacts = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));
      for(QRecord record : existingContacts.getRecords())
      {
         String email = record.getValueString("email");
         if(StringUtils.hasContent(email))
         {
            existingEmails.add(email.toLowerCase());
         }
      }

      int importedCount = 0;
      int skippedCount  = 0;
      int errorCount    = 0;

      for(QRecord inputRecord : inputRecords)
      {
         String firstName = inputRecord.getValueString("firstName");
         String lastName  = inputRecord.getValueString("lastName");
         String email     = inputRecord.getValueString("email");

         ///////////////////////////////////////////
         // validate required fields              //
         ///////////////////////////////////////////
         if(!StringUtils.hasContent(firstName) || !StringUtils.hasContent(lastName) || !StringUtils.hasContent(email))
         {
            errorCount++;
            continue;
         }

         ///////////////////////////////////////////
         // check for duplicate by email          //
         ///////////////////////////////////////////
         if(existingEmails.contains(email.toLowerCase()))
         {
            skippedCount++;
            continue;
         }

         ///////////////////////////////////////////
         // insert the new contact                //
         ///////////////////////////////////////////
         QRecord newContact = new QRecord()
            .withValue("firstName", firstName)
            .withValue("lastName", lastName)
            .withValue("email", email)
            .withValue("phone", inputRecord.getValueString("phone"))
            .withValue("ownerUserId", sessionUserId);

         new InsertAction().execute(
            new InsertInput(Contact.TABLE_NAME).withRecord(newContact));

         existingEmails.add(email.toLowerCase());

         importedCount++;
      }

      output.addValue("importedCount", importedCount);
      output.addValue("skippedCount", skippedCount);
      output.addValue("errorCount", errorCount);
   }

}
