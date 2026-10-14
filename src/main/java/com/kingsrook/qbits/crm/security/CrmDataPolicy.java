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

package com.kingsrook.qbits.crm.security;


import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.CrmAppMetaData;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** CRM field classification and optional tenant record locks.
 *******************************************************************************/
public final class CrmDataPolicy
{
   private static final Map<String, List<String>> PERSONAL_FIELDS = Map.of(
      "crm_contact", List.of("firstName", "lastName", "email", "secondaryEmail", "phone", "mobilePhone", "addressLine1",
         "addressLine2", "city", "state", "postalCode", "country", "linkedinUrl", "timezone", "jobTitle", "department", "website", "description"),
      "crm_company", List.of("phone", "addressLine1", "addressLine2", "city", "state", "postalCode", "country"),
      "crm_form_submission", List.of("firstName", "lastName", "email", "phone", "message", "clientHash", "visitorKey", "consentText", "company"),
      "crm_email_list_member", List.of("email", "firstName", "consentText", "confirmationTokenHash"),
      "crm_email_message", List.of("fromAddress", "fromName", "toAddresses", "ccAddresses", "bccAddresses", "subject"),
      "crm_consent_record", List.of("consentText"),
      "crm_activity", List.of("description"));

   private static final Map<String, String> PARENTS = Map.ofEntries(
      Map.entry("crm_pipeline_stage", "crm_pipeline"),
      Map.entry("crm_assignment_rule_member", "crm_assignment_rule"),
      Map.entry("crm_consent_record", "crm_contact"),
      Map.entry("crm_email_list_member", "crm_contact"),
      Map.entry("crm_email_message", "crm_contact"),
      Map.entry("crm_sequence_enrollment", "crm_contact"),
      Map.entry("crm_sequence_step", "crm_email_sequence"),
      Map.entry("crm_activity_participant", "crm_activity"),
      Map.entry("crm_deal_contact", "crm_deal"),
      Map.entry("crm_deal_product", "crm_deal"),
      Map.entry("crm_deal_stage_history", "crm_deal"));


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmDataPolicy()
   {
   }


   /*******************************************************************************
    ** Apply after joins have their final names, before backend name inference.
    *******************************************************************************/
   public static void apply(MetaDataProducerMultiOutput output, QInstance instance, CrmQBitConfig config)
   {
      String keyType = config.getTenantSecurityKeyType();
      if(keyType != null && instance.getSecurityKeyType(keyType) == null)
      {
         output.add(new QSecurityKeyType().withName(keyType).withAllAccessKeyName(config.getTenantAllAccessKeyName()));
      }
      for(QTableMetaData table : output.getEach(QTableMetaData.class))
      {
         for(String name : PERSONAL_FIELDS.getOrDefault(table.getName(), List.of()))
         {
            QFieldMetaData field = table.getField(name);
            if(field == null)
            {
               throw (new IllegalStateException("Unknown CRM PII field: " + table.getName() + "." + name));
            }
            AiFieldMetaData ai = AiFieldMetaData.of(field);
            field.withSupplementalMetaData((ai == null ? new AiFieldMetaData() : ai).withClassification(DataClassification.PII));
         }
         if(keyType == null)
         {
            continue;
         }
         RecordSecurityLock lock = new RecordSecurityLock().withSecurityKeyType(keyType);
         String parent = PARENTS.get(table.getName());
         if(parent == null)
         {
            // Polymorphic external mappings and internal job checkpoints need a direct key too.
            table.withField(new QFieldMetaData("tenantKey", QFieldType.STRING).withMaxLength(250));
            if(table.getSections() != null && !table.getSections().isEmpty())
            {
               table.withSection(new QFieldSection("crm_tenancy", "Tenancy", new QIcon("lock"), Tier.T3, List.of("tenantKey")));
            }
            lock.withFieldName("tenantKey");
         }
         else
         {
            QJoinMetaData join = output.getEach(QJoinMetaData.class).stream()
               .filter(candidate -> parent.equals(candidate.getLeftTable()) && table.getName().equals(candidate.getRightTable()))
               .findFirst().orElseThrow(() -> new IllegalStateException("Missing tenant parent join for " + table.getName()));
            lock.withFieldName(parent + ".tenantKey").withJoinNameChain(List.of(join.getName()));
         }
         table.withRecordSecurityLock(lock);
      }
   }


   /*******************************************************************************
    ** Main and settings tables exposed through the host API.
    *******************************************************************************/
   public static boolean isPublishedTable(String name)
   {
      return (CrmAppMetaData.MAIN_TABLES.contains(name) || CrmAppMetaData.SETTINGS_TABLES.contains(name));
   }
}
