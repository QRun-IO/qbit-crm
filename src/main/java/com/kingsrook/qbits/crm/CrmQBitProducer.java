/*******************************************************************************
 ** QBitMetaDataProducer for the CRM QBit.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;


/*******************************************************************************
 ** Producer for the CRM QBit. Auto-discovers all entity producers in this
 ** package via the Gen 2 QBitMetaDataProducer pattern.
 *******************************************************************************/
public class CrmQBitProducer implements QBitMetaDataProducer<CrmQBitConfig>
{
   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "crm";
   public static final String VERSION     = "0.1.0";

   private CrmQBitConfig qBitConfig;



   /*******************************************************************************
    ** Produce the QBitMetaData for the CRM QBit.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return new QBitMetaData()
         .withGroupId(GROUP_ID)
         .withArtifactId(ARTIFACT_ID)
         .withVersion(VERSION)
         .withNamespace(getNamespace())
         .withConfig(getQBitConfig());
   }



   /*******************************************************************************
    ** Post-production actions: register global customizers on tracked entities.
    *******************************************************************************/
   @Override
   public void postProduceActions(MetaDataProducerMultiOutput metaDataProducerMultiOutput, QInstance qInstance) throws QException
   {
      ///////////////////////////////////////////////
      // global customizers registered here later  //
      // (audit log, auto-assign, webhook firing)  //
      ///////////////////////////////////////////////
   }



   /*******************************************************************************
    ** Produce an app section containing all CRM tables for navigation.
    *******************************************************************************/
   public static QAppSection produceAppSection()
   {
      return new QAppSection()
         .withName("crm")
         .withLabel("CRM")
         .withIcon(new QIcon().withName("contacts"));
   }



   /*******************************************************************************
    ** Getter for qBitConfig
    *******************************************************************************/
   @Override
   public CrmQBitConfig getQBitConfig()
   {
      return (this.qBitConfig);
   }



   /*******************************************************************************
    ** Setter for qBitConfig
    *******************************************************************************/
   public void setQBitConfig(CrmQBitConfig qBitConfig)
   {
      this.qBitConfig = qBitConfig;
   }



   /*******************************************************************************
    ** Fluent setter for qBitConfig
    *******************************************************************************/
   public CrmQBitProducer withQBitConfig(CrmQBitConfig qBitConfig)
   {
      this.qBitConfig = qBitConfig;
      return (this);
   }

}
