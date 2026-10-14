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


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.security.CrmTokens;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** End-to-end API contract tests through core actions, covering consent, attribution and double opt-in.
 *******************************************************************************/
class CrmApisTest extends BaseTest
{
   private CrmQBitConfig config;
   private FakeTransport transport;
   private CrmIntakeApi intake;
   private final CrmMarketingApi marketing = new CrmMarketingApi();

   /*******************************************************************************
    ** Setup api.
    *******************************************************************************/
   @BeforeEach
   void setupApi() throws Exception
   {
      config = CrmApi.config().withTokenSecret("test-only-signing-secret").withPublicBaseUrl("https://crm.example.test");
      transport = new FakeTransport(config);
      intake = new CrmIntakeApi(config, transport);
   }

   /*******************************************************************************
    ** Installation readiness and required configuration.
    *******************************************************************************/
   @Test
   void installationReadinessAndRequiredConfiguration() throws Exception
   {
      assertThat(CrmApi.isInstalled(null)).isFalse();
      assertThat(CrmApi.isInstalled(new QInstance())).isFalse();
      assertThat(CrmApi.isInstalled(QContext.getQInstance())).isTrue();
      assertThat(marketing.isAvailable()).isTrue();
      assertThat(CrmApi.readiness()).isEqualTo(CrmReadiness.NOT_CONFIGURED);
      config.withMessagingProviderName("missing");
      assertThat(new CrmEmailTransport(config).readiness()).isEqualTo(CrmReadiness.NOT_CONFIGURED);
      assertThatThrownBy(() -> new CrmEmailTransport(config).send("one-to-one", "1", 1, "a@example.test", "Hello", "Body", false))
         .isInstanceOf(QUserFacingException.class);
      config.withTokenSecret(null);
      assertThatThrownBy(() -> intake.submitForm(form("a@example.test", false, "first"))).hasMessageContaining("withTokenSecret");
      assertThat(CrmStore.query("crm_contact", null)).isEmpty();
   }

   /*******************************************************************************
    ** Form preserves first touch and never grants implicit marketing consent.
    *******************************************************************************/
   @Test
   void formPreservesFirstTouchAndNeverGrantsImplicitMarketingConsent() throws Exception
   {
      CrmStore.insert("crm_lifecycle_stage", new QRecord().withValue("name", "Lead").withValue("sortOrder", 1));
      CrmFormResult first = intake.submitForm(form("Ada@Example.test", false, "first"));
      assertThat(first.contactCreated()).isTrue();
      QRecord before = CrmStore.get("crm_contact", first.contactId());
      CrmFormResult last = intake.submitForm(form("ada@example.TEST", false, "second"));
      assertThat(last.contactCreated()).isFalse();
      assertThat(last.contactId()).isEqualTo(first.contactId());
      QRecord contact = CrmStore.get("crm_contact", first.contactId());
      assertThat(contact.getValueString("firstTouchUtmCampaign")).isEqualTo("first");
      assertThat(contact.getValueString("lastTouchUtmCampaign")).isEqualTo("second");
      assertThat(contact.getValueInstant("firstTouchDate")).isEqualTo(before.getValueInstant("firstTouchDate"));
      assertThat(contact.getValueInteger("lifecycleStageId")).isNotNull();
      assertThat(contact.getValueString("firstTouchLandingPage")).isEqualTo("https://example.test/form");
      assertThat(marketing.checkEmailEligibility(first.contactId()).reason()).isEqualTo("no_email_consent");
      QRecord consent = CrmStore.by("crm_consent_record", "contactId", first.contactId()).get(0);
      assertThat(consent.getValueInteger("consentType")).isEqualTo(5);
      assertThat(consent.getValueInteger("legalBasis")).isEqualTo(2);
      assertThat(consent.getValueInteger("evidenceFormSubmissionId")).isEqualTo(first.submissionId());
      assertThat(CrmStore.by("crm_activity", "contactId", first.contactId())).hasSize(2);
      CrmStarterData.ensure();
      assertThat(CrmStore.query("crm_activity_type", null)).hasSize(6);
      assertThat(marketing.listFormSubmissions("first", Instant.EPOCH)).containsExactly(
         new CrmFormSubmissionRef(first.submissionId(), first.contactId(), CrmStore.get("crm_form_submission", first.submissionId()).getValueInstant("createDate")));
      assertThat(marketing.listFormSubmissions("first", Instant.now().plusSeconds(60))).isEmpty();
      assertThat(marketing.listFormSubmissions(null, null)).isEmpty();
      assertThat(marketing.listFormSubmissions("", null)).isEmpty();
   }

