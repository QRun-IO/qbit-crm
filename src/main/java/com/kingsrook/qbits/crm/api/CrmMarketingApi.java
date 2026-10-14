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

package com.kingsrook.qbits.crm.api;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qbits.crm.security.CrmTokens;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Supported marketing contract. Only eligible recipients expose addresses; all other responses contain identifiers or reason codes.
 *******************************************************************************/
public class CrmMarketingApi
{
   /*******************************************************************************
    ** Whether the current application contains the CRM contract tables.
    *******************************************************************************/
   public boolean isAvailable()
   {
      return (CrmApi.isInstalled(QContext.getQInstance()));
   }

   /*******************************************************************************
    ** Resolve crm-list:id or crm-contact:id with fresh suppression checks.
    *******************************************************************************/
   public List<CrmRecipient> resolveRecipients(String scope) throws QException
   {
      List<CrmRecipient> result = new ArrayList<>();
      if(scope == null)
      {
         return (result);
      }
      String[] parts = scope.split(":", -1);
      if(parts.length != 2 || !("crm-list".equals(parts[0]) || "crm-contact".equals(parts[0])))
      {
         throw (new QUserFacingException("Use a CRM recipient scope: crm-list:<id> or crm-contact:<id>."));
      }
      Integer id;
      try
      {
         id = Integer.valueOf(parts[1]);
         if(id <= 0)
         {
            throw (new NumberFormatException());
         }
      }
      catch(NumberFormatException e)
      {
         throw (new QUserFacingException("A CRM recipient scope must end in a positive numeric identifier."));
      }
      if("crm-contact".equals(parts[0]))
      {
         result.add(recipient(id, null));
      }
      else
      {
         QRecord list = CrmStore.get("crm_email_list", id);
         for(QRecord member : CrmStore.by("crm_email_list_member", "emailListId", id))
         {
            Integer contactId = member.getValueInteger("contactId");
            if(contactId != null)
            {
               String reason = !Boolean.TRUE.equals(list.getValueBoolean("isActive")) ? "list_inactive"
                  : Integer.valueOf(1).equals(member.getValueInteger("status")) ? null : "list_member_not_subscribed";
               result.add(recipient(contactId, reason));
            }
         }
      }
      return (result);
   }

