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
import com.kingsrook.qbits.crm.help.CrmHelpBundleStandIn;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
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
    ** Help covers app every table main field process and enum.
    *******************************************************************************/
   @Test
   void helpCoversAppEveryTableMainFieldProcessAndEnum() throws Exception
   {
      var entries = CrmHelpBundleStandIn.entries();
      var instance = QContext.getQInstance();
      assertThat(entries).containsKey("app:crm;slot:help");
      for(var table : instance.getTables().values())
      {
         assertThat(entries).containsKey("table:" + table.getName() + ";slot:help");
         assertThat(table.getHelpContent().get("help")).isNotEmpty();
         if(CrmAppMetaData.MAIN_TABLES.contains(table.getName()))
         {
            for(var field : table.getFields().values())
            {
               assertThat(entries).containsKey("table:" + table.getName() + ";field:" + field.getName());
               assertThat(field.getHelpContents()).as(table.getName() + "." + field.getName()).isNotEmpty();
            }
         }
      }
      for(var process : instance.getProcesses().values())
      {
         if(process.getName().startsWith("crm_"))
         {
            assertThat(entries).containsKey("process:" + process.getName() + ";slot:help");
            // Core creates dotted bulk-process names after QBit production; they retain core screen help.
            if(!process.getName().contains("."))
            {
               process.getStepList().stream().filter(QFrontendStepMetaData.class::isInstance).map(QFrontendStepMetaData.class::cast)
                  .forEach(step -> assertThat(step.getHelpContents()).isNotEmpty());
            }
         }
      }
      for(var pvs : instance.getPossibleValueSources().values())
      {
         if(pvs.getType() == QPossibleValueSourceType.ENUM && pvs.getName().startsWith("Crm"))
         {
            assertThat(entries).containsKey("possibleValueSource:" + pvs.getName() + ";slot:help");
         }
      }
      entries.values().forEach(entry ->
      {
         assertThat(entry.get("summary").asText()).hasSizeLessThanOrEqualTo(120).isNotBlank();
         assertThat(entry.get("content").asText()).isNotBlank();
      });
   }
}

