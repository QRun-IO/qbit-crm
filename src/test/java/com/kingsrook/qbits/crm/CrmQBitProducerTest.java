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


/*******************************************************************************
 ** Unit tests for CrmQBitProducer.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;
import com.kingsrook.qqq.backend.module.rdbms.actions.AbstractRDBMSAction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for CrmQBitProducer: metadata, app section, and fluent setters.
 *******************************************************************************/
class CrmQBitProducerTest extends BaseTest
{

   /*******************************************************************************
    ** Test getQBitMetaData() returns correct groupId/artifactId/version.
    *******************************************************************************/
   @Test
   void testGetQBitMetaData()
   {
      CrmQBitProducer producer = new CrmQBitProducer()
         .withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables("memory"));

      QBitMetaData metaData = producer.getQBitMetaData();

      assertThat(metaData).isNotNull();
      assertThat(metaData.getGroupId()).isEqualTo(CrmQBitProducer.GROUP_ID);
      assertThat(metaData.getArtifactId()).isEqualTo(CrmQBitProducer.ARTIFACT_ID);
      assertThat(metaData.getVersion()).isEqualTo(System.getProperty("qbit.artifactVersion")).isEqualTo(CrmQBitProducer.VERSION);
   }



   /*******************************************************************************
    ** Test produceAppSection() returns a non-null section.
    *******************************************************************************/
   @Test
   void testProduceAppSection()
   {
      QAppSection section = CrmQBitProducer.produceAppSection();

      assertThat(section).isNotNull();
      assertThat(section.getName()).isEqualTo("crm");
      assertThat(section.getLabel()).isEqualTo("CRM");
      assertThat(section.getIcon()).isNotNull();
      assertThat(section.getIcon().getName()).isEqualTo("contacts");
   }



   /*******************************************************************************
    ** Test fluent setter and getter for qBitConfig.
    *******************************************************************************/
   @Test
   void testFluentSetterQBitConfig()
   {
      CrmQBitConfig config = new CrmQBitConfig()
         .withDefaultBackendNameForTables("memory");

      CrmQBitProducer producer = new CrmQBitProducer();
      producer.setQBitConfig(config);
      assertThat(producer.getQBitConfig()).isSameAs(config);

      CrmQBitConfig config2 = new CrmQBitConfig()
         .withDefaultBackendNameForTables("rdbms");
      CrmQBitProducer returned = producer.withQBitConfig(config2);
      assertThat(returned).isSameAs(producer);
      assertThat(producer.getQBitConfig()).isSameAs(config2);
   }



   /*******************************************************************************
    ** Test constants are set correctly.
    *******************************************************************************/
   @Test
   void testConstants()
   {
      assertThat(CrmQBitProducer.GROUP_ID).isEqualTo("com.kingsrook.qbits");
      assertThat(CrmQBitProducer.ARTIFACT_ID).isEqualTo("crm");
      assertThat(CrmQBitProducer.VERSION).isEqualTo(System.getProperty("qbit.artifactVersion"));
   }

   /*******************************************************************************
    ** Every produced table and field uses the standard backend names.
    *******************************************************************************/
   @Test
   void testBackendNames()
   {
      QContext.getQInstance().getTables().values().forEach(table ->
      {
         assertThat(table.getName()).startsWith("crm_");
         assertThat(AbstractRDBMSAction.getTableName(table)).isEqualTo(table.getName());
         table.getFields().values().forEach(field ->
            assertThat(field.getBackendName()).isEqualTo(field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT)));
      });
   }

   /*******************************************************************************
    ** Platform audit replaces bespoke tables on the four tracked entities.
    *******************************************************************************/
   @Test
   void testCoreAuditRules()
   {
      for(String tableName : java.util.List.of("crm_contact", "crm_company", "crm_deal", "crm_activity"))
      {
         assertThat(QContext.getQInstance().getTable(tableName).getAuditRules().getAuditLevel()).isEqualTo(AuditLevel.FIELD);
      }
      assertThat(QContext.getQInstance().getTables()).hasSize(34)
         .doesNotContainKeys("crm_tag", "crm_contact_tag", "crm_company_tag", "crm_deal_tag", "crm_attachment", "crm_audit_log");
      assertThat(QContext.getQInstance().getProcesses()).doesNotContainKeys("crm_bulk_add_tag", "crm_bulk_remove_tag");
   }

   /*******************************************************************************
    ** All produced metadata is protected and attributed to this QBit.
    *******************************************************************************/
   @Test
   void testPermissionsAndProvenance() throws QException
   {
      MetaDataProducerMultiOutput produced = new CrmQBitProducer()
         .withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME))
         .produce(new com.kingsrook.qqq.backend.core.model.metadata.QInstance());
      String source = new CrmQBitProducer().getQBitMetaData().getName();
      QContext.getQInstance().getTables().values().forEach(table ->
      {
         assertThat(table.getName()).matches("crm_[a-z0-9_]+");
         assertThat(table.getSourceQBitName()).isEqualTo(source);
         assertThat(table.getPermissionRules()).isNotNull();
         assertThat(table.getPermissionRules().getDenyBehavior()).isEqualTo(DenyBehavior.HIDDEN);
         assertThat(table.getPermissionRules().getLevel()).isEqualTo(PermissionLevel.READ_WRITE_PERMISSIONS);
      });
      produced.getEach(QProcessMetaData.class).forEach(process ->
      {
         assertThat(process.getName()).matches("crm_[a-z0-9_]+");
         assertThat(process.getSourceQBitName()).isEqualTo(source);
         assertThat(process.getPermissionRules()).isNotNull();
         assertThat(process.getPermissionRules().getDenyBehavior()).isEqualTo(DenyBehavior.HIDDEN);
         assertThat(process.getPermissionRules().getLevel()).isEqualTo(PermissionLevel.HAS_ACCESS_PERMISSION);
      });
      produced.getEach(QWidgetMetaData.class).forEach(widget ->
      {
         assertThat(widget.getName()).matches("crm_[a-z0-9_]+");
         assertThat(widget).isInstanceOf(SourceQBitAware.class);
         assertThat(((SourceQBitAware) widget).getSourceQBitName()).isEqualTo(source);
         assertThat(widget.getPermissionRules()).isNotNull();
         assertThat(widget.getPermissionRules().getDenyBehavior()).isEqualTo(DenyBehavior.HIDDEN);
         assertThat(widget.getPermissionRules().getLevel()).isEqualTo(PermissionLevel.HAS_ACCESS_PERMISSION);
      });
      QContext.getQInstance().getJoins().values().forEach(join -> assertThat(join.getName()).matches("crm_[a-z0-9_]+"));
   }

}
