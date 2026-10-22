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
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qbits.crm.widgets.CrmHomeWidgets;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** The CRM home and the two navigation sections. Child tables stay on records.
 *******************************************************************************/
public final class CrmAppMetaData
{
   public static final List<String> MAIN_TABLES = List.of("crm_contact", "crm_company", "crm_deal", "crm_activity", "crm_form_submission", "crm_email_list");
   public static final List<String> SETTINGS_TABLES = List.of("crm_pipeline", "crm_product", "crm_email_template", "crm_email_sequence",
      "crm_lead_score_rule", "crm_assignment_rule", "crm_sales_goal", "crm_currency", "crm_lifecycle_stage", "crm_lead_source",
      "crm_industry", "crm_activity_type", "crm_activity_outcome", "crm_contact_role", "crm_win_loss_reason");


   /*******************************************************************************
    ** Metadata factory only.
    *******************************************************************************/
   private CrmAppMetaData()
   {
   }


   /*******************************************************************************
    ** Main navigation, also available to hosts embedding these tables.
    *******************************************************************************/
   public static QAppSection mainSection()
   {
      return (new QAppSection().withName("crm").withLabel("CRM").withIcon(new QIcon("contacts"))
         .withTables(new ArrayList<>(MAIN_TABLES)));
   }


   /*******************************************************************************
    ** Construct the app after package discovery has produced the tables.
    *******************************************************************************/
   public static QAppMetaData build(MetaDataProducerMultiOutput output, String source)
   {
      List<String> settings = SETTINGS_TABLES.stream().filter(name -> output.get(QTableMetaData.class, name) != null).toList();
      QAppMetaData app = new QAppMetaData().withName("crm").withLabel("CRM").withIcon(new QIcon("contacts"))
         .withPermissionRules(CrmPermissions.hasAccessRules(CrmPermissions.BASE))
         .withWidgets(new ArrayList<>(CrmHomeWidgets.HOME))
         .withSection(mainSection())
         .withSection(new QAppSection().withName("crm_settings").withLabel("Settings").withIcon(new QIcon("settings"))
            .withTables(new ArrayList<>(settings)));
      List<String> tables = new ArrayList<>(MAIN_TABLES);
      tables.addAll(settings);
      for(String name : tables)
      {
         app.withChild(output.get(QTableMetaData.class, name));
      }
      return (app);
   }
}
