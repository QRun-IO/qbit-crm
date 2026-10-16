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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The Setup card: what is missing is called out with its consequence, what is
 ** in place is marked set up, and the card renders on the home.
 *******************************************************************************/
class CrmSetupStatusTest extends BaseTest
{
   /*******************************************************************************
    ** Nothing configured: the business setup the CRM needs are flagged.
    *******************************************************************************/
   @Test
   void emptyInstallNeedsSetup() throws Exception
   {
      Map<String, CrmSetupStatus.Item> items = items();
      assertThat(items).containsOnlyKeys("Default pipeline", "Pipeline stages", "Lifecycle stages", "Base currency");
      assertThat(items.get("Default pipeline").state()).isEqualTo(CrmSetupStatus.State.TODO);
      assertThat(items.get("Default pipeline").detail()).contains("Convert lead fails");
      assertThat(items.get("Pipeline stages").state()).isEqualTo(CrmSetupStatus.State.TODO);
      assertThat(items.get("Lifecycle stages").detail()).contains("Lead and Opportunity");
      assertThat(items.get("Base currency").state()).isEqualTo(CrmSetupStatus.State.TODO);
   }



   /*******************************************************************************
    ** A default pipeline without a closed-won or closed-lost stage still needs setup.
    *******************************************************************************/
   @Test
   void pipelineNeedsClosedStages() throws Exception
   {
      CrmHomeWidgetsTest.seed("crm_pipeline", new QRecord().withValue("id", 1).withValue("name", "Sales").withValue("isDefault", true));
      assertThat(items().get("Default pipeline").state()).isEqualTo(CrmSetupStatus.State.OK);
      assertThat(items().get("Pipeline stages").detail()).contains("no stages");

      CrmHomeWidgetsTest.seed("crm_pipeline_stage", new QRecord().withValue("id", 1).withValue("pipelineId", 1).withValue("name", "Open").withValue("sortOrder", 1));
      assertThat(items().get("Pipeline stages").state()).isEqualTo(CrmSetupStatus.State.TODO);
      assertThat(items().get("Pipeline stages").detail()).contains("no Closed Won");

      CrmHomeWidgetsTest.seed("crm_pipeline_stage", new QRecord().withValue("id", 2).withValue("pipelineId", 1).withValue("name", "Won").withValue("sortOrder", 2).withValue("isClosedWon", true));
      assertThat(items().get("Pipeline stages").detail()).contains("no Closed Lost");

      CrmHomeWidgetsTest.seed("crm_pipeline_stage", new QRecord().withValue("id", 3).withValue("pipelineId", 1).withValue("name", "Lost").withValue("sortOrder", 3).withValue("isClosedLost", true));
      assertThat(items().get("Pipeline stages").state()).isEqualTo(CrmSetupStatus.State.OK);
   }



   /*******************************************************************************
    ** Lifecycle stages name exactly what is missing; currency needs a base.
    *******************************************************************************/
   @Test
   void lifecycleAndCurrency() throws Exception
   {
      CrmHomeWidgetsTest.seed("crm_lifecycle_stage", new QRecord().withValue("id", 1).withValue("name", "Lead"));
      assertThat(items().get("Lifecycle stages").detail()).contains("Missing Opportunity").doesNotContain("web leads");
      assertThat(items().get("Lifecycle stages").nextStep()).contains("\"Opportunity\"");
      CrmHomeWidgetsTest.seed("crm_lifecycle_stage", new QRecord().withValue("id", 2).withValue("name", "Opportunity"));
      assertThat(items().get("Lifecycle stages").state()).isEqualTo(CrmSetupStatus.State.OK);

      CrmHomeWidgetsTest.seed("crm_currency", new QRecord().withValue("currencyCode", "EUR").withValue("isBaseCurrency", false));
      assertThat(items().get("Base currency").state()).isEqualTo(CrmSetupStatus.State.TODO);
      CrmHomeWidgetsTest.seed("crm_currency", new QRecord().withValue("currencyCode", "USD").withValue("isBaseCurrency", true));
      assertThat(items().get("Base currency").detail()).startsWith("USD");
   }



