/*******************************************************************************
 ** QBitMetaDataProducer for the CRM QBit.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import java.util.List;
import com.kingsrook.qbits.crm.activities.customizers.ActivityLastDateCustomizer;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.core.customizers.FormSubmissionAutoLinkCustomizer;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.audit.customizers.CrmAuditLogCustomizer;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.deals.customizers.DealInitializationCustomizer;
import com.kingsrook.qbits.crm.deals.customizers.DealProductRecalculationCustomizer;
import com.kingsrook.qbits.crm.deals.customizers.DealStageChangeCustomizer;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qqq.backend.core.actions.customizers.MultiCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


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
      /////////////////////////////////////////////////////////////////////////
      // iterate tables produced by this QBit and register customizers       //
      /////////////////////////////////////////////////////////////////////////
      for(QTableMetaData table : metaDataProducerMultiOutput.getEach(QTableMetaData.class))
      {
         String tableName = table.getName();

         ///////////////////////////////////////////////////////////////
         // wire audit log on tracked entities                        //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Contact.TABLE_NAME) || tableName.equals(Company.TABLE_NAME)
            || tableName.equals(Deal.TABLE_NAME) || tableName.equals(Activity.TABLE_NAME))
         {
            QCodeReference auditRef = new QCodeReference(CrmAuditLogCustomizer.class);
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, auditRef);
            addOrComposeCustomizer(table, TableCustomizers.POST_UPDATE_RECORD, auditRef);
            addOrComposeCustomizer(table, TableCustomizers.POST_DELETE_RECORD, auditRef);
         }

         ///////////////////////////////////////////////////////////////
         // wire activity timestamp propagation                       //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Activity.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ActivityLastDateCustomizer.class));
         }

         ///////////////////////////////////////////////////////////////
         // wire deal stage change and initialization                  //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Deal.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(DealStageChangeCustomizer.class));
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(DealInitializationCustomizer.class));
         }

         ///////////////////////////////////////////////////////////////
         // wire deal product recalculation                           //
         ///////////////////////////////////////////////////////////////
         ///////////////////////////////////////////////////////////////
         // wire form submission auto-link                            //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(FormSubmission.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(FormSubmissionAutoLinkCustomizer.class));
         }

         ///////////////////////////////////////////////////////////////
         // wire deal product recalculation                           //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(DealProduct.TABLE_NAME))
         {
            QCodeReference dpRef = new QCodeReference(DealProductRecalculationCustomizer.class);
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, dpRef);
            addOrComposeCustomizer(table, TableCustomizers.POST_UPDATE_RECORD, dpRef);
            addOrComposeCustomizer(table, TableCustomizers.POST_DELETE_RECORD, dpRef);
         }
      }
   }



   /*******************************************************************************
    ** Add a customizer to a table for a given role. If a customizer already exists
    ** for that role, compose both using MultiCustomizer.
    *******************************************************************************/
   private void addOrComposeCustomizer(QTableMetaData table, TableCustomizers role, QCodeReference newRef)
   {
      java.util.Optional<QCodeReference> existing = table.getCustomizer(role.getRole());

      if(existing.isEmpty())
      {
         table.withCustomizer(role, newRef);
      }
      else
      {
         QCodeReference existingRef = existing.get();

         if(existingRef instanceof QCodeReferenceWithProperties existingMulti
            && MultiCustomizer.class.getName().equals(existingMulti.getName()))
         {
            ///////////////////////////////////////////////////////////////////
            // already a MultiCustomizer -- just add the new ref to it      //
            ///////////////////////////////////////////////////////////////////
            MultiCustomizer.addTableCustomizer(existingMulti, newRef);
         }
         else
         {
            ///////////////////////////////////////////////////////////////////
            // wrap existing + new in a MultiCustomizer                      //
            ///////////////////////////////////////////////////////////////////
            QCodeReferenceWithProperties multiRef = MultiCustomizer.of(existingRef, newRef);
            table.getCustomizers().put(role.getRole(), multiRef);
         }
      }
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
