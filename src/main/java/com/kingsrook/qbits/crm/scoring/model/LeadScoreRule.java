/*******************************************************************************
 ** QRecord Entity for LeadScoreRule table
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmScorableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
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
 ** QRecord Entity for LeadScoreRule table -- a configurable rule that adjusts
 ** a contact's lead score based on field conditions.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = LeadScoreRule.TableMetaDataCustomizer.class
)
public class LeadScoreRule extends QRecordEntity
{
   public static final String TABLE_NAME = "crmLeadScoreRule";



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
            .withIcon(new QIcon().withName("score"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withSection(SectionFactory.defaultT1("id", "name", "entityType", "isActive", "sortOrder"))
            .withSection(SectionFactory.defaultT2("description", "fieldPath", "operator", "fieldValue", "scoreAdjustment"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField()
   private String description;

   @QField(isRequired = true, possibleValueSourceName = CrmScorableEntity.NAME)
   private Integer entityType;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fieldPath;

   @QField(isRequired = true, possibleValueSourceName = CrmScoreOperator.NAME)
   private Integer operator;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fieldValue;

   @QField(isRequired = true)
   private Integer scoreAdjustment;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField(isRequired = true)
   private Integer sortOrder;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public LeadScoreRule()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public LeadScoreRule(QRecord record)
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
   public LeadScoreRule withId(Integer id)
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
   public LeadScoreRule withName(String name)
   {
      this.name = name;
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
   public LeadScoreRule withDescription(String description)
   {
      this.description = description;
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
   public LeadScoreRule withEntityType(Integer entityType)
   {
      this.entityType = entityType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldPath
    *******************************************************************************/
   public String getFieldPath()
   {
      return (this.fieldPath);
   }



   /*******************************************************************************
    ** Setter for fieldPath
    *******************************************************************************/
   public void setFieldPath(String fieldPath)
   {
      this.fieldPath = fieldPath;
   }



   /*******************************************************************************
    ** Fluent setter for fieldPath
    *******************************************************************************/
   public LeadScoreRule withFieldPath(String fieldPath)
   {
      this.fieldPath = fieldPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for operator
    *******************************************************************************/
   public Integer getOperator()
   {
      return (this.operator);
   }



   /*******************************************************************************
    ** Setter for operator
    *******************************************************************************/
   public void setOperator(Integer operator)
   {
      this.operator = operator;
   }



   /*******************************************************************************
    ** Fluent setter for operator
    *******************************************************************************/
   public LeadScoreRule withOperator(Integer operator)
   {
      this.operator = operator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldValue
    *******************************************************************************/
   public String getFieldValue()
   {
      return (this.fieldValue);
   }



   /*******************************************************************************
    ** Setter for fieldValue
    *******************************************************************************/
   public void setFieldValue(String fieldValue)
   {
      this.fieldValue = fieldValue;
   }



   /*******************************************************************************
    ** Fluent setter for fieldValue
    *******************************************************************************/
   public LeadScoreRule withFieldValue(String fieldValue)
   {
      this.fieldValue = fieldValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scoreAdjustment
    *******************************************************************************/
   public Integer getScoreAdjustment()
   {
      return (this.scoreAdjustment);
   }



   /*******************************************************************************
    ** Setter for scoreAdjustment
    *******************************************************************************/
   public void setScoreAdjustment(Integer scoreAdjustment)
   {
      this.scoreAdjustment = scoreAdjustment;
   }



   /*******************************************************************************
    ** Fluent setter for scoreAdjustment
    *******************************************************************************/
   public LeadScoreRule withScoreAdjustment(Integer scoreAdjustment)
   {
      this.scoreAdjustment = scoreAdjustment;
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
   public LeadScoreRule withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
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
   public LeadScoreRule withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
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
   public LeadScoreRule withCreateDate(Instant createDate)
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
   public LeadScoreRule withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