   /*******************************************************************************
    ** Latest Email consent decides; suppression always wins. No consent means no marketing email.
    *******************************************************************************/
   public Eligibility checkEmailEligibility(Integer contactId) throws QException
   {
      List<QRecord> contacts = contactId == null ? List.of() : CrmStore.by("crm_contact", "id", contactId);
      if(contacts.isEmpty())
      {
         return (no("contact_not_found"));
      }
      QRecord contact = contacts.get(0);
      if(Boolean.TRUE.equals(contact.getValueBoolean("doNotEmail")))
      {
         return (no("do_not_email"));
      }
      if(contact.getValue("unsubscribeDate") != null)
      {
         return (no("unsubscribed"));
      }
      if(contact.getValueString("email") == null || contact.getValueString("email").isBlank())
      {
         return (no("no_email_address"));
      }
      for(QRecord member : CrmStore.by("crm_email_list_member", "contactId", contactId))
      {
         if(!Integer.valueOf(1).equals(member.getValueInteger("status")))
         {
            return (no("list_member_not_subscribed"));
         }
      }
      List<QRecord> consents = CrmStore.query("crm_consent_record", new QQueryFilter(
         new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId), new QFilterCriteria("consentType", QCriteriaOperator.EQUALS, 1)));
      if(consents.isEmpty())
      {
         return (no("no_email_consent"));
      }
      QRecord latest = consents.stream().max(Comparator.comparing((QRecord row) -> row.getValueInstant("consentDate") == null
         ? Instant.EPOCH : row.getValueInstant("consentDate")).thenComparing(row -> row.getValueInteger("id"))).orElseThrow();
      if(!Integer.valueOf(1).equals(latest.getValueInteger("status")))
      {
         return (no("email_consent_withdrawn"));
      }
      Instant expiry = latest.getValueInstant("expiryDate");
      if(expiry != null && !expiry.isAfter(Instant.now()))
      {
         return (no("email_consent_expired"));
      }
      return (new Eligibility(true, "eligible"));
   }

   /*******************************************************************************
    ** Campaign references contain no form contents or addresses.
    *******************************************************************************/
   public List<CrmFormSubmissionRef> listFormSubmissions(String utmCampaign, Instant since) throws QException
   {
      if(utmCampaign == null || utmCampaign.isBlank())
      {
         return (List.of());
      }
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("utmCampaign", QCriteriaOperator.EQUALS, utmCampaign));
      if(since != null)
      {
         filter.addCriteria(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since));
      }
      return (CrmStore.query("crm_form_submission", filter).stream().map(row -> new CrmFormSubmissionRef(row.getValueInteger("id"),
         row.getValueInteger("contactId"), row.getValueInstant("createDate"))).toList());
   }

   /*******************************************************************************
    ** Log a typed interaction through the normal activity table actions.
    *******************************************************************************/
   public void recordActivity(Integer contactId, ActivityKind kind, String subject, String description) throws QException
   {
      CrmStore.get("crm_contact", contactId);
      if(kind == null || subject == null || subject.isBlank())
      {
         throw (new QUserFacingException("An activity kind and subject are required."));
      }
      CrmStarterData.ensure();
      Integer typeId = CrmStore.by("crm_activity_type", "code", kind.name()).get(0).getValueInteger("id");
      CrmStore.insert("crm_activity", new QRecord().withValue("contactId", contactId).withValue("activityTypeId", typeId)
         .withValue("subject", subject).withValue("description", description).withValue("ownerUserId", CrmSessionUtils.getCurrentUserId())
         .withValue("isCompleted", true).withValue("completedDate", Instant.now()));
   }

   /*******************************************************************************
    ** Convenience overload for the existing marketing gateway's Note activity contract.
    *******************************************************************************/
   public void recordActivity(Integer contactId, String subject, String description) throws QException
   {
      recordActivity(contactId, ActivityKind.NOTE, subject, description);
   }

   /*******************************************************************************
    ** Record an integration-originated withdrawal of email consent.
    *******************************************************************************/
   public void recordUnsubscribe(Integer contactId, Integer listId, String source) throws QException
   {
      new CrmIntakeApi().unsubscribeByContact(contactId, listId, source);
   }

   /*******************************************************************************
    ** Build a ninety-day signed link without returning any personal data.
    *******************************************************************************/
   public String buildUnsubscribeUrl(Integer contactId, Integer listId) throws QException
   {
      CrmStore.get("crm_contact", contactId);
      if(listId != null)
      {
         CrmStore.get("crm_email_list", listId);
      }
      CrmTokens tokens = new CrmTokens(CrmApi.config());
      return (tokens.url("unsubscribe", tokens.issue("unsubscribe", contactId, listId)));
   }

   /*******************************************************************************
    ** Offer only active lists visible to this caller.
    *******************************************************************************/
   public List<RecipientScopeOption> recipientScopeOptions() throws QException
   {
      return (CrmStore.by("crm_email_list", "isActive", true).stream()
         .map(row -> new RecipientScopeOption("crm-list:" + row.getValueInteger("id"), row.getValueString("name"))).toList());
   }

   /*******************************************************************************
    ** Fetch an address only after eligibility is established.
    *******************************************************************************/
   private CrmRecipient recipient(Integer contactId, String reason) throws QException
   {
      Eligibility eligibility = reason == null ? checkEmailEligibility(contactId) : no(reason);
      return (new CrmRecipient(contactId, eligibility.eligible() ? CrmStore.get("crm_contact", contactId).getValueString("email") : null, eligibility));
   }

   /*******************************************************************************
    ** Construct a suppression result without personal data.
    *******************************************************************************/
   private Eligibility no(String reason)
   {
      return (new Eligibility(false, reason));
   }
}
