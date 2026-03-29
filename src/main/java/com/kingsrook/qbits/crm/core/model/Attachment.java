/*******************************************************************************
 ** QRecord Entity for Attachment table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;


/*******************************************************************************
 ** QRecord Entity for Attachment table -- polymorphic file attachments linked
 ** to any CRM entity via entityType + entityId. Insert-and-delete-only; no
 ** modifyDate. Carries denormalized clientId for security lock.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = Attachment.TableMetaDataCustomizer.class
)
public class Attachment extends QRecordEntity
{
   public static final String TABLE_NAME = "crmAttachment";



   /***************************************************************************
    **
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
            .withIcon(new QIcon().withName("attach_file"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("fileName")
            .withSection(SectionFactory.defaultT1("id", "entityType", "entityId", "fileName"))
            .withSection(SectionFactory.defaultT2("fileType", "fileSizeBytes", "storageReference", "uploadedByUserId"))
            .withSection(SectionFactory.defaultT3("createDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = CrmEntityType.NAME)
   private Integer entityType;

   @QField(isRequired = true)
   private Integer entityId;

   @QField(isRequired = true, maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fileName;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fileType;

   @QField()
   private Long fileSizeBytes;

   @QField(isRequired = true, maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String storageReference;

   @QField(isRequired = true)
   private String uploadedByUserId;

   @QField(isEditable = false)
   private Instant createDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public Attachment()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public Attachment(QRecord record)
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
   public Attachment withId(Integer id)
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
   public Attachment withEntityType(Integer entityType)
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
   public Attachment withEntityId(Integer entityId)
   {
      this.entityId = entityId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileName
    *******************************************************************************/
   public String getFileName()
   {
      return (this.fileName);
   }



   /*******************************************************************************
    ** Setter for fileName
    *******************************************************************************/
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
   }



   /*******************************************************************************
    ** Fluent setter for fileName
    *******************************************************************************/
   public Attachment withFileName(String fileName)
   {
      this.fileName = fileName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileType
    *******************************************************************************/
   public String getFileType()
   {
      return (this.fileType);
   }



   /*******************************************************************************
    ** Setter for fileType
    *******************************************************************************/
   public void setFileType(String fileType)
   {
      this.fileType = fileType;
   }



   /*******************************************************************************
    ** Fluent setter for fileType
    *******************************************************************************/
   public Attachment withFileType(String fileType)
   {
      this.fileType = fileType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileSizeBytes
    *******************************************************************************/
   public Long getFileSizeBytes()
   {
      return (this.fileSizeBytes);
   }



   /*******************************************************************************
    ** Setter for fileSizeBytes
    *******************************************************************************/
   public void setFileSizeBytes(Long fileSizeBytes)
   {
      this.fileSizeBytes = fileSizeBytes;
   }



   /*******************************************************************************
    ** Fluent setter for fileSizeBytes
    *******************************************************************************/
   public Attachment withFileSizeBytes(Long fileSizeBytes)
   {
      this.fileSizeBytes = fileSizeBytes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for storageReference
    *******************************************************************************/
   public String getStorageReference()
   {
      return (this.storageReference);
   }



   /*******************************************************************************
    ** Setter for storageReference
    *******************************************************************************/
   public void setStorageReference(String storageReference)
   {
      this.storageReference = storageReference;
   }



   /*******************************************************************************
    ** Fluent setter for storageReference
    *******************************************************************************/
   public Attachment withStorageReference(String storageReference)
   {
      this.storageReference = storageReference;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uploadedByUserId
    *******************************************************************************/
   public String getUploadedByUserId()
   {
      return (this.uploadedByUserId);
   }



   /*******************************************************************************
    ** Setter for uploadedByUserId
    *******************************************************************************/
   public void setUploadedByUserId(String uploadedByUserId)
   {
      this.uploadedByUserId = uploadedByUserId;
   }



   /*******************************************************************************
    ** Fluent setter for uploadedByUserId
    *******************************************************************************/
   public Attachment withUploadedByUserId(String uploadedByUserId)
   {
      this.uploadedByUserId = uploadedByUserId;
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
   public Attachment withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
