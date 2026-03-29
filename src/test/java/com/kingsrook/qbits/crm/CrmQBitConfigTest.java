/*******************************************************************************
 ** Unit tests for CrmQBitConfig.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
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
         .withTableNamePrefix("crm_")
         .withTableMetaDataCustomizer(customizer)
         .withSecurityFields(securityFields)
         .withRecordSecurityLocks(locks)
         .withQuickSearchQBitNamespace("quickSearch")
         .withWebhooksQBitNamespace("webhooks")
         .withWorkflowsQBitNamespace("workflows");

      assertThat(config.getDefaultBackendNameForTables()).isEqualTo("myBackend");
      assertThat(config.getTableNamePrefix()).isEqualTo("crm_");
      assertThat(config.getTableMetaDataCustomizer()).isSameAs(customizer);
      assertThat(config.getSecurityFields()).isSameAs(securityFields);
      assertThat(config.getRecordSecurityLocks()).isSameAs(locks);
      assertThat(config.getQuickSearchQBitNamespace()).isEqualTo("quickSearch");
      assertThat(config.getWebhooksQBitNamespace()).isEqualTo("webhooks");
      assertThat(config.getWorkflowsQBitNamespace()).isEqualTo("workflows");
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
      assertThat(CrmQBitConfig.DEFAULT_CONFIG.getDefaultBackendNameForTables()).isNull();
   }

}
