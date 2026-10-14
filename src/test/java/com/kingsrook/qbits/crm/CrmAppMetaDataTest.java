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


import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.widgets.CrmHomeWidgets;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** App navigation and record sections are tested against the validated instance.
 *******************************************************************************/
class CrmAppMetaDataTest extends BaseTest
{
   /*******************************************************************************
    ** Exactly six main and fifteen settings tables, no sub-tables or processes.
    *******************************************************************************/
   @Test
   void app()
   {
      QAppMetaData app = QContext.getQInstance().getApp("crm");
      assertThat(QContext.getQInstance().getApps()).hasSize(1);
      assertThat(app.getLabel()).isEqualTo("CRM");
      assertThat(app.getIcon().getName()).isEqualTo("contacts");
      assertThat(app.getSections()).hasSize(2);
      assertThat(app.getSections().get(0).getTables()).containsExactly("crm_contact", "crm_company", "crm_deal", "crm_activity", "crm_form_submission", "crm_email_list");
      assertThat(app.getSections().get(1).getTables()).containsExactly("crm_pipeline", "crm_product", "crm_email_template", "crm_email_sequence",
         "crm_lead_score_rule", "crm_assignment_rule", "crm_sales_goal", "crm_currency", "crm_lifecycle_stage", "crm_lead_source", "crm_industry",
         "crm_activity_type", "crm_activity_outcome", "crm_contact_role", "crm_win_loss_reason");
      assertThat(app.getSections().get(1).getLabel()).isEqualTo("Settings");
      for(QAppSection section : app.getSections())
      {
         assertThat(section.getProcesses()).isNullOrEmpty();
      }
      assertThat(app.getChildren()).hasSize(21);
      assertThat(app.getWidgets()).containsExactlyElementsOf(CrmHomeWidgets.HOME);
      assertThat(app.getPermissionRules().getPermissionBaseName()).isEqualTo("crm");
      assertThat(app.getPermissionRules().getLevel()).isEqualTo(PermissionLevel.HAS_ACCESS_PERMISSION);
      assertThat(((SourceQBitAware) app).getSourceQBitName()).isEqualTo("com.kingsrook.qbits:crm");
      assertThat(CrmQBitProducer.produceAppSection().getTables()).containsExactlyElementsOf(CrmAppMetaData.MAIN_TABLES);
      assertThat(QContext.getQInstance().getTable("crm_form_submission").getLabel()).isEqualTo("Web Leads");
      assertThat(QContext.getQInstance().getTable("crm_email_list").getLabel()).isEqualTo("Lists");
   }


   /*******************************************************************************
    ** Hosts can embed tables without the standalone app; null keeps the default.
    *******************************************************************************/
   @Test
   void optOut() throws Exception
   {
      assertThat(new CrmQBitConfig().getAppEnabled()).isTrue();
      for(Boolean enabled : new Boolean[] {false, true, null})
      {
         QInstance instance = new QInstance();
         new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME).withAppEnabled(enabled))
            .produce(instance).addSelfToInstance(instance);
         assertThat(instance.getApps()).hasSize(Boolean.FALSE.equals(enabled) ? 0 : 1);
         assertThat(instance.getTables()).hasSize(34);
      }
   }


   /*******************************************************************************
    ** Every child widget has a real join and both joined fields exist.
    *******************************************************************************/
   @Test
   void childLists()
   {
      Map<String, Set<String>> expected = Map.of(
         "crm_contact", Set.of("crm_deal", "crm_activity", "crm_email_list_member", "crm_consent_record", "crm_sequence_enrollment", "crm_email_message", "crm_form_submission"),
         "crm_company", Set.of("crm_contact", "crm_deal", "crm_activity"),
         "crm_deal", Set.of("crm_deal_contact", "crm_deal_product", "crm_deal_stage_history", "crm_activity"),
         "crm_pipeline", Set.of("crm_pipeline_stage"), "crm_email_sequence", Set.of("crm_sequence_step", "crm_sequence_enrollment"),
         "crm_email_list", Set.of("crm_email_list_member"), "crm_assignment_rule", Set.of("crm_assignment_rule_member"),
         "crm_activity", Set.of("crm_activity_participant", "crm_email_message"));
      QInstance instance = QContext.getQInstance();
      expected.forEach((parent, children) ->
      {
         Set<String> actual = instance.getTable(parent).getSections().stream().filter(section -> section.getWidgetName() != null)
            .map(section -> instance.getWidget(section.getWidgetName()))
            .filter(widget -> WidgetType.CHILD_RECORD_LIST.getType().equals(widget.getType()))
            .map(widget ->
            {
               String joinName = (String) widget.getDefaultValues().get("joinName");
               assertThat(joinName).matches("crm_[a-z0-9_]+");
               QJoinMetaData join = instance.getJoin(joinName);
               assertThat(join).isNotNull();
               assertThat(join.getLeftTable()).isEqualTo(parent);
               join.getJoinOns().forEach(on ->
               {
                  assertThat(instance.getTable(parent).getField(on.getLeftField())).isNotNull();
                  assertThat(instance.getTable(join.getRightTable()).getField(on.getRightField())).isNotNull();
               });
               if(parent.equals("crm_contact") && join.getRightTable().equals("crm_deal"))
               {
                  assertThat(join.getJoinOns().getFirst().getRightField()).isEqualTo("primaryContactId");
               }
               return (join.getRightTable());
            }).collect(Collectors.toSet());
         assertThat(actual).isEqualTo(children);
      });
      assertThat(instance.getTable("crm_contact").getSections()).extracting(QFieldSection::getWidgetName).contains("crm_contact_timeline");
   }


   /*******************************************************************************
    ** No retired trends; all widgets have explicit protection and full width.
    *******************************************************************************/
   @Test
   void widgets()
   {
      QInstance instance = QContext.getQInstance();
      assertThat(instance.getWidgets()).doesNotContainKeys("crm_win_rate", "crm_stage_duration", "crm_sales_cycle_length", "crm_pipeline_conversion", "crm_avg_deal_size", "crm_activity_feed");
      for(QWidgetMetaDataInterface widget : instance.getWidgets().values())
      {
         assertThat(widget.getPermissionRules()).isNotNull();
         assertThat(widget.getGridColumns()).isEqualTo(12);
         assertThat(widget.getType()).isNotEqualTo(WidgetType.HTML.getType());
      }
   }


   /*******************************************************************************
    ** Primary-contact accessors round-trip the new child-list relationship.
    *******************************************************************************/
   @Test
   void primaryContact()
   {
      Deal deal = new Deal().withPrimaryContactId(7);
      assertThat(deal.getPrimaryContactId()).isEqualTo(7);
      deal.setPrimaryContactId(8);
      assertThat(deal.getPrimaryContactId()).isEqualTo(8);
   }
}
