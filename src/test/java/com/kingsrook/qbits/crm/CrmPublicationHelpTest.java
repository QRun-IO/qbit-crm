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
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.testing.HelpBundleAssertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Published fields, search declaration and shipped help coverage.
 *******************************************************************************/
class CrmPublicationHelpTest extends BaseTest
{
   /*******************************************************************************
    ** Api matches every published table.
    *******************************************************************************/
   @Test
   void apiMatchesEveryPublishedTable() throws Exception
   {
      QInstance instance = P4TestSupport.host();
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(new ApiInstanceMetaData()
         .withName("host").withPath("/api/").withLabel("Host API").withDescription("CRM test API").withContactEmail("test@example.invalid").withCurrentVersion(new APIVersion("v1")).withSupportedVersions(List.of(new APIVersion("v1")))));
      new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME)
         .withApiName("host").withApiVersion("v1")).produce(instance).addSelfToInstance(instance);
      new QInstanceValidator().validate(instance);
      QContext.setQInstance(instance);
      GetTableApiFieldsAction.clearCaches();
      List<String> published = new ArrayList<>(CrmAppMetaData.MAIN_TABLES);
      published.addAll(CrmAppMetaData.SETTINGS_TABLES);
      for(String name : published)
      {
         var table = instance.getTable(name);
         assertThat(ApiTableMetaDataContainer.of(table).getApiTableMetaData("host")).isNotNull();
         var fields = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsInput().withApiName("host").withVersion("v1").withTableName(name));
         assertThat(fields.keySet()).as(name).containsExactlyInAnyOrderElementsOf(table.getFields().keySet());
      }
      assertThat(ApiTableMetaDataContainer.of(instance.getTable("crm_deal_product"))).isNull();
      GetTableApiFieldsAction.clearCaches();
   }


   /*******************************************************************************
    ** Esb fails closed until core excludes classified values.
    *******************************************************************************/
   @Test
   void esbFailsClosedUntilCoreExcludesClassifiedValues()
   {
      assertThatThrownBy(() -> new CrmQBitProducer().withQBitConfig(new CrmQBitConfig()
         .withDefaultBackendNameForTables(BACKEND_NAME).withEsbDestinationName("crmEvents")).produce(P4TestSupport.host()))
         .hasMessageContaining("classified-value exclusion");
   }


   /*******************************************************************************
    ** Main tables declare search fields.
    *******************************************************************************/
   @Test
   void mainTablesDeclareSearchFields()
   {
      for(String table : CrmAppMetaData.MAIN_TABLES)
      {
         assertThat(QContext.getQInstance().getTable(table).getSearchFields()).isNotEmpty();
      }
      assertThat(QContext.getQInstance().getTable("crm_contact").getSearchFields()).containsExactly("firstName", "lastName", "email");
      assertThat(QContext.getQInstance().getTable("crm_company").getSearchFields()).containsExactly("name", "domain");
   }


   /*******************************************************************************
    ** Native registration is safe without help tables and covers all CRM metadata.
    *******************************************************************************/
   @Test
   void helpCoversOwnedMetadataWithoutHelpTable()
   {
      var instance = QContext.getQInstance();
      assertThat(instance.getTable("helpContent")).isNull();
      assertThat(HelpBundles.of(instance).getBundles()).singleElement().satisfies(bundle ->
      {
         assertThat(bundle.getId()).isEqualTo("qbit-crm");
         assertThat(bundle.getVersion()).isEqualTo(CrmQBitProducer.VERSION);
         assertThat(bundle.getLayer().name()).isEqualTo("QBIT");
      });
      HelpBundles.syncOnStartup(instance);
      HelpBundleAssertions.forInstance(instance)
         .bundles("help/qbit-crm.yaml")
         .owning(target -> target.name().equals("crm") || target.name().startsWith("crm_") || target.name().startsWith("Crm"))
         .assertComplete();
      // Core's gate validates these entries but does not require widget or possible-value coverage.
      var entries = HelpBundles.of(instance).resolveWinningEntries();
      instance.getWidgets().keySet().stream().filter(name -> name.startsWith("crm_")).forEach(name ->
         assertThat(entries).containsKey(new HelpBundles.EntryKey("widget:" + name + ";slot:label", "ALL_SCREENS")));
      instance.getPossibleValueSources().keySet().stream().filter(name -> name.startsWith("crm_") || name.startsWith("Crm")).forEach(name ->
         assertThat(entries).containsKey(new HelpBundles.EntryKey("possibleValueSource:" + name + ";slot:help", "ALL_SCREENS")));
   }
}
