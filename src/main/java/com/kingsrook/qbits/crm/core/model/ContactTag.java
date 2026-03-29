/*******************************************************************************
 ** QRecord Entity for ContactTag table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** QRecord Entity for ContactTag junction table -- many-to-many link between
 ** contacts and tags.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = ContactTag.TableMetaDataCustomizer.class
)
public class ContactTag extends QRecordEntity
{
   public static final String TABLE_NAME = "crmContactTag";



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
            .withIcon(new QIcon().withName("label"))
            .withRecordLabelFormat("Contact %s - Tag %s")
            .withRecordLabelFields("contactId", "tagId")
            .withUniqueKey(new UniqueKey("contactId", "tagId"))
            .withSection(SectionFactory.defaultT1("id", "contactId", "tagId"))
            .withSection(SectionFactory.defaultT3("createDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Contact.TABLE_NAME)
   private Integer contactId;

   @QField(isRequired = true, possibleValueSourceName = Tag.TABLE_NAME)
   private Integer tagId;

   @QField(isEditable = false)
   private Instant createDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public ContactTag()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public ContactTag(QRecord record)
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
   public ContactTag withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contactId
    *******************************************************************************/
   public Integer getContactId()
   {
      return (this.contactId);
   }



   /*******************************************************************************
    ** Setter for contactId
    *******************************************************************************/
   public void setContactId(Integer contactId)
   {
      this.contactId = contactId;
   }



   /*******************************************************************************
    ** Fluent setter for contactId
    *******************************************************************************/
   public ContactTag withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tagId
    *******************************************************************************/
   public Integer getTagId()
   {
      return (this.tagId);
   }



   /*******************************************************************************
    ** Setter for tagId
    *******************************************************************************/
   public void setTagId(Integer tagId)
   {
      this.tagId = tagId;
   }



   /*******************************************************************************
    ** Fluent setter for tagId
    *******************************************************************************/
   public ContactTag withTagId(Integer tagId)
   {
      this.tagId = tagId;
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
   public ContactTag withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
