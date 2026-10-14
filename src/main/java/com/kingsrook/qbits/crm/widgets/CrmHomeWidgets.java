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


package com.kingsrook.qbits.crm.widgets;


import java.util.List;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Standard widgets for the single CRM home. Today holds five statistics.
 *******************************************************************************/
public class CrmHomeWidgets extends MetaDataProducer<MetaDataProducerMultiOutput>
{
   public static final List<String> HOME = List.of("crm_today", "crm_pipeline_summary", "crm_deal_forecast", "crm_rotting_deals",
      "crm_recent_web_leads", "crm_goals", "crm_sales_leaderboard");
   public static final List<String> TODAY = List.of("crm_open_pipeline", "crm_won_month", "crm_new_contacts", "crm_new_web_leads", "crm_overdue_activities");


   /*******************************************************************************
    ** All metrics and tables use the same session-aware data renderer.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance)
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      List<String> labels = List.of("Today", "Pipeline by stage", "Forecast", "Needs attention", "Recent web leads", "Goals", "Leaderboard");
      for(int i = 0; i < HOME.size(); i++)
      {
         output.add(widget(HOME.get(i), labels.get(i), i == 0 ? WidgetType.PARENT_WIDGET : WidgetType.TABLE));
      }
      List<String> statistics = List.of("Open pipeline (base currency)", "Won this month (base currency)", "New contacts (30 days)", "Web leads (7 days)", "My overdue activities");
      for(int i = 0; i < TODAY.size(); i++)
      {
         output.add(widget(TODAY.get(i), statistics.get(i), WidgetType.STATISTICS));
      }
      return (output);
   }


   /*******************************************************************************
    ** A full-width, permission-protected standard widget.
    *******************************************************************************/
   private QWidgetMetaData widget(String name, String label, WidgetType type)
   {
      QWidgetMetaData widget = new QWidgetMetaData().withName(name).withLabel(label).withType(type.getType()).withGridColumns(12)
         .withIsCard(type != WidgetType.PARENT_WIDGET).withShowReloadButton(true)
         .withPermissionRules(CrmPermissions.hasAccessRules(CrmPermissions.BASE))
         .withCodeReference(new QCodeReference(CrmHomeRenderer.class));
      if("crm_goals".equals(name))
      {
         widget.getPermissionRules().withCustomPermissionChecker(new QCodeReference(CrmGoalsVisibility.class));
      }
      return (widget);
   }
}
