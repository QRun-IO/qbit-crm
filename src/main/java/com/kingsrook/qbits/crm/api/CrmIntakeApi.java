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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
import com.kingsrook.qbits.crm.core.model.enums.CrmLegalBasis;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.scoring.CrmAutoAssign;
import com.kingsrook.qbits.crm.security.CrmTokens;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOption;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Trusted host work: the host or signed public hook validates requests before entry, so caller CRM permissions are not required.
 ** Website intake, evidence-backed consent, double opt-in and signed unsubscribe operations under the current QContext.
 *******************************************************************************/
public class CrmIntakeApi
{
   private final CrmStore store;
   private final CrmQBitConfig config;
   private final CrmEmailTransport transport;

   /*******************************************************************************
    ** Use the current instance configuration and standard transport.
    *******************************************************************************/
   public CrmIntakeApi() throws QException
   {
      this(CrmApi.config(), null);
   }

   /*******************************************************************************
    ** Supply a transport for host integration or deterministic delivery tests.
    *******************************************************************************/
   public CrmIntakeApi(CrmQBitConfig config, CrmEmailTransport transport)
   {
      this(config, transport, CrmStore.trusted());
   }

   /*******************************************************************************
    ** Preserve enforcement when the signed-in marketing API delegates a withdrawal.
    *******************************************************************************/
   CrmIntakeApi(CrmQBitConfig config, CrmEmailTransport transport, CrmStore store)
   {
      this.store = store;
      this.config = config;
      this.transport = transport == null ? new CrmEmailTransport(config) : transport;
   }

   /*******************************************************************************
    ** Store a form and attribution, recording marketing consent only for explicit opt-in.
    *******************************************************************************/
   public CrmFormResult submitForm(CrmFormRequest request) throws QException
   {
      validate(request);
      CrmStarterData.ensure(store);
      List<QRecord> contacts = contacts(request.email());
      boolean created = contacts.isEmpty();
      QRecord contact = contact(request, contacts);
      Integer id = contact.getValueInteger("id");
      QRecord submission = submission(request, id);
      consent(id, request.marketingConsent() ? CrmConsentType.EMAIL : CrmConsentType.REPLY_TO_INQUIRY, CrmConsentStatus.GRANTED,
         request.marketingConsent() ? CrmLegalBasis.CONSENT : CrmLegalBasis.LEGITIMATE_INTEREST,
         request.formKey(), request.consentText(), submission.getValueInteger("id"));
      store.recordActivity(id, ActivityKind.FORM_SUBMISSION, "Form submission", request.message());
      notifyLead(submission, NotificationAudience.permission("crm.read"));
      if(!isUnassigned(contact.getValueString("ownerUserId")))
      {
         notifyLead(submission, NotificationAudience.user(contact.getValueString("ownerUserId")));
      }
      return (new CrmFormResult(submission.getValueInteger("id"), id, created));
   }