   /*******************************************************************************
    ** Explicit consent activities and scopes.
    *******************************************************************************/
   @Test
   void explicitConsentActivitiesAndScopes() throws Exception
   {
      CrmFormResult result = intake.submitForm(form("yes@example.test", true, "campaign"));
      assertThat(marketing.checkEmailEligibility(result.contactId()).eligible()).isTrue();
      assertThat(marketing.resolveRecipients("crm-contact:" + result.contactId())).containsExactly(
         new CrmRecipient(result.contactId(), "yes@example.test", new Eligibility(true, "eligible")));
      QRecord consent = CrmStore.by("crm_consent_record", "contactId", result.contactId()).get(0);
      assertThat(consent.getValueString("consentText")).isEqualTo("Send me news");
      assertThat(consent.getValueInteger("legalBasis")).isEqualTo(1);
      for(ActivityKind kind : ActivityKind.values())
      {
         marketing.recordActivity(result.contactId(), kind, "Interaction", "Recorded by an integration");
      }
      marketing.recordActivity(result.contactId(), "Gateway note", "Description");
      assertThat(CrmStore.by("crm_activity", "contactId", result.contactId())).hasSize(8);
      assertThatThrownBy(() -> marketing.recordActivity(result.contactId(), null, "", null)).isInstanceOf(QUserFacingException.class);
      assertThat(marketing.resolveRecipients(null)).isEmpty();
      for(String scope : List.of("other:1", "crm-list:x", "crm-list:-1", "crm-contact:1:2"))
      {
         assertThatThrownBy(() -> marketing.resolveRecipients(scope)).isInstanceOf(QUserFacingException.class);
      }
   }

   /*******************************************************************************
    ** Eligibility matrix.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"none", "granted", "expired", "withdrawn", "doNotEmail", "unsubscribed", "pending", "bounced", "noAddress", "missing"})
   void eligibilityMatrix(String state) throws Exception
   {
      Integer id = intake.submitForm(form("matrix@example.test", false, "matrix")).contactId();
      if(!"none".equals(state))
      {
         CrmStore.insert("crm_consent_record", new QRecord().withValue("contactId", id).withValue("consentType", 1)
            .withValue("status", "withdrawn".equals(state) ? 2 : 1).withValue("consentDate", Instant.now())
            .withValue("expiryDate", "expired".equals(state) ? Instant.now().minusSeconds(1) : null));
      }
      String reason = "eligible";
      switch(state)
      {
         case "none" -> reason = "no_email_consent";
         case "expired" -> reason = "email_consent_expired";
         case "withdrawn" -> reason = "email_consent_withdrawn";
         case "doNotEmail" ->
         {
            CrmStore.update("crm_contact", new QRecord().withValue("id", id).withValue("doNotEmail", true));
            reason = "do_not_email";
         }
         case "unsubscribed" ->
         {
            CrmStore.update("crm_contact", new QRecord().withValue("id", id).withValue("unsubscribeDate", Instant.now()));
            reason = "unsubscribed";
         }
         case "pending", "bounced" ->
         {
            Integer listId = list("matrix", true);
            CrmStore.insert("crm_email_list_member", new QRecord().withValue("contactId", id).withValue("emailListId", listId)
               .withValue("email", "matrix@example.test").withValue("status", "pending".equals(state) ? 5 : 3));
            reason = "list_member_not_subscribed";
         }
         case "noAddress" ->
         {
            CrmStore.update("crm_contact", new QRecord().withValue("id", id).withValue("email", null));
            reason = "no_email_address";
         }
         case "missing" ->
         {
            id = 9999;
            reason = "contact_not_found";
         }
         default -> reason = "eligible";
      }
      Eligibility eligibility = marketing.checkEmailEligibility(id);
      assertThat(eligibility.reason()).isEqualTo(reason);
      assertThat(eligibility.eligible()).isEqualTo("granted".equals(state));
      CrmRecipient recipient = marketing.resolveRecipients("crm-contact:" + id).get(0);
      assertThat(recipient.address()).isEqualTo(eligibility.eligible() ? "matrix@example.test" : null);
   }

   /*******************************************************************************
    ** Double opt in refresh confirm and unsubscribe end to end.
    *******************************************************************************/
   @Test
   void doubleOptInRefreshConfirmAndUnsubscribeEndToEnd() throws Exception
   {
      Integer listId = list("news", true);
      CrmSubscribeRequest request = signup("news");
      assertThat(intake.subscribe(request).status()).isEqualTo(SubscriptionStatus.PENDING_CONFIRMATION);
      String oldToken = transport.token;
      QRecord member = CrmStore.query("crm_email_list_member", null).get(0);
      Integer contactId = member.getValueInteger("contactId");
      assertThat(member.getValue("subscribedDate")).isNull();
      assertThat(member.getValueString("confirmationTokenHash")).isEqualTo(CrmTokens.hash(oldToken)).isNotEqualTo(oldToken);
      assertThat(member.getValueString("consentText")).isEqualTo("Send me news");
      assertThat(CrmStore.by("crm_consent_record", "contactId", contactId)).isEmpty();
      assertThat(marketing.resolveRecipients("crm-list:" + listId).get(0).address()).isNull();
      intake.subscribe(request);
      assertThat(transport.token).isNotEqualTo(oldToken);
      assertThatThrownBy(() -> intake.confirm(oldToken)).isInstanceOf(QUserFacingException.class);
      intake.confirm(transport.token);
      intake.confirm(transport.token);
      assertThat(CrmStore.by("crm_consent_record", "contactId", contactId)).hasSize(1);
      assertThat(CrmStore.get("crm_email_list_member", member.getValueInteger("id")).getValue("confirmedDate")).isNotNull();
      assertThat(marketing.resolveRecipients("crm-list:" + listId).get(0).address()).isEqualTo("subscriber@example.test");
      assertThat(intake.subscribe(request).status()).isEqualTo(SubscriptionStatus.ALREADY_SUBSCRIBED);
      String url = marketing.buildUnsubscribeUrl(contactId, listId);
      intake.unsubscribe(url.substring(url.lastIndexOf('/') + 1));
      assertThat(CrmStore.get("crm_contact", contactId).getValue("unsubscribeDate")).isNull();
      assertThat(CrmStore.get("crm_email_list_member", member.getValueInteger("id")).getValueInteger("status")).isEqualTo(2);
      assertThatThrownBy(() -> intake.confirm(transport.token)).isInstanceOf(QUserFacingException.class);
      marketing.recordUnsubscribe(contactId, null, "Marketing");
      assertThat(CrmStore.get("crm_contact", contactId).getValue("unsubscribeDate")).isNotNull();
      assertThat(marketing.checkEmailEligibility(contactId).reason()).isEqualTo("unsubscribed");
      assertThat(marketing.buildUnsubscribeUrl(contactId, null)).contains("/qqq/hooks/crm/unsubscribe/");
   }

