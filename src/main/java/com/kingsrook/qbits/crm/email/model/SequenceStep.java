/*******************************************************************************
 ** QRecord Entity for SequenceStep table
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmSequenceStepType;
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
 ** QRecord Entity for SequenceStep table -- a single step within an email
 ** sequence, defining what action to take and after what delay.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = SequenceStep.TableMetaDataCustomizer.class
)
public class SequenceStep extends QRecordEntity
{
   public static final String TABLE_NAME = "crmSequenceStep";



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
            .withIcon(new QIcon().withName("format_list_numbered"))
            .withRecordLabelFormat("Step %s")
            .withRecordLabelFields("stepNumber")
            .withUniqueKey(new UniqueKey("sequenceId", "stepNumber"))
            .withSection(SectionFactory.defaultT1("id", "sequenceId", "stepNumber", "stepType"))
            .withSection(SectionFactory.defaultT2("delayDays", "delayHours", "emailTemplateId", "taskSubject", "taskDescription"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = EmailSequence.TABLE_NAME)
   private Integer sequenceId;

   @QField(isRequired = true)
   private Integer stepNumber;

   @QField(isRequired = true, possibleValueSourceName = CrmSequenceStepType.NAME)
   private Integer stepType;

   @QField(isRequired = true, defaultValue = "0")
   private Integer delayDays;

   @QField(defaultValue = "0")
   private Integer delayHours;

   @QField(possibleValueSourceName = EmailTemplate.TABLE_NAME)
   private Integer emailTemplateId;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String taskSubject;

   @QField()
   private String taskDescription;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public SequenceStep()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public SequenceStep(QRecord record)
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
   public SequenceStep withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sequenceId
    *******************************************************************************/
   public Integer getSequenceId()
   {
      return (this.sequenceId);
   }



   /*******************************************************************************
    ** Setter for sequenceId
    *******************************************************************************/
   public void setSequenceId(Integer sequenceId)
   {
      this.sequenceId = sequenceId;
   }



   /*******************************************************************************
    ** Fluent setter for sequenceId
    *******************************************************************************/
   public SequenceStep withSequenceId(Integer sequenceId)
   {
      this.sequenceId = sequenceId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stepNumber
    *******************************************************************************/
   public Integer getStepNumber()
   {
      return (this.stepNumber);
   }



   /*******************************************************************************
    ** Setter for stepNumber
    *******************************************************************************/
   public void setStepNumber(Integer stepNumber)
   {
      this.stepNumber = stepNumber;
   }



   /*******************************************************************************
    ** Fluent setter for stepNumber
    *******************************************************************************/
   public SequenceStep withStepNumber(Integer stepNumber)
   {
      this.stepNumber = stepNumber;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stepType
    *******************************************************************************/
   public Integer getStepType()
   {
      return (this.stepType);
   }



   /*******************************************************************************
    ** Setter for stepType
    *******************************************************************************/
   public void setStepType(Integer stepType)
   {
      this.stepType = stepType;
   }



   /*******************************************************************************
    ** Fluent setter for stepType
    *******************************************************************************/
   public SequenceStep withStepType(Integer stepType)
   {
      this.stepType = stepType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for delayDays
    *******************************************************************************/
   public Integer getDelayDays()
   {
      return (this.delayDays);
   }



   /*******************************************************************************
    ** Setter for delayDays
    *******************************************************************************/
   public void setDelayDays(Integer delayDays)
   {
      this.delayDays = delayDays;
   }



   /*******************************************************************************
    ** Fluent setter for delayDays
    *******************************************************************************/
   public SequenceStep withDelayDays(Integer delayDays)
   {
      this.delayDays = delayDays;
      return (this);
   }



   /*******************************************************************************
    ** Getter for delayHours
    *******************************************************************************/
   public Integer getDelayHours()
   {
      return (this.delayHours);
   }



   /*******************************************************************************
    ** Setter for delayHours
    *******************************************************************************/
   public void setDelayHours(Integer delayHours)
   {
      this.delayHours = delayHours;
   }



   /*******************************************************************************
    ** Fluent setter for delayHours
    *******************************************************************************/
   public SequenceStep withDelayHours(Integer delayHours)
   {
      this.delayHours = delayHours;
      return (this);
   }



   /*******************************************************************************
    ** Getter for emailTemplateId
    *******************************************************************************/
   public Integer getEmailTemplateId()
   {
      return (this.emailTemplateId);
   }



   /*******************************************************************************
    ** Setter for emailTemplateId
    *******************************************************************************/
   public void setEmailTemplateId(Integer emailTemplateId)
   {
      this.emailTemplateId = emailTemplateId;
   }



   /*******************************************************************************
    ** Fluent setter for emailTemplateId
    *******************************************************************************/
   public SequenceStep withEmailTemplateId(Integer emailTemplateId)
   {
      this.emailTemplateId = emailTemplateId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for taskSubject
    *******************************************************************************/
   public String getTaskSubject()
   {
      return (this.taskSubject);
   }



   /*******************************************************************************
    ** Setter for taskSubject
    *******************************************************************************/
   public void setTaskSubject(String taskSubject)
   {
      this.taskSubject = taskSubject;
   }



   /*******************************************************************************
    ** Fluent setter for taskSubject
    *******************************************************************************/
   public SequenceStep withTaskSubject(String taskSubject)
   {
      this.taskSubject = taskSubject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for taskDescription
    *******************************************************************************/
   public String getTaskDescription()
   {
      return (this.taskDescription);
   }



   /*******************************************************************************
    ** Setter for taskDescription
    *******************************************************************************/
   public void setTaskDescription(String taskDescription)
   {
      this.taskDescription = taskDescription;
   }



   /*******************************************************************************
    ** Fluent setter for taskDescription
    *******************************************************************************/
   public SequenceStep withTaskDescription(String taskDescription)
   {
      this.taskDescription = taskDescription;
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
   public SequenceStep withCreateDate(Instant createDate)
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
   public SequenceStep withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
