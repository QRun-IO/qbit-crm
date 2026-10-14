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

package com.kingsrook.qbits.crm.email;


import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.api.ActivityKind;
import com.kingsrook.qbits.crm.api.CrmMarketingApi;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Shared CRM template rendering, transport delivery and timeline recording.
 *******************************************************************************/
public final class CrmEmails
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmEmails()
   {
   }

   /*******************************************************************************
    ** A skip reason suitable for standard WARNING summary lines.
    *******************************************************************************/
   public static String skipReason(QRecord contact, CrmEmailTransport transport) throws QException
   {
      if(Boolean.TRUE.equals(contact.getValueBoolean("doNotEmail")))
      {
         return ("Contact has opted out of email");
      }
      if(contact.getValueString("email") == null || contact.getValueString("email").isBlank())
      {
         return ("Contact has no email address");
      }
      return (transport.readiness() == CrmReadiness.NOT_CONFIGURED ? "CRM email is not configured" : null);
   }

   /*******************************************************************************
    ** Send once using the caller's stable purpose/id and record the transport outcome.
    *******************************************************************************/
   public static void send(CrmEmailTransport transport, QRecord contact, Integer templateId, String subject, String body,
                           String purpose, String id, Integer enrollmentId) throws QException
   {
      String key = "crm:" + purpose + ":" + id;
      List<QRecord> existing = new QueryAction().execute(new QueryInput("crm_email_message").withFilter(
         new QQueryFilter(new QFilterCriteria("idempotencyKey", QCriteriaOperator.EQUALS, key)))).getRecords();
      if(!existing.isEmpty())
      {
         recordActivity(existing.getFirst());
         return;
      }
      if(templateId != null)
      {
         QRecord template = GetAction.execute("crm_email_template", templateId);
         if(template == null)
         {
            throw (new QUserFacingException("Email template is unavailable"));
         }
         subject = subject == null || subject.isBlank() ? template.getValueString("subject") : subject;
         body = body == null || body.isBlank() ? template.getValueString("bodyText") : body;
         if(body == null)
         {
            body = template.getValueString("bodyHtml");
         }
      }
      if(subject == null || subject.isBlank() || body == null || body.isBlank())
      {
         throw (new QUserFacingException("Email subject and body are required"));
      }
      subject = render(subject, contact);
      body = render(body, contact);
      CrmEmailTransport.Delivery delivery = transport.sendMessage(purpose, id, contact.getValueInteger("id"),
         contact.getValueString("email"), subject, body, enrollmentId != null);
      QRecord message = CrmRecords.insert("crm_email_message", new QRecord().withValue("contactId", contact.getValue("id"))
         .withValue("emailTemplateId", templateId).withValue("sequenceEnrollmentId", enrollmentId)
         .withValue("toAddresses", contact.getValue("email")).withValue("subject", subject)
         .withValue("direction", CrmDirection.OUTBOUND.getId()).withValue("status", delivery.status().getId())
         .withValue("idempotencyKey", key).withValue("emailMessageId", delivery.messageId())
         .withValue("sentDate", delivery.sentDate()).withValue("deliveredDate", delivery.deliveredDate()));
      recordActivity(message);
   }

   /*******************************************************************************
    ** Repair a partially recorded send without sending again or duplicating its activity.
    *******************************************************************************/
   private static void recordActivity(QRecord message) throws QException
   {
      if(message.getValue("activityId") != null)
      {
         return;
      }
      String description = "Email: " + message.getValueString("idempotencyKey");
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, message.getValue("contactId")),
         new QFilterCriteria("description", QCriteriaOperator.EQUALS, description));
      List<QRecord> activities = new QueryAction().execute(new QueryInput("crm_activity").withFilter(filter)).getRecords();
      if(activities.isEmpty())
      {
         new CrmMarketingApi().recordActivity(message.getValueInteger("contactId"),
            message.getValue("sequenceEnrollmentId") == null ? ActivityKind.EMAIL : ActivityKind.SEQUENCE_STEP,
            message.getValueString("subject"), description);
         activities = new QueryAction().execute(new QueryInput("crm_activity").withFilter(filter)).getRecords();
      }
      CrmRecords.update("crm_email_message", new QRecord().withValue("id", message.getValue("id"))
         .withValue("activityId", activities.getFirst().getValue("id")));
   }

   /*******************************************************************************
    ** Literal replacements avoid treating customer data as a template program.
    *******************************************************************************/
   public static String render(String text, QRecord contact) throws QException
   {
      QRecord company = contact.getValue("companyId") == null ? null : GetAction.execute("crm_company", contact.getValue("companyId"));
      Map<String, String> fields = Map.of("contact.firstName", value(contact, "firstName"), "contact.lastName", value(contact, "lastName"),
         "company.name", value(company, "name"), "owner.name", value(contact, "ownerUserId"));
      // Replace each token in the original text only, so inserted values cannot introduce merge fields.
      java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{(contact.firstName|contact.lastName|company.name|owner.name|unsubscribeUrl)\\}\\}").matcher(text);
      StringBuilder rendered = new StringBuilder();
      while(matcher.find())
      {
         String replacement = "unsubscribeUrl".equals(matcher.group(1))
            ? new CrmMarketingApi().buildUnsubscribeUrl(contact.getValueInteger("id"), null) : fields.get(matcher.group(1));
         matcher.appendReplacement(rendered, java.util.regex.Matcher.quoteReplacement(replacement));
      }
      matcher.appendTail(rendered);
      return (rendered.toString());
   }

   /*******************************************************************************
    ** Missing merge values render as empty strings.
    *******************************************************************************/
   private static String value(QRecord row, String field)
   {
      return (row == null || row.getValueString(field) == null ? "" : row.getValueString(field));
   }
}