   /*******************************************************************************
    ** Immediate subscription and active options.
    *******************************************************************************/
   @Test
   void immediateSubscriptionAndActiveOptions() throws Exception
   {
      Integer activeId = list("immediate", false);
      Integer inactiveId = list("inactive", false);
      CrmStore.update("crm_email_list", new QRecord().withValue("id", inactiveId).withValue("isActive", false));
      assertThat(marketing.recipientScopeOptions()).containsExactly(new RecipientScopeOption("crm-list:" + activeId, "immediate"));
      assertThat(intake.subscribe(signup("immediate")).status()).isEqualTo(SubscriptionStatus.SUBSCRIBED);
      Integer id = CrmStore.query("crm_contact", null).get(0).getValueInteger("id");
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
      assertThat(transport.token).isNull();
      assertThatThrownBy(() -> intake.subscribe(signup("inactive"))).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> intake.subscribe(signup("absent"))).isInstanceOf(QUserFacingException.class);
      CrmStore.update("crm_email_list", new QRecord().withValue("id", activeId).withValue("isActive", false));
      assertThat(marketing.resolveRecipients("crm-list:" + activeId).get(0).eligibility().reason()).isEqualTo("list_inactive");
   }

   /*******************************************************************************
    ** Tokens reject tampering wrong purpose and expiry.
    *******************************************************************************/
   @Test
   void tokensRejectTamperingWrongPurposeAndExpiry() throws Exception
   {
      Instant now = Instant.parse("2026-10-14T12:00:00Z");
      CrmTokens tokens = new CrmTokens(config, Clock.fixed(now, ZoneOffset.UTC));
      String token = tokens.issue("confirm", 1, 2);
      assertThat(tokens.verify(token, "confirm").id()).isEqualTo(1);
      assertThat(tokens.verify(token, "confirm").listId()).isEqualTo(2);
      assertThat(tokens.verify(token, "confirm").purpose()).isEqualTo("confirm");
      assertThat(tokens.verify(token, "confirm").expiresAt()).isEqualTo(now.plus(Duration.ofDays(90)));
      assertThatThrownBy(() -> tokens.verify(token, "unsubscribe")).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> tokens.verify("x" + token.substring(1), "confirm")).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> tokens.verify("bad", "confirm")).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> tokens.verify(null, "confirm")).isInstanceOf(QUserFacingException.class);
      CrmTokens expired = new CrmTokens(config, Clock.fixed(now.plus(Duration.ofDays(90)), ZoneOffset.UTC));
      assertThatThrownBy(() -> expired.verify(token, "confirm")).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> tokens.issue("other", 1, null)).isInstanceOf(QUserFacingException.class);
      config.withPublicBaseUrl(null);
      assertThatThrownBy(() -> tokens.url("confirm", token)).hasMessageContaining("withPublicBaseUrl");
   }

   /*******************************************************************************
    ** Validates before writing and does not bypass permissions.
    *******************************************************************************/
   @Test
   void validatesBeforeWritingAndDoesNotBypassPermissions() throws Exception
   {
      assertThatThrownBy(() -> intake.submitForm(null)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> intake.subscribe(null)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> intake.submitForm(form("invalid", false, "x"))).isInstanceOf(QUserFacingException.class);
      QContext.getQSession().setPermissions(java.util.Set.of());
      assertThatThrownBy(() -> intake.submitForm(form("denied@example.test", false, "x"))).isInstanceOf(QException.class);
   }

   /*******************************************************************************
    ** A later Email consent wins, with id as the deterministic equal-time tie breaker.
    *******************************************************************************/
   @Test
   void latestEmailConsentWinsAndInquiryCannotOverrideIt() throws Exception
   {
      Integer id = intake.submitForm(form("latest@example.test", true, "first")).contactId();
      Instant now = Instant.now().plusSeconds(10);
      CrmStore.insert("crm_consent_record", new QRecord().withValue("contactId", id).withValue("consentType", 1)
         .withValue("status", 2).withValue("consentDate", now));
      intake.submitForm(form("latest@example.test", false, "reply"));
      assertThat(marketing.checkEmailEligibility(id).reason()).isEqualTo("email_consent_withdrawn");
      CrmStore.insert("crm_consent_record", new QRecord().withValue("contactId", id).withValue("consentType", 1)
         .withValue("status", 1).withValue("consentDate", now));
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
      assertThat(marketing.resolveRecipients("crm-contact:" + id).get(0).key()).isEqualTo("crm-contact:" + id);
   }



   /*******************************************************************************
    ** Synthetic form with complete attribution.
    *******************************************************************************/
   private CrmFormRequest form(String email, boolean consent, String campaign)
   {
      return (new CrmFormRequest("contact", FormType.CONTACT, "Ada", "Lovelace", email, "555-0100", "Example", "Hello",
         consent, consent ? "Send me news" : "Reply to my inquiry", "search", "organic", campaign, "banner", "crm",
         "https://example.test/form", "https://search.example.test", "a".repeat(64), "b".repeat(64)));
   }

   /*******************************************************************************
    ** Synthetic opt-in request.
    *******************************************************************************/
   private CrmSubscribeRequest signup(String listKey)
   {
      return (new CrmSubscribeRequest(listKey, "subscriber@example.test", null, null, "Send me news", "Website",
         null, null, "signup", null, null, null, null));
   }

   /*******************************************************************************
    ** Active list fixture written through the same actions as host configuration.
    *******************************************************************************/
   private Integer list(String key, boolean doubleOptIn) throws Exception
   {
      return (CrmStore.insert("crm_email_list", new QRecord().withValue("listKey", key).withValue("name", key)
         .withValue("slug", key).withValue("sortOrder", 1).withValue("requireDoubleOptIn", doubleOptIn)
         .withValue("consentText", "Default wording")).getValueInteger("id"));
   }

   /*******************************************************************************
    ** Capture confirmation mail without a network provider.
    *******************************************************************************/
   private static class FakeTransport extends CrmEmailTransport
   {
      private String token;

      /*******************************************************************************
       ** Configure the fake with the same instance settings.
       *******************************************************************************/
      FakeTransport(CrmQBitConfig config)
      {
         super(config);
      }

      @Override
      public CrmReadiness readiness()
      {
         return (CrmReadiness.CORE_MESSAGING);
      }

      @Override
      public void send(String purpose, String id, Integer contactId, String address, String subject, String body, boolean marketing)
      {
         assertThat(purpose).isEqualTo("confirm");
         assertThat(marketing).isTrue();
         assertThat(body).contains("https://crm.example.test/qqq/hooks/crm/confirm/");
         token = body.substring(body.lastIndexOf('/') + 1);
      }
   }
}
