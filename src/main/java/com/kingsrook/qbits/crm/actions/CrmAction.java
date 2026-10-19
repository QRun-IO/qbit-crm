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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qbits.crm.activities.processes.LogActivityProcess;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.processes.ConvertFormToContactProcess;
import com.kingsrook.qbits.crm.core.processes.GdprAnonymizeContactProcess;
import com.kingsrook.qbits.crm.core.processes.MergeCompaniesProcess;
import com.kingsrook.qbits.crm.core.processes.MergeContactsProcess;
import com.kingsrook.qbits.crm.core.processes.SubscribeToListProcess;
import com.kingsrook.qbits.crm.core.processes.UnsubscribeFromListProcess;
import com.kingsrook.qbits.crm.deals.processes.CloneDealProcess;
import com.kingsrook.qbits.crm.deals.processes.ConvertLeadProcess;
import com.kingsrook.qbits.crm.deals.processes.ManageContactOnDealProcess;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.email.CrmEmails;
import com.kingsrook.qbits.crm.email.processes.EnrollInSequenceProcess;
import com.kingsrook.qbits.crm.email.processes.UnenrollFromSequenceProcess;
import com.kingsrook.qbits.crm.scoring.processes.AutoAssignProcess;
import com.kingsrook.qbits.crm.scoring.processes.CalculateLeadScoresProcess;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Shared streamed action lifecycle. Review never invokes a mutating operation.
 *******************************************************************************/
public final class CrmAction
{
   private static final QLogger LOG = QLogger.getLogger(CrmAction.class);



   /*******************************************************************************
    ** Utility holder.
    *******************************************************************************/
   private CrmAction()
   {
   }

   /*******************************************************************************
    ** Pin extraction to metadata and require an explicit record selection/filter.
    *******************************************************************************/
   public static class Extract extends ExtractViaQueryStep
   {
      @Override
      public void preRun(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String table = CrmActions.action(input.getProcessName()).table();
         input.addValue(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, table);
         input.addValue(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, table);
         super.preRun(input, output);
      }

      /*******************************************************************************
       ** Reuse the core selection parser for preparation and merge validation.
       *******************************************************************************/
      QQueryFilter selection(RunBackendStepInput input) throws QException
      {
         if(!StringUtils.hasContent(input.getValueString("queryFilterJson"))
            && (input.getCallback() == null || input.getCallback().getQueryFilter() == null))
         {
            throw (new QUserFacingException("Select records before running this action"));
         }
         return (super.getQueryFilter(input).clone());
      }

      @Override
      protected QQueryFilter getQueryFilter(RunBackendStepInput input) throws QException
      {
         return (selection(input));
      }
   }

