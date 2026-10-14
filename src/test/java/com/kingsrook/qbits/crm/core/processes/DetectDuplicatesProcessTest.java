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
 ** Unit tests for DetectDuplicatesProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for detecting duplicate contacts and companies.
 *******************************************************************************/
class DetectDuplicatesProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Insert contacts with duplicate emails and detect them.
    *******************************************************************************/
   @Test
   void testDetectContactDuplicates() throws QException
   {
      ///////////////////////////////////////
      // insert contacts with same email   //
      ///////////////////////////////////////
      insertContact("dupe@example.com", "Alice", "Smith");
      insertContact("dupe@example.com", "Alice2", "Smith2");
      insertContact("unique@example.com", "Bob", "Jones");

      ////////////////////////////////////
      // run detect duplicates          //
      ////////////////////////////////////
      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(DetectDuplicatesProcess.NAME);
      processInput.addValue("entityType", "CONTACT");

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

      assertEquals(1, processOutput.getValues().get("duplicateGroupCount"));
      assertEquals(1, processOutput.getRecords().size());
      assertEquals(2, processOutput.getRecords().get(0).getValueInteger("count"));
   }



   /*******************************************************************************
    ** Insert companies with duplicate domains and detect them.
    *******************************************************************************/
   @Test
   void testDetectCompanyDuplicates() throws QException
   {
      insertCompany("Corp A", "dupe.com");
      insertCompany("Corp B", "dupe.com");
      insertCompany("Corp C", "unique.com");

      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(DetectDuplicatesProcess.NAME);
      processInput.addValue("entityType", "COMPANY");

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

      assertEquals(1, processOutput.getValues().get("duplicateGroupCount"));
   }



   /*******************************************************************************
    ** No duplicates should return empty results.
    *******************************************************************************/
   @Test
   void testNoDuplicates() throws QException
   {
      insertContact("one@example.com", "One", "User");
      insertContact("two@example.com", "Two", "User");

      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(DetectDuplicatesProcess.NAME);
      processInput.addValue("entityType", "CONTACT");

      RunBackendStepOutput processOutput = runBusinessStep(processInput);

      assertEquals(0, processOutput.getValues().get("duplicateGroupCount"));
      assertTrue(processOutput.getRecords().isEmpty());
   }



   /*******************************************************************************
    ** Insert Contact.
    *******************************************************************************/
   private void insertContact(String email, String firstName, String lastName) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail(email)
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
   }



   /*******************************************************************************
    ** Insert Company.
    *******************************************************************************/
   private void insertCompany(String name, String domain) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Company.TABLE_NAME).withRecordEntity(
            new Company()
               .withName(name)
               .withDomain(domain)
               .withOwnerUserId("user-001")));
   }

}
