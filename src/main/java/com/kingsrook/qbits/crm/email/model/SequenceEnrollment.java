/*******************************************************************************
 ** QRecord Entity for SequenceEnrollment table
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
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
import java.util.List;


/*******************************************************************************
 ** QRecord Entity for SequenceEnrollment table -- tracks a contact's enrollment
 ** in an email sequence, including current step, status, and scheduling.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = SequenceEnrollment.TableMetaDataCustomizer.class
)
public class SequenceEnrollment extends QRecordEntity
{
   public static final String TABLE_NAME = "crmSequenceEnrollment";



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
            .withIcon(new QIcon().withName("person_play"))
            .withRecordLabelFormat("Enrollment #%s")
            .withRecordLabelFields("id")
            .withUniqueKey(new UniqueKey("sequenceId", "contactId"))
            .withSection(SectionFactory.defaultT1("id", "sequenceId", "contactId", "dealId", "status"))
            .withSection(new QFieldSection("progress", new QIcon().withName("trending_up"), Tier.T2,
               List.of("currentStepNumber", "enrolledDate", "enrolledByUserId", "nextStepDate")))
            .withSection(new QFieldSection("completion", new QIcon().withName("check_circle"), Tier.T2,
               List.of("completedDate", "unenrolledDate", "unenrollReason")))
            .withSection(new QFieldSection("errors", new QIcon().withName("error"), Tier.T2,
               List.of("failureCount", "lastFailureMessage")))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = EmailSequence.TABLE_NAME)
   private Integer sequenceId;

   @QField(isRequired = true)
   private Integer contactId;

   @QField()
   private Integer dealId;

   @QField(isRequired = true, defaultValue = "0")
   private Integer currentStepNumber;

   @QField(isRequired = true, possibleValueSourceName = CrmEnrollmentStatus.NAME)
   private Integer status;

   @QField(isRequired = true)
   private Instant enrolledDate;

   @QField(isRequired = true)
   private String enrolledByUserId;

   @QField()
   private Instant completedDate;

   @QField()
   private Instant unenrolledDate;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String unenrollReason;

   @QField()
   private Instant nextStepDate;

   @QField(defaultValue = "0")
   private Integer failureCount;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String lastFailureMessage;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public SequenceEnrollment()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public SequenceEnrollment(QRecord record)
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
   public SequenceEnrollment withId(Integer id)
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
   public SequenceEnrollment withSequenceId(Integer sequenceId)
   {
      this.sequenceId = sequenceId;
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
   public SequenceEnrollment withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dealId
    *******************************************************************************/
   public Integer getDealId()
   {
      return (this.dealId);
   }



   /*******************************************************************************
    ** Setter for dealId
    *******************************************************************************/
   public void setDealId(Integer dealId)
   {
      this.dealId = dealId;
   }



   /*******************************************************************************
    ** Fluent setter for dealId
    *******************************************************************************/
   public SequenceEnrollment withDealId(Integer dealId)
   {
      this.dealId = dealId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for currentStepNumber
    *******************************************************************************/
   public Integer getCurrentStepNumber()
   {
      return (this.currentStepNumber);
   }



   /*******************************************************************************
    ** Setter for currentStepNumber
    *******************************************************************************/
   public void setCurrentStepNumber(Integer currentStepNumber)
   {
      this.currentStepNumber = currentStepNumber;
   }



   /*******************************************************************************
    ** Fluent setter for currentStepNumber
    *******************************************************************************/
   public SequenceEnrollment withCurrentStepNumber(Integer currentStepNumber)
   {
      this.currentStepNumber = currentStepNumber;
      return (this);
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public Integer getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(Integer status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public SequenceEnrollment withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enrolledDate
    *******************************************************************************/
   public Instant getEnrolledDate()
   {
      return (this.enrolledDate);
   }



   /*******************************************************************************
    ** Setter for enrolledDate
    *******************************************************************************/
   public void setEnrolledDate(Instant enrolledDate)
   {
      this.enrolledDate = enrolledDate;
   }



   /*******************************************************************************
    ** Fluent setter for enrolledDate
    *******************************************************************************/
   public SequenceEnrollment withEnrolledDate(Instant enrolledDate)
   {
      this.enrolledDate = enrolledDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enrolledByUserId
    *******************************************************************************/
   public String getEnrolledByUserId()
   {
      return (this.enrolledByUserId);
   }



   /*******************************************************************************
    ** Setter for enrolledByUserId
    *******************************************************************************/
   public void setEnrolledByUserId(String enrolledByUserId)
   {
      this.enrolledByUserId = enrolledByUserId;
   }



   /*******************************************************************************
    ** Fluent setter for enrolledByUserId
    *******************************************************************************/
   public SequenceEnrollment withEnrolledByUserId(String enrolledByUserId)
   {
      this.enrolledByUserId = enrolledByUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for completedDate
    *******************************************************************************/
   public Instant getCompletedDate()
   {
      return (this.completedDate);
   }



   /*******************************************************************************
    ** Setter for completedDate
    *******************************************************************************/
   public void setCompletedDate(Instant completedDate)
   {
      this.completedDate = completedDate;
   }



   /*******************************************************************************
    ** Fluent setter for completedDate
    *******************************************************************************/
   public SequenceEnrollment withCompletedDate(Instant completedDate)
   {
      this.completedDate = completedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unenrolledDate
    *******************************************************************************/
   public Instant getUnenrolledDate()
   {
      return (this.unenrolledDate);
   }



   /*******************************************************************************
    ** Setter for unenrolledDate
    *******************************************************************************/
   public void setUnenrolledDate(Instant unenrolledDate)
   {
      this.unenrolledDate = unenrolledDate;
   }



   /*******************************************************************************
    ** Fluent setter for unenrolledDate
    *******************************************************************************/
   public SequenceEnrollment withUnenrolledDate(Instant unenrolledDate)
   {
      this.unenrolledDate = unenrolledDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unenrollReason
    *******************************************************************************/
   public String getUnenrollReason()
   {
      return (this.unenrollReason);
   }



   /*******************************************************************************
    ** Setter for unenrollReason
    *******************************************************************************/
   public void setUnenrollReason(String unenrollReason)
   {
      this.unenrollReason = unenrollReason;
   }



   /*******************************************************************************
    ** Fluent setter for unenrollReason
    *******************************************************************************/
   public SequenceEnrollment withUnenrollReason(String unenrollReason)
   {
      this.unenrollReason = unenrollReason;
      return (this);
   }



   /*******************************************************************************
    ** Getter for nextStepDate
    *******************************************************************************/
   public Instant getNextStepDate()
   {
      return (this.nextStepDate);
   }



   /*******************************************************************************
    ** Setter for nextStepDate
    *******************************************************************************/
   public void setNextStepDate(Instant nextStepDate)
   {
      this.nextStepDate = nextStepDate;
   }



   /*******************************************************************************
    ** Fluent setter for nextStepDate
    *******************************************************************************/
   public SequenceEnrollment withNextStepDate(Instant nextStepDate)
   {
      this.nextStepDate = nextStepDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for failureCount
    *******************************************************************************/
   public Integer getFailureCount()
   {
      return (this.failureCount);
   }



   /*******************************************************************************
    ** Setter for failureCount
    *******************************************************************************/
   public void setFailureCount(Integer failureCount)
   {
      this.failureCount = failureCount;
   }



   /*******************************************************************************
    ** Fluent setter for failureCount
    *******************************************************************************/
   public SequenceEnrollment withFailureCount(Integer failureCount)
   {
      this.failureCount = failureCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastFailureMessage
    *******************************************************************************/
   public String getLastFailureMessage()
   {
      return (this.lastFailureMessage);
   }



   /*******************************************************************************
    ** Setter for lastFailureMessage
    *******************************************************************************/
   public void setLastFailureMessage(String lastFailureMessage)
   {
      this.lastFailureMessage = lastFailureMessage;
   }



   /*******************************************************************************
    ** Fluent setter for lastFailureMessage
    *******************************************************************************/
   public SequenceEnrollment withLastFailureMessage(String lastFailureMessage)
   {
      this.lastFailureMessage = lastFailureMessage;
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
   public SequenceEnrollment withCreateDate(Instant createDate)
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
   public SequenceEnrollment withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
