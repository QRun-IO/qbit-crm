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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** CRM presentation shared by package-discovered tables and child widgets.
 *******************************************************************************/
public final class CrmPresentation
{
   private static final Map<String, String> LABELS = Map.ofEntries(
      Map.entry("crm_contact", "Contacts"),
      Map.entry("crm_company", "Companies"),
      Map.entry("crm_deal", "Deals"),
      Map.entry("crm_activity", "Activities"),
      Map.entry("crm_form_submission", "Web Leads"),
      Map.entry("crm_email_list", "Lists"),
      Map.entry("crm_pipeline", "Pipelines"),
      Map.entry("crm_pipeline_stage", "Pipeline Stages"),
      Map.entry("crm_product", "Products"),
      Map.entry("crm_email_template", "Email Templates"),
      Map.entry("crm_email_sequence", "Email Sequences"),
      Map.entry("crm_sequence_step", "Sequence Steps"),
      Map.entry("crm_sequence_enrollment", "Sequence Enrollments"),
      Map.entry("crm_lead_score_rule", "Lead Score Rules"),
      Map.entry("crm_assignment_rule", "Assignment Rules"),
      Map.entry("crm_assignment_rule_member", "Assignment Rule Members"),
      Map.entry("crm_sales_goal", "Sales Goals"),
      Map.entry("crm_currency", "Currencies"),
      Map.entry("crm_lifecycle_stage", "Lifecycle Stages"),
      Map.entry("crm_lead_source", "Lead Sources"),
      Map.entry("crm_industry", "Industries"),
      Map.entry("crm_activity_type", "Activity Types"),
      Map.entry("crm_activity_outcome", "Activity Outcomes"),
      Map.entry("crm_contact_role", "Contact Roles"),
      Map.entry("crm_win_loss_reason", "Win/Loss Reasons"),
      Map.entry("crm_consent_record", "Consent Records"),
      Map.entry("crm_email_list_member", "List Members"),
      Map.entry("crm_email_message", "Email Messages"),
      Map.entry("crm_deal_contact", "Deal Contacts"),
      Map.entry("crm_deal_product", "Deal Products"),
      Map.entry("crm_deal_stage_history", "Deal Stage History"),
      Map.entry("crm_activity_participant", "Activity Participants"),
      Map.entry("crm_external_mapping", "External Mappings"),
      Map.entry("crm_job_state", "Job State"));
   private static final Map<String, List<String>> CHILD_FIELDS = Map.ofEntries(
      Map.entry("crm_contact", List.of("firstName", "lastName", "email", "phone")),
      Map.entry("crm_deal", List.of("name", "pipelineStageId", "amount", "expectedCloseDate")),
      Map.entry("crm_activity", List.of("subject", "activityTypeId", "startDate")),
      Map.entry("crm_form_submission", List.of("formKey", "createDate", "conversionStatus")),
      Map.entry("crm_email_list_member", List.of("emailListId", "status")),
      Map.entry("crm_consent_record", List.of("consentType", "status", "consentDate")),
      Map.entry("crm_sequence_enrollment", List.of("sequenceId", "status", "enrolledDate")),
      Map.entry("crm_email_message", List.of("subject", "status", "sentDate")),
      Map.entry("crm_deal_contact", List.of("contactId", "contactRoleId", "isPrimary")),
      Map.entry("crm_deal_product", List.of("productId", "quantity", "unitPrice", "totalAmount")),
      Map.entry("crm_deal_stage_history", List.of("fromStageId", "toStageId", "transitionDate", "userId")));


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmPresentation()
   {
   }


   /*******************************************************************************
    ** Explicit labels avoid exposing technical table and user-id names.
    *******************************************************************************/
   public static void configureTable(QTableMetaData table)
   {
      table.setLabel(LABELS.get(table.getName()));
      Map<String, String> users = Map.of("ownerUserId", "Owner", "assignedToUserId", "Assigned To",
         "assignToUserId", "Assign To", "enrolledByUserId", "Enrolled By", "convertedByUserId", "Converted By", "userId", "User");
      table.getFields().values().forEach(field ->
      {
         if(users.containsKey(field.getName()))
         {
            field.setLabel(users.get(field.getName()));
         }
      });
      Map<String, List<String>> money = Map.of("crm_deal", List.of("amount", "amountInBaseCurrency", "weightedAmount"),
         "crm_deal_product", List.of("unitPrice", "totalAmount"), "crm_product", List.of("unitPrice"), "crm_sales_goal", List.of("targetAmount"));
      for(String field : money.getOrDefault(table.getName(), List.of()))
      {
         table.getField(field).setDisplayFormat(DisplayFormat.CURRENCY);
      }
   }


   /*******************************************************************************
    ** RC.6 supports a per-widget field list, preserving each full child table.
    *******************************************************************************/
   public static void configureChildList(QWidgetMetaData widget, String childTable)
   {
      List<String> fields = CHILD_FIELDS.get(childTable);
      if(fields != null)
      {
         widget.withDefaultValue(ChildRecordListRenderer.KEY_ONLY_INCLUDE_FIELD_NAMES, new ArrayList<>(fields));
      }
   }
}