   /*******************************************************************************
    ** Populate read-only pipeline context and the merge survivor dropdown.
    *******************************************************************************/
   public static class Prepare implements BackendStep
   {
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         CrmActions.Action action = CrmActions.action(input.getProcessName());
         QQueryFilter filter = new Extract().selection(input);
         if(action.operation().equals("merge"))
         {
            List<QRecord> rows = selectedPair(action, input);
            output.addValue("mergeIds", new ArrayList<>(rows.stream().map(r -> r.getValue("id")).toList()));
            output.addValue("primaryId", rows.getFirst().getValue("id"));
         }
         else
         {
            List<QRecord> rows = new QueryAction().execute(new QueryInput(action.table()).withFilter(filter.withLimit(2))
               .withFieldNamesToInclude(Set.of("pipelineId")).withSelectDistinct(true)).getRecords();
            List<Serializable> pipelines = rows.stream().map(r -> r.getValue("pipelineId")).distinct().toList();
            if(pipelines.size() != 1)
            {
               throw (new QUserFacingException("Select deals from one pipeline to choose a target stage"));
            }
            output.addValue("pipelineId", pipelines.getFirst());
         }
      }
   }

   /*******************************************************************************
    ** Validate and describe each selected row, preserving keys for standard links.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      /*******************************************************************************
       ** Resolve the host transport; tests may supply a fake.
       *******************************************************************************/
      protected CrmEmailTransport emailTransport() throws QException
      {
         return (new CrmEmailTransport(CrmApi.config()));
      }

      private final Map<String, ProcessSummaryLine> lines = new LinkedHashMap<>();
      private final Map<String, List<Serializable>> duplicateGroups = new LinkedHashMap<>();
      private List<QRecord> pair;

      @Override
      public void preRun(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         CrmActions.Action action = CrmActions.action(input.getProcessName());
         if(action.operation().equals("merge"))
         {
            pair = selectedPair(action, input);
            if(pair.stream().noneMatch(r -> Objects.equals(r.getValue("id"), input.getValue("primaryId"))))
            {
               throw (new QUserFacingException("The record to keep must be one of the two selected records"));
            }
         }
      }

      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         CrmActions.Action action = CrmActions.action(input.getProcessName());
         for(QRecord extracted : input.getRecords())
         {
            Serializable id = extracted.getValue("id");
            try
            {
               QRecord row = requireRow(action, input, id);
               String reason = check(action, input, row, action.operation().equals("email") ? emailTransport() : null);
               if(reason != null)
               {
                  add(lines, Status.WARNING, reason, reason, id);
                  continue;
               }
               if(action.operation().equals("merge"))
               {
                  if(!Objects.equals(id, input.getValue("primaryId")))
                  {
                     continue;
                  }
                  Serializable secondary = pair.stream().map(r -> r.getValue("id")).filter(key -> !key.equals(id)).findFirst().orElseThrow();
                  requireRow(action, input, secondary);
                  row.setValue("mergeSecondaryId", secondary);
               }
               if(action.operation().equals("duplicates"))
               {
                  String key = row.getValueString(action.table().equals("crm_contact") ? "email" : "domain");
                  if(StringUtils.hasContent(key))
                  {
                     duplicateGroups.computeIfAbsent(key.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(id);
                  }
               }
               String future = action.operation().equals("auto") ? "will be evaluated for assignment" : "will be " + action.past();
               add(lines, Status.OK, future, "was " + action.past(), id);
               output.addRecord(row);
            }
            catch(QUserFacingException e)
            {
               add(lines, Status.WARNING, e.getMessage(), e.getMessage(), id);
            }
            catch(QException | RuntimeException e)
            {
               LOG.warn("CRM action validation failed", e);
               add(lines, Status.ERROR, "cannot be processed; validation failed (see server log)", "was not processed; validation failed (see server log)", id);
            }
         }
      }

      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         ArrayList<ProcessSummaryLineInterface> summary = new ArrayList<>(lines.values());
         int group = 0;
         for(List<Serializable> ids : duplicateGroups.values())
         {
            if(ids.size() > 1)
            {
               ProcessSummaryLine line = line(Status.WARNING, "will appear in duplicate group " + (++group), "appeared in duplicate group " + group);
               ids.forEach(line::incrementCountAndAddPrimaryKey);
               summary.add(line);
            }
         }
         return (summary);
      }
   }

   /*******************************************************************************
    ** Reauthorize and validate at execution time; count only actual outcomes.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      /*******************************************************************************
       ** Resolve the host transport; tests may supply a fake.
       *******************************************************************************/
      protected CrmEmailTransport emailTransport() throws QException
      {
         return (new CrmEmailTransport(CrmApi.config()));
      }

      private final Map<String, ProcessSummaryLine> lines = new LinkedHashMap<>();
      private boolean duplicates;

      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         CrmActions.Action action = CrmActions.action(input.getProcessName());
         duplicates = action.operation().equals("duplicates");
         for(QRecord reviewed : input.getRecords())
         {
            Serializable id = reviewed.getValue("id");
            try
            {
               QRecord row = requireRow(action, input, id);
               String reason = check(action, input, row, action.operation().equals("email") ? emailTransport() : null);
               if(reason != null)
               {
                  add(lines, Status.WARNING, reason, reason, id);
                  continue;
               }
               row.setValue("mergeSecondaryId", reviewed.getValue("mergeSecondaryId"));
               String skipped = perform(action, input, row, action.operation().equals("email") ? emailTransport() : null);
               if(skipped == null)
               {
                  add(lines, Status.OK, "will be " + action.past(), "was " + action.past(), id);
               }
               else
               {
                  add(lines, Status.WARNING, skipped, skipped, id);
               }
            }
            catch(QException | RuntimeException e)
            {
               LOG.warn("CRM action execution failed", e);
               String reason = e instanceof QUserFacingException ? e.getMessage() : "see server log";
               add(lines, Status.ERROR, "cannot be processed: " + reason, "failed: " + reason, id);
            }
         }
      }

      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         ArrayList<ProcessSummaryLineInterface> summary = new ArrayList<>(lines.values());
         if(getTransformStep() instanceof Transform transform)
         {
            if(duplicates)
            {
               return (transform.getProcessSummary(output, result));
            }
            transform.lines.values().stream().filter(line -> line.getStatus() != Status.OK).forEach(summary::add);
         }
         return (summary);
      }
   }

   /*******************************************************************************
    ** Read current records under the session; read access alone cannot mutate.
    *******************************************************************************/
   private static QRecord requireRow(CrmActions.Action action, RunBackendStepInput input, Serializable id) throws QException
   {
      TablePermissionSubType permission = action.operation().equals("duplicates") ? TablePermissionSubType.READ : TablePermissionSubType.EDIT;
      if(!PermissionsHelper.hasProcessPermission(input, action.name()) || !PermissionsHelper.hasTablePermission(input, action.table(), permission))
      {
         throw (new QUserFacingException("Permission denied for this action"));
      }
      QRecord row = GetAction.execute(action.table(), id);
      if(row == null)
      {
         throw (new QUserFacingException("Record is unavailable"));
      }
      return (row);
   }

   /*******************************************************************************
    ** Merge only an explicit pair, including when called outside the UI.
    *******************************************************************************/
   private static List<QRecord> selectedPair(CrmActions.Action action, RunBackendStepInput input) throws QException
   {
      QQueryFilter filter = new Extract().selection(input);
      filter.setLimit(3);
      List<QRecord> rows = new QueryAction().execute(new QueryInput(action.table()).withFilter(filter)).getRecords();
      if(rows.size() != 2)
      {
         throw (new QUserFacingException("Select exactly two records to merge"));
      }
      return (rows);
   }

   /*******************************************************************************
    ** Shared read-only checks for Review and Result.
    *******************************************************************************/
   private static String check(CrmActions.Action action, RunBackendStepInput input, QRecord row, CrmEmailTransport transport) throws QException
   {
      for(QFieldMetaData field : CrmActions.inputFields(action))
      {
         Serializable value = input.getValue(field.getName());
         if(Boolean.TRUE.equals(field.getIsRequired()) && (value == null || value instanceof String text && text.isBlank()))
         {
            return ("Supply " + field.getLabel() + " before reviewing");
         }
         if(value != null && field.getPossibleValueSourceName() != null && !field.getName().equals("primaryId"))
         {
            if("emailTemplateId".equals(field.getName()) && transport != null && transport.usesSharedTemplates())
            {
               transport.templateVariableNames(input.getValueInteger("emailTemplateId"));
            }
            else if(GetAction.execute(field.getPossibleValueSourceName(), value) == null)
            {
               return (field.getLabel() + " is unavailable");
            }
         }
      }
      switch(action.operation())
      {
         case "email":
            return (CrmEmails.skipReason(row, transport));
         case "subscribe", "unsubscribe":
            if(!StringUtils.hasContent(row.getValueString("email")))
            {
               return ("Contact has no email address");
            }
            break;
         case "enroll":
            if(Boolean.TRUE.equals(row.getValueBoolean("doNotEmail")))
            {
               return ("Contact has opted out of email");
            }
            if(!Boolean.TRUE.equals(GetAction.execute("crm_email_sequence", input.getValue("sequenceId")).getValueBoolean("isActive")))
            {
               return ("Sequence is not active");
            }
            if(!enrollments(input, row).isEmpty())
            {
               return ("Contact is already enrolled in this sequence");
            }
            break;
         case "unenroll":
            if(enrollments(input, row).isEmpty())
            {
               return ("Contact has no active enrollment in this sequence");
            }
            break;
         case "stage", "won", "lost", "reopen":
            QRecord target = targetStage(action, input, row);
            if(target == null)
            {
               return ("No matching target stage exists in the deal's pipeline");
            }
            if(!Objects.equals(row.getValue("pipelineId"), target.getValue("pipelineId")))
            {
               return ("Target stage does not belong to this deal's pipeline");
            }
            if(Objects.equals(row.getValue("pipelineStageId"), target.getValue("id")))
            {
               return ("Deal is already in the target stage");
            }
            if(action.operation().equals("reopen"))
            {
               QRecord current = GetAction.execute("crm_pipeline_stage", row.getValue("pipelineStageId"));
               if(current == null || !closed(current) || closed(target))
               {
                  return ("Reopen requires a closed deal and an open target stage");
               }
            }
            if(StringUtils.hasContent(target.getValueString("requiredFieldsJson")))
            {
               try
               {
                  for(Object field : JsonUtils.toObject(target.getValueString("requiredFieldsJson"), List.class))
                  {
                     if(row.getValue(field.toString()) == null)
                     {
                        return ("Required field '" + field + "' must be set before moving to this stage");
                     }
                  }
               }
               catch(java.io.IOException e)
               {
                  throw (new QException("Invalid stage required fields", e));
               }
            }
            break;
         case "createDeal":
            if(query("crm_pipeline_stage", "pipelineId", input.getValue("pipelineId")).isEmpty())
            {
               return ("The pipeline has no stages");
            }
            break;
         default:
            break;
      }
      return (null);
   }

   /*******************************************************************************
    ** Terminal stage lookup stays within each selected deal's pipeline.
    *******************************************************************************/
   private static QRecord targetStage(CrmActions.Action action, RunBackendStepInput input, QRecord row) throws QException
   {
      if(List.of("stage", "reopen").contains(action.operation()))
      {
         return (GetAction.execute("crm_pipeline_stage", input.getValue("targetStageId")));
      }
      List<QRecord> stages = new QueryAction().execute(new QueryInput("crm_pipeline_stage").withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, row.getValue("pipelineId")))
         .withCriteria(new QFilterCriteria(action.operation().equals("won") ? "isClosedWon" : "isClosedLost", QCriteriaOperator.EQUALS, true))
         .withOrderBy(new QFilterOrderBy("sortOrder")).withOrderBy(new QFilterOrderBy("id")).withLimit(1))).getRecords();
      return (stages.isEmpty() ? null : stages.getFirst());
   }

   /*******************************************************************************
    ** Closed-stage predicate.
    *******************************************************************************/
   private static boolean closed(QRecord stage)
   {
      return (Boolean.TRUE.equals(stage.getValueBoolean("isClosedWon")) || Boolean.TRUE.equals(stage.getValueBoolean("isClosedLost")));
   }

   /*******************************************************************************
    ** Invoke the existing business operations only at Load time.
    *******************************************************************************/
   private static String perform(CrmActions.Action action, RunBackendStepInput input, QRecord row, CrmEmailTransport transport) throws QException
   {
      RunBackendStepInput call = new RunBackendStepInput().withProcessName(input.getProcessName()).withValues(new HashMap<>());
      for(QFieldMetaData field : CrmActions.inputFields(action))
      {
         call.addValue(field.getName(), input.getValue(field.getName()));
      }
      Serializable id = row.getValue("id");
      call.addValue("contactId", action.table().equals("crm_contact") ? id : input.getValue("contactId"));
      call.addValue("companyId", action.table().equals("crm_company") ? id : null);
      call.addValue("dealId", action.table().equals("crm_deal") ? id : null);
      call.addValue("formSubmissionId", id);
      RunBackendStepOutput result = new RunBackendStepOutput();
      switch(action.operation())
      {
         case "email" ->
         {
            String processUUID = input.getValueString("crmEmailProcessUUID");
            if(processUUID == null)
            {
               throw (new QUserFacingException("Email process identity is missing; start the Send Email action again"));
            }
            CrmEmails.send(transport, row, input.getValueInteger("emailTemplateId"), input.getValueString("subject"), input.getValueString("body"),
               "email", id + ":" + processUUID, null);
         }
         case "assign" -> update(action.table(), new QRecord().withValue("id", id).withValue("ownerUserId", input.getValue("newOwnerUserId")));
         case "auto" ->
         {
            call.addValue("entityType", "CONTACT");
            call.addValue("recordId", id);
            new AutoAssignProcess().run(call, result);
            if(!"ASSIGNED".equals(result.getValue("action")))
            {
               return ("No assignment rule matched; owner unchanged");
            }
         }
         case "subscribe", "unsubscribe" ->
         {
            call.addValue("email", row.getValue("email"));
            call.addValue("firstName", row.getValue("firstName"));
            call.addValue("subscriptionSource", "CRM record action");
            if(action.operation().equals("subscribe"))
            {
               new SubscribeToListProcess().run(call, result);
            }
            else
            {
               new UnsubscribeFromListProcess().run(call, result);
            }
            if(List.of("NOOP", "NOT_FOUND").contains(result.getValue("action")))
            {
               return ("List membership unchanged");
            }
         }
         case "enroll" -> new EnrollInSequenceProcess().run(call, result);
         case "unenroll" ->
         {
            for(QRecord enrollment : enrollments(input, row))
            {
               call.addValue("enrollmentId", enrollment.getValue("id"));
               new UnenrollFromSequenceProcess().run(call, result);
            }
         }
         case "activity" -> new LogActivityProcess().run(call, result);
         case "score" -> new CalculateLeadScoresProcess().recalculate(row);
         case "merge" ->
         {
            Serializable secondary = row.getValue("mergeSecondaryId");
            List<QRecord> pair = selectedPair(action, input);
            if(secondary == null || secondary.equals(id) || pair.stream().noneMatch(r -> Objects.equals(r.getValue("id"), secondary)))
            {
               throw (new QUserFacingException("Merge selection changed; review it again"));
            }
            requireRow(action, input, secondary);
            String entity = action.table().equals("crm_contact") ? "Contact" : "Company";
            call.addValue("primary" + entity + "Id", id);
            call.addValue("secondary" + entity + "Id", secondary);
            if(entity.equals("Contact"))
            {
               new MergeContactsProcess().run(call, result);
            }
            else
            {
               new MergeCompaniesProcess().run(call, result);
            }
         }
         case "duplicates" ->
         {
         }
         case "createDeal" -> new ConvertLeadProcess().run(call, result);
         case "erase" -> new GdprAnonymizeContactProcess().run(call, result);
         case "stage", "won", "lost", "reopen" ->
         {
            QRecord change = new QRecord().withValue("id", id).withValue("pipelineStageId", targetStage(action, input, row).getValue("id"));
            if(List.of("won", "lost").contains(action.operation()))
            {
               change.setValue("winLossReasonId", input.getValue("winLossReasonId"));
            }
            update(action.table(), change);
         }
         case "clone" -> new CloneDealProcess().run(call, result);
         case "addContact" ->
         {
            call.addValue("action", "ADD");
            new ManageContactOnDealProcess().run(call, result);
         }
         case "convert" -> new ConvertFormToContactProcess().run(call, result);
         case "activate", "pause" -> update(action.table(), new QRecord().withValue("id", id).withValue("isActive", action.operation().equals("activate")));
         default -> throw (new QUserFacingException("Action is not available"));
      }
      return (null);
   }

   /*******************************************************************************
    ** Core DML can return record errors; never present those as successful writes.
    *******************************************************************************/
   private static void update(String table, QRecord change) throws QException
   {
      List<QRecord> records = new UpdateAction().execute(new UpdateInput(table).withRecord(change)).getRecords();
      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            throw (new QUserFacingException("Update was rejected: " + record.getErrors()));
         }
      }
   }

   /*******************************************************************************
    ** Active enrollments belonging to the selected contact and chosen sequence.
    *******************************************************************************/
   private static List<QRecord> enrollments(RunBackendStepInput input, QRecord row) throws QException
   {
      return (new QueryAction().execute(new QueryInput("crm_sequence_enrollment").withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, row.getValue("id")))
         .withCriteria(new QFilterCriteria("sequenceId", QCriteriaOperator.EQUALS, input.getValue("sequenceId")))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getPossibleValueId())))).getRecords());
   }

   /*******************************************************************************
    ** Session-scoped related rows.
    *******************************************************************************/
   private static List<QRecord> query(String table, String field, Serializable value) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value)))).getRecords());
   }

   /*******************************************************************************
    ** Standard future and past messages work on both Review and Result.
    *******************************************************************************/
   private static ProcessSummaryLine line(Status status, String future, String past)
   {
      return (new ProcessSummaryLine(status).withSingularFutureMessage(future).withPluralFutureMessage(future)
         .withSingularPastMessage(past).withPluralPastMessage(past.startsWith("was ") ? "were " + past.substring(4) : past));
   }

   /*******************************************************************************
    ** Group equal outcomes and retain primary keys for standard record links.
    *******************************************************************************/
   private static void add(Map<String, ProcessSummaryLine> lines, Status status, String future, String past, Serializable id)
   {
      lines.computeIfAbsent(status + ":" + future, ignored -> line(status, future, past)).incrementCountAndAddPrimaryKey(id);
   }
}