   /*******************************************************************************
    ** The card renders on the home with one linked row per item.
    *******************************************************************************/
   @Test
   void rendersOnTheHome() throws Exception
   {
      assertThat(CrmHomeWidgets.HOME).contains("crm_setup");
      TableData data = (TableData) CrmHomeWidgetsTest.render("crm_setup");
      assertThat(data.getRows()).hasSize(4);
      CrmHomeWidgetsTest.assertCell(data, 0, "first", "Default pipeline");
      assertThat(data.getRows().get(0).get("second").toString()).contains("Needs setup");
      
   }



   /*******************************************************************************
    ** The checklist is business setup only: no email-provider row.
    *******************************************************************************/
   @Test
   void noEmailProviderRow() throws Exception
   {
      assertThat(items().keySet()).noneMatch(label -> label.toLowerCase().contains("email"));
      TableData data = (TableData) CrmHomeWidgetsTest.render("crm_setup");
      assertThat(data.getRows().toString()).doesNotContain("email qbit").doesNotContain("Email delivery");
   }



   /*******************************************************************************
    ** Settings holders get the card while setup is incomplete, and not once complete.
    *******************************************************************************/
   @Test
   void settingsHolderSeesCardOnlyWhileIncomplete() throws Exception
   {
      assertThat(PermissionsHelper.hasWidgetPermission(new RenderWidgetInput(), "crm_setup")).isTrue();

      completeSetup();
      assertThat(CrmSetupStatus.isIncomplete()).isFalse();
      assertThat(PermissionsHelper.hasWidgetPermission(new RenderWidgetInput(), "crm_setup")).isFalse();
      assertThatThrownBy(() -> renderEnforced()).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** A CRM user without the settings permission never gets the card, incomplete or not.
    *******************************************************************************/
   @Test
   void nonSettingsUserNeverSeesCard() throws Exception
   {
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.APP_ACCESS, CrmPermissions.READ, CrmPermissions.WRITE));
      assertThat(CrmSetupStatus.isIncomplete()).isTrue();
      assertThat(PermissionsHelper.hasWidgetPermission(new RenderWidgetInput(), "crm_setup")).isFalse();
      assertThatThrownBy(() -> renderEnforced()).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** Render the setup widget the way the UI API does, with permissions enforced.
    *******************************************************************************/
   private void renderEnforced() throws Exception
   {
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(QContext.getQInstance().getWidget("crm_setup"));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      new RenderWidgetAction().execute(input);
   }



   /*******************************************************************************
    ** Seed everything the checklist asks for.
    *******************************************************************************/
   private void completeSetup() throws Exception
   {
      CrmHomeWidgetsTest.seed("crm_pipeline", new QRecord().withValue("id", 1).withValue("name", "Sales").withValue("isDefault", true));
      CrmHomeWidgetsTest.seed("crm_pipeline_stage", new QRecord().withValue("id", 2).withValue("pipelineId", 1).withValue("name", "Won").withValue("sortOrder", 2).withValue("isClosedWon", true));
      CrmHomeWidgetsTest.seed("crm_pipeline_stage", new QRecord().withValue("id", 3).withValue("pipelineId", 1).withValue("name", "Lost").withValue("sortOrder", 3).withValue("isClosedLost", true));
      CrmHomeWidgetsTest.seed("crm_lifecycle_stage", new QRecord().withValue("id", 1).withValue("name", "Lead"));
      CrmHomeWidgetsTest.seed("crm_lifecycle_stage", new QRecord().withValue("id", 2).withValue("name", "Opportunity"));
      CrmHomeWidgetsTest.seed("crm_currency", new QRecord().withValue("currencyCode", "USD").withValue("isBaseCurrency", true));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, CrmSetupStatus.Item> items() throws Exception
   {
      List<CrmSetupStatus.Item> items = CrmSetupStatus.load();
      return (items.stream().collect(Collectors.toMap(CrmSetupStatus.Item::label, item -> item, (a, b) -> a, java.util.LinkedHashMap::new)));
   }

}
