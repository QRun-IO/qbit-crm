/*******************************************************************************
 ** QRecord Entity for EmailList table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
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
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** QRecord Entity for EmailList table -- marketing email lists / subscription
 ** channels. Distinct from sales sequences.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = EmailList.TableMetaDataCustomizer.class
)
public class EmailList extends QRecordEntity
{
   public static final String TABLE_NAME = "crmEmailList";



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
            .withIcon(new QIcon().withName("mail"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withUniqueKey(new UniqueKey("name"))
            .withUniqueKey(new UniqueKey("slug"))
            .withSection(SectionFactory.defaultT1("id", "name", "slug", "isActive", "isPublic", "sortOrder"))
            .withSection(SectionFactory.defaultT2("description", "memberCount"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String slug;

   @QField()
   private String description;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isPublic;

   @QField(isRequired = true)
   private Integer sortOrder;

   @QField(defaultValue = "0", isEditable = false)
   private Integer memberCount;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public EmailList()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public EmailList(QRecord record)
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
   public EmailList withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public EmailList withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for slug
    *******************************************************************************/
   public String getSlug()
   {
      return (this.slug);
   }



   /*******************************************************************************
    ** Setter for slug
    *******************************************************************************/
   public void setSlug(String slug)
   {
      this.slug = slug;
   }



   /*******************************************************************************
    ** Fluent setter for slug
    *******************************************************************************/
   public EmailList withSlug(String slug)
   {
      this.slug = slug;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public EmailList withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isActive
    *******************************************************************************/
   public Boolean getIsActive()
   {
      return (this.isActive);
   }



   /*******************************************************************************
    ** Setter for isActive
    *******************************************************************************/
   public void setIsActive(Boolean isActive)
   {
      this.isActive = isActive;
   }



   /*******************************************************************************
    ** Fluent setter for isActive
    *******************************************************************************/
   public EmailList withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isPublic
    *******************************************************************************/
   public Boolean getIsPublic()
   {
      return (this.isPublic);
   }



   /*******************************************************************************
    ** Setter for isPublic
    *******************************************************************************/
   public void setIsPublic(Boolean isPublic)
   {
      this.isPublic = isPublic;
   }



   /*******************************************************************************
    ** Fluent setter for isPublic
    *******************************************************************************/
   public EmailList withIsPublic(Boolean isPublic)
   {
      this.isPublic = isPublic;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sortOrder
    *******************************************************************************/
   public Integer getSortOrder()
   {
      return (this.sortOrder);
   }



   /*******************************************************************************
    ** Setter for sortOrder
    *******************************************************************************/
   public void setSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
   }



   /*******************************************************************************
    ** Fluent setter for sortOrder
    *******************************************************************************/
   public EmailList withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
      return (this);
   }



   /*******************************************************************************
    ** Getter for memberCount
    *******************************************************************************/
   public Integer getMemberCount()
   {
      return (this.memberCount);
   }



   /*******************************************************************************
    ** Setter for memberCount
    *******************************************************************************/
   public void setMemberCount(Integer memberCount)
   {
      this.memberCount = memberCount;
   }



   /*******************************************************************************
    ** Fluent setter for memberCount
    *******************************************************************************/
   public EmailList withMemberCount(Integer memberCount)
   {
      this.memberCount = memberCount;
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
   public EmailList withCreateDate(Instant createDate)
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
   public EmailList withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
