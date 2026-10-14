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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.privacy.CrmPersonalData;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Seed all CRM copies and audit copies; verify no original personal strings survive.
 *******************************************************************************/
class CrmPersonalDataTest extends BaseTest
{
   /*******************************************************************************
    ** Erasure covers all copies including audit values and messages.
    *******************************************************************************/
   @Test
   void erasureCoversAllCopiesIncludingAuditValuesAndMessages() throws Exception
   {
      new AuditsMetaDataProvider().defineStandardAuditTables(QContext.getQInstance(), BACKEND_NAME, ignored ->
      {
         // Host has no additional backend details.
      });
      List<String> originals = new ArrayList<>();
      QRecord contact = personal(new QRecord().withValue("id", 1), originals, "firstName", "lastName", "email", "secondaryEmail", "phone", "mobilePhone",
         "addressLine1", "addressLine2", "city", "state", "postalCode", "country", "timezone", "jobTitle", "department", "website", "linkedinUrl", "description");
      P4TestSupport.seed("crm_contact", contact);
      P4TestSupport.seed("crm_form_submission", personal(new QRecord().withValue("id", 1).withValue("contactId", 1), originals,
         "firstName", "lastName", "email", "phone", "message", "clientHash", "visitorKey", "consentText"),
         personal(new QRecord().withValue("id", 2), originals, "firstName", "lastName", "phone", "message", "clientHash", "visitorKey")
            .withValue("email", contact.getValue("email")));
      P4TestSupport.seed("crm_email_list_member", personal(new QRecord().withValue("id", 1).withValue("contactId", 1), originals,
         "email", "firstName", "confirmationTokenHash", "consentText"));
      P4TestSupport.seed("crm_activity", personal(new QRecord().withValue("id", 1).withValue("contactId", 1), originals, "description", "subject"));
      P4TestSupport.seed("crm_email_message", personal(new QRecord().withValue("id", 1).withValue("contactId", 1), originals,
         "fromAddress", "fromName", "toAddresses", "ccAddresses", "bccAddresses", "subject", "bodyHtml", "bodyText"),
         personal(new QRecord().withValue("id", 2).withValue("activityId", 1), originals, "fromAddress", "toAddresses", "subject"));
      P4TestSupport.seed("crm_consent_record", personal(new QRecord().withValue("id", 1).withValue("contactId", 1)
         .withValue("consentType", CrmConsentType.EMAIL.getId()).withValue("status", CrmConsentStatus.GRANTED.getId()), originals, "consentText"));
      P4TestSupport.seed("crm_external_mapping", personal(new QRecord().withValue("id", 1).withValue("entityId", 1)
         .withValue("entityType", CrmEntityType.CONTACT.getId()), originals, "externalId"));
      P4TestSupport.seed("crm_sequence_enrollment", new QRecord().withValue("id", 1).withValue("contactId", 1)
         .withValue("status", CrmEnrollmentStatus.ACTIVE.getId()).withValue("nextStepDate", Instant.now()));
      int id = 1;
      for(String table : List.of("crm_contact", "crm_form_submission", "crm_email_list_member", "crm_activity", "crm_email_message",
         "crm_consent_record", "crm_external_mapping", "crm_sequence_enrollment"))
      {
         P4TestSupport.seed("auditTable", new QRecord().withValue("id", id).withValue("name", table));
         P4TestSupport.seed("audit", personal(new QRecord().withValue("id", (long) id).withValue("auditTableId", id).withValue("recordId", 1), originals, "message"));
         P4TestSupport.seed("auditDetail", personal(new QRecord().withValue("id", (long) id).withValue("auditId", (long) id), originals,
            "oldValue", "newValue", "message"));
         id++;
      }
      CrmPersonalData.erase(1);
      for(var table : QContext.getQInstance().getTables().values())
      {
         for(QRecord row : new QueryAction().execute(new QueryInput(table.getName())).getRecords())
         {
            for(Object value : row.getValues().values())
            {
               if(value instanceof String text)
               {
                  originals.forEach(original -> assertThat(text).as(table.getName() + ":" + row.getValue("id")).doesNotContain(original));
               }
            }
         }
      }
      assertThat(new QueryAction().execute(new QueryInput("crm_external_mapping")).getRecords()).isEmpty();
      assertThat(GetAction.execute("crm_sequence_enrollment", 1).getValueInteger("status")).isEqualTo(CrmEnrollmentStatus.UNENROLLED.getId());
      var consents = new QueryAction().execute(new QueryInput("crm_consent_record")).getRecords();
      assertThat(consents).hasSize(2);
      assertThat(consents).anySatisfy(row ->
      {
         assertThat(row.getValueInteger("status")).isEqualTo(CrmConsentStatus.WITHDRAWN.getId());
         assertThat(row.getValueInteger("consentType")).isEqualTo(CrmConsentType.DATA_PROCESSING.getId());
         assertThat(row.getValueString("consentText")).startsWith("Erased on ");
      });
   }


   /*******************************************************************************
    ** Missing contact fails and ordinary consent editing stays forbidden.
    *******************************************************************************/
   @Test
   void missingContactFailsAndOrdinaryConsentEditingStaysForbidden() throws Exception
   {
      assertThatThrownBy(() -> CrmPersonalData.erase(999)).hasMessageContaining("not found");
      P4TestSupport.seed("crm_consent_record", new QRecord().withValue("id", 1).withValue("contactId", 1).withValue("consentText", "original"));
      assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput("crm_consent_record")
         .withRecord(new QRecord().withValue("id", 1).withValue("consentText", "erased")))).hasMessageContaining("cannot be modified");
   }


   /*******************************************************************************
    ** Distinct sentinels make missing field coverage visible without false matches.
    *******************************************************************************/
   private QRecord personal(QRecord record, List<String> originals, String... fields)
   {
      for(String field : fields)
      {
         String value = "pii-sentinel-" + originals.size() + "-" + field;
         originals.add(value);
         record.withValue(field, value);
      }
      return (record);
   }
}
