/*******************************************************************************
 ** Unit tests for CrmQBitProducer.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
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
      assertThat(metaData.getVersion()).isEqualTo(CrmQBitProducer.VERSION);
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
      assertThat(CrmQBitProducer.VERSION).isEqualTo("0.1.0");
   }

}
