/*******************************************************************************
 ** QRecord Entity for the crm_email_message table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmBounceType;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
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


/*******************************************************************************
 ** Email message entity. Each outbound email creates a companion crm_activity
 ** row for timeline visibility. Inbound synced emails may initially lack an
 ** activityId until matched to a contact.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = EmailMessage.TableMetaDataCustomizer.class
)
public class EmailMessage extends QRecordEntity
{
   public static final String TABLE_NAME = "crmEmailMessage";



   /***************************************************************************
    ** Customizer that sets icon, record label, and section layout.
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
            .withIcon(new QIcon().withName("email"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("subject")
            .withSection(SectionFactory.defaultT1("id", "subject", "direction", "status", "sentDate"))
            .withSection(new QFieldSection("addresses", new QIcon("contacts"), Tier.T2,
               List.of("fromAddress", "fromName", "toAddresses", "ccAddresses", "bccAddresses")))
            .withSection(new QFieldSection("content", new QIcon("article"), Tier.T2,
               List.of("bodyHtml", "bodyText")))
            .withSection(new QFieldSection("tracking", new QIcon("visibility"), Tier.T2,
               List.of("isTracked", "openCount", "clickCount", "firstOpenedDate", "firstClickedDate",
                  "bounceType", "isSpamComplaint")))
            .withSection(new QFieldSection("references", new QIcon("link"), Tier.T2,
               List.of("activityId", "emailTemplateId", "sequenceEnrollmentId",
                  "threadId", "replyToEmailId")))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField()
   private Integer activityId;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fromAddress;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fromName;

   @QField(isRequired = true)
   private String toAddresses;

   @QField()
   private String ccAddresses;

   @QField()
   private String bccAddresses;

   @QField(isRequired = true, maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String subject;

   @QField()
   private String bodyHtml;

   @QField()
   private String bodyText;

   @QField()
   private Instant sentDate;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String threadId;

   @QField(possibleValueSourceName = EmailTemplate.TABLE_NAME)
   private Integer emailTemplateId;

   @QField()
   private Integer sequenceEnrollmentId;

   @QField(isRequired = true, possibleValueSourceName = CrmDirection.NAME)
   private Integer direction;

   @QField(possibleValueSourceName = CrmEmailStatus.NAME)
   private Integer status;

   @QField(defaultValue = "true")
   private Boolean isTracked;

   @QField(defaultValue = "0", isEditable = false)
   private Integer openCount;

   @QField(defaultValue = "0", isEditable = false)
   private Integer clickCount;

   @QField(isEditable = false)
   private Instant firstOpenedDate;

   @QField(isEditable = false)
   private Instant firstClickedDate;

   @QField(possibleValueSourceName = CrmBounceType.NAME)
   private Integer bounceType;

   @QField(defaultValue = "false")
   private Boolean isSpamComplaint;

   @QField()
   private Integer replyToEmailId;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public EmailMessage()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public EmailMessage(QRecord record)
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
   public EmailMessage withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for activityId
    *******************************************************************************/
   public Integer getActivityId()
   {
      return (this.activityId);
   }



   /*******************************************************************************
    ** Setter for activityId
    *******************************************************************************/
   public void setActivityId(Integer activityId)
   {
      this.activityId = activityId;
   }



   /*******************************************************************************
    ** Fluent setter for activityId
    *******************************************************************************/
   public EmailMessage withActivityId(Integer activityId)
   {
      this.activityId = activityId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fromAddress
    *******************************************************************************/
   public String getFromAddress()
   {
      return (this.fromAddress);
   }



   /*******************************************************************************
    ** Setter for fromAddress
    *******************************************************************************/
   public void setFromAddress(String fromAddress)
   {
      this.fromAddress = fromAddress;
   }



   /*******************************************************************************
    ** Fluent setter for fromAddress
    *******************************************************************************/
   public EmailMessage withFromAddress(String fromAddress)
   {
      this.fromAddress = fromAddress;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fromName
    *******************************************************************************/
   public String getFromName()
   {
      return (this.fromName);
   }



   /*******************************************************************************
    ** Setter for fromName
    *******************************************************************************/
   public void setFromName(String fromName)
   {
      this.fromName = fromName;
   }



   /*******************************************************************************
    ** Fluent setter for fromName
    *******************************************************************************/
   public EmailMessage withFromName(String fromName)
   {
      this.fromName = fromName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for toAddresses
    *******************************************************************************/
   public String getToAddresses()
   {
      return (this.toAddresses);
   }



   /*******************************************************************************
    ** Setter for toAddresses
    *******************************************************************************/
   public void setToAddresses(String toAddresses)
   {
      this.toAddresses = toAddresses;
   }



   /*******************************************************************************
    ** Fluent setter for toAddresses
    *******************************************************************************/
   public EmailMessage withToAddresses(String toAddresses)
   {
      this.toAddresses = toAddresses;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ccAddresses
    *******************************************************************************/
   public String getCcAddresses()
   {
      return (this.ccAddresses);
   }



   /*******************************************************************************
    ** Setter for ccAddresses
    *******************************************************************************/
   public void setCcAddresses(String ccAddresses)
   {
      this.ccAddresses = ccAddresses;
   }



   /*******************************************************************************
    ** Fluent setter for ccAddresses
    *******************************************************************************/
   public EmailMessage withCcAddresses(String ccAddresses)
   {
      this.ccAddresses = ccAddresses;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bccAddresses
    *******************************************************************************/
   public String getBccAddresses()
   {
      return (this.bccAddresses);
   }



   /*******************************************************************************
    ** Setter for bccAddresses
    *******************************************************************************/
   public void setBccAddresses(String bccAddresses)
   {
      this.bccAddresses = bccAddresses;
   }



   /*******************************************************************************
    ** Fluent setter for bccAddresses
    *******************************************************************************/
   public EmailMessage withBccAddresses(String bccAddresses)
   {
      this.bccAddresses = bccAddresses;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subject
    *******************************************************************************/
   public String getSubject()
   {
      return (this.subject);
   }



   /*******************************************************************************
    ** Setter for subject
    *******************************************************************************/
   public void setSubject(String subject)
   {
      this.subject = subject;
   }



   /*******************************************************************************
    ** Fluent setter for subject
    *******************************************************************************/
   public EmailMessage withSubject(String subject)
   {
      this.subject = subject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bodyHtml
    *******************************************************************************/
   public String getBodyHtml()
   {
      return (this.bodyHtml);
   }



   /*******************************************************************************
    ** Setter for bodyHtml
    *******************************************************************************/
   public void setBodyHtml(String bodyHtml)
   {
      this.bodyHtml = bodyHtml;
   }



   /*******************************************************************************
    ** Fluent setter for bodyHtml
    *******************************************************************************/
   public EmailMessage withBodyHtml(String bodyHtml)
   {
      this.bodyHtml = bodyHtml;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bodyText
    *******************************************************************************/
   public String getBodyText()
   {
      return (this.bodyText);
   }



   /*******************************************************************************
    ** Setter for bodyText
    *******************************************************************************/
   public void setBodyText(String bodyText)
   {
      this.bodyText = bodyText;
   }



   /*******************************************************************************
    ** Fluent setter for bodyText
    *******************************************************************************/
   public EmailMessage withBodyText(String bodyText)
   {
      this.bodyText = bodyText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sentDate
    *******************************************************************************/
   public Instant getSentDate()
   {
      return (this.sentDate);
   }



   /*******************************************************************************
    ** Setter for sentDate
    *******************************************************************************/
   public void setSentDate(Instant sentDate)
   {
      this.sentDate = sentDate;
   }



   /*******************************************************************************
    ** Fluent setter for sentDate
    *******************************************************************************/
   public EmailMessage withSentDate(Instant sentDate)
   {
      this.sentDate = sentDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for threadId
    *******************************************************************************/
   public String getThreadId()
   {
      return (this.threadId);
   }



   /*******************************************************************************
    ** Setter for threadId
    *******************************************************************************/
   public void setThreadId(String threadId)
   {
      this.threadId = threadId;
   }



   /*******************************************************************************
    ** Fluent setter for threadId
    *******************************************************************************/
   public EmailMessage withThreadId(String threadId)
   {
      this.threadId = threadId;
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
   public EmailMessage withEmailTemplateId(Integer emailTemplateId)
   {
      this.emailTemplateId = emailTemplateId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sequenceEnrollmentId
    *******************************************************************************/
   public Integer getSequenceEnrollmentId()
   {
      return (this.sequenceEnrollmentId);
   }



   /*******************************************************************************
    ** Setter for sequenceEnrollmentId
    *******************************************************************************/
   public void setSequenceEnrollmentId(Integer sequenceEnrollmentId)
   {
      this.sequenceEnrollmentId = sequenceEnrollmentId;
   }



   /*******************************************************************************
    ** Fluent setter for sequenceEnrollmentId
    *******************************************************************************/
   public EmailMessage withSequenceEnrollmentId(Integer sequenceEnrollmentId)
   {
      this.sequenceEnrollmentId = sequenceEnrollmentId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for direction
    *******************************************************************************/
   public Integer getDirection()
   {
      return (this.direction);
   }



   /*******************************************************************************
    ** Setter for direction
    *******************************************************************************/
   public void setDirection(Integer direction)
   {
      this.direction = direction;
   }



   /*******************************************************************************
    ** Fluent setter for direction
    *******************************************************************************/
   public EmailMessage withDirection(Integer direction)
   {
      this.direction = direction;
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
   public EmailMessage withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isTracked
    *******************************************************************************/
   public Boolean getIsTracked()
   {
      return (this.isTracked);
   }



   /*******************************************************************************
    ** Setter for isTracked
    *******************************************************************************/
   public void setIsTracked(Boolean isTracked)
   {
      this.isTracked = isTracked;
   }



   /*******************************************************************************
    ** Fluent setter for isTracked
    *******************************************************************************/
   public EmailMessage withIsTracked(Boolean isTracked)
   {
      this.isTracked = isTracked;
      return (this);
   }



   /*******************************************************************************
    ** Getter for openCount
    *******************************************************************************/
   public Integer getOpenCount()
   {
      return (this.openCount);
   }



   /*******************************************************************************
    ** Setter for openCount
    *******************************************************************************/
   public void setOpenCount(Integer openCount)
   {
      this.openCount = openCount;
   }



   /*******************************************************************************
    ** Fluent setter for openCount
    *******************************************************************************/
   public EmailMessage withOpenCount(Integer openCount)
   {
      this.openCount = openCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clickCount
    *******************************************************************************/
   public Integer getClickCount()
   {
      return (this.clickCount);
   }



   /*******************************************************************************
    ** Setter for clickCount
    *******************************************************************************/
   public void setClickCount(Integer clickCount)
   {
      this.clickCount = clickCount;
   }



   /*******************************************************************************
    ** Fluent setter for clickCount
    *******************************************************************************/
   public EmailMessage withClickCount(Integer clickCount)
   {
      this.clickCount = clickCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for firstOpenedDate
    *******************************************************************************/
   public Instant getFirstOpenedDate()
   {
      return (this.firstOpenedDate);
   }



   /*******************************************************************************
    ** Setter for firstOpenedDate
    *******************************************************************************/
   public void setFirstOpenedDate(Instant firstOpenedDate)
   {
      this.firstOpenedDate = firstOpenedDate;
   }



   /*******************************************************************************
    ** Fluent setter for firstOpenedDate
    *******************************************************************************/
   public EmailMessage withFirstOpenedDate(Instant firstOpenedDate)
   {
      this.firstOpenedDate = firstOpenedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for firstClickedDate
    *******************************************************************************/
   public Instant getFirstClickedDate()
   {
      return (this.firstClickedDate);
   }



   /*******************************************************************************
    ** Setter for firstClickedDate
    *******************************************************************************/
   public void setFirstClickedDate(Instant firstClickedDate)
   {
      this.firstClickedDate = firstClickedDate;
   }



   /*******************************************************************************
    ** Fluent setter for firstClickedDate
    *******************************************************************************/
   public EmailMessage withFirstClickedDate(Instant firstClickedDate)
   {
      this.firstClickedDate = firstClickedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bounceType
    *******************************************************************************/
   public Integer getBounceType()
   {
      return (this.bounceType);
   }



   /*******************************************************************************
    ** Setter for bounceType
    *******************************************************************************/
   public void setBounceType(Integer bounceType)
   {
      this.bounceType = bounceType;
   }



   /*******************************************************************************
    ** Fluent setter for bounceType
    *******************************************************************************/
   public EmailMessage withBounceType(Integer bounceType)
   {
      this.bounceType = bounceType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isSpamComplaint
    *******************************************************************************/
   public Boolean getIsSpamComplaint()
   {
      return (this.isSpamComplaint);
   }



   /*******************************************************************************
    ** Setter for isSpamComplaint
    *******************************************************************************/
   public void setIsSpamComplaint(Boolean isSpamComplaint)
   {
      this.isSpamComplaint = isSpamComplaint;
   }



   /*******************************************************************************
    ** Fluent setter for isSpamComplaint
    *******************************************************************************/
   public EmailMessage withIsSpamComplaint(Boolean isSpamComplaint)
   {
      this.isSpamComplaint = isSpamComplaint;
      return (this);
   }



   /*******************************************************************************
    ** Getter for replyToEmailId
    *******************************************************************************/
   public Integer getReplyToEmailId()
   {
      return (this.replyToEmailId);
   }



   /*******************************************************************************
    ** Setter for replyToEmailId
    *******************************************************************************/
   public void setReplyToEmailId(Integer replyToEmailId)
   {
      this.replyToEmailId = replyToEmailId;
   }



   /*******************************************************************************
    ** Fluent setter for replyToEmailId
    *******************************************************************************/
   public EmailMessage withReplyToEmailId(Integer replyToEmailId)
   {
      this.replyToEmailId = replyToEmailId;
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
   public EmailMessage withCreateDate(Instant createDate)
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
   public EmailMessage withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
