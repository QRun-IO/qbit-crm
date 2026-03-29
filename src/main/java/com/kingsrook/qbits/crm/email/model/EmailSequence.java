/*******************************************************************************
 ** QRecord Entity for EmailSequence table
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.model;


import java.time.Instant;
import java.util.List;
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
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** QRecord Entity for EmailSequence table -- an automated multi-step email
 ** sequence that enrolls contacts and executes steps on a schedule.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = EmailSequence.TableMetaDataCustomizer.class,
   childTables = {
      @ChildTable(
         childTableEntityClass = SequenceStep.class,
         joinFieldName = "sequenceId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Steps", enabled = true, maxRows = 50)),
      @ChildTable(
         childTableEntityClass = SequenceEnrollment.class,
         joinFieldName = "sequenceId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Enrollments", enabled = true, maxRows = 50))
   }
)
public class EmailSequence extends QRecordEntity
{
   public static final String TABLE_NAME = "crmEmailSequence";



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
            .withIcon(new QIcon().withName("playlist_play"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withSection(SectionFactory.defaultT1("id", "name", "ownerUserId", "isActive"))
            .withSection(new QFieldSection("details", new QIcon().withName("info"), Tier.T2,
               List.of("description", "totalSteps", "businessDaysOnly")))
            .withSection(new QFieldSection("sendWindow", new QIcon().withName("schedule"), Tier.T2,
               List.of("sendWindowStartHour", "sendWindowEndHour")))
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

   @QField(isRequired = true)
   private String ownerUserId;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField(isEditable = false)
   private Integer totalSteps;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean businessDaysOnly;

   @QField()
   private Integer sendWindowStartHour;

   @QField()
   private Integer sendWindowEndHour;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public EmailSequence()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public EmailSequence(QRecord record)
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
   public EmailSequence withId(Integer id)
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
   public EmailSequence withName(String name)
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
   public EmailSequence withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ownerUserId
    *******************************************************************************/
   public String getOwnerUserId()
   {
      return (this.ownerUserId);
   }



   /*******************************************************************************
    ** Setter for ownerUserId
    *******************************************************************************/
   public void setOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
   }



   /*******************************************************************************
    ** Fluent setter for ownerUserId
    *******************************************************************************/
   public EmailSequence withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
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
   public EmailSequence withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for totalSteps
    *******************************************************************************/
   public Integer getTotalSteps()
   {
      return (this.totalSteps);
   }



   /*******************************************************************************
    ** Setter for totalSteps
    *******************************************************************************/
   public void setTotalSteps(Integer totalSteps)
   {
      this.totalSteps = totalSteps;
   }



   /*******************************************************************************
    ** Fluent setter for totalSteps
    *******************************************************************************/
   public EmailSequence withTotalSteps(Integer totalSteps)
   {
      this.totalSteps = totalSteps;
      return (this);
   }



   /*******************************************************************************
    ** Getter for businessDaysOnly
    *******************************************************************************/
   public Boolean getBusinessDaysOnly()
   {
      return (this.businessDaysOnly);
   }



   /*******************************************************************************
    ** Setter for businessDaysOnly
    *******************************************************************************/
   public void setBusinessDaysOnly(Boolean businessDaysOnly)
   {
      this.businessDaysOnly = businessDaysOnly;
   }



   /*******************************************************************************
    ** Fluent setter for businessDaysOnly
    *******************************************************************************/
   public EmailSequence withBusinessDaysOnly(Boolean businessDaysOnly)
   {
      this.businessDaysOnly = businessDaysOnly;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sendWindowStartHour
    *******************************************************************************/
   public Integer getSendWindowStartHour()
   {
      return (this.sendWindowStartHour);
   }



   /*******************************************************************************
    ** Setter for sendWindowStartHour
    *******************************************************************************/
   public void setSendWindowStartHour(Integer sendWindowStartHour)
   {
      this.sendWindowStartHour = sendWindowStartHour;
   }



   /*******************************************************************************
    ** Fluent setter for sendWindowStartHour
    *******************************************************************************/
   public EmailSequence withSendWindowStartHour(Integer sendWindowStartHour)
   {
      this.sendWindowStartHour = sendWindowStartHour;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sendWindowEndHour
    *******************************************************************************/
   public Integer getSendWindowEndHour()
   {
      return (this.sendWindowEndHour);
   }



   /*******************************************************************************
    ** Setter for sendWindowEndHour
    *******************************************************************************/
   public void setSendWindowEndHour(Integer sendWindowEndHour)
   {
      this.sendWindowEndHour = sendWindowEndHour;
   }



   /*******************************************************************************
    ** Fluent setter for sendWindowEndHour
    *******************************************************************************/
   public EmailSequence withSendWindowEndHour(Integer sendWindowEndHour)
   {
      this.sendWindowEndHour = sendWindowEndHour;
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
   public EmailSequence withCreateDate(Instant createDate)
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
   public EmailSequence withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
