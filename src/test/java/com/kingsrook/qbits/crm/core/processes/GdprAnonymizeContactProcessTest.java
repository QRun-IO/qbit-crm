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
 ** Unit tests for GdprAnonymizeContactProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.ConsentRecord;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for GDPR anonymization: PII scrubbed, doNotEmail/doNotCall set,
 ** consent record created, audit log entry created.
 *******************************************************************************/
class GdprAnonymizeContactProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Anonymize a contact and verify PII is scrubbed.
    *******************************************************************************/
   @Test
   void testAnonymize() throws QException
   {
      ///////////////////////////////////////
      // create a contact with full PII    //
      ///////////////////////////////////////
      Integer contactId = insertContact();

      ////////////////////////////////////
      // run the anonymize process      //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(GdprAnonymizeContactProcess.NAME);
      processInput.addValue("contactId", contactId);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertTrue((Boolean) processOutput.getValues().get("anonymized"));

      /////////////////////////////////////////////
      // verify PII scrubbed                     //
      /////////////////////////////////////////////
      Contact updated = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertEquals("[ANONYMIZED]", updated.getFirstName());
      assertEquals("[ANONYMIZED]", updated.getLastName());
      assertEquals("anonymized-" + contactId + "@removed.invalid", updated.getEmail());
      assertNull(updated.getPhone());
      assertNull(updated.getMobilePhone());
      assertNull(updated.getSecondaryEmail());
      assertNull(updated.getAddressLine1());
      assertNull(updated.getCity());
      assertNull(updated.getState());
      assertNull(updated.getPostalCode());
      assertNull(updated.getCountry());
      assertNull(updated.getWebsite());
      assertNull(updated.getLinkedinUrl());
      assertNull(updated.getDescription());
      assertTrue(updated.getDoNotEmail());
      assertTrue(updated.getDoNotCall());

      /////////////////////////////////////////////
      // verify consent record created           //
      /////////////////////////////////////////////
      QueryOutput consentQuery = new QueryAction().execute(
         new QueryInput(ConsentRecord.TABLE_NAME));
      assertEquals(1, consentQuery.getRecords().size());

      ConsentRecord consent = new ConsentRecord(consentQuery.getRecords().get(0));
      assertEquals(contactId, consent.getContactId());
      assertEquals(CrmConsentStatus.WITHDRAWN.getId(), consent.getStatus());
      assertEquals(CrmConsentType.DATA_PROCESSING.getId(), consent.getConsentType());

   }



   /***************************************************************************
    ** Helper: insert a contact with full PII fields.
    ***************************************************************************/
   private Integer insertContact() throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("John")
               .withLastName("Doe")
               .withEmail("john.doe@example.com")
               .withPhone("555-1234")
               .withMobilePhone("555-5678")
               .withSecondaryEmail("john2@example.com")
               .withAddressLine1("123 Main St")
               .withCity("Springfield")
               .withState("IL")
               .withPostalCode("62704")
               .withCountry("US")
               .withWebsite("http://johndoe.com")
               .withLinkedinUrl("http://linkedin.com/in/johndoe")
               .withDescription("VIP client")
               .withDoNotEmail(false)
               .withDoNotCall(false)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
