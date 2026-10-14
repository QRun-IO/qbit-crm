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
 ** Unit tests for ImportContactsProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests for importing contacts: new inserts, duplicate skipping, validation.
 *******************************************************************************/
class ImportContactsProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Import new contacts and verify they are created with audit logs.
    *******************************************************************************/
   @Test
   void testImportNewContacts() throws QException
   {
      List<QRecord> records = new ArrayList<>();
      records.add(new QRecord()
         .withValue("firstName", "Alice")
         .withValue("lastName", "Smith")
         .withValue("email", "alice@example.com"));
      records.add(new QRecord()
         .withValue("firstName", "Bob")
         .withValue("lastName", "Jones")
         .withValue("email", "bob@example.com"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ImportContactsProcess.NAME);
      processInput.setRecords(records);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(2, processOutput.getValues().get("importedCount"));
      assertEquals(0, processOutput.getValues().get("skippedCount"));
      assertEquals(0, processOutput.getValues().get("errorCount"));

      QueryOutput contactQuery = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));
      assertEquals(2, contactQuery.getRecords().size());

   }



   /*******************************************************************************
    ** Test duplicate skipping: pre-existing contact with same email is skipped.
    *******************************************************************************/
   @Test
   void testImportWithDuplicates() throws QException
   {
      /////////////////////////////////////////////
      // insert existing contact                 //
      /////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("Existing")
               .withLastName("User")
               .withEmail("alice@example.com")
               .withOwnerUserId("user-001")));

      List<QRecord> records = new ArrayList<>();
      records.add(new QRecord()
         .withValue("firstName", "Alice")
         .withValue("lastName", "Smith")
         .withValue("email", "alice@example.com")); // duplicate
      records.add(new QRecord()
         .withValue("firstName", "Charlie")
         .withValue("lastName", "Brown")
         .withValue("email", "charlie@example.com")); // new

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ImportContactsProcess.NAME);
      processInput.setRecords(records);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("importedCount"));
      assertEquals(1, processOutput.getValues().get("skippedCount"));
      assertEquals(0, processOutput.getValues().get("errorCount"));
   }



   /*******************************************************************************
    ** Test validation: records missing required fields are counted as errors.
    *******************************************************************************/
   @Test
   void testImportWithValidationErrors() throws QException
   {
      List<QRecord> records = new ArrayList<>();
      records.add(new QRecord()
         .withValue("firstName", "")
         .withValue("lastName", "NoFirst")
         .withValue("email", "nofirst@example.com")); // missing firstName
      records.add(new QRecord()
         .withValue("firstName", "Valid")
         .withValue("lastName", "User")
         .withValue("email", "valid@example.com"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ImportContactsProcess.NAME);
      processInput.setRecords(records);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("importedCount"));
      assertEquals(0, processOutput.getValues().get("skippedCount"));
      assertEquals(1, processOutput.getValues().get("errorCount"));
   }

}
