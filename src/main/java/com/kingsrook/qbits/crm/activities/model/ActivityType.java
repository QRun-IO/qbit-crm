/*******************************************************************************
 ** QRecord Entity for the crm_activity_type table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;


/*******************************************************************************
 ** Activity type reference table (Call, Email, Meeting, Note, Task, etc.).
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = ActivityType.TableMetaDataCustomizer.class
)
public class ActivityType extends QRecordEntity
{
   public static final String TABLE_NAME = "crmActivityType";



   /***************************************************************************
    ** Customizer that sets icon, record label, unique key, sections, and
    ** registers PRE_DELETE customizer to protect system activity types.
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
            .withIcon(new QIcon().withName("category"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withUniqueKey(new UniqueKey("name"))
            .withSection(SectionFactory.defaultT1("id", "name", "iconName"))
            .withSection(SectionFactory.defaultT2("isSystem", "sortOrder", "isActive"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         ////////////////////////////////////////////////////////////
         // register PRE_DELETE to protect system activity types   //
         ////////////////////////////////////////////////////////////
         table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(PreDeleteCustomizer.class));

         return (table);
      }
   }



   /***************************************************************************
    ** PRE_DELETE customizer that rejects deletion of system activity types.
    ***************************************************************************/
   public static class PreDeleteCustomizer extends AbstractPreDeleteCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         for(QRecord record : records)
         {
            if(Boolean.TRUE.equals(record.getValueBoolean("isSystem")))
            {
               record.addError(new BadInputStatusMessage("System activity types cannot be deleted"));
            }
         }
         return (records);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String iconName;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isSystem;

   @QField(isRequired = true)
   private Integer sortOrder;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public ActivityType()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public ActivityType(QRecord record)
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
   public ActivityType withId(Integer id)
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
   public ActivityType withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for iconName
    *******************************************************************************/
   public String getIconName()
   {
      return (this.iconName);
   }



   /*******************************************************************************
    ** Setter for iconName
    *******************************************************************************/
   public void setIconName(String iconName)
   {
      this.iconName = iconName;
   }



   /*******************************************************************************
    ** Fluent setter for iconName
    *******************************************************************************/
   public ActivityType withIconName(String iconName)
   {
      this.iconName = iconName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isSystem
    *******************************************************************************/
   public Boolean getIsSystem()
   {
      return (this.isSystem);
   }



   /*******************************************************************************
    ** Setter for isSystem
    *******************************************************************************/
   public void setIsSystem(Boolean isSystem)
   {
      this.isSystem = isSystem;
   }



   /*******************************************************************************
    ** Fluent setter for isSystem
    *******************************************************************************/
   public ActivityType withIsSystem(Boolean isSystem)
   {
      this.isSystem = isSystem;
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
   public ActivityType withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
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
   public ActivityType withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
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
   public ActivityType withCreateDate(Instant createDate)
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
   public ActivityType withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
