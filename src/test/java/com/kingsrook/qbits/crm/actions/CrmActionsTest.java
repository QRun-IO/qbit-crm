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


package com.kingsrook.qbits.crm.actions;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Every UI action traverses real ETL Review and Result against persisted data.
 *******************************************************************************/
class CrmActionsTest extends BaseTest
{
   /*******************************************************************************
    ** Cover the complete registered action inventory.
    *******************************************************************************/
   static Stream<String> actions()
   {
      return (CrmActions.ACTIONS.stream().map(CrmActions.Action::name));
   }

   /*******************************************************************************
    ** Review is read-only; Result describes the actual effect on selected rows.
    *******************************************************************************/
   @ParameterizedTest
   @MethodSource("actions")
   void everyActionReviewsAndExecutes(String name) throws Exception
   {
      CrmActions.Action action = CrmActions.action(name);
      seed(action);
      Map<String, Serializable> inputs = inputs();
      Map<String, String> before = snapshot();
      RunProcessOutput review = review(name, inputs, List.of(1, 2));
      assertThat(snapshot()).as("Review must not write any CRM table").isEqualTo(before);
      List<ProcessSummaryLine> reviewLines = lines(review, false);
      assertThat(reviewLines).isNotEmpty();
      assertThat(reviewLines).allSatisfy(line ->
      {
         assertThat(line.getSingularFutureMessage()).isNotBlank();
         assertThat(line.getPluralFutureMessage()).isNotBlank();
         assertThat(line.getSingularPastMessage()).isNotBlank();
         assertThat(line.getPluralPastMessage()).isNotBlank();
         assertThat(line.getPrimaryKeys()).isNotEmpty();
      });
      RunProcessOutput result = resume(name, review, Map.of());
      assertThat(result.getProcessState().getNextStepName()).hasValue("result");
      List<ProcessSummaryLine> resultLines = lines(result, true);
      assertThat(resultLines).isNotEmpty();
      assertThat(resultLines).noneMatch(line -> line.getStatus() == Status.ERROR);
      if(action.operation().equals("email"))
      {
         assertThat(resultLines).allMatch(line -> line.getStatus() == Status.WARNING);
         assertThat(resultLines.getFirst().getSingularPastMessage()).isEqualTo(CrmAction.EMAIL_WARNING);
         assertThat(snapshot()).isEqualTo(before);
      }
      else
      {
         assertThat(reviewLines).anyMatch(line -> line.getStatus() == Status.OK);
         assertThat(resultLines).anyMatch(line -> line.getStatus() == Status.OK);
         verifyEffect(action, resultLines);
      }
      if(!List.of("merge", "convert").contains(action.operation()))
      {
         assertThat(GetAction.execute(action.table(), 3).getValueString("ownerUserId")).isEqualTo("original");
      }
   }

   /*******************************************************************************
    ** Bindings, input screens, permissions, selection limits and removed processes.
    *******************************************************************************/
   @Test
   void metadataIsComplete()
   {
      assertThat(CrmActions.ACTIONS).hasSize(28);
      assertThat(QContext.getQInstance().getProcesses().keySet().stream().filter(name -> !name.contains("."))).hasSize(30);
      for(CrmActions.Action action : CrmActions.ACTIONS)
      {
         QProcessMetaData process = QContext.getQInstance().getProcess(action.name());
         assertThat(process.getTableName()).isEqualTo(action.table());
         assertThat(process.getMinInputRecords()).isPositive();
         assertThat(process.getPermissionRules().getDenyBehavior()).isEqualTo(DenyBehavior.HIDDEN);
         assertThat(process.getFrontendStep("review")).isNotNull();
         assertThat(process.getFrontendStep("result")).isNotNull();
         if(!CrmActions.inputFields(action).isEmpty())
         {
            assertThat(process.getFrontendStep("details")).isNotNull();
            assertThat(process.getStepList().indexOf(process.getFrontendStep("details")))
               .isLessThan(process.getStepList().indexOf(process.getFrontendStep("review")));
         }
         if(action.operation().equals("merge"))
         {
            assertThat(process.getMinInputRecords()).isEqualTo(2);
            assertThat(process.getMaxInputRecords()).isEqualTo(2);
         }
      }
      for(String name : List.of("crm_process_sequence_steps", "crm_calculate_lead_scores"))
      {
         assertThat(QContext.getQInstance().getProcess(name).getTableName()).isNull();
      }
      assertThat(QContext.getQInstance().getProcesses()).doesNotContainKeys("crm_fire_reminders", "crm_process_overdue_tasks", "crm_import_contacts", "crm_track_email_event");
      assertThat(QContext.getQInstance().getProcess("crm_move_deal_stage").getInputField("targetStageId").orElseThrow().getPossibleValueSourceFilter()).isNotNull();
   }