   /*******************************************************************************
    ** Subscribe to an active list. Pending confirmation never grants Email consent.
    *******************************************************************************/
   public CrmSubscribeResult subscribe(CrmSubscribeRequest request) throws QException
   {
      if(request == null || request.listKey() == null || request.listKey().isBlank())
      {
         throw (new QUserFacingException("A mailing list key is required."));
      }
      List<QRecord> lists = store.by("crm_email_list", "listKey", request.listKey());
      if(lists.isEmpty() || !Boolean.TRUE.equals(lists.get(0).getValueBoolean("isActive")))
      {
         throw (new QUserFacingException("This mailing list is unavailable."));
      }
      QRecord list = lists.get(0);
      Integer listId = list.getValueInteger("id");
      String wording = request.consentText() == null || request.consentText().isBlank() ? list.getValueString("consentText") : request.consentText();
      CrmFormRequest form = new CrmFormRequest(request.listKey(), FormType.NEWSLETTER, request.firstName(), request.lastName(),
         request.email(), null, null, null, true, wording, request.utmSource(), request.utmMedium(), request.utmCampaign(),
         request.utmContent(), request.utmTerm(), null, null, request.visitorKey(), request.clientHash());
      validate(form);
      List<QRecord> contacts = contacts(request.email());
      QRecord member = null;
      if(!contacts.isEmpty())
      {
         List<QRecord> members = store.query("crm_email_list_member", new QQueryFilter(
            new QFilterCriteria("emailListId", QCriteriaOperator.EQUALS, listId),
            new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contacts.get(0).getValueInteger("id"))));
         member = members.isEmpty() ? null : members.get(0);
         if(member != null && CrmListMemberStatus.SUBSCRIBED.getId().equals(member.getValueInteger("status")))
         {
            return (new CrmSubscribeResult(SubscriptionStatus.ALREADY_SUBSCRIBED));
         }
      }
      boolean doubleOptIn = !Boolean.FALSE.equals(list.getValueBoolean("requireDoubleOptIn"));
      CrmTokens tokens = doubleOptIn ? new CrmTokens(config) : null;
      if(doubleOptIn)
      {
         tokens.url("confirm", "configuration-check");
         if(transport.readiness() == CrmReadiness.NOT_CONFIGURED)
         {
            throw (new QUserFacingException("CRM email is not configured for list confirmation."));
         }
      }
      CrmStarterData.ensure(store);
      QRecord contact = contact(form, contacts);
      Integer contactId = contact.getValueInteger("id");
      QRecord submission = submission(form, contactId);
      QRecord values = new QRecord().withValue("emailListId", listId).withValue("contactId", contactId)
         .withValue("email", normalize(request.email())).withValue("firstName", request.firstName())
         .withValue("status", (doubleOptIn ? CrmListMemberStatus.PENDING_CONFIRMATION : CrmListMemberStatus.SUBSCRIBED).getId()).withValue("source", request.source()).withValue("consentText", wording)
         .withValue("formSubmissionId", submission.getValueInteger("id")).withValue("unsubscribedDate", null)
         .withValue("subscribedDate", doubleOptIn ? null : Instant.now()).withValue("confirmedDate", null);
      if(member == null)
      {
         member = store.insert("crm_email_list_member", values);
      }
      else
      {
         member = store.update("crm_email_list_member", values.withValue("id", member.getValueInteger("id")));
      }
      Integer memberId = member.getValueInteger("id");
      if(doubleOptIn)
      {
         String token = tokens.issue("confirm", memberId, listId);
         String hash = CrmTokens.hash(token);
         store.update("crm_email_list_member", new QRecord().withValue("id", memberId)
            .withValue("confirmationTokenHash", hash).withValue("confirmationSentDate", Instant.now()));
         transport.send("confirm", memberId + ":" + hash, contactId, normalize(request.email()), "Confirm your subscription",
            "Please confirm your subscription: " + tokens.url("confirm", token), true);
         return (new CrmSubscribeResult(SubscriptionStatus.PENDING_CONFIRMATION));
      }
      consent(contactId, CrmConsentType.EMAIL, CrmConsentStatus.GRANTED, CrmLegalBasis.CONSENT, request.source(), wording, submission.getValueInteger("id"));
      store.update("crm_contact", new QRecord().withValue("id", contactId).withValue("unsubscribeDate", null));
      return (new CrmSubscribeResult(SubscriptionStatus.SUBSCRIBED));
   }

   /*******************************************************************************
    ** Confirm the current token only. Repeated confirmation is harmless; refreshed and withdrawn tokens fail.
    *******************************************************************************/
   public void confirm(String token) throws QException
   {
      CrmTokens.Claims claims = new CrmTokens(config).verify(token, "confirm");
      QRecord member = store.get("crm_email_list_member", claims.id());
      if(!Objects.equals(claims.listId(), member.getValueInteger("emailListId"))
         || !Objects.equals(CrmTokens.hash(token), member.getValueString("confirmationTokenHash")))
      {
         throw (new QUserFacingException("This confirmation link is no longer available. Please subscribe again."));
      }
      if(CrmListMemberStatus.SUBSCRIBED.getId().equals(member.getValueInteger("status")))
      {
         return;
      }
      if(!CrmListMemberStatus.PENDING_CONFIRMATION.getId().equals(member.getValueInteger("status"))
         || !Boolean.TRUE.equals(store.get("crm_email_list", claims.listId()).getValueBoolean("isActive")))
      {
         throw (new QUserFacingException("This subscription cannot be confirmed."));
      }
      store.get("crm_contact", member.getValueInteger("contactId"));
      consent(member.getValueInteger("contactId"), CrmConsentType.EMAIL, CrmConsentStatus.GRANTED, CrmLegalBasis.CONSENT, member.getValueString("source"), member.getValueString("consentText"), member.getValueInteger("formSubmissionId"));
      store.update("crm_contact", new QRecord().withValue("id", member.getValueInteger("contactId")).withValue("unsubscribeDate", null));
      store.update("crm_email_list_member", new QRecord().withValue("id", claims.id()).withValue("status", CrmListMemberStatus.SUBSCRIBED.getId())
         .withValue("confirmedDate", Instant.now()).withValue("subscribedDate", Instant.now()));
   }

   /*******************************************************************************
    ** Apply a signed list-level or contact-level unsubscribe.
    *******************************************************************************/
   public void unsubscribe(String token) throws QException
   {
      CrmTokens.Claims claims = new CrmTokens(config).verify(token, "unsubscribe");
      unsubscribeByContact(claims.id(), claims.listId(), "Signed unsubscribe link");
   }

   /*******************************************************************************
    ** Withdraw consent and mark the selected memberships. Null listId also suppresses the contact globally.
    *******************************************************************************/
   public void unsubscribeByContact(Integer contactId, Integer listId, String source) throws QException
   {
      store.get("crm_contact", contactId);
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId));
      if(listId != null)
      {
         store.get("crm_email_list", listId);
         filter.addCriteria(new QFilterCriteria("emailListId", QCriteriaOperator.EQUALS, listId));
      }
      for(QRecord member : store.query("crm_email_list_member", filter))
      {
         store.update("crm_email_list_member", new QRecord().withValue("id", member.getValueInteger("id"))
            .withValue("status", CrmListMemberStatus.UNSUBSCRIBED.getId()).withValue("unsubscribedDate", Instant.now()).withValue("confirmationTokenHash", null));
      }
      if(listId == null)
      {
         store.update("crm_contact", new QRecord().withValue("id", contactId).withValue("unsubscribeDate", Instant.now()));
      }
      consent(contactId, CrmConsentType.EMAIL, CrmConsentStatus.WITHDRAWN, CrmLegalBasis.CONSENT, source, "Email consent withdrawn", null);
   }

   /*******************************************************************************
    ** Validate before any writes, including the caller's pre-hashed pseudonymous identifiers.
    *******************************************************************************/
   private void validate(CrmFormRequest request) throws QUserFacingException
   {
      if(request == null || request.formKey() == null || request.formKey().isBlank() || request.formType() == null
         || request.email() == null || !normalize(request.email()).matches("[^@\\s]+@[^@\\s]+"))
      {
         throw (new QUserFacingException("A form key, form type and valid email address are required."));
      }
      if(request.marketingConsent() && (request.consentText() == null || request.consentText().isBlank()))
      {
         throw (new QUserFacingException("Marketing consent requires the exact wording shown to the visitor."));
      }
      for(String hash : new String[] {request.visitorKey(), request.clientHash()})
      {
         if(hash != null && !hash.matches("[a-fA-F0-9]{64}"))
         {
            throw (new QUserFacingException("Visitor and client identifiers must be SHA-256 hashes, never raw identifiers."));
         }
      }
   }

   /*******************************************************************************
    ** Resolve email without case sensitivity using the selected storage mode.
    *******************************************************************************/
   private List<QRecord> contacts(String email) throws QException
   {
      return (store.query("crm_contact", new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, normalize(email))
         .withOption(CriteriaOption.CASE_INSENSITIVE))));
   }

   /*******************************************************************************
    ** Create a lead if needed, then preserve the first touch and refresh last-touch attribution only when supplied.
    *******************************************************************************/
   private QRecord contact(CrmFormRequest request, List<QRecord> contacts) throws QException
   {
      QRecord contact;
      if(contacts.isEmpty())
      {
         String source = request.utmSource() == null || request.utmSource().isBlank() ? "Website" : request.utmSource();
         List<QRecord> sources = store.by("crm_lead_source", "name", source);
         QRecord leadSource = sources.isEmpty() ? store.insert("crm_lead_source", new QRecord().withValue("name", source).withValue("sortOrder", 0)) : sources.get(0);
         List<QRecord> stages = store.by("crm_lifecycle_stage", "name", "Lead");
         contact = store.insert("crm_contact", new QRecord().withValue("email", normalize(request.email()))
            .withValue("firstName", name(request.firstName())).withValue("lastName", name(request.lastName()))
            .withValue("phone", request.phone()).withValue("ownerUserId", CrmSessionUtils.getCurrentUserId())
            .withValue("leadSourceId", leadSource.getValueInteger("id")).withValue("lifecycleStageId", stages.isEmpty() ? null : stages.get(0).getValueInteger("id")));
      }
      else
      {
         contact = contacts.get(0);
      }
      QRecord update = new QRecord().withValue("id", contact.getValueInteger("id"));
      String[] suffixes = {"UtmSource", "UtmMedium", "UtmCampaign", "UtmContent", "UtmTerm", "LandingPage", "Referrer"};
      String[] values = {request.utmSource(), request.utmMedium(), request.utmCampaign(), request.utmContent(), request.utmTerm(), request.landingPage(), request.referrer()};
      boolean attributed = Arrays.stream(values, 0, 6).anyMatch(value -> value != null && !value.isBlank());
      boolean first = contact.getValue("firstTouchDate") == null;
      for(int i = 0; i < suffixes.length; i++)
      {
         if(first && contact.getValue("firstTouch" + suffixes[i]) == null)
         {
            update.withValue("firstTouch" + suffixes[i], values[i]);
         }
         if(attributed)
         {
            update.withValue("lastTouch" + suffixes[i], values[i]);
         }
      }
      if(first)
      {
         update.withValue("firstTouchDate", Instant.now());
      }
      update.withValue("lastTouchDate", Instant.now());
      store.update("crm_contact", update);
      if(contacts.isEmpty() && isUnassigned(contact.getValueString("ownerUserId")))
      {
         CrmAutoAssign.assign(CrmAssignableEntity.CONTACT, contact.getValueInteger("id"));
      }
      return (store.get("crm_contact", contact.getValueInteger("id")));
   }

   /*******************************************************************************
    ** Persist exact submission evidence without raw network identifiers.
    *******************************************************************************/
   private QRecord submission(CrmFormRequest request, Integer contactId) throws QException
   {
      CrmFormType formType;
      switch(request.formType())
      {
         case CONTACT -> formType = CrmFormType.CONTACT;
         case DEMO -> formType = CrmFormType.DEMO_REQUEST;
         case NEWSLETTER -> formType = CrmFormType.NEWSLETTER;
         default -> formType = CrmFormType.GENERAL;
      }
      return (store.insert("crm_form_submission", new QRecord().withValue("formKey", request.formKey()).withValue("formType", formType.getId())
         .withValue("email", normalize(request.email())).withValue("firstName", request.firstName()).withValue("lastName", request.lastName())
         .withValue("phone", request.phone()).withValue("company", request.company()).withValue("message", request.message())
         .withValue("marketingConsent", request.marketingConsent()).withValue("consentText", request.consentText())
         .withValue("utmSource", request.utmSource()).withValue("utmMedium", request.utmMedium()).withValue("utmCampaign", request.utmCampaign())
         .withValue("utmContent", request.utmContent()).withValue("utmTerm", request.utmTerm()).withValue("landingPage", request.landingPage())
         .withValue("referrer", request.referrer()).withValue("visitorKey", request.visitorKey()).withValue("clientHash", request.clientHash())
         .withValue("contactId", contactId).withValue("conversionStatus", CrmFormConversionStatus.CONVERTED_TO_CONTACT.getId()).withValue("convertedDate", Instant.now())));
   }

   /*******************************************************************************
    ** Append consent evidence. Existing records remain immutable.
    *******************************************************************************/
   private void consent(Integer contactId, CrmConsentType type, CrmConsentStatus status, CrmLegalBasis basis, String source, String wording, Integer submissionId) throws QException
   {
      store.insert("crm_consent_record", new QRecord().withValue("contactId", contactId).withValue("consentType", type.getId())
         .withValue("status", status.getId()).withValue("legalBasis", basis.getId()).withValue("source", source).withValue("consentText", wording)
         .withValue("evidenceFormSubmissionId", submissionId).withValue("consentDate", Instant.now()));
   }

   /*******************************************************************************
    ** Core signals accept one audience; publish the same lead to each intended audience.
    *******************************************************************************/
   private void notifyLead(QRecord submission, NotificationAudience audience)
   {
      QAlerts.notify(new NotificationSignal("New web lead received").withSourceName("crm.lead.received")
         .withSourceKey("crm.lead.received:" + submission.getValueInteger("id"))
         .withSourceTableName("crm_form_submission").withSourceRecordId(submission.getValueString("id"))
         .withAudience(audience));
   }

   /*******************************************************************************
    ** System and anonymous placeholders do not represent a sales owner.
    *******************************************************************************/
   private boolean isUnassigned(String owner)
   {
      return (owner == null || owner.isBlank() || "system".equalsIgnoreCase(owner) || "anonymous".equalsIgnoreCase(owner));
   }

   /*******************************************************************************
    ** Normalize email for new writes.
    *******************************************************************************/
   private String normalize(String email)
   {
      return (email.trim().toLowerCase(Locale.ROOT));
   }

   /*******************************************************************************
    ** Existing CRM metadata requires both name fields; retain an honest missing-name label.
    *******************************************************************************/
   private String name(String value)
   {
      return (value == null || value.isBlank() ? "Not provided" : value);
   }
}
