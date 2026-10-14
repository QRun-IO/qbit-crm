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
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** CRM record actions. The installed definition owns the table and operation.
 *******************************************************************************/
public class CrmActions implements MetaDataProducerInterface<MetaDataProducerMultiOutput>
{
   /*******************************************************************************
    ** Immutable action definition; no session state is shared between runs.
    *******************************************************************************/
   public record Action(String name, String table, String label, String operation, String past)
   {
   }

   public static final List<Action> ACTIONS = List.of(
      new Action("crm_bulk_assign_owner", "crm_contact", "Assign Owner", "assign", "assigned a new owner"),
      new Action("crm_auto_assign", "crm_contact", "Auto-Assign", "auto", "auto-assigned"),
      new Action("crm_subscribe_to_list", "crm_contact", "Add to List", "subscribe", "added to the list"),
      new Action("crm_unsubscribe_from_list", "crm_contact", "Remove from List", "unsubscribe", "removed from the list"),
      new Action("crm_enroll_in_sequence", "crm_contact", "Enroll in Sequence", "enroll", "enrolled in the sequence"),
      new Action("crm_unenroll_from_sequence", "crm_contact", "Unenroll from Sequence", "unenroll", "unenrolled from the sequence"),
      new Action("crm_log_activity", "crm_contact", "Log Activity", "activity", "given a logged activity"),
      new Action("crm_recalculate_score", "crm_contact", "Recalculate Score", "score", "rescored"),
      new Action("crm_merge_contacts", "crm_contact", "Merge Contacts", "merge", "merged"),
      new Action("crm_detect_duplicates", "crm_contact", "Find Duplicates", "duplicates", "checked for duplicates"),
      new Action("crm_convert_lead", "crm_contact", "Create Deal", "createDeal", "given a new deal"),
      new Action("crm_gdpr_anonymize_contact", "crm_contact", "Erase Personal Data", "erase", "anonymized"),
      new Action("crm_send_email", "crm_contact", "Send Email", "email", "sent email"),
      new Action("crm_assign_company_owner", "crm_company", "Assign Owner", "assign", "assigned a new owner"),
      new Action("crm_merge_companies", "crm_company", "Merge Companies", "merge", "merged"),
      new Action("crm_find_duplicate_companies", "crm_company", "Find Duplicates", "duplicates", "checked for duplicates"),
      new Action("crm_log_company_activity", "crm_company", "Log Activity", "activity", "given a logged activity"),
      new Action("crm_move_deal_stage", "crm_deal", "Move Stage", "stage", "moved to the target stage"),
      new Action("crm_mark_deal_won", "crm_deal", "Mark Won", "won", "marked won"),
      new Action("crm_mark_deal_lost", "crm_deal", "Mark Lost", "lost", "marked lost"),
      new Action("crm_reopen_deal", "crm_deal", "Reopen", "reopen", "reopened"),
      new Action("crm_clone_deal", "crm_deal", "Clone", "clone", "cloned"),
      new Action("crm_transfer_ownership", "crm_deal", "Assign Owner", "assign", "assigned a new owner"),
      new Action("crm_manage_contact_on_deal", "crm_deal", "Add Contact", "addContact", "linked to the contact"),
      new Action("crm_log_deal_activity", "crm_deal", "Log Activity", "activity", "given a logged activity"),
      new Action("crm_convert_form_to_contact", "crm_form_submission", "Convert to Contact", "convert", "converted to contacts"),
      new Action("crm_activate_sequence", "crm_email_sequence", "Activate", "activate", "activated"),
      new Action("crm_pause_sequence", "crm_email_sequence", "Pause", "pause", "paused"));

   /*******************************************************************************
    ** Resolve exclusively by the installed process name, never client values.
    *******************************************************************************/
   public static Action action(String name)
   {
      return (ACTIONS.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow());
   }