   /*******************************************************************************
    ** Administrative and management actions have distinct permission grants.
    *******************************************************************************/
   @Test
   void permissionBoundaries() throws Exception
   {
      QueryInput input = new QueryInput("crm_contact");
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.APP_ACCESS, CrmPermissions.READ, CrmPermissions.WRITE));
      for(String name : List.of("crm_bulk_assign_owner", "crm_assign_company_owner", "crm_transfer_ownership", "crm_merge_contacts", "crm_merge_companies",
         "crm_subscribe_to_list", "crm_enroll_in_sequence", "crm_activate_sequence", "crm_pause_sequence", "crm_gdpr_anonymize_contact"))
      {
         assertThat(PermissionsHelper.hasProcessPermission(input, name)).as(name).isFalse();
      }
      assertThat(PermissionsHelper.hasProcessPermission(input, "crm_log_activity")).isTrue();
   }

   /*******************************************************************************
    ** No writes when the selected target belongs to another pipeline.
    *******************************************************************************/
   @Test
   void crossPipelineIsWarningInReviewAndResult() throws Exception
   {
      seed(CrmActions.action("crm_move_deal_stage"));
      Map<String, Serializable> values = inputs();
      values.put("targetStageId", 5);
      RunProcessOutput review = review("crm_move_deal_stage", values, List.of(1, 2));
      assertThat(lines(review, false)).allMatch(line -> line.getStatus() == Status.WARNING);
      RunProcessOutput result = resume("crm_move_deal_stage", review, Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.WARNING);
      assertThat(GetAction.execute("crm_deal", 1).getValueInteger("pipelineStageId")).isEqualTo(1);
   }

   /*******************************************************************************
    ** Missing input is not silently accepted through the process API.
    *******************************************************************************/
   @Test
   void missingOwnerIsWarning() throws Exception
   {
      seed(CrmActions.action("crm_bulk_assign_owner"));
      RunProcessOutput review = review("crm_bulk_assign_owner", Map.of(), List.of(1, 2));
      assertThat(lines(review, false)).allMatch(line -> line.getStatus() == Status.WARNING);
      RunProcessOutput result = resume("crm_bulk_assign_owner", review, Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.WARNING);
      assertThat(GetAction.execute("crm_contact", 1).getValueString("ownerUserId")).isEqualTo("original");
   }

   /*******************************************************************************
    ** Read-only users cannot mutate even if they can see the process.
    *******************************************************************************/
   @Test
   void readOnlySelectionIsSkipped() throws Exception
   {
      seed(CrmActions.action("crm_log_activity"));
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.APP_ACCESS, CrmPermissions.READ, CrmPermissions.MANAGE_READ));
      RunProcessOutput review = review("crm_log_activity", inputs(), List.of(1));
      assertThat(lines(review, false)).allMatch(line -> line.getStatus() == Status.WARNING);
      RunProcessOutput result = resume("crm_log_activity", review, Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.WARNING);
      assertThat(rows("crm_activity")).isEmpty();
   }

   /*******************************************************************************
    ** API callers cannot merge one/three records or select an unrelated survivor.
    *******************************************************************************/
   @Test
   void mergeRequiresExactlyTwoSelectedRecords() throws Exception
   {
      seed(CrmActions.action("crm_merge_contacts"));
      Map<String, String> before = snapshot();
      for(List<Integer> ids : List.of(List.of(1), List.of(1, 2, 3)))
      {
         assertThatThrownBy(() -> review("crm_merge_contacts", inputs(), ids)).isInstanceOf(QException.class)
            .hasMessageContaining("exactly two");
      }
      Map<String, Serializable> values = inputs();
      values.put("primaryId", 3);
      assertThatThrownBy(() -> review("crm_merge_contacts", values, List.of(1, 2))).isInstanceOf(QException.class)
         .hasMessageContaining("one of the two selected");
      assertThat(snapshot()).isEqualTo(before);
   }

   /*******************************************************************************
    ** Missing selections cannot accidentally process an entire table.
    *******************************************************************************/
   @Test
   void selectionIsRequired() throws Exception
   {
      seed(CrmActions.action("crm_auto_assign"));
      assertThatThrownBy(() -> new RunProcessAction().execute(new RunProcessInput().withProcessName("crm_auto_assign")))
         .isInstanceOf(QException.class).hasMessageContaining("Select records");
      assertThat(GetAction.execute("crm_contact", 1).getValueString("ownerUserId")).isEqualTo("original");
   }

   /*******************************************************************************
    ** A failure on one row is reported as ERROR while other selected rows complete.
    *******************************************************************************/
   @Test
   void resultReportsRejectedUpdates() throws Exception
   {
      seed(CrmActions.action("crm_assign_company_owner"));
      RunProcessOutput review = review("crm_assign_company_owner", inputs(), List.of(1, 2));
      assertThat(lines(review, false)).allMatch(line -> line.getStatus() == Status.OK);
      QContext.getQInstance().getTable("crm_company").withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(RejectFirstUpdate.class));
      RunProcessOutput result = resume("crm_assign_company_owner", review, Map.of());
      assertThat(lines(result, true)).anySatisfy(line ->
      {
         assertThat(line.getStatus()).isEqualTo(Status.ERROR);
         assertThat(line.getPrimaryKeys()).containsExactly(1);
         assertThat(line.getSingularPastMessage()).contains("rejected");
      });
      assertThat(lines(result, true)).anySatisfy(line ->
      {
         assertThat(line.getStatus()).isEqualTo(Status.OK);
         assertThat(line.getPrimaryKeys()).containsExactly(2);
      });
      assertThat(GetAction.execute("crm_company", 1).getValueString("ownerUserId")).isEqualTo("original");
      assertThat(GetAction.execute("crm_company", 2).getValueString("ownerUserId")).isEqualTo("new-owner");
   }

   /*******************************************************************************
    ** Test-only backend rejection after the user has reviewed the selection.
    *******************************************************************************/
   public static class RejectFirstUpdate extends AbstractPreUpdateCustomizer
   {
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         records.stream().filter(row -> Integer.valueOf(1).equals(row.getValueInteger("id")))
            .forEach(row -> row.addError(new BadInputStatusMessage("Update rejected for testing")));
         return (records);
      }
   }

   /*******************************************************************************
    ** Revalidate consent-related suppression changed after the Review screen.
    *******************************************************************************/
   @Test
   void enrollmentRechecksSuppressionAfterReview() throws Exception
   {
      seed(CrmActions.action("crm_enroll_in_sequence"));
      RunProcessOutput review = review("crm_enroll_in_sequence", inputs(), List.of(1, 2));
      new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(
         new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput("crm_contact")
            .withRecord(new QRecord().withValue("id", 1).withValue("doNotEmail", true)));
      RunProcessOutput result = resume("crm_enroll_in_sequence", review, Map.of());
      assertThat(lines(result, true)).anyMatch(line -> line.getStatus() == Status.WARNING && line.getPrimaryKeys().equals(List.of(1)));
      assertThat(rows("crm_sequence_enrollment")).extracting(row -> row.getValueInteger("contactId")).containsExactly(2);
   }

   /*******************************************************************************
    ** Required stage fields are checked before any writes.
    *******************************************************************************/
   @Test
   void stageRequiredFieldIsReviewed() throws Exception
   {
      seed(CrmActions.action("crm_move_deal_stage"));
      change("crm_pipeline_stage", new QRecord().withValue("id", 2).withValue("pipelineId", 1).withValue("name", "Needs company")
         .withValue("requiredFieldsJson", "[\"companyId\"]"));
      RunProcessOutput review = review("crm_move_deal_stage", inputs(), List.of(1, 2));
      assertThat(lines(review, false)).allMatch(line -> line.getStatus() == Status.WARNING);
      RunProcessOutput result = resume("crm_move_deal_stage", review, Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.WARNING);
      assertThat(rows("crm_deal_stage_history")).isEmpty();
   }

   /*******************************************************************************
    ** Invalid configuration produces standard ERROR lines on both screens.
    *******************************************************************************/
   @Test
   void validationErrorsAppearInReviewAndResult() throws Exception
   {
      seed(CrmActions.action("crm_move_deal_stage"));
      change("crm_pipeline_stage", new QRecord().withValue("id", 2).withValue("requiredFieldsJson", "invalid json"));
      RunProcessOutput review = review("crm_move_deal_stage", inputs(), List.of(1, 2));
      assertThat(lines(review, false)).allMatch(line -> line.getStatus() == Status.ERROR);
      RunProcessOutput result = resume("crm_move_deal_stage", review, Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.ERROR);
      assertThat(rows("crm_deal_stage_history")).isEmpty();
   }

   /*******************************************************************************
    ** Request values cannot redirect an action to another table or record id.
    *******************************************************************************/
   @Test
   void installedBindingOwnsTheSelection() throws Exception
   {
      seed(CrmActions.action("crm_bulk_assign_owner"));
      Map<String, Serializable> values = inputs();
      values.put("sourceTable", "crm_company");
      values.put("destinationTable", "crm_company");
      values.put("recordId", 3);
      values.put("contactId", 3);
      RunProcessOutput result = resume("crm_bulk_assign_owner", review("crm_bulk_assign_owner", values, List.of(1, 2)), Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.OK);
      assertThat(rows("crm_company")).allMatch(row -> "original".equals(row.getValueString("ownerUserId")));
      assertThat(GetAction.execute("crm_contact", 1).getValueString("ownerUserId")).isEqualTo("new-owner");
      assertThat(GetAction.execute("crm_contact", 3).getValueString("ownerUserId")).isEqualTo("original");
   }

   /*******************************************************************************
    ** No rules produces an honest warning instead of a successful assignment.
    *******************************************************************************/
   @Test
   void autoAssignNoMatchIsWarning() throws Exception
   {
      seed(CrmActions.action("crm_auto_assign"));
      change("crm_assignment_rule", new QRecord().withValue("id", 1).withValue("isActive", false));
      RunProcessOutput result = resume("crm_auto_assign", review("crm_auto_assign", inputs(), List.of(1, 2)), Map.of());
      assertThat(lines(result, true)).allMatch(line -> line.getStatus() == Status.WARNING);
      assertThat(GetAction.execute("crm_contact", 1).getValueString("ownerUserId")).isEqualTo("original");
   }

   /*******************************************************************************
    ** Scoring crosses two full keyset pages, including non-contiguous ids.
    *******************************************************************************/
   @Test
   void scoringPagesInBatchesOf200() throws Exception
   {
      seed(CrmActions.action("crm_recalculate_score"));
      for(int i = 4; i <= 405; i++)
      {
         put("crm_contact", new QRecord().withValue("id", 10 + i * 2).withValue("lastName", "Contact").withValue("ownerUserId", "original"));
      }
      PageObserver.sizes.clear();
      QContext.getQInstance().getTable("crm_contact").withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PageObserver.class));
      RunProcessOutput result = new RunProcessAction().execute(new RunProcessInput().withProcessName("crm_calculate_lead_scores"));
      assertThat(result.getValues().get("totalContacts")).isEqualTo(405);
      assertThat(result.getValues().get("updatedCount")).isEqualTo(405);
      assertThat(PageObserver.sizes).containsExactly(200, 200, 5);
      QContext.getQInstance().getTable("crm_contact").getCustomizers().remove(TableCustomizers.POST_QUERY_RECORD.getRole());
      assertThat(rows("crm_contact")).hasSize(405).allMatch(row -> row.getValueInteger("leadScore") == 10);
   }

   /*******************************************************************************
    ** Changing enrollment status cannot shift an offset and skip the next page.
    *******************************************************************************/
   @Test
   void sequencePagesInBatchesOf200() throws Exception
   {
      seed(CrmActions.action("crm_enroll_in_sequence"));
      for(int i = 1; i <= 405; i++)
      {
         put("crm_sequence_enrollment", new QRecord().withValue("id", i * 2).withValue("sequenceId", 1).withValue("contactId", 1)
            .withValue("status", 1).withValue("currentStepNumber", 0).withValue("failureCount", 0).withValue("nextStepDate", Instant.now().minusSeconds(60)));
      }
      put("crm_sequence_enrollment", new QRecord().withValue("id", 9999).withValue("sequenceId", 1).withValue("contactId", 1)
         .withValue("status", 1).withValue("currentStepNumber", 0).withValue("nextStepDate", Instant.now().plusSeconds(86400)));
      PageObserver.sizes.clear();
      QContext.getQInstance().getTable("crm_sequence_enrollment").withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PageObserver.class));
      RunProcessOutput result = new RunProcessAction().execute(new RunProcessInput().withProcessName("crm_process_sequence_steps"));
      assertThat(result.getValues().get("processedCount")).isEqualTo(405);
      assertThat(PageObserver.sizes).containsExactly(200, 200, 5);
      QContext.getQInstance().getTable("crm_sequence_enrollment").getCustomizers().remove(TableCustomizers.POST_QUERY_RECORD.getRole());
      assertThat(rows("crm_sequence_enrollment").stream().filter(row -> row.getValueInteger("id") != 9999))
         .hasSize(405).allMatch(row -> row.getValueInteger("status") == 3);
      assertThat(GetAction.execute("crm_sequence_enrollment", 9999).getValueInteger("status")).isEqualTo(1);
   }

   /*******************************************************************************
    ** Observe the actual query bounds, not just the final job counts.
    *******************************************************************************/
   public static class PageObserver extends AbstractPostQueryCustomizer
   {
      private static final List<Integer> sizes = new ArrayList<>();

      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         if(input instanceof QueryInput query && query.getFilter().getOrderBys() != null && !query.getFilter().getOrderBys().isEmpty())
         {
            assertThat(query.getFilter().getLimit()).isEqualTo(200);
            assertThat(query.getFilter().getOrderBys().getFirst().getFieldName()).isEqualTo("id");
            sizes.add(records.size());
         }
         return (records);
      }
   }

   /*******************************************************************************
    ** Verify observable writes, including merge relationships and duplicate links.
    *******************************************************************************/
   private void verifyEffect(CrmActions.Action action, List<ProcessSummaryLine> summary) throws QException
   {
      QRecord first = GetAction.execute(action.table(), 1);
      switch(action.operation())
      {
         case "assign" -> assertThat(first.getValueString("ownerUserId")).isEqualTo("new-owner");
         case "auto" -> assertThat(first.getValueString("ownerUserId")).isEqualTo("auto-owner");
         case "subscribe" -> assertThat(rows("crm_email_list_member")).hasSize(2);
         case "unsubscribe" -> assertThat(rows("crm_email_list_member")).allMatch(row -> row.getValueInstant("unsubscribedDate") != null);
         case "enroll" -> assertThat(rows("crm_sequence_enrollment")).hasSize(2);
         case "unenroll" -> assertThat(rows("crm_sequence_enrollment")).allMatch(row -> row.getValueInstant("unenrolledDate") != null);
         case "activity" ->
         {
            assertThat(rows("crm_activity")).hasSize(2);
            String foreignKey = action.table().substring(4) + "Id";
            assertThat(rows("crm_activity")).extracting(row -> row.getValueInteger(foreignKey)).containsExactlyInAnyOrder(1, 2);
         }
         case "score" -> assertThat(first.getValueInteger("leadScore")).isEqualTo(10);
         case "merge" ->
         {
            assertThat(GetAction.execute(action.table(), 2)).isNull();
            assertThat(GetAction.execute(action.table(), 3)).isNotNull();
         }
         case "duplicates" ->
         {
            assertThat(summary).anySatisfy(line ->
            {
               assertThat(line.getStatus()).isEqualTo(Status.WARNING);
               assertThat(line.getPrimaryKeys()).containsExactlyInAnyOrder(1, 2);
            });
            assertThat(rows(action.table())).hasSize(3);
         }
         case "createDeal", "clone" -> assertThat(rows("crm_deal")).hasSize(5);
         case "erase" ->
         {
            assertThat(first.getValueBoolean("doNotEmail")).isTrue();
            assertThat(first.getValueString("email")).endsWith("@removed.invalid");
         }
         case "stage" -> assertThat(first.getValueInteger("pipelineStageId")).isEqualTo(2);
         case "won" ->
         {
            assertThat(first.getValueInteger("pipelineStageId")).isEqualTo(3);
            assertThat(first.getValueInteger("winLossReasonId")).isEqualTo(1);
            assertThat(first.getValueLocalDate("actualCloseDate")).isNotNull();
            assertThat(rows("crm_deal_stage_history")).hasSize(2);
         }
         case "lost" -> assertThat(first.getValueInteger("pipelineStageId")).isEqualTo(4);
         case "reopen" ->
         {
            assertThat(first.getValueInteger("pipelineStageId")).isEqualTo(2);
            assertThat(first.getValue("actualCloseDate")).isNull();
            assertThat(first.getValue("winLossReasonId")).isNull();
         }
         case "addContact" -> assertThat(rows("crm_deal_contact")).hasSize(2);
         case "convert" ->
         {
            assertThat(first.getValueInteger("contactId")).isNotNull();
            assertThat(GetAction.execute("crm_form_submission", 2).getValueInteger("contactId")).isNotNull();
            assertThat(rows("crm_contact")).hasSize(5);
         }
         case "activate" -> assertThat(first.getValueBoolean("isActive")).isTrue();
         case "pause" -> assertThat(first.getValueBoolean("isActive")).isFalse();
         default -> throw (new AssertionError(action.name()));
      }
   }

   /*******************************************************************************
    ** Start, complete input details, and stop at the actual standard Review screen.
    *******************************************************************************/
   private RunProcessOutput review(String name, Map<String, Serializable> values, List<Integer> ids) throws QException
   {
      RunProcessInput request = new RunProcessInput();
      request.setProcessName(name);
      request.setValues(new HashMap<>(values));
      request.addValue("queryFilterJson", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, ids))));
      RunProcessOutput output = new RunProcessAction().execute(request);
      assertThat(output.getException()).isEmpty();
      if(output.getProcessState().getNextStepName().orElse("").equals("details"))
      {
         output = resume(name, output, values);
      }
      assertThat(output.getProcessState().getNextStepName()).hasValue("review");
      return (output);
   }

   /*******************************************************************************
    ** Resume the process just as the frontend submits its current screen.
    *******************************************************************************/
   private RunProcessOutput resume(String name, RunProcessOutput prior, Map<String, Serializable> values) throws QException
   {
      RunProcessInput request = new RunProcessInput();
      request.setProcessName(name);
      request.setProcessUUID(prior.getProcessUUID());
      request.setStartAfterStep(prior.getProcessState().getNextStepName().orElseThrow());
      request.setValues(new HashMap<>(values));
      RunProcessOutput output = new RunProcessAction().execute(request);
      assertThat(output.getException()).isEmpty();
      return (output);
   }

   /*******************************************************************************
    ** Read standard ETL summaries, not test-specific step state.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private List<ProcessSummaryLine> lines(RunProcessOutput output, boolean result)
   {
      return ((List<ProcessSummaryLine>) output.getValues().get(result ? StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY : StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY));
   }

   /*******************************************************************************
    ** Supply all action inputs; each operation accepts only its declared fields.
    *******************************************************************************/
   private Map<String, Serializable> inputs()
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put("newOwnerUserId", "new-owner");
      values.put("emailListId", 1);
      values.put("sequenceId", 1);
      values.put("primaryId", 1);
      values.put("pipelineId", 1);
      values.put("targetStageId", 2);
      values.put("winLossReasonId", 1);
      values.put("dealName", "New deal");
      values.put("dealAmount", new BigDecimal("250"));
      values.put("contactId", 1);
      values.put("activityTypeId", 1);
      values.put("subject", "Phone conversation");
      values.put("description", "Discussed next steps");
      return (values);
   }

   /*******************************************************************************
    ** Stable persisted fixtures, including a third record excluded from selections.
    *******************************************************************************/
   private void seed(CrmActions.Action action) throws QException
   {
      put("crm_pipeline", new QRecord().withValue("id", 1).withValue("name", "Sales").withValue("isDefault", true));
      put("crm_pipeline", new QRecord().withValue("id", 2).withValue("name", "Other"));
      for(int id = 1; id <= 5; id++)
      {
         put("crm_pipeline_stage", new QRecord().withValue("id", id).withValue("name", "Stage " + id).withValue("pipelineId", id == 5 ? 2 : 1)
            .withValue("sortOrder", id).withValue("probabilityPct", id == 3 ? 100 : 50).withValue("isClosedWon", id == 3).withValue("isClosedLost", id == 4));
      }
      put("crm_win_loss_reason", new QRecord().withValue("id", 1).withValue("name", "Budget"));
      put("crm_activity_type", new QRecord().withValue("id", 1).withValue("name", "Note"));
      put("crm_email_list", new QRecord().withValue("id", 1).withValue("name", "Newsletter").withValue("isActive", true));
      put("crm_assignment_rule", new QRecord().withValue("id", 1).withValue("name", "Assign").withValue("entityType", 1)
         .withValue("assignStrategy", 1).withValue("assignToUserId", "auto-owner").withValue("isActive", true).withValue("sortOrder", 1));
      put("crm_lead_score_rule", new QRecord().withValue("id", 1).withValue("name", "Name present").withValue("fieldPath", "lastName")
         .withValue("operator", 6).withValue("scoreAdjustment", 10).withValue("isActive", true).withValue("sortOrder", 1));
      for(int id = 1; id <= 3; id++)
      {
         String email = action.operation().equals("duplicates") && id < 3 ? (id == 1 ? "A@EXAMPLE.COM" : "a@example.com") : "person" + id + "@example.com";
         put("crm_contact", new QRecord().withValue("id", id).withValue("firstName", "Person").withValue("lastName", "Contact " + id)
            .withValue("email", email).withValue("ownerUserId", "original").withValue("leadScore", 0));
         put("crm_company", new QRecord().withValue("id", id).withValue("name", "Company " + id).withValue("ownerUserId", "original")
            .withValue("domain", id < 3 ? (id == 1 ? "EXAMPLE.COM" : "example.com") : "different.com"));
         put("crm_deal", new QRecord().withValue("id", id).withValue("name", "Deal " + id).withValue("pipelineId", 1)
            .withValue("pipelineStageId", action.operation().equals("reopen") ? 3 : 1).withValue("amount", new BigDecimal("100"))
            .withValue("currencyCode", "USD").withValue("ownerUserId", "original").withValue("stageEnteredDate", Instant.now().minusSeconds(86400)));
         put("crm_form_submission", new QRecord().withValue("id", id).withValue("email", "form" + id + "@example.com")
            .withValue("firstName", "Web").withValue("lastName", "Lead " + id).withValue("ownerUserId", "original"));
         put("crm_email_sequence", new QRecord().withValue("id", id).withValue("name", "Sequence " + id)
            .withValue("isActive", !action.operation().equals("activate")).withValue("ownerUserId", "original"));
         if(id <= 2 && action.operation().equals("unsubscribe"))
         {
            put("crm_email_list_member", new QRecord().withValue("id", id).withValue("emailListId", 1).withValue("contactId", id)
               .withValue("email", email).withValue("status", 1));
         }
         if(id <= 2 && action.operation().equals("unenroll"))
         {
            put("crm_sequence_enrollment", new QRecord().withValue("id", id).withValue("sequenceId", 1).withValue("contactId", id)
               .withValue("status", 1).withValue("currentStepNumber", 0).withValue("failureCount", 0));
         }
      }
   }

   /*******************************************************************************
    ** Raw fixtures intentionally bypass insert automation and uniqueness for duplicates.
    *******************************************************************************/
   private void put(String table, QRecord row) throws QException
   {
      MemoryRecordStore.getInstance().insert(new InsertInput(table).withRecord(row), true);
   }

   /*******************************************************************************
    ** Update an existing fixture through normal DML.
    *******************************************************************************/
   private void change(String table, QRecord row) throws QException
   {
      new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(
         new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(table).withRecord(row));
   }

   /*******************************************************************************
    ** Query through the normal session.
    *******************************************************************************/
   private List<QRecord> rows(String table) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table)).getRecords());
   }

   /*******************************************************************************
    ** Capture persisted values in every table to detect accidental Review writes.
    *******************************************************************************/
   private Map<String, String> snapshot() throws QException
   {
      Map<String, String> snapshot = new TreeMap<>();
      for(String table : QContext.getQInstance().getTables().keySet())
      {
         List<Map<String, Serializable>> values = new ArrayList<>();
         rows(table).forEach(row -> values.add(new TreeMap<>(row.getValues())));
         snapshot.put(table, JsonUtils.toJson(values));
      }
      return (snapshot);
   }
}
