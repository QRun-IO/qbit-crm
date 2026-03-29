/*******************************************************************************
 ** QRecord Entity for the crm_external_mapping table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.sync.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.core.model.enums.CrmSyncStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** External system ID mapping for bidirectional sync (calendar, email provider,
 ** marketing automation). Maps a CRM entity to its counterpart in an external
 ** system via (entityType, entityId, externalSystem) unique key.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = ExternalMapping.TableMetaDataCustomizer.class
)
public class ExternalMapping extends QRecordEntity
{
   public static final String TABLE_NAME = "crmExternalMapping";



   /***************************************************************************
    ** Customizer that sets icon, unique key, and section layout.
    ***************************************************************************/
   public static class TableMetaDataCustomizer implements MetaDataCustomizerInterface<QTableMetaData>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QTableMetaData customizeMetaData(QInstance qInstance, QTableMetaData table) throws QException
      {
         table
            .withIcon(new QIcon().withName("sync"))
            .withUniqueKey(new UniqueKey("entityType", "entityId", "externalSystem"))
            .withSection(SectionFactory.defaultT1("id", "entityType", "entityId", "externalSystem", "externalId"))
            .withSection(new QFieldSection("sync", new QIcon("sync"), Tier.T2,
               List.of("lastSyncDate", "syncStatus")))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = CrmEntityType.NAME)
   private Integer entityType;

   @QField(isRequired = true)
   private Integer entityId;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String externalSystem;

   @QField(isRequired = true, maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String externalId;

   @QField()
   private Instant lastSyncDate;

   @QField(possibleValueSourceName = CrmSyncStatus.NAME)
   private Integer syncStatus;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public ExternalMapping()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public ExternalMapping(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public ExternalMapping withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for entityType
    *******************************************************************************/
   public Integer getEntityType()
   {
      return (this.entityType);
   }



   /*******************************************************************************
    ** Setter for entityType
    *******************************************************************************/
   public void setEntityType(Integer entityType)
   {
      this.entityType = entityType;
   }



   /*******************************************************************************
    ** Fluent setter for entityType
    *******************************************************************************/
   public ExternalMapping withEntityType(Integer entityType)
   {
      this.entityType = entityType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for entityId
    *******************************************************************************/
   public Integer getEntityId()
   {
      return (this.entityId);
   }



   /*******************************************************************************
    ** Setter for entityId
    *******************************************************************************/
   public void setEntityId(Integer entityId)
   {
      this.entityId = entityId;
   }



   /*******************************************************************************
    ** Fluent setter for entityId
    *******************************************************************************/
   public ExternalMapping withEntityId(Integer entityId)
   {
      this.entityId = entityId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for externalSystem
    *******************************************************************************/
   public String getExternalSystem()
   {
      return (this.externalSystem);
   }



   /*******************************************************************************
    ** Setter for externalSystem
    *******************************************************************************/
   public void setExternalSystem(String externalSystem)
   {
      this.externalSystem = externalSystem;
   }



   /*******************************************************************************
    ** Fluent setter for externalSystem
    *******************************************************************************/
   public ExternalMapping withExternalSystem(String externalSystem)
   {
      this.externalSystem = externalSystem;
      return (this);
   }



   /*******************************************************************************
    ** Getter for externalId
    *******************************************************************************/
   public String getExternalId()
   {
      return (this.externalId);
   }



   /*******************************************************************************
    ** Setter for externalId
    *******************************************************************************/
   public void setExternalId(String externalId)
   {
      this.externalId = externalId;
   }



   /*******************************************************************************
    ** Fluent setter for externalId
    *******************************************************************************/
   public ExternalMapping withExternalId(String externalId)
   {
      this.externalId = externalId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastSyncDate
    *******************************************************************************/
   public Instant getLastSyncDate()
   {
      return (this.lastSyncDate);
   }



   /*******************************************************************************
    ** Setter for lastSyncDate
    *******************************************************************************/
   public void setLastSyncDate(Instant lastSyncDate)
   {
      this.lastSyncDate = lastSyncDate;
   }



   /*******************************************************************************
    ** Fluent setter for lastSyncDate
    *******************************************************************************/
   public ExternalMapping withLastSyncDate(Instant lastSyncDate)
   {
      this.lastSyncDate = lastSyncDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for syncStatus
    *******************************************************************************/
   public Integer getSyncStatus()
   {
      return (this.syncStatus);
   }



   /*******************************************************************************
    ** Setter for syncStatus
    *******************************************************************************/
   public void setSyncStatus(Integer syncStatus)
   {
      this.syncStatus = syncStatus;
   }



   /*******************************************************************************
    ** Fluent setter for syncStatus
    *******************************************************************************/
   public ExternalMapping withSyncStatus(Integer syncStatus)
   {
      this.syncStatus = syncStatus;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public ExternalMapping withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public ExternalMapping withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
