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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qbits.crm.actions.CrmAction;
import com.kingsrook.qbits.crm.actions.CrmActions;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
import com.kingsrook.qbits.crm.core.model.enums.CrmSequenceStepType;
import com.kingsrook.qbits.crm.deals.CrmDealNotifications;
import com.kingsrook.qbits.crm.email.CrmEmailProcessIdentity;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.email.CrmEmails;
import com.kingsrook.qbits.crm.email.processes.ExpireConsentsProcess;
import com.kingsrook.qbits.crm.email.processes.ProcessSequenceStepsProcess;
import com.kingsrook.qbits.crm.email.processes.SyncEmailDeliveryProcess;
import com.kingsrook.qbits.crm.scoring.processes.CalculateLeadScoresProcess;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.LoggingAlertSink;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** P4 contracts exercised through memory DML, fake transport and real QAlerts dispatch.
 *******************************************************************************/
class PlatformReuseTest extends BaseTest
{
   private FakeTransport transport;

   /*******************************************************************************
    ** Keep effects local to this test.
    *******************************************************************************/
   @BeforeEach
   void configure() throws Exception
   {
      transport = new FakeTransport();
      CrmApi.config().withPublicBaseUrl("https://crm.example.test").withTokenSecret("test-only-signing-key");
      Sink.notifications.clear();
      Sink.alerts.clear();
      Sink.resolutions.clear();
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(Sink.class));
   }

   /*******************************************************************************
    ** Send action renders and records transactional delivery.
    *******************************************************************************/
   @Test
   void sendActionRendersAndRecordsTransactionalDelivery() throws Exception
   {
      QRecord contact = contact(1).withValue("companyId", 1);
      put("crm_company", new QRecord().withValue("id", 1).withValue("name", "Example"));
      put("crm_contact", contact);
      put("crm_email_template", new QRecord().withValue("id", 1).withValue("subject", "Hello {{contact.firstName}}")
         .withValue("bodyText", "{{contact.lastName}} at {{company.name}} / {{owner.name}} / {{unsubscribeUrl}}"));
      CrmAction.Load load = load();
      RunBackendStepInput input = new RunBackendStepInput().withProcessName("crm_send_email").withRecords(List.of(contact))
         .addValue("emailTemplateId", 1).addValue("crmEmailProcessUUID", "run-123");
      load.runOnePage(input, new RunBackendStepOutput());
      assertThat(transport.key).isEqualTo("crm:email:1:run-123");
      assertThat(transport.marketing).isFalse();
      assertThat(transport.subject).isEqualTo("Hello Alice");
      assertThat(transport.body).startsWith("Smith at Example / owner / https://crm.example.test/qqq/hooks/crm/unsubscribe/");
      assertThat(rows("crm_email_message")).singleElement().satisfies(row ->
      {
         assertThat(row.getValueInteger("status")).isEqualTo(CrmEmailStatus.QUEUED.getId());
         assertThat(row.getValueString("emailMessageId")).isEqualTo("provider-1");
         assertThat(row.getValueString("idempotencyKey")).isEqualTo(transport.key);
      });
      assertActivity("EMAIL");
      load.runOnePage(input, new RunBackendStepOutput());
      assertThat(transport.sends).isEqualTo(1);
      assertThat(rows("crm_activity")).hasSize(1);
   }

   /*******************************************************************************
    ** Overrides and missing templates and literal merge values.
    *******************************************************************************/
   @Test
   void overridesAndMissingTemplatesAndLiteralMergeValues() throws Exception
   {
      QRecord contact = contact(1).withValue("firstName", "{{owner.name}}");
      assertThat(CrmEmails.render("{{contact.firstName}} {{contact.lastName}}", contact)).isEqualTo("{{owner.name}} Smith");
      assertThrows(QException.class, () -> CrmEmails.send(transport, contact, 999, null, null, "email", "missing", null));
      assertThrows(QException.class, () -> CrmEmails.send(transport, contact, null, "subject", "", "email", "missing", null));
      put("crm_email_template", new QRecord().withValue("id", 1).withValue("subject", "template").withValue("bodyText", "template"));
      CrmEmails.send(transport, contact, 1, "override", "body", "email", "1:run", null);
      assertThat(transport.subject).isEqualTo("override");
      assertThat(transport.body).isEqualTo("body");
   }

   /*******************************************************************************
    ** Skips opt out missing address and unavailable transport.
    *******************************************************************************/
   @Test
   void skipsOptOutMissingAddressAndUnavailableTransport() throws Exception
   {
      List<QRecord> contacts = List.of(contact(1).withValue("doNotEmail", true), contact(2).withValue("email", null), contact(3));
      for(QRecord contact : contacts)
      {
         put("crm_contact", contact);
      }
      transport.ready = false;
      CrmAction.Load load = load();
      RunBackendStepOutput output = new RunBackendStepOutput();
      load.runOnePage(new RunBackendStepInput().withProcessName("crm_send_email").withRecords(contacts), output);
      assertThat(load.getProcessSummary(output, true)).hasSize(3);
      assertThat(transport.sends).isZero();
      assertThat(rows("crm_email_message")).isEmpty();
      assertThat(rows("crm_activity")).isEmpty();
   }

   /*******************************************************************************
    ** Process identity uses core uuid.
    *******************************************************************************/
   @Test
   void processIdentityUsesCoreUuid() throws Exception
   {
      var output = new RunProcessAction().execute(new RunProcessInput().withProcessName("crm_send_email"));
      assertThat(output.getException()).isEmpty();
      assertThat(output.getValues().get("crmEmailProcessUUID")).isEqualTo(output.getProcessUUID());
      var listener = new CrmEmailProcessIdentity();
      assertThat(listener.appliesTo("crm_send_email")).isTrue();
      assertThat(listener.appliesTo("crm_other")).isFalse();
      listener.onProcessCompleted(new RunProcessInput(), output);
      listener.onProcessFailed(new RunProcessInput(), new Exception("test"));
      assertThat(CrmActions.inputFields(CrmActions.action("crm_send_email"))).extracting(field -> field.getName())
         .containsExactly("emailTemplateId", "subject", "body");
   }

   /*******************************************************************************
    ** Active sequence sends marketing and advances once.
    *******************************************************************************/
   @Test
   void activeSequenceSendsMarketingAndAdvancesOnce() throws Exception
   {
      seedSequence(true, true);
      sequence();
      assertThat(transport.key).isEqualTo("crm:seq:1:1");
      assertThat(transport.marketing).isTrue();
      assertThat(rows("crm_email_message")).hasSize(1);
      assertActivity("SEQUENCE_STEP");
      assertThat(GetAction.execute("crm_sequence_enrollment", 1).getValueInteger("status")).isEqualTo(CrmEnrollmentStatus.COMPLETED.getId());
      sequence();
      assertThat(transport.sends).isEqualTo(1);
   }

   /*******************************************************************************
    ** Paused sequence does not advance or send.
    *******************************************************************************/
   @Test
   void pausedSequenceDoesNotAdvanceOrSend() throws Exception
   {
      seedSequence(false, true);
      sequence();
      assertThat(transport.sends).isZero();
      assertThat(GetAction.execute("crm_sequence_enrollment", 1).getValueInteger("currentStepNumber")).isZero();
      assertThat(Sink.alerts).isEmpty();
   }

   /*******************************************************************************
    ** Missing consent pauses enrollment with reason.
    *******************************************************************************/
   @Test
   void missingConsentPausesEnrollmentWithReason() throws Exception
   {
      seedSequence(true, false);
      sequence();
      QRecord enrollment = GetAction.execute("crm_sequence_enrollment", 1);
      assertThat(enrollment.getValueInteger("status")).isEqualTo(CrmEnrollmentStatus.PAUSED.getId());
      assertThat(enrollment.getValueString("lastFailureMessage")).isEqualTo("no_email_consent");
      assertThat(enrollment.getValueInteger("failureCount")).isZero();
      assertThat(transport.sends).isZero();
   }

   /*******************************************************************************
    ** Transport failures raise on third attempt.
    *******************************************************************************/
   @Test
   void transportFailuresRaiseOnThirdAttempt() throws Exception
   {
      seedSequence(true, true);
      transport.fail = true;
      sequence();
      sequence();
      sequence();
      assertThat(GetAction.execute("crm_sequence_enrollment", 1).getValueInteger("failureCount")).isEqualTo(3);
      assertThat(GetAction.execute("crm_sequence_enrollment", 1).getValueInteger("status")).isEqualTo(CrmEnrollmentStatus.FAILED.getId());
      assertThat(Sink.alerts).singleElement().satisfies(signal ->
      {
         assertThat(signal.getAlertTypeName()).isEqualTo("crm.sequence.failing");
         assertThat(signal.getDedupKey()).isEqualTo("enrollment:1");
      });
      assertThat(rows("crm_email_message")).isEmpty();
   }

   /*******************************************************************************
    ** Unavailable transport raises then resolves without failing enrollment.
    *******************************************************************************/
   @Test
   void unavailableTransportRaisesThenResolvesWithoutFailingEnrollment() throws Exception
   {
      seedSequence(true, true);
      transport.ready = false;
      sequence();
      assertThat(Sink.alerts).singleElement().satisfies(signal ->
      {
         assertThat(signal.getAlertTypeName()).isEqualTo("crm.email.unavailable");
         assertThat(signal.getDedupKey()).isEqualTo("transport");
      });
      assertThat(GetAction.execute("crm_sequence_enrollment", 1).getValueInteger("failureCount")).isZero();
      transport.ready = true;
      sequence();
      assertThat(Sink.resolutions).hasSize(1);
      assertThat(transport.sends).isEqualTo(1);
   }

   /*******************************************************************************
    ** Delivery sync uses recent nonfinal messages and preserves dates.
    *******************************************************************************/
   @Test
   void deliverySyncUsesRecentNonfinalMessagesAndPreservesDates() throws Exception
   {
      contact(1);
      message(1, CrmEmailStatus.SENT, Instant.now());
      message(2, CrmEmailStatus.SENT, Instant.now().minus(31, ChronoUnit.DAYS));
      message(3, CrmEmailStatus.FAILED, Instant.now());
      Instant now = Instant.now();
      transport.deliveries.put("key:1", new CrmEmailTransport.Delivery("provider", CrmEmailStatus.CLICKED, now, now, now, now, null));
      sync();
      assertThat(transport.lookups).containsExactly("key:1");
      QRecord message = GetAction.execute("crm_email_message", 1);
      assertThat(message.getValueInstant("deliveredDate")).isEqualTo(now);
      assertThat(message.getValueInstant("firstOpenedDate")).isEqualTo(now);
      assertThat(message.getValueInstant("firstClickedDate")).isEqualTo(now);
      transport.deliveries.put("key:1", new CrmEmailTransport.Delivery("provider", CrmEmailStatus.CLICKED, null, null, null, null, null));
      sync();
      assertThat(GetAction.execute("crm_email_message", 1).getValueInstant("firstOpenedDate")).isEqualTo(now);
   }

   /*******************************************************************************
    ** Bounce and complaint withdraw once and suppress contact.
    *******************************************************************************/
   @Test
   void bounceAndComplaintWithdrawOnceAndSuppressContact() throws Exception
   {
      for(int id = 1; id <= 2; id++)
      {
         contact(id);
         message(id, CrmEmailStatus.DELIVERED, Instant.now());
         CrmEmailStatus status = id == 1 ? CrmEmailStatus.BOUNCED : CrmEmailStatus.COMPLAINED;
         transport.deliveries.put("key:" + id, new CrmEmailTransport.Delivery("provider", status, null, null, null, null, Instant.now()));
      }
      sync();
      sync();
      assertThat(rows("crm_contact")).allSatisfy(row -> assertThat(row.getValueBoolean("doNotEmail")).isTrue());
      assertThat(rows("crm_consent_record")).hasSize(2).allSatisfy(row ->
      {
         assertThat(row.getValueInteger("consentType")).isEqualTo(CrmConsentType.EMAIL.getId());
         assertThat(row.getValueInteger("status")).isEqualTo(CrmConsentStatus.WITHDRAWN.getId());
      });
      assertThat(rows("crm_consent_record")).extracting(row -> row.getValueString("source")).containsExactlyInAnyOrder("Hard bounce", "Complaint");
   }

   /*******************************************************************************
    ** Absent email qbit is no op.
    *******************************************************************************/
   @Test
   void absentEmailQbitIsNoOp() throws Exception
   {
      message(1, CrmEmailStatus.SENT, Instant.now());
      RunBackendStepOutput output = new RunBackendStepOutput();
      new SyncEmailDeliveryProcess().run(new RunBackendStepInput(), output);
      assertThat(output.getValueInteger("updatedCount")).isZero();
      assertThat(new CrmEmailTransport(CrmApi.config()).lookup("key:1")).isEmpty();
   }

   /*******************************************************************************
    ** Stale pending members expire without new consent rows.
    *******************************************************************************/
   @Test
   void stalePendingMembersExpireWithoutNewConsentRows() throws Exception
   {
      for(int id = 1; id <= 3; id++)
      {
         put("crm_email_list_member", new QRecord().withValue("id", id).withValue("emailListId", 1).withValue("email", "a" + id + "@example.test")
            .withValue("status", id == 3 ? CrmListMemberStatus.SUBSCRIBED.getId() : CrmListMemberStatus.PENDING_CONFIRMATION.getId())
            .withValue("confirmationSentDate", Instant.now().minus(id == 2 ? 13 : 15, ChronoUnit.DAYS)).withValue("confirmationTokenHash", "hash"));
      }
      put("crm_consent_record", new QRecord().withValue("id", 1).withValue("expiryDate", Instant.now().minusSeconds(1)));
      RunBackendStepOutput output = new RunBackendStepOutput();
      new ExpireConsentsProcess().run(new RunBackendStepInput(), output);
      assertThat(output.getValueInteger("expiredPendingCount")).isEqualTo(1);
      assertThat(output.getValueInteger("expiredConsentCount")).isEqualTo(1);
      assertThat(GetAction.execute("crm_email_list_member", 1).getValue("confirmationTokenHash")).isNull();
      assertThat(GetAction.execute("crm_email_list_member", 1).getValueInteger("status")).isEqualTo(CrmListMemberStatus.UNSUBSCRIBED.getId());
      assertThat(GetAction.execute("crm_email_list_member", 2).getValueString("confirmationTokenHash")).isEqualTo("hash");
      assertThat(rows("crm_consent_record")).hasSize(1);
   }

   /*******************************************************************************
    ** Schedules require scheduler and can be disabled.
    *******************************************************************************/
   @Test
   void schedulesRequireSchedulerAndCanBeDisabled() throws Exception
   {
      Map<String, Integer> intervals = Map.of("crm_process_sequence_steps", 300, "crm_calculate_lead_scores", 3600,
         "crm_sync_email_delivery", 900, "crm_expire_consents", 86400);
      for(String name : intervals.keySet())
      {
         assertThat(QContext.getQInstance().getProcess(name).getSchedule()).isNull();
      }
      CrmQBitConfig config = new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME).withSchedulerName("jobs");
      var output = new CrmQBitProducer().withQBitConfig(config).produce(new com.kingsrook.qqq.backend.core.model.metadata.QInstance());
      for(var process : output.getEach(com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData.class))
      {
         if(intervals.containsKey(process.getName()))
         {
            assertThat(process.getSchedule().getRepeatSeconds()).isEqualTo(intervals.get(process.getName()));
            assertThat(process.getSchedule().getSchedulerName()).isEqualTo("jobs");
         }
      }
      assertThat(config.getSchedulesEnabled()).isTrue();
      output = new CrmQBitProducer().withQBitConfig(config.withSchedulesEnabled(false)).produce(new com.kingsrook.qqq.backend.core.model.metadata.QInstance());
      assertThat(output.getEach(com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData.class))
         .allSatisfy(process -> assertThat(process.getSchedule()).isNull());
   }

   /*******************************************************************************
    ** Collaboration and alerts use core metadata.
    *******************************************************************************/
   @Test
   void collaborationAndAlertsUseCoreMetadata() throws Exception
   {
      for(String table : List.of("crm_contact", "crm_company", "crm_deal", "crm_activity"))
      {
         var features = RecordFeaturesMetaData.of(QContext.getQInstance().getTable(table));
         for(RecordFeature feature : List.of(RecordFeature.TAGS, RecordFeature.COMMENTS, RecordFeature.ATTACHMENTS,
            RecordFeature.FOLLOW, RecordFeature.TIMELINE, RecordFeature.RECENTS, RecordFeature.FAVORITES))
         {
            assertThat(features.getEnabled().get(feature)).isTrue();
         }
         assertThat(features.getOwnership().getOwnerFieldName()).isEqualTo("ownerUserId");
         assertThat(features.getEnabled().get(RecordFeature.TASKS)).isEqualTo("crm_activity".equals(table) ? null : true);
         assertThat(features.getEnabled().get(RecordFeature.ASSIGNMENT)).isEqualTo("crm_activity".equals(table) ? null : true);
      }
      assertThat(AlertsInstanceMetaData.of(QContext.getQInstance()).getAlertTypes()).containsKeys("crm.sequence.failing", "crm.email.unavailable",
         "crm.lead.assigned", "crm.deal.won", "crm.deal.rotting", "crm.task.due");
   }

   /*******************************************************************************
    ** Scoring uses dates incremental changes and daily full pass.
    *******************************************************************************/
   @Test
   void scoringUsesDatesIncrementalChangesAndDailyFullPass() throws Exception
   {
      put("crm_contact", contact(1).withValue("lastActivityDate", Instant.now().minus(20, ChronoUnit.DAYS)).withValue("modifyDate", Instant.now().minusSeconds(300)));
      put("crm_contact", contact(2).withValue("lastActivityDate", Instant.now().minus(2, ChronoUnit.DAYS)).withValue("modifyDate", Instant.now().minusSeconds(300)));
      rule(1, "lastActivityDate", CrmScoreOperator.DAYS_AGO_GT, "10", -5);
      rule(2, "lastActivityDate", CrmScoreOperator.DAYS_AGO_LT, "10", 10);
      assertThat(scores(false).getValueInteger("totalContacts")).isEqualTo(2);
      assertThat(GetAction.execute("crm_contact", 1).getValueInteger("leadScore")).isEqualTo(-5);
      assertThat(GetAction.execute("crm_contact", 2).getValueInteger("leadScore")).isEqualTo(10);
      assertThat(scores(false).getValueInteger("totalContacts")).isZero();
      CrmRecords.update("crm_contact", new QRecord().withValue("id", 2).withValue("firstName", "Changed"));
      assertThat(scores(false).getValueInteger("totalContacts")).isEqualTo(1);
      assertThat(scores(true).getValueInteger("totalContacts")).isEqualTo(2);
      QRecord state = rows("crm_job_state").getFirst();
      CrmRecords.update("crm_job_state", state.withValue("lastFullPass", Instant.now().minus(25, ChronoUnit.HOURS)));
      assertThat(scores(false).getValueInteger("totalContacts")).isEqualTo(2);
   }

   /*******************************************************************************
    ** Scoring handles calendar dates and malformed rules.
    *******************************************************************************/
   @Test
   void scoringHandlesCalendarDatesAndMalformedRules() throws Exception
   {
      put("crm_contact", contact(1).withValue("lastName", LocalDate.now().minusDays(30).toString()));
      rule(1, "lastName", CrmScoreOperator.DAYS_AGO_GT, "10", 7);
      rule(2, "firstName", CrmScoreOperator.DAYS_AGO_LT, "bad", 100);
      rule(3, "email", CrmScoreOperator.DAYS_AGO_GT, "10", 100);
      scores(true);
      assertThat(GetAction.execute("crm_contact", 1).getValueInteger("leadScore")).isEqualTo(7);
   }

   /*******************************************************************************
    ** Insert assignment preserves owners and notifies.
    *******************************************************************************/
   @Test
   void insertAssignmentPreservesOwnersAndNotifies() throws Exception
   {
      for(CrmAssignableEntity entity : CrmAssignableEntity.values())
      {
         put("crm_assignment_rule", new QRecord().withValue("id", entity.getId()).withValue("name", "Assign")
            .withValue("entityType", entity.getId()).withValue("assignStrategy", CrmAssignStrategy.SPECIFIC_USER.getId())
            .withValue("assignToUserId", "assigned").withValue("isActive", true).withValue("sortOrder", 1));
      }
      QRecord inserted = CrmRecords.insert("crm_contact", new QRecord().withValue("firstName", "New").withValue("lastName", "Contact"));
      assertThat(GetAction.execute("crm_contact", inserted.getValue("id")).getValueString("ownerUserId")).isEqualTo("assigned");
      CrmRecords.insert("crm_contact", new QRecord().withValue("firstName", "Already").withValue("lastName", "Owned").withValue("ownerUserId", "explicit"));
      put("crm_pipeline", new QRecord().withValue("id", 1).withValue("name", "Sales"));
      put("crm_pipeline_stage", new QRecord().withValue("id", 1).withValue("pipelineId", 1).withValue("name", "Open"));
      QRecord deal = CrmRecords.insert("crm_deal", new QRecord().withValue("name", "New deal").withValue("pipelineId", 1)
         .withValue("pipelineStageId", 1).withValue("ownerUserId", "system").withValue("currencyCode", "USD"));
      assertThat(GetAction.execute("crm_deal", deal.getValue("id")).getValueString("ownerUserId")).isEqualTo("assigned");
      assertThat(Sink.notifications).hasSize(2).allSatisfy(signal ->
      {
         assertThat(signal.getSourceName()).isEqualTo("crm.lead.assigned");
         assertThat(signal.getAudience()).isEqualTo(NotificationAudience.user("assigned"));
      });
   }

   /*******************************************************************************
    ** Won and rotting notifications target owner and deduplicate stage.
    *******************************************************************************/
   @Test
   void wonAndRottingNotificationsTargetOwnerAndDeduplicateStage() throws Exception
   {
      put("crm_pipeline_stage", new QRecord().withValue("id", 1).withValue("name", "Open").withValue("pipelineId", 1).withValue("rotDays", 5));
      put("crm_pipeline_stage", new QRecord().withValue("id", 2).withValue("name", "Won").withValue("pipelineId", 1).withValue("isClosedWon", true));
      put("crm_deal", new QRecord().withValue("id", 1).withValue("name", "Old deal").withValue("pipelineId", 1)
         .withValue("pipelineStageId", 1).withValue("ownerUserId", "owner").withValue("stageEnteredDate", Instant.now().minus(10, ChronoUnit.DAYS)));
      CrmDealNotifications.checkRotting();
      CrmDealNotifications.checkRotting();
      assertThat(Sink.notifications).singleElement().satisfies(signal -> assertThat(signal.getSourceKey()).isEqualTo("deal:1:stage:1"));
      CrmRecords.update("crm_deal", new QRecord().withValue("id", 1).withValue("pipelineStageId", 2));
      assertThat(Sink.notifications).hasSize(2);
      assertThat(Sink.notifications.getLast().getSourceName()).isEqualTo("crm.deal.won");
      assertThat(Sink.notifications.getLast().getAudience()).isEqualTo(NotificationAudience.user("owner"));
      CrmRecords.update("crm_deal", new QRecord().withValue("id", 1).withValue("pipelineStageId", 2));
      CrmDealNotifications.checkRotting();
      assertThat(Sink.notifications).hasSize(2);
   }


   /*******************************************************************************
    ** Company loading scales with pages, not contacts or rules.
    *******************************************************************************/
   @Test
   void scoringLoadsCompaniesOncePerPage() throws Exception
   {
      put("crm_company", new QRecord().withValue("id", 1).withValue("name", "Example"));
      for(int id = 1; id <= 201; id++)
      {
         put("crm_contact", contact(id).withValue("companyId", 1));
      }
      rule(1, "company.name", CrmScoreOperator.EQUALS, "Example", 3);
      rule(2, "company.name", CrmScoreOperator.IS_SET, null, 2);
      CompanyReads.count = 0;
      QContext.getQInstance().getTable("crm_company").withCustomizer(
         com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers.POST_QUERY_RECORD, new QCodeReference(CompanyReads.class));
      assertThat(scores(true).getValueInteger("updatedCount")).isEqualTo(201);
      // Two explicit page loads, plus at most two core audit-label translations.
      assertThat(CompanyReads.count).isBetween(2, 4);
      assertThat(GetAction.execute("crm_contact", 201).getValueInteger("leadScore")).isEqualTo(5);
   }

   /*******************************************************************************
    ** Failed scoring must leave the successful checkpoint unchanged.
    *******************************************************************************/
   @Test
   void scoringFailureDoesNotAdvanceCheckpoint() throws Exception
   {
      contact(1);
      scores(true);
      Instant checkpoint = rows("crm_job_state").getFirst().getValueInstant("lastSuccess");
      rule(1, "firstName", CrmScoreOperator.IS_SET, null, 1);
      QContext.getQInstance().getTable("crm_contact").withCustomizer(
         com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(RejectScore.class));
      assertThrows(QException.class, () -> scores(true));
      assertThat(rows("crm_job_state").getFirst().getValueInstant("lastSuccess")).isEqualTo(checkpoint);
   }

   /*******************************************************************************
    ** Retry repairs a missing activity without sending the persisted message again.
    *******************************************************************************/
   @Test
   void retryRepairsActivityAfterPartialRecording() throws Exception
   {
      QRecord contact = contact(1);
      put("crm_email_message", new QRecord().withValue("id", 1).withValue("contactId", 1).withValue("subject", "Already sent")
         .withValue("idempotencyKey", "crm:email:1:retry").withValue("status", CrmEmailStatus.SENT.getId()));
      CrmEmails.send(transport, contact, null, "Already sent", "body", "email", "1:retry", null);
      assertThat(transport.sends).isZero();
      assertActivity("EMAIL");
      assertThat(GetAction.execute("crm_email_message", 1).getValue("activityId")).isNotNull();
   }

   /*******************************************************************************
    ** Count actual backend company queries.
    *******************************************************************************/
   public static class CompanyReads extends com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer
   {
      private static int count;

      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         count++;
         return (records);
      }
   }

   /*******************************************************************************
    ** Return a record-level error to exercise checked background writes.
    *******************************************************************************/
   public static class RejectScore extends com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer
   {
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         records.forEach(row -> row.addError(new com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage("Rejected score")));
         return (records);
      }
   }

   /*******************************************************************************
    ** Send action with a local fake.
    *******************************************************************************/
   private CrmAction.Load load()
   {
      return (new CrmAction.Load()
      {
         @Override
         protected CrmEmailTransport emailTransport()
         {
            return (transport);
         }
      });
   }

   /*******************************************************************************
    ** Sequence runner with a local fake.
    *******************************************************************************/
   private void sequence() throws QException
   {
      new ProcessSequenceStepsProcess()
      {
         @Override
         protected CrmEmailTransport emailTransport()
         {
            return (transport);
         }
      }.run(new RunBackendStepInput(), new RunBackendStepOutput());
   }

   /*******************************************************************************
    ** Sync with a local fake.
    *******************************************************************************/
   private void sync() throws QException
   {
      new SyncEmailDeliveryProcess()
      {
         @Override
         protected CrmEmailTransport emailTransport()
         {
            return (transport);
         }
      }.run(new RunBackendStepInput(), new RunBackendStepOutput());
   }

   /*******************************************************************************
    ** Run score work using normal QQQ actions.
    *******************************************************************************/
   private RunBackendStepOutput scores(boolean full) throws QException
   {
      RunBackendStepOutput output = new RunBackendStepOutput();
      new CalculateLeadScoresProcess().run(new RunBackendStepInput().addValue("fullRecalc", full), output);
      return (output);
   }

   /*******************************************************************************
    ** Contact fixture.
    *******************************************************************************/
   private QRecord contact(int id) throws QException
   {
      QRecord row = new QRecord().withValue("id", id).withValue("firstName", "Alice").withValue("lastName", "Smith")
         .withValue("email", "alice" + id + "@example.test").withValue("ownerUserId", "owner");
      put("crm_contact", row);
      return (row);
   }

   /*******************************************************************************
    ** Sequence fixtures use enum identifiers.
    *******************************************************************************/
   private void seedSequence(boolean active, boolean eligible) throws QException
   {
      contact(1);
      put("crm_email_sequence", new QRecord().withValue("id", 1).withValue("name", "Sequence").withValue("isActive", active).withValue("totalSteps", 1));
      put("crm_email_template", new QRecord().withValue("id", 1).withValue("subject", "Hello {{contact.firstName}}").withValue("bodyText", "Welcome"));
      put("crm_sequence_step", new QRecord().withValue("id", 1).withValue("sequenceId", 1).withValue("stepNumber", 1)
         .withValue("stepType", CrmSequenceStepType.EMAIL.getId()).withValue("emailTemplateId", 1));
      put("crm_sequence_enrollment", new QRecord().withValue("id", 1).withValue("sequenceId", 1).withValue("contactId", 1)
         .withValue("status", CrmEnrollmentStatus.ACTIVE.getId()).withValue("currentStepNumber", 0).withValue("failureCount", 0)
         .withValue("nextStepDate", Instant.now().minusSeconds(60)));
      if(eligible)
      {
         put("crm_consent_record", new QRecord().withValue("id", 1).withValue("contactId", 1).withValue("consentType", CrmConsentType.EMAIL.getId())
            .withValue("status", CrmConsentStatus.GRANTED.getId()).withValue("consentDate", Instant.now()));
      }
   }

   /*******************************************************************************
    ** Transport-linked message fixture.
    *******************************************************************************/
   private void message(int id, CrmEmailStatus status, Instant created) throws QException
   {
      put("crm_email_message", new QRecord().withValue("id", id).withValue("contactId", id).withValue("subject", "Test")
         .withValue("idempotencyKey", "key:" + id).withValue("status", status.getId()).withValue("createDate", created));
   }

   /*******************************************************************************
    ** Score rule fixture.
    *******************************************************************************/
   private void rule(int id, String field, CrmScoreOperator operator, String expected, int points) throws QException
   {
      put("crm_lead_score_rule", new QRecord().withValue("id", id).withValue("name", "Rule " + id).withValue("fieldPath", field)
         .withValue("operator", operator.getId()).withValue("fieldValue", expected).withValue("scoreAdjustment", points).withValue("isActive", true));
   }

   /*******************************************************************************
    ** Assert the activity's stable code.
    *******************************************************************************/
   private void assertActivity(String code) throws QException
   {
      QRecord activity = rows("crm_activity").getFirst();
      assertThat(GetAction.execute("crm_activity_type", activity.getValue("activityTypeId")).getValueString("code")).isEqualTo(code);
   }

   /*******************************************************************************
    ** Insert isolated fixture data.
    *******************************************************************************/
   private void put(String table, QRecord row) throws QException
   {
      if(row.getValue("id") != null && GetAction.execute(table, row.getValue("id")) != null)
      {
         MemoryRecordStore.getInstance().update(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(table).withRecord(row), true);
      }
      else
      {
         MemoryRecordStore.getInstance().insert(new InsertInput(table).withRecord(row), true);
      }
   }

   /*******************************************************************************
    ** Query in the current session.
    *******************************************************************************/
   private List<QRecord> rows(String table) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table)).getRecords());
   }

   /*******************************************************************************
    ** Fake transport with independent send and delivery controls.
    *******************************************************************************/
   private static class FakeTransport extends CrmEmailTransport
   {
      private boolean ready = true;
      private boolean fail;
      private boolean marketing;
      private int sends;
      private String key;
      private String subject;
      private String body;
      private final Map<String, Delivery> deliveries = new HashMap<>();
      private final List<String> lookups = new ArrayList<>();

      /*******************************************************************************
       ** No external provider is installed.
       *******************************************************************************/
      FakeTransport()
      {
         super(new CrmQBitConfig());
      }

      @Override
      public CrmReadiness readiness()
      {
         return (ready ? CrmReadiness.QBIT_EMAIL : CrmReadiness.NOT_CONFIGURED);
      }

      @Override
      public Delivery sendMessage(String purpose, String id, Integer contactId, String address, String subject, String body, boolean marketing) throws QException
      {
         if(fail)
         {
            throw (new QException("Fake transport failure"));
         }
         sends++;
         key = "crm:" + purpose + ":" + id;
         this.subject = subject;
         this.body = body;
         this.marketing = marketing;
         return (new Delivery("provider-1", CrmEmailStatus.QUEUED, null, null, null, null, null));
      }

      @Override
      public Optional<Delivery> lookup(String key)
      {
         lookups.add(key);
         return (Optional.ofNullable(deliveries.get(key)));
      }
   }

   /*******************************************************************************
    ** Capture the core dispatch without an alerts backend.
    *******************************************************************************/
   public static class Sink extends LoggingAlertSink
   {
      private static final List<NotificationSignal> notifications = new ArrayList<>();
      private static final List<AlertSignal> alerts = new ArrayList<>();
      private static final List<AlertResolution> resolutions = new ArrayList<>();

      @Override
      public void notify(NotificationSignal signal)
      {
         notifications.add(signal);
      }

      @Override
      public void raise(AlertSignal signal)
      {
         alerts.add(signal);
      }

      @Override
      public void resolve(AlertResolution resolution)
      {
         resolutions.add(resolution);
      }
   }
}
