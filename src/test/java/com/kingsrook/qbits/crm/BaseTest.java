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
 ** Base test class for the CRM QBit. Sets up QInstance with all CRM entities
 ** using MemoryRecordStore and anonymous auth for testing.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Base test for CRM QBit tests. Initializes QContext with a full QInstance
 ** containing all CRM metadata before each test.
 *******************************************************************************/
public class BaseTest
{
   public static final String BACKEND_NAME    = "memory";
   public static final String TEST_USER_ID    = "test-user-1";



   /*******************************************************************************
    ** Set up QContext with a validated QInstance before each test.
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach() throws Exception
   {
      QInstance qInstance = defineQInstance();
      new QInstanceValidator().validate(qInstance);

      QSession session = new QSession().withPermissions(CrmPermissions.ALL);
      session.setIdReference(TEST_USER_ID);
      QContext.init(qInstance, session);

      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Clear QContext and reset MemoryRecordStore after each test.
    *******************************************************************************/
   @AfterEach
   void baseAfterEach()
   {
      QContext.clear();
      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Define the QInstance with the CRM QBit configured for in-memory testing.
    *******************************************************************************/
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = new QInstance();

      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous")
         .withType(QAuthenticationType.FULLY_ANONYMOUS));

      qInstance.addBackend(new QBackendMetaData()
         .withName(BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class));

      CrmQBitConfig config = new CrmQBitConfig()
         .withDefaultBackendNameForTables(BACKEND_NAME);

      MetaDataProducerMultiOutput output = new CrmQBitProducer()
         .withQBitConfig(config)
         .produce(qInstance);
      output.addSelfToInstance(qInstance);

      return (qInstance);
   }



   /*******************************************************************************
    ** Verify the QBit produces metadata and registers with the QInstance.
    *******************************************************************************/
   @Test
   void testQBitProduces() throws QException
   {
      assertThat(QContext.getQInstance().getQBits()).isNotEmpty();
      assertThat(QContext.getQInstance().getQBits()).containsKey(
         CrmQBitProducer.GROUP_ID + ":" + CrmQBitProducer.ARTIFACT_ID);
   }

   /*******************************************************************************
    ** Existing operation regressions run helpers directly. CrmActionsTest covers ETL.
    *******************************************************************************/
   protected com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput runBusinessStep(
      com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput input) throws QException
   {
      com.kingsrook.qqq.backend.core.actions.processes.BackendStep step;
      step = switch(input.getProcessName())
      {
         case "crm_auto_assign" -> new com.kingsrook.qbits.crm.scoring.processes.AutoAssignProcess();
         case "crm_enroll_in_sequence" -> new com.kingsrook.qbits.crm.email.processes.EnrollInSequenceProcess();
         case "crm_unenroll_from_sequence" -> new com.kingsrook.qbits.crm.email.processes.UnenrollFromSequenceProcess();
         case "crm_clone_deal" -> new com.kingsrook.qbits.crm.deals.processes.CloneDealProcess();
         case "crm_convert_lead" -> new com.kingsrook.qbits.crm.deals.processes.ConvertLeadProcess();
         case "crm_manage_contact_on_deal" -> new com.kingsrook.qbits.crm.deals.processes.ManageContactOnDealProcess();
         case "crm_move_deal_stage" -> new com.kingsrook.qbits.crm.deals.processes.MoveDealStageProcess();
         case "crm_reopen_deal" -> new com.kingsrook.qbits.crm.deals.processes.ReopenDealProcess();
         case "crm_transfer_ownership" -> new com.kingsrook.qbits.crm.deals.processes.TransferOwnershipProcess();
         case "crm_convert_form_to_contact" -> new com.kingsrook.qbits.crm.core.processes.ConvertFormToContactProcess();
         case "crm_detect_duplicates" -> new com.kingsrook.qbits.crm.core.processes.DetectDuplicatesProcess();
         case "crm_gdpr_anonymize_contact" -> new com.kingsrook.qbits.crm.core.processes.GdprAnonymizeContactProcess();
         case "crm_merge_companies" -> new com.kingsrook.qbits.crm.core.processes.MergeCompaniesProcess();
         case "crm_merge_contacts" -> new com.kingsrook.qbits.crm.core.processes.MergeContactsProcess();
         case "crm_subscribe_to_list" -> new com.kingsrook.qbits.crm.core.processes.SubscribeToListProcess();
         case "crm_unsubscribe_from_list" -> new com.kingsrook.qbits.crm.core.processes.UnsubscribeFromListProcess();
         case "crm_log_activity" -> new com.kingsrook.qbits.crm.activities.processes.LogActivityProcess();
         default -> throw (new IllegalArgumentException(input.getProcessName()));
      };
      com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      step.run(input, output);
      return (output);
   }

}
