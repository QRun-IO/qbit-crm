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

package com.kingsrook.qbits.crm.privacy;


import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Erases the CRM's copies of a contact's personal data using tenant-scoped actions.
 *******************************************************************************/
public final class CrmPersonalData
{
   /*******************************************************************************
    ** Narrow consent exception: only replacing evidence text with "erased".
    *******************************************************************************/
   public enum ErasureFlag implements ActionFlag
   {
      REDACT_CONSENT
   }


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmPersonalData()
   {
   }


   /*******************************************************************************
    ** Erase linked data before the contact so retries still have the original email.
    *******************************************************************************/
   public static void erase(Integer contactId) throws QException
   {
      QRecord contact = GetAction.execute("crm_contact", contactId);
      if(contact == null)
      {
         throw (new QException("Contact not found: " + contactId));
      }
      List<Serializable> emails = new ArrayList<>();
      for(String field : List.of("email", "secondaryEmail"))
      {
         if(contact.getValueString(field) != null)
         {
            emails.add(contact.getValueString(field));
         }
      }
      // Two queries avoid OR filters crossing tenant scopes and make retries idempotent.
      eraseForms(new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId)));
      if(!emails.isEmpty())
      {
         eraseForms(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.IN, emails)));
      }
      for(QRecord row : linked("crm_email_list_member", contactId))
      {
         redact("crm_email_list_member", row, "email", "firstName", "confirmationTokenHash", "consentText");
      }
      for(QRecord row : linked("crm_email_message", contactId))
      {
         redact("crm_email_message", row, "fromAddress", "fromName", "toAddresses", "ccAddresses", "bccAddresses", "subject", "bodyHtml", "bodyText");
      }
      for(QRecord row : linked("crm_activity", contactId))
      {
         redact("crm_activity", row, "description", "subject");
         for(QRecord message : query("crm_email_message", new QQueryFilter(new QFilterCriteria("activityId", QCriteriaOperator.EQUALS, row.getValue("id")))))
         {
            redact("crm_email_message", message, "fromAddress", "fromName", "toAddresses", "ccAddresses", "bccAddresses", "subject", "bodyHtml", "bodyText");
         }
      }
      for(QRecord row : linked("crm_consent_record", contactId))
      {
         checkedUpdate("crm_consent_record", new QRecord().withValue("id", row.getValue("id")).withValue("consentText", "erased"));
         redactAudit("crm_consent_record", row.getValue("id"));
      }
      for(QRecord row : linked("crm_sequence_enrollment", contactId))
      {
         checkedUpdate("crm_sequence_enrollment", new QRecord().withValue("id", row.getValue("id"))
            .withValue("status", CrmEnrollmentStatus.UNENROLLED.getId()).withValue("nextStepDate", null));
         redactAudit("crm_sequence_enrollment", row.getValue("id"));
      }
      for(QRecord row : query("crm_external_mapping", new QQueryFilter(
         new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, CrmEntityType.CONTACT.getId()),
         new QFilterCriteria("entityId", QCriteriaOperator.EQUALS, contactId))))
      {
         var deleted = new DeleteAction().execute(new DeleteInput("crm_external_mapping").withPrimaryKey(row.getValue("id")).withOmitDmlAudit(true));
         if(deleted.getDeletedRecordCount() != 1)
         {
            throw (new QException("CRM erase could not delete an external mapping"));
         }
         redactAudit("crm_external_mapping", row.getValue("id"));
      }
      redact("crm_contact", contact, "firstName", "lastName", "phone", "mobilePhone", "secondaryEmail", "addressLine1", "addressLine2",
         "city", "state", "postalCode", "country", "timezone", "jobTitle", "department", "website", "linkedinUrl", "description");
      checkedUpdate("crm_contact", new QRecord().withValue("id", contactId).withValue("email", "anonymized-" + contactId + "@removed.invalid")
         .withValue("doNotEmail", true).withValue("doNotCall", true));
      CrmRecords.insert("crm_consent_record", new QRecord().withValue("contactId", contactId)
         .withValue("consentType", CrmConsentType.DATA_PROCESSING.getId()).withValue("status", CrmConsentStatus.WITHDRAWN.getId())
         .withValue("consentText", "Erased on " + LocalDate.now(ZoneOffset.UTC)).withValue("source", "GDPR erasure").withValue("consentDate", Instant.now()));
   }


   /*******************************************************************************
    ** Erase both direct and email-matched web leads.
    *******************************************************************************/
   private static void eraseForms(QQueryFilter filter) throws QException
   {
      for(QRecord row : query("crm_form_submission", filter))
      {
         redact("crm_form_submission", row, "firstName", "lastName", "email", "phone", "message", "clientHash", "visitorKey", "consentText", "company");
      }
   }


   /*******************************************************************************
    ** Collect rows through core record locks, never through a backend bypass.
    *******************************************************************************/
   private static List<QRecord> linked(String table, Integer contactId) throws QException
   {
      return (query(table, new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))));
   }


   /*******************************************************************************
    ** Page queries to avoid the backend's default result limit silently omitting PII.
    *******************************************************************************/
   private static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      List<QRecord> result = new ArrayList<>();
      Serializable after = null;
      while(true)
      {
         QQueryFilter page = filter.clone().withOrderBy(new QFilterOrderBy("id")).withLimit(250);
         if(after != null)
         {
            page.withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, after));
         }
         List<QRecord> rows = new QueryAction().execute(new QueryInput(table).withFilter(page)).getRecords();
         result.addAll(rows);
         if(rows.size() < 250)
         {
            return (result);
         }
         after = rows.getLast().getValue("id");
      }
   }


   /*******************************************************************************
    ** Keep required fields nonempty; never audit the original values during erasure.
    *******************************************************************************/
   private static void redact(String table, QRecord row, String... fields) throws QException
   {
      QRecord update = new QRecord().withValue("id", row.getValue("id"));
      for(String field : fields)
      {
         boolean required = Boolean.TRUE.equals(QContext.getQInstance().getTable(table).getField(field).getIsRequired());
         update.withValue(field, required || field.equals("firstName") || field.equals("lastName") ? "[ANONYMIZED]" : null);
      }
      checkedUpdate(table, update);
      redactAudit(table, row.getValue("id"));
   }


   /*******************************************************************************
    ** A failed write must fail erasure; never report a partially erased contact as done.
    *******************************************************************************/
   private static void checkedUpdate(String table, QRecord update) throws QException
   {
      var output = new UpdateAction().execute(new UpdateInput(table).withRecord(update).withOmitDmlAudit(true).withFlag(ErasureFlag.REDACT_CONSENT));
      if(output.getRecords().isEmpty() || output.getRecords().stream().anyMatch(row -> row.getErrors() != null && !row.getErrors().isEmpty()))
      {
         throw (new QException("CRM erase could not redact " + table));
      }
   }


   /*******************************************************************************
    ** RC.6 core audits resolve table ids through auditTable. Redact both values and
    ** human-readable messages, which can embed old record labels and personal values.
    *******************************************************************************/
   private static void redactAudit(String table, Serializable recordId) throws QException
   {
      if(QContext.getQInstance().getTable("auditTable") == null || QContext.getQInstance().getTable("audit") == null
         || QContext.getQInstance().getTable("auditDetail") == null)
      {
         return;
      }
      for(QRecord auditTable : query("auditTable", new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, table))))
      {
         for(QRecord audit : query("audit", new QQueryFilter(
            new QFilterCriteria("auditTableId", QCriteriaOperator.EQUALS, auditTable.getValue("id")),
            new QFilterCriteria("recordId", QCriteriaOperator.EQUALS, recordId))))
         {
            for(QRecord detail : query("auditDetail", new QQueryFilter(new QFilterCriteria("auditId", QCriteriaOperator.EQUALS, audit.getValue("id")))))
            {
               checkedUpdate("auditDetail", new QRecord().withValue("id", detail.getValue("id"))
                  .withValue("oldValue", "erased").withValue("newValue", "erased").withValue("message", "erased"));
            }
            checkedUpdate("audit", new QRecord().withValue("id", audit.getValue("id")).withValue("message", "Personal data erased"));
         }
      }
   }
}
