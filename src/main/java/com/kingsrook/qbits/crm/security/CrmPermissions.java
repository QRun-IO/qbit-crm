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


package com.kingsrook.qbits.crm.security;


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;


/*******************************************************************************
 ** Standard QQQ permission suffixes for CRM data, settings and administration.
 *******************************************************************************/
public final class CrmPermissions
{
   public static final String BASE = "crm";
   public static final String MANAGE_BASE = "crm.manage";
   public static final String ADMIN_BASE = "crm.admin";
   public static final String APP_ACCESS = BASE + ".hasAccess";
   public static final String READ = BASE + ".read";
   public static final String WRITE = BASE + ".write";
   public static final String MANAGE = MANAGE_BASE + ".hasAccess";
   public static final String MANAGE_READ = MANAGE_BASE + ".read";
   public static final String MANAGE_WRITE = MANAGE_BASE + ".write";
   public static final String ADMIN = ADMIN_BASE + ".hasAccess";
   public static final String ADMIN_READ = ADMIN_BASE + ".read";
   public static final String ADMIN_WRITE = ADMIN_BASE + ".write";
   public static final List<String> ALL = List.of(APP_ACCESS, READ, WRITE, MANAGE, MANAGE_READ, MANAGE_WRITE, ADMIN, ADMIN_READ, ADMIN_WRITE);

   private static final Set<String> SETTINGS_TABLES = Set.of(
      "crm_job_state", "crm_pipeline", "crm_product", "crm_email_template", "crm_email_sequence",
      "crm_lead_score_rule", "crm_assignment_rule", "crm_sales_goal", "crm_lifecycle_stage",
      "crm_lead_source", "crm_industry", "crm_activity_type", "crm_activity_outcome",
      "crm_contact_role", "crm_win_loss_reason");

   private static final Set<String> MANAGEMENT_PROCESSES = Set.of(
      "crm_merge_contacts", "crm_merge_companies", "crm_bulk_assign_owner",
      "crm_subscribe_to_list", "crm_unsubscribe_from_list", "crm_enroll_in_sequence",
      "crm_unenroll_from_sequence", "crm_process_sequence_steps", "crm_assign_company_owner",
      "crm_transfer_ownership", "crm_activate_sequence", "crm_pause_sequence", "crm_sync_email_delivery", "crm_expire_consents", "crm_calculate_lead_scores");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmPermissions()
   {
   }



   /*******************************************************************************
    ** Main and child data use CRM permissions; settings and currency are separate.
    *******************************************************************************/
   public static QPermissionRules tableRules(String tableName)
   {
      String base = "crm_currency".equals(tableName) || SETTINGS_TABLES.contains(tableName) ? MANAGE_BASE : BASE;
      return (rules(PermissionLevel.READ_WRITE_PERMISSIONS, base));
   }



   /*******************************************************************************
    ** Erasure is administrative; management actions have a separate access grant.
    *******************************************************************************/
   public static QPermissionRules processRules(String processName)
   {
      String base = "crm_gdpr_anonymize_contact".equals(processName) ? ADMIN_BASE : MANAGEMENT_PROCESSES.contains(processName) ? MANAGE_BASE : BASE;
      return (hasAccessRules(base));
   }



   /*******************************************************************************
    ** Processes and widgets use the standard hasAccess suffix.
    *******************************************************************************/
   public static QPermissionRules hasAccessRules(String base)
   {
      return (rules(PermissionLevel.HAS_ACCESS_PERMISSION, base));
   }



   /*******************************************************************************
    ** Set both protection and denial explicitly, independent of host defaults.
    *******************************************************************************/
   private static QPermissionRules rules(PermissionLevel level, String base)
   {
      return (new QPermissionRules().withLevel(level).withPermissionBaseName(base).withDenyBehavior(DenyBehavior.HIDDEN));
   }

}
