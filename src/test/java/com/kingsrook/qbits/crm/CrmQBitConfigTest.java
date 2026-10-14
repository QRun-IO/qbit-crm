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
 ** Unit tests for CrmQBitConfig.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for CrmQBitConfig: fluent setters, getters, and validation.
 *******************************************************************************/
class CrmQBitConfigTest
{

   /*******************************************************************************
    ** Test all fluent setters and corresponding getters.
    *******************************************************************************/
   @Test
   void testFluentSettersAndGetters()
   {
      List<QFieldMetaData> securityFields = List.of(
         new QFieldMetaData("tenantId", QFieldType.INTEGER));

      List<RecordSecurityLock> locks = List.of(
         new RecordSecurityLock().withFieldName("tenantId"));

      MetaDataCustomizerInterface<QTableMetaData> customizer = (qInstance, table) -> table;

      CrmQBitConfig config = new CrmQBitConfig()
         .withDefaultBackendNameForTables("myBackend")
         .withInferBackendNames(false)
         .withTableMetaDataCustomizer(customizer)
         .withSecurityFields(securityFields)
         .withRecordSecurityLocks(locks);

      assertThat(config.getDefaultBackendNameForTables()).isEqualTo("myBackend");
      assertThat(config.getInferBackendNames()).isFalse();
      assertThat(config.getTableMetaDataCustomizer()).isSameAs(customizer);
      assertThat(config.getSecurityFields()).isSameAs(securityFields);
      assertThat(config.getRecordSecurityLocks()).isSameAs(locks);
   }



   /*******************************************************************************
    ** Test setter methods (non-fluent).
    *******************************************************************************/
   @Test
   void testSetters()
   {
      CrmQBitConfig config = new CrmQBitConfig();

      config.setDefaultBackendNameForTables("backend1");
      assertThat(config.getDefaultBackendNameForTables()).isEqualTo("backend1");

      MetaDataCustomizerInterface<QTableMetaData> customizer = (qInstance, table) -> table;
      config.setTableMetaDataCustomizer(customizer);
      assertThat(config.getTableMetaDataCustomizer()).isSameAs(customizer);
   }



   /*******************************************************************************
    ** Test validate() with missing backendName triggers error.
    *******************************************************************************/
   @Test
   void testValidateMissingBackendName()
   {
      CrmQBitConfig config = new CrmQBitConfig();

      List<String> errors = new ArrayList<>();
      config.validate(new QInstance(), errors);

      assertThat(errors).isNotEmpty();
      assertThat(errors.get(0)).contains("defaultBackendNameForTables");
   }



   /*******************************************************************************
    ** Test validate() with valid config passes.
    *******************************************************************************/
   @Test
   void testValidateValidConfig()
   {
      CrmQBitConfig config = new CrmQBitConfig()
         .withDefaultBackendNameForTables("memory");

      List<String> errors = new ArrayList<>();
      config.validate(new QInstance(), errors);

      assertThat(errors).isEmpty();
   }



   /*******************************************************************************
    ** Test DEFAULT_CONFIG constant is non-null.
    *******************************************************************************/
   @Test
   void testDefaultConfig()
   {
      assertThat(CrmQBitConfig.DEFAULT_CONFIG).isNotNull();
      assertThat(CrmQBitConfig.DEFAULT_CONFIG.getInferBackendNames()).isTrue();
      assertThat(CrmQBitConfig.DEFAULT_CONFIG.getDefaultBackendNameForTables()).isNull();
   }

}
