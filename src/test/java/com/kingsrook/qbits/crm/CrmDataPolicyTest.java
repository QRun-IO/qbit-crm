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

package com.kingsrook.qbits.crm;


import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** PII inventory and real tenant isolation through core QueryAction.
 *******************************************************************************/
class CrmDataPolicyTest extends BaseTest
{
   /*******************************************************************************
    ** Complete pii inventory.
    *******************************************************************************/
   @Test
   void completePiiInventory()
   {
      Map<String, List<String>> expected = Map.of(
         "crm_contact", List.of("firstName", "lastName", "email", "secondaryEmail", "phone", "mobilePhone", "addressLine1", "addressLine2",
            "city", "state", "postalCode", "country", "linkedinUrl", "timezone", "jobTitle", "department", "website", "description"),
         "crm_company", List.of("phone", "addressLine1", "addressLine2", "city", "state", "postalCode", "country"),
         "crm_form_submission", List.of("firstName", "lastName", "email", "phone", "message", "clientHash", "visitorKey", "consentText", "company"),
         "crm_email_list_member", List.of("email", "firstName", "consentText", "confirmationTokenHash"),
         "crm_email_message", List.of("fromAddress", "fromName", "toAddresses", "ccAddresses", "bccAddresses", "subject"),
         "crm_consent_record", List.of("consentText"), "crm_activity", List.of("description"));
      TreeSet<String> actual = new TreeSet<>();
      QContext.getQInstance().getTables().values().forEach(table -> table.getFields().values().forEach(field ->
      {
         if(AiFieldMetaData.of(field) != null && AiFieldMetaData.of(field).getClassification() == DataClassification.PII)
         {
            actual.add(table.getName() + "." + field.getName());
         }
      }));
      TreeSet<String> inventory = new TreeSet<>();
      expected.forEach((table, fields) -> fields.forEach(field -> inventory.add(table + "." + field)));
      assertThat(actual).containsExactlyElementsOf(inventory);
   }


   /*******************************************************************************
    ** Personal field names cannot lose classification.
    *******************************************************************************/
   @Test
   void personalFieldNamesCannotLoseClassification()
   {
      Pattern pattern = Pattern.compile("email|phone|firstName|lastName|address|postal|linkedin", Pattern.CASE_INSENSITIVE);
      QContext.getQInstance().getTables().values().forEach(table -> table.getFields().values().forEach(field ->
      {
         // Foreign-key ids, preference booleans and transport message ids are not personal text.
         if(field.getType() == QFieldType.STRING && pattern.matcher(field.getName()).find() && !field.getName().equals("emailMessageId"))
         {
            assertThat(AiFieldMetaData.of(field)).as(table.getName() + "." + field.getName()).isNotNull();
            assertThat(AiFieldMetaData.of(field).getClassification()).isEqualTo(DataClassification.PII);
         }
      }));
   }


   /*******************************************************************************
    ** Tenancy off has no fields or locks.
    *******************************************************************************/
   @Test
   void tenancyOffHasNoFieldsOrLocks()
   {
      assertThat(new CrmQBitConfig().getTenantSecurityKeyType()).isNull();
      QContext.getQInstance().getTables().values().forEach(table ->
      {
         assertThat(table.getFields().get("tenantKey")).isNull();
         assertThat(table.getRecordSecurityLocks()).isNullOrEmpty();
      });
   }


   /*******************************************************************************
    ** Tenant asees only its contacts deals and joined products.
    *******************************************************************************/
   @Test
   void tenantASeesOnlyItsContactsDealsAndJoinedProducts() throws Exception
   {
      QInstance instance = P4TestSupport.host();
      new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME)
         .withTenantSecurityKeyType("tenant").withTenantAllAccessKeyName("allTenants")).produce(instance).addSelfToInstance(instance);
      new QInstanceValidator().validate(instance);
      QContext.init(instance, new QSession().withPermissions(CrmPermissions.ALL).withSecurityKeyValue("tenant", "A"));
      for(String table : List.of("crm_contact", "crm_deal"))
      {
         P4TestSupport.seed(table, new QRecord().withValue("id", 1).withValue("tenantKey", "A"),
            new QRecord().withValue("id", 2).withValue("tenantKey", "B"), new QRecord().withValue("id", 3));
      }
      P4TestSupport.seed("crm_deal_product", new QRecord().withValue("id", 11).withValue("dealId", 1),
         new QRecord().withValue("id", 22).withValue("dealId", 2));
      assertThat(ids("crm_contact")).containsExactly(1);
      assertThat(ids("crm_deal")).containsExactly(1);
      assertThat(ids("crm_deal_product")).containsExactly(11);
      QContext.getQSession().withSecurityKeyValue("allTenants", true);
      assertThat(ids("crm_contact")).contains(1, 2);
      assertThat(ids("crm_deal")).contains(1, 2);
      assertThat(ids("crm_deal_product")).containsExactlyInAnyOrder(11, 22);
      instance.getTables().values().forEach(table -> assertThat(table.getRecordSecurityLocks()).as(table.getName()).isNotEmpty());
      assertThat(instance.getTable("crm_deal_product").getFields().get("tenantKey")).isNull();
   }


   /*******************************************************************************
    ** Preserves host key type.
    *******************************************************************************/
   @Test
   void preservesHostKeyType() throws Exception
   {
      QInstance instance = P4TestSupport.host();
      QSecurityKeyType key = new QSecurityKeyType().withName("tenant").withAllAccessKeyName("hostAll");
      instance.addSecurityKeyType(key);
      new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME)
         .withTenantSecurityKeyType("tenant").withTenantAllAccessKeyName("ignored")).produce(instance).addSelfToInstance(instance);
      assertThat(instance.getSecurityKeyType("tenant")).isSameAs(key);
   }


   /*******************************************************************************
    ** Read through real record locks.
    *******************************************************************************/
   private List<Integer> ids(String table) throws Exception
   {
      return (new QueryAction().execute(new QueryInput(table)).getRecords().stream().map(row -> row.getValueInteger("id")).toList());
   }
}
