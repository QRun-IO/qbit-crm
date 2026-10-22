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
 ** Unit tests for CrmQBitProducer.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import com.kingsrook.qbits.email.EmailQBitConfig;
import com.kingsrook.qbits.email.EmailQBitProducer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Migration opt-in removes metadata completely without changing legacy hosts.
 *******************************************************************************/
class SharedEmailTemplateMetadataTest extends BaseTest
{
   /*******************************************************************************
    ** Existing hosts keep their actual legacy table, source and configuration.
    *******************************************************************************/
   @Test
   void legacyDefaultRemains()
   {
      assertThat(new CrmQBitConfig().getSharedEmailTemplates()).isFalse();
      assertThat(QContext.getQInstance().getTable("crm_email_template")).isNotNull();
      assertThat(QContext.getQInstance().getPossibleValueSource("crm_email_template")).isNotNull();
   }

   /*******************************************************************************
    ** The opt-in host has one owner and no retired-table or dummy-source metadata.
    *******************************************************************************/
   @Test
   void sharedOwnerMetadataValidates() throws Exception
   {
      QInstance instance = new QInstance().withInstanceDefaultAuthentication(new QAuthenticationMetaData()
         .withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      new EmailQBitProducer().withQBitConfig(new EmailQBitConfig().withBackendName("memory").withTemplatePermissionBaseName("crm.manage")).produce(instance);
      new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables("memory").withSharedEmailTemplates(true))
         .produce(instance).addSelfToInstance(instance);
      new QInstanceValidator().validate(instance);
      assertThat(instance.getTable("crm_email_template")).isNull();
      assertThat(instance.getPossibleValueSource("crm_email_template")).isNull();
      assertThat(instance.getTable("email_template")).isNotNull();
      assertThat(instance.getTable("crm_sequence_step").getField("emailTemplateId").getPossibleValueSourceName()).isEqualTo("email_template");
      assertThat(instance.getTable("crm_email_message").getField("emailTemplateId").getPossibleValueSourceName()).isEqualTo("email_template");
      assertThat(instance.getProcess("crm_send_email").getInputField("emailTemplateId").orElseThrow().getPossibleValueSourceName()).isEqualTo("email_template");
      assertThat(instance.getApp("crm").getSections()).allSatisfy(section -> assertThat(section.getTables()).doesNotContain("crm_email_template"));
   }

   /*******************************************************************************
    ** Unsupported tenant opt-in fails before changing existing metadata.
    *******************************************************************************/
   @Test
   void optInRejectsMissingOwnerAndTenancy()
   {
      assertThatThrownBy(() -> new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withSharedEmailTemplates(true))
         .produce(new QInstance())).hasMessageContaining("installed qbit-email");
      assertThatThrownBy(() -> new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withSharedEmailTemplates(true).withTenantSecurityKeyType("tenant"))
         .produce(new QInstance())).hasMessageContaining("non-tenant");
   }
}