   /*******************************************************************************
    ** Package-discovered metadata for every UI action.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance)
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      ACTIONS.forEach(action -> output.add(metadata(action)));
      return (output);
   }

   /*******************************************************************************
    ** Input details precede the standard ETL Review and Result screens.
    *******************************************************************************/
   public static QProcessMetaData metadata(Action action)
   {
      Map<String, Serializable> values = Map.of(
         StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, action.table(),
         StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, action.table(),
         StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, true,
         StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true,
         StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT,
         StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, action.label() + " for the selected records.");
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(CrmAction.Extract.class, CrmAction.Transform.class, CrmAction.Load.class, values)
         .withName(action.name()).withLabel(action.label()).withTableName(action.table()).withMinInputRecords(1)
         .withIcon(new QIcon("play_arrow")).withPermissionRules(CrmPermissions.processRules(action.name()));
      if(action.operation().equals("merge"))
      {
         process.withMinInputRecords(2).withMaxInputRecords(2);
      }
      List<QFieldMetaData> fields = inputFields(action);
      if(!fields.isEmpty())
      {
         QFrontendStepMetaData details = new QFrontendStepMetaData().withName("details").withLabel(action.label())
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM));
         fields.forEach(details::withFormField);
         process.withStep(0, details);
         if(List.of("stage", "reopen", "merge").contains(action.operation()))
         {
            process.withStep(0, new QBackendStepMetaData().withName("prepare").withCode(new QCodeReference(CrmAction.Prepare.class)));
         }
      }
      return (process);
   }

   /*******************************************************************************
    ** Required user input, including server-validated contingent stage choices.
    *******************************************************************************/
   public static List<QFieldMetaData> inputFields(Action action)
   {
      List<QFieldMetaData> fields = new ArrayList<>();
      switch(action.operation())
      {
         case "assign" -> fields.add(field("newOwnerUserId", "Owner", QFieldType.STRING, null));
         case "subscribe", "unsubscribe" -> fields.add(field("emailListId", "List", QFieldType.INTEGER, "crm_email_list"));
         case "enroll", "unenroll" -> fields.add(field("sequenceId", "Sequence", QFieldType.INTEGER, "crm_email_sequence"));
         case "merge" -> fields.add(field("primaryId", "Record to Keep", QFieldType.INTEGER, action.table())
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, "${input.mergeIds}"))));
         case "stage", "reopen" ->
         {
            fields.add(field("pipelineId", "Pipeline", QFieldType.INTEGER, "crm_pipeline").withIsEditable(false));
            fields.add(field("targetStageId", "Target Stage", QFieldType.INTEGER, "crm_pipeline_stage")
               .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, "${input.pipelineId}"))));
         }
         case "won", "lost" -> fields.add(field("winLossReasonId", "Win/Loss Reason", QFieldType.INTEGER, "crm_win_loss_reason"));
         case "createDeal" ->
         {
            fields.add(field("pipelineId", "Pipeline", QFieldType.INTEGER, "crm_pipeline"));
            fields.add(field("dealName", "Deal Name", QFieldType.STRING, null));
            fields.add(field("dealAmount", "Amount", QFieldType.DECIMAL, null).withIsRequired(false));
         }
         case "addContact" ->
         {
            fields.add(field("contactId", "Contact", QFieldType.INTEGER, "crm_contact"));
            fields.add(field("contactRoleId", "Role", QFieldType.INTEGER, "crm_contact_role").withIsRequired(false));
            fields.add(field("isPrimary", "Primary Contact", QFieldType.BOOLEAN, null).withIsRequired(false));
         }
         case "activity" ->
         {
            fields.add(field("activityTypeId", "Activity Type", QFieldType.INTEGER, "crm_activity_type"));
            fields.add(field("subject", "Subject", QFieldType.STRING, null));
            fields.add(field("description", "Description", QFieldType.TEXT, null).withIsRequired(false));
         }
         default ->
         {
         }
      }
      return (fields);
   }

   /*******************************************************************************
    ** Consistent required field metadata.
    *******************************************************************************/
   private static QFieldMetaData field(String name, String label, QFieldType type, String pvs)
   {
      return (new QFieldMetaData(name, type).withLabel(label).withIsRequired(true).withPossibleValueSourceName(pvs));
   }
}
