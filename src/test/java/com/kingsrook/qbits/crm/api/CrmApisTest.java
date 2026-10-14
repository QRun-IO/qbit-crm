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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
import com.kingsrook.qbits.crm.core.model.enums.CrmLegalBasis;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qbits.crm.security.CrmTokens;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.LoggingAlertSink;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** End-to-end API contract tests through core actions, covering consent, attribution and double opt-in.
 *******************************************************************************/
public class CrmApisTest extends BaseTest
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

   }

   /*******************************************************************************
    ** Email-only intake preserves absent names and never assigns the service user.
    *******************************************************************************/
   @Test
   void emailOnlyIntakeHasNoOwner() throws Exception
   {
      QContext.getQSession().setIdReference("site-api-system");
      CrmFormResult result = intake.submitForm(new CrmFormRequest("contact", FormType.CONTACT, null, " ", "only@example.test",
         null, null, "Hello", false, "Reply", null, null, null, null, null, null, null, null, null));
      QRecord contact = CrmStore.asUser().get("crm_contact", result.contactId());
      assertThat(contact.getValue("firstName")).isNull();
      assertThat(contact.getValue("lastName")).isNull();
      assertThat(contact.getValue("ownerUserId")).isNull();
      assertThat(contact.getRecordLabel()).isEqualTo("only@example.test");
   }


   /*******************************************************************************
    ** Form preserves first touch and never grants implicit marketing consent.
    *******************************************************************************/
   @Test
   void formPreservesFirstTouchAndNeverGrantsImplicitMarketingConsent() throws Exception
   {
      CrmStore.asUser().insert("crm_lifecycle_stage", new QRecord().withValue("name", "Lead").withValue("sortOrder", 1));
      CrmFormResult first = intake.submitForm(form("Ada@Example.test", false, "first"));
      assertThat(first.contactCreated()).isTrue();
      QRecord before = CrmStore.asUser().get("crm_contact", first.contactId());
      CrmFormResult last = intake.submitForm(form("ada@example.TEST", false, "second"));
      assertThat(last.contactCreated()).isFalse();
      assertThat(last.contactId()).isEqualTo(first.contactId());
      QRecord contact = CrmStore.asUser().get("crm_contact", first.contactId());
      assertThat(contact.getValueString("firstTouchUtmCampaign")).isEqualTo("first");
      assertThat(contact.getValueString("lastTouchUtmCampaign")).isEqualTo("second");
      assertThat(contact.getValueInstant("firstTouchDate")).isEqualTo(before.getValueInstant("firstTouchDate"));
      assertThat(contact.getValueInteger("lifecycleStageId")).isNotNull();
      assertThat(contact.getValueString("firstTouchLandingPage")).isEqualTo("https://example.test/form");
      assertThat(marketing.checkEmailEligibility(first.contactId()).reason()).isEqualTo("no_email_consent");
      QRecord consent = CrmStore.asUser().by("crm_consent_record", "contactId", first.contactId()).get(0);
      assertThat(consent.getValueInteger("consentType")).isEqualTo(5);
      assertThat(consent.getValueInteger("legalBasis")).isEqualTo(2);
      assertThat(consent.getValueInteger("evidenceFormSubmissionId")).isEqualTo(first.submissionId());
      assertThat(CrmStore.asUser().by("crm_activity", "contactId", first.contactId())).hasSize(2);
      CrmStarterData.ensure();
      assertThat(CrmStore.asUser().query("crm_activity_type", null)).hasSize(6);
      assertThat(marketing.listFormSubmissions("first", Instant.EPOCH)).containsExactly(
         new CrmFormSubmissionRef(first.submissionId(), first.contactId(), CrmStore.asUser().get("crm_form_submission", first.submissionId()).getValueInstant("createDate")));
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
      QRecord consent = CrmStore.asUser().by("crm_consent_record", "contactId", result.contactId()).get(0);
      assertThat(consent.getValueString("consentText")).isEqualTo("Send me news");
      assertThat(consent.getValueInteger("legalBasis")).isEqualTo(1);
      for(ActivityKind kind : ActivityKind.values())
      {
         marketing.recordActivity(result.contactId(), kind, "Interaction", "Recorded by an integration");
      }
      marketing.recordActivity(result.contactId(), "Gateway note", "Description");
      assertThat(CrmStore.asUser().by("crm_activity", "contactId", result.contactId())).hasSize(8);
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
         CrmStore.asUser().insert("crm_consent_record", new QRecord().withValue("contactId", id).withValue("consentType", 1)
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
            CrmStore.asUser().update("crm_contact", new QRecord().withValue("id", id).withValue("doNotEmail", true));
            reason = "do_not_email";
         }
         case "unsubscribed" ->
         {
            CrmStore.asUser().update("crm_contact", new QRecord().withValue("id", id).withValue("unsubscribeDate", Instant.now()));
            reason = "unsubscribed";
         }
         case "pending", "bounced" ->
         {
            Integer listId = list("matrix", true);
            CrmStore.asUser().insert("crm_email_list_member", new QRecord().withValue("contactId", id).withValue("emailListId", listId)
               .withValue("email", "matrix@example.test").withValue("status", "pending".equals(state) ? 5 : 3));
            reason = "eligible";
         }
         case "noAddress" ->
         {
            CrmStore.asUser().update("crm_contact", new QRecord().withValue("id", id).withValue("email", null));
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
      assertThat(eligibility.eligible()).isEqualTo(Set.of("granted", "pending", "bounced").contains(state));
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
      QRecord member = CrmStore.asUser().query("crm_email_list_member", null).get(0);
      Integer contactId = member.getValueInteger("contactId");
      assertThat(member.getValue("subscribedDate")).isNull();
      assertThat(member.getValueString("confirmationTokenHash")).isEqualTo(CrmTokens.hash(oldToken)).isNotEqualTo(oldToken);
      assertThat(member.getValueString("consentText")).isEqualTo("Send me news");
      assertThat(CrmStore.asUser().by("crm_consent_record", "contactId", contactId)).isEmpty();
      assertThat(marketing.resolveRecipients("crm-list:" + listId).get(0).address()).isNull();
      intake.subscribe(request);
      assertThat(transport.token).isNotEqualTo(oldToken);
      assertThatThrownBy(() -> intake.confirm(oldToken)).isInstanceOf(QUserFacingException.class);
      intake.confirm(transport.token);
      intake.confirm(transport.token);
      assertThat(CrmStore.asUser().by("crm_consent_record", "contactId", contactId)).hasSize(1);
      assertThat(CrmStore.asUser().get("crm_email_list_member", member.getValueInteger("id")).getValue("confirmedDate")).isNotNull();
      assertThat(marketing.resolveRecipients("crm-list:" + listId).get(0).address()).isEqualTo("subscriber@example.test");
      assertThat(intake.subscribe(request).status()).isEqualTo(SubscriptionStatus.ALREADY_SUBSCRIBED);
      String url = marketing.buildUnsubscribeUrl(contactId, listId);
      intake.unsubscribe(url.substring(url.lastIndexOf('/') + 1));
      assertThat(CrmStore.asUser().get("crm_contact", contactId).getValue("unsubscribeDate")).isNull();
      assertThat(CrmStore.asUser().get("crm_email_list_member", member.getValueInteger("id")).getValueInteger("status")).isEqualTo(2);
      assertThatThrownBy(() -> intake.confirm(transport.token)).isInstanceOf(QUserFacingException.class);
      marketing.recordUnsubscribe(contactId, null, "Marketing");
      assertThat(CrmStore.asUser().get("crm_contact", contactId).getValue("unsubscribeDate")).isNotNull();
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
      CrmStore.asUser().update("crm_email_list", new QRecord().withValue("id", inactiveId).withValue("isActive", false));
      assertThat(marketing.recipientScopeOptions()).containsExactly(new RecipientScopeOption("crm-list:" + activeId, "immediate"));
      assertThat(intake.subscribe(signup("immediate")).status()).isEqualTo(SubscriptionStatus.SUBSCRIBED);
      Integer id = CrmStore.asUser().query("crm_contact", null).get(0).getValueInteger("id");
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
      assertThat(transport.token).isNull();
      assertThatThrownBy(() -> intake.subscribe(signup("inactive"))).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> intake.subscribe(signup("absent"))).isInstanceOf(QUserFacingException.class);
      CrmStore.asUser().update("crm_email_list", new QRecord().withValue("id", activeId).withValue("isActive", false));
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
    ** Validates public input before writing.
    *******************************************************************************/
   @Test
   void validatesBeforeWriting() throws Exception
   {
      assertThatThrownBy(() -> intake.submitForm(null)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> intake.subscribe(null)).isInstanceOf(QUserFacingException.class);
      assertThatThrownBy(() -> intake.submitForm(form("invalid", false, "x"))).isInstanceOf(QUserFacingException.class);
      assertThat(CrmStore.asUser().query("crm_contact", null)).isEmpty();
   }

   /*******************************************************************************
    ** A later Email consent wins, with id as the deterministic equal-time tie breaker.
    *******************************************************************************/
   @Test
   void latestEmailConsentWinsAndInquiryCannotOverrideIt() throws Exception
   {
      Integer id = intake.submitForm(form("latest@example.test", true, "first")).contactId();
      Instant now = Instant.now().plusSeconds(10);
      CrmStore.asUser().insert("crm_consent_record", new QRecord().withValue("contactId", id).withValue("consentType", 1)
         .withValue("status", 2).withValue("consentDate", now));
      intake.submitForm(form("latest@example.test", false, "reply"));
      assertThat(marketing.checkEmailEligibility(id).reason()).isEqualTo("email_consent_withdrawn");
      CrmStore.asUser().insert("crm_consent_record", new QRecord().withValue("contactId", id).withValue("consentType", 1)
         .withValue("status", 1).withValue("consentDate", now));
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
      assertThat(marketing.resolveRecipients("crm-contact:" + id).get(0).key()).isEqualTo("crm-contact:" + id);
   }



   /*******************************************************************************
    ** A public host session needs no CRM permissions for any intake operation.
    *******************************************************************************/
   @Test
   void intakeRunsWithoutCallerPermissions() throws Exception
   {
      Integer listId = list("public", true);
      list("immediate", false);
      QContext.getQSession().setPermissions(Set.of());
      Integer id = intake.submitForm(form("public@example.test", true, "public")).contactId();
      assertThat(CrmStore.trusted().by("crm_activity", "contactId", id)).hasSize(1);
      assertThat(intake.subscribe(signup("public")).status()).isEqualTo(SubscriptionStatus.PENDING_CONFIRMATION);
      intake.confirm(transport.token);
      QRecord member = CrmStore.trusted().by("crm_email_list_member", "emailListId", listId).get(0);
      assertThat(member.getValueInteger("status")).isEqualTo(CrmListMemberStatus.SUBSCRIBED.getId());
      String token = new CrmTokens(config).issue("unsubscribe", member.getValueInteger("contactId"), listId);
      intake.unsubscribe(token);
      assertThat(CrmStore.trusted().get("crm_email_list_member", member.getValueInteger("id")).getValueInteger("status"))
         .isEqualTo(CrmListMemberStatus.UNSUBSCRIBED.getId());
      intake.unsubscribeByContact(id, null, "Host request");
      assertThat(CrmStore.trusted().get("crm_contact", id).getValue("unsubscribeDate")).isNotNull();
      assertThat(intake.subscribe(signup("immediate")).status()).isEqualTo(SubscriptionStatus.SUBSCRIBED);
      assertThat(QContext.getQSession().getPermissions()).isEmpty();
   }

   /*******************************************************************************
    ** Marketing reads may succeed, but writes still require crm.write.
    *******************************************************************************/
   @Test
   void marketingCannotWriteWithoutCrmWrite() throws Exception
   {
      Integer id = intake.submitForm(form("readonly@example.test", true, "read")).contactId();
      QContext.getQSession().setPermissions(CrmPermissions.ALL.stream().filter(permission -> !CrmPermissions.WRITE.equals(permission)).collect(java.util.stream.Collectors.toSet()));
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
      assertThatThrownBy(() -> marketing.recordActivity(id, ActivityKind.NOTE, "Denied", "No write permission")).isInstanceOf(QException.class);
      assertThatThrownBy(() -> marketing.recordUnsubscribe(id, null, "Denied")).isInstanceOf(QException.class);
      assertThat(CrmStore.trusted().by("crm_activity", "contactId", id)).hasSize(1);
      assertThat(CrmStore.trusted().get("crm_contact", id).getValue("unsubscribeDate")).isNull();
      QContext.getQSession().setPermissions(Set.of());
      assertThatThrownBy(() -> marketing.recordActivity(id, "Denied", "No permissions")).isInstanceOf(QException.class);
   }

   /*******************************************************************************
    ** Membership suppression is specific to the requested list, not the contact.
    *******************************************************************************/
   @Test
   void unsubscribedListDoesNotBlockAnotherListOrDirectSend() throws Exception
   {
      Integer listA = list("list-a", false);
      Integer listB = list("list-b", false);
      intake.subscribe(signup("list-a"));
      Integer id = CrmStore.asUser().query("crm_contact", null).get(0).getValueInteger("id");
      intake.unsubscribeByContact(id, listA, "Only list A");
      intake.subscribe(signup("list-b"));
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
      assertThat(marketing.resolveRecipients("crm-contact:" + id).get(0).address()).isEqualTo("subscriber@example.test");
      assertThat(marketing.resolveRecipients("crm-list:" + listB).get(0).address()).isEqualTo("subscriber@example.test");
      assertThat(marketing.resolveRecipients("crm-list:" + listA).get(0).eligibility().reason()).isEqualTo("list_member_not_subscribed");
      assertThat(marketing.resolveRecipients("crm-list:" + listA).get(0).address()).isNull();
   }

   /*******************************************************************************
    ** Both explicit subscription paths supersede unsubscribe but retain bounce suppression.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void resubscribeClearsGlobalUnsubscribeOnly(boolean doubleOptIn) throws Exception
   {
      list("again", doubleOptIn);
      Integer id = intake.submitForm(form("subscriber@example.test", true, "original")).contactId();
      intake.unsubscribeByContact(id, null, "Global withdrawal");
      CrmStore.asUser().update("crm_contact", new QRecord().withValue("id", id).withValue("doNotEmail", true));
      intake.subscribe(signup("again"));
      if(doubleOptIn)
      {
         assertThat(CrmStore.asUser().get("crm_contact", id).getValue("unsubscribeDate")).isNotNull();
         intake.confirm(transport.token);
      }
      QRecord contact = CrmStore.asUser().get("crm_contact", id);
      assertThat(contact.getValue("unsubscribeDate")).isNull();
      assertThat(contact.getValueBoolean("doNotEmail")).isTrue();
      assertThat(marketing.checkEmailEligibility(id).reason()).isEqualTo("do_not_email");
      CrmStore.asUser().update("crm_contact", new QRecord().withValue("id", id).withValue("doNotEmail", false));
      assertThat(marketing.checkEmailEligibility(id).eligible()).isTrue();
   }

   /*******************************************************************************
    ** Only signed-link operations need token configuration; failure precedes writes.
    *******************************************************************************/
   @Test
   void tokenSecretIsRequiredOnlyForSignedOperations() throws Exception
   {
      list("immediate", false);
      list("double", true);
      config.withTokenSecret(null);
      Integer id = intake.submitForm(form("no-secret@example.test", false, "plain")).contactId();
      intake.unsubscribeByContact(id, null, "Host withdrawal");
      assertThat(CrmStore.asUser().get("crm_contact", id).getValue("unsubscribeDate")).isNotNull();
      assertThat(intake.subscribe(signup("immediate")).status()).isEqualTo(SubscriptionStatus.SUBSCRIBED);
      int submissions = CrmStore.asUser().query("crm_form_submission", null).size();
      assertThatThrownBy(() -> intake.subscribe(signup("double"))).hasMessageContaining("withTokenSecret");
      assertThat(CrmStore.asUser().query("crm_form_submission", null)).hasSize(submissions);
      assertThatThrownBy(() -> intake.confirm("token")).hasMessageContaining("withTokenSecret");
      assertThatThrownBy(() -> intake.unsubscribe("token")).hasMessageContaining("withTokenSecret");
      assertThatThrownBy(() -> marketing.buildUnsubscribeUrl(id, null)).hasMessageContaining("withTokenSecret");
   }

   /*******************************************************************************
    ** Every public form kind maps to a CRM enum, with typed conversion and consent evidence.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(FormType.class)
   void formKindsPersistEnumIds(FormType type) throws Exception
   {
      CrmFormRequest request = new CrmFormRequest("typed", type, "Ada", "Test", "typed@example.test", null, null, null,
         false, "Reply", null, null, null, null, null, null, null, null, null);
      CrmFormResult result = intake.submitForm(request);
      CrmFormType expected = java.util.Map.of(FormType.CONTACT, CrmFormType.CONTACT, FormType.DEMO, CrmFormType.DEMO_REQUEST,
         FormType.NEWSLETTER, CrmFormType.NEWSLETTER, FormType.OTHER, CrmFormType.GENERAL).get(type);
      QRecord submission = CrmStore.asUser().get("crm_form_submission", result.submissionId());
      assertThat(submission.getValueInteger("formType")).isEqualTo(expected.getId());
      assertThat(submission.getValueInteger("conversionStatus")).isEqualTo(CrmFormConversionStatus.CONVERTED_TO_CONTACT.getId());
      QRecord consent = CrmStore.asUser().by("crm_consent_record", "contactId", result.contactId()).get(0);
      assertThat(consent.getValueInteger("consentType")).isEqualTo(CrmConsentType.REPLY_TO_INQUIRY.getId());
      assertThat(consent.getValueInteger("status")).isEqualTo(CrmConsentStatus.GRANTED.getId());
      assertThat(consent.getValueInteger("legalBasis")).isEqualTo(CrmLegalBasis.LEGITIMATE_INTEREST.getId());
   }

   /*******************************************************************************
    ** Unattributed repeat submissions retain every attribution field but refresh the date.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"absent", "blank", "referrer", "utm", "landing"})
   void lastTouchChangesOnlyWithAttribution(String mode) throws Exception
   {
      Integer id = intake.submitForm(form("touch@example.test", false, "original")).contactId();
      Instant earlier = Instant.parse("2020-01-01T00:00:00Z");
      CrmStore.asUser().update("crm_contact", new QRecord().withValue("id", id).withValue("lastTouchDate", earlier));
      QRecord before = CrmStore.asUser().get("crm_contact", id);
      String empty = "blank".equals(mode) ? " " : null;
      CrmFormRequest request = new CrmFormRequest("repeat", FormType.CONTACT, "Ada", "Test", "touch@example.test", null, null, null,
         false, "Reply", empty, empty, empty, empty, "utm".equals(mode) ? "new-term" : empty,
         "landing".equals(mode) ? "https://example.test/new" : empty, "referrer".equals(mode) ? "https://example.test/referrer" : empty, null, null);
      intake.submitForm(request);
      QRecord after = CrmStore.asUser().get("crm_contact", id);
      for(String suffix : List.of("UtmSource", "UtmMedium", "UtmCampaign", "UtmContent", "UtmTerm", "LandingPage", "Referrer"))
      {
         assertThat(after.getValue("firstTouch" + suffix)).isEqualTo(before.getValue("firstTouch" + suffix));
         if(!Set.of("utm", "landing").contains(mode))
         {
            assertThat(after.getValue("lastTouch" + suffix)).isEqualTo(before.getValue("lastTouch" + suffix));
         }
      }
      if("utm".equals(mode))
      {
         assertThat(after.getValueString("lastTouchUtmTerm")).isEqualTo("new-term");
         assertThat(after.getValue("lastTouchUtmCampaign")).isNull();
      }
      if("landing".equals(mode))
      {
         assertThat(after.getValueString("lastTouchLandingPage")).isEqualTo("https://example.test/new");
         assertThat(after.getValue("lastTouchUtmCampaign")).isNull();
      }
      assertThat(after.getValueInstant("firstTouchDate")).isEqualTo(before.getValueInstant("firstTouchDate"));
      assertThat(after.getValueInstant("lastTouchDate")).isAfter(earlier);
   }

   /*******************************************************************************
    ** Public intake rotates owners using the shared rules and notifies both audiences.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"system", "anonymous"})
   void intakeAutoAssignsNewUnownedContactsAndNotifiesOwner(String user) throws Exception
   {
      Integer ruleId = CrmStore.asUser().insert("crm_assignment_rule", new QRecord().withValue("name", "Intake round robin")
         .withValue("entityType", CrmAssignableEntity.CONTACT.getId()).withValue("assignStrategy", CrmAssignStrategy.ROUND_ROBIN.getId())
         .withValue("isActive", true).withValue("sortOrder", 1)).getValueInteger("id");
      for(int i = 0; i < 2; i++)
      {
         CrmStore.asUser().insert("crm_assignment_rule_member", new QRecord().withValue("assignmentRuleId", ruleId)
            .withValue("userId", "sales-" + i).withValue("sortOrder", i).withValue("isActive", true));
      }
      list("assigned", false);
      CapturingSink.signals.clear();
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(CapturingSink.class));
      QContext.getQSession().setIdReference(user);
      QContext.getQSession().setPermissions(Set.of());
      CrmFormResult first = intake.submitForm(form("first@example.test", false, "round-robin"));
      CrmFormResult second = intake.submitForm(form("second@example.test", false, "round-robin"));
      assertThat(CrmStore.trusted().get("crm_contact", first.contactId()).getValueString("ownerUserId")).isEqualTo("sales-0");
      assertThat(CrmStore.trusted().get("crm_contact", second.contactId()).getValueString("ownerUserId")).isEqualTo("sales-1");
      assertThat(CapturingSink.signals).filteredOn(signal -> ("crm.lead.received:" + first.submissionId()).equals(signal.getSourceKey()))
         .extracting(NotificationSignal::getAudience).containsExactlyInAnyOrder(NotificationAudience.permission("crm.read"), NotificationAudience.user("sales-0"));
      assertThat(CapturingSink.signals).filteredOn(signal -> ("crm.lead.received:" + second.submissionId()).equals(signal.getSourceKey()))
         .extracting(NotificationSignal::getAudience).containsExactlyInAnyOrder(NotificationAudience.permission("crm.read"), NotificationAudience.user("sales-1"));
      intake.submitForm(form("second@example.test", false, "repeat"));
      assertThat(CrmStore.trusted().get("crm_contact", second.contactId()).getValueString("ownerUserId")).isEqualTo("sales-1");
      intake.subscribe(signup("assigned"));
      assertThat(CrmStore.trusted().by("crm_contact", "email", "subscriber@example.test").get(0).getValueString("ownerUserId")).isEqualTo("sales-0");
      QContext.getQSession().setIdReference("named-owner");
      Integer owned = intake.submitForm(form("owned@example.test", false, "owned")).contactId();
      assertThat(CrmStore.trusted().get("crm_contact", owned).getValueString("ownerUserId")).isEqualTo("sales-1");
      CrmStore.trusted().update("crm_contact", new QRecord().withValue("id", first.contactId()).withValue("ownerUserId", "system"));
      intake.submitForm(form("first@example.test", false, "existing-unowned"));
      assertThat(CrmStore.trusted().get("crm_contact", first.contactId()).getValueString("ownerUserId")).isEqualTo("system");
   }

   /*******************************************************************************
    ** Capture the real QAlerts dispatch without installing a notification backend.
    *******************************************************************************/
   public static class CapturingSink extends LoggingAlertSink
   {
      private static final List<NotificationSignal> signals = new ArrayList<>();

      @Override
      public void notify(NotificationSignal signal)
      {
         signals.add(signal);
      }
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
      return (CrmStore.asUser().insert("crm_email_list", new QRecord().withValue("listKey", key).withValue("name", key)
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
