/*******************************************************************************
 ** QRecord Entity for AssignmentRuleMember table
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.model;


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
 ** QRecord Entity for AssignmentRuleMember table -- a team member participating
 ** in a round-robin or least-active assignment rule.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = AssignmentRuleMember.TableMetaDataCustomizer.class
)
public class AssignmentRuleMember extends QRecordEntity
{
   public static final String TABLE_NAME = "crmAssignmentRuleMember";



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
            .withIcon(new QIcon().withName("group"))
            .withRecordLabelFormat("Member #%s")
            .withRecordLabelFields("id")
            .withUniqueKey(new UniqueKey("assignmentRuleId", "userId"))
            .withSection(SectionFactory.defaultT1("id", "assignmentRuleId", "userId", "sortOrder", "isActive"))
            .withSection(SectionFactory.defaultT2("lastAssignedDate"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = AssignmentRule.TABLE_NAME)
   private Integer assignmentRuleId;

   @QField(isRequired = true)
   private String userId;

   @QField(isRequired = true)
   private Integer sortOrder;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField()
   private Instant lastAssignedDate;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public AssignmentRuleMember()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public AssignmentRuleMember(QRecord record)
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
   public AssignmentRuleMember withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for assignmentRuleId
    *******************************************************************************/
   public Integer getAssignmentRuleId()
   {
      return (this.assignmentRuleId);
   }



   /*******************************************************************************
    ** Setter for assignmentRuleId
    *******************************************************************************/
   public void setAssignmentRuleId(Integer assignmentRuleId)
   {
      this.assignmentRuleId = assignmentRuleId;
   }



   /*******************************************************************************
    ** Fluent setter for assignmentRuleId
    *******************************************************************************/
   public AssignmentRuleMember withAssignmentRuleId(Integer assignmentRuleId)
   {
      this.assignmentRuleId = assignmentRuleId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public AssignmentRuleMember withUserId(String userId)
   {
      this.userId = userId;
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
   public AssignmentRuleMember withSortOrder(Integer sortOrder)
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
   public AssignmentRuleMember withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastAssignedDate
    *******************************************************************************/
   public Instant getLastAssignedDate()
   {
      return (this.lastAssignedDate);
   }



   /*******************************************************************************
    ** Setter for lastAssignedDate
    *******************************************************************************/
   public void setLastAssignedDate(Instant lastAssignedDate)
   {
      this.lastAssignedDate = lastAssignedDate;
   }



   /*******************************************************************************
    ** Fluent setter for lastAssignedDate
    *******************************************************************************/
   public AssignmentRuleMember withLastAssignedDate(Instant lastAssignedDate)
   {
      this.lastAssignedDate = lastAssignedDate;
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
   public AssignmentRuleMember withCreateDate(Instant createDate)
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
   public AssignmentRuleMember withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
