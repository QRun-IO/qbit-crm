/*******************************************************************************
 ** QRecord Entity for AssignmentRule table
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildJoin;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildRecordListWidget;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildTable;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;


/*******************************************************************************
 ** QRecord Entity for AssignmentRule table -- a configurable rule that
 ** determines how new contacts or deals are assigned to team members.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = AssignmentRule.TableMetaDataCustomizer.class,
   childTables = {
      @ChildTable(
         childTableEntityClass = AssignmentRuleMember.class,
         joinFieldName = "assignmentRuleId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Members", enabled = true, maxRows = 50))
   }
)
public class AssignmentRule extends QRecordEntity
{
   public static final String TABLE_NAME = "crmAssignmentRule";



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
            .withIcon(new QIcon().withName("assignment_ind"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withSection(SectionFactory.defaultT1("id", "name", "entityType", "assignStrategy", "isActive", "sortOrder"))
            .withSection(SectionFactory.defaultT2("description", "criteriaJson", "assignToUserId"))
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

   @QField(isRequired = true, possibleValueSourceName = CrmAssignableEntity.NAME)
   private Integer entityType;

   @QField()
   private String criteriaJson;

   @QField(isRequired = true, possibleValueSourceName = CrmAssignStrategy.NAME)
   private Integer assignStrategy;

   @QField()
   private String assignToUserId;

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
   public AssignmentRule()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public AssignmentRule(QRecord record)
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
   public AssignmentRule withId(Integer id)
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
   public AssignmentRule withName(String name)
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
   public AssignmentRule withDescription(String description)
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
   public AssignmentRule withEntityType(Integer entityType)
   {
      this.entityType = entityType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for criteriaJson
    *******************************************************************************/
   public String getCriteriaJson()
   {
      return (this.criteriaJson);
   }



   /*******************************************************************************
    ** Setter for criteriaJson
    *******************************************************************************/
   public void setCriteriaJson(String criteriaJson)
   {
      this.criteriaJson = criteriaJson;
   }



   /*******************************************************************************
    ** Fluent setter for criteriaJson
    *******************************************************************************/
   public AssignmentRule withCriteriaJson(String criteriaJson)
   {
      this.criteriaJson = criteriaJson;
      return (this);
   }



   /*******************************************************************************
    ** Getter for assignStrategy
    *******************************************************************************/
   public Integer getAssignStrategy()
   {
      return (this.assignStrategy);
   }



   /*******************************************************************************
    ** Setter for assignStrategy
    *******************************************************************************/
   public void setAssignStrategy(Integer assignStrategy)
   {
      this.assignStrategy = assignStrategy;
   }



   /*******************************************************************************
    ** Fluent setter for assignStrategy
    *******************************************************************************/
   public AssignmentRule withAssignStrategy(Integer assignStrategy)
   {
      this.assignStrategy = assignStrategy;
      return (this);
   }



   /*******************************************************************************
    ** Getter for assignToUserId
    *******************************************************************************/
   public String getAssignToUserId()
   {
      return (this.assignToUserId);
   }



   /*******************************************************************************
    ** Setter for assignToUserId
    *******************************************************************************/
   public void setAssignToUserId(String assignToUserId)
   {
      this.assignToUserId = assignToUserId;
   }



   /*******************************************************************************
    ** Fluent setter for assignToUserId
    *******************************************************************************/
   public AssignmentRule withAssignToUserId(String assignToUserId)
   {
      this.assignToUserId = assignToUserId;
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
   public AssignmentRule withIsActive(Boolean isActive)
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
   public AssignmentRule withSortOrder(Integer sortOrder)
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
   public AssignmentRule withCreateDate(Instant createDate)
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
   public AssignmentRule withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
