/*******************************************************************************
 ** QRecord Entity for the crm_activity table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmActivityPriority;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.email.model.EmailMessage;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
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
 ** Central activity entity. Every CRM interaction (call, email, meeting, note,
 ** task) is recorded as an activity linked to contacts, companies, and/or deals.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = Activity.TableMetaDataCustomizer.class,
   childTables = {
      @ChildTable(
         childTableEntityClass = ActivityParticipant.class,
         joinFieldName = "activityId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Participants", enabled = true, maxRows = 50)),
      @ChildTable(
         childTableEntityClass = EmailMessage.class,
         joinFieldName = "activityId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Email Details", enabled = true, maxRows = 5))
   }
)
public class Activity extends QRecordEntity
{
   public static final String TABLE_NAME = "crmActivity";



   /***************************************************************************
    ** Customizer that sets icon, record label, child joins, and section layout
    ** per the spec: T1 identity, T2-details, T2-scheduling, T2-completion,
    ** T2-participants (widget), T3 system.
    ***************************************************************************/
   public static class TableMetaDataCustomizer implements MetaDataCustomizerInterface<QTableMetaData>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QTableMetaData customizeMetaData(QInstance qInstance, QTableMetaData table) throws QException
      {
         String participantChildJoinName = QJoinMetaData.makeInferredJoinName(Activity.TABLE_NAME, ActivityParticipant.TABLE_NAME);

         table
            .withIcon(new QIcon().withName("event_note"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("subject")
            .withSection(SectionFactory.defaultT1("id", "subject", "activityTypeId", "direction", "priority"))
            .withSection(new QFieldSection("details", new QIcon("info"), Tier.T2,
               List.of("description", "contactId", "companyId", "dealId", "ownerUserId", "assignedToUserId", "outcomeId")))
            .withSection(new QFieldSection("scheduling", new QIcon("schedule"), Tier.T2,
               List.of("startDate", "endDate", "dueDate", "location", "conferenceLink", "durationMinutes")))
            .withSection(new QFieldSection("completion", new QIcon("task_alt"), Tier.T2,
               List.of("isCompleted", "completedDate", "isReminderSet", "reminderDate")))
            .withSection(SectionFactory.customT2("participants", new QIcon("group")).withWidgetName(participantChildJoinName))
            .withSection(SectionFactory.defaultT3("externalCalendarId", "createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = ActivityType.TABLE_NAME)
   private Integer activityTypeId;

   @QField(isRequired = true, maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String subject;

   @QField()
   private String description;

   @QField(possibleValueSourceName = Contact.TABLE_NAME)
   private Integer contactId;

   @QField(possibleValueSourceName = Company.TABLE_NAME)
   private Integer companyId;

   @QField(possibleValueSourceName = Deal.TABLE_NAME)
   private Integer dealId;

   @QField(isRequired = true)
   private String ownerUserId;

   @QField()
   private String assignedToUserId;

   @QField()
   private Instant startDate;

   @QField()
   private Instant endDate;

   @QField()
   private Instant dueDate;

   @QField()
   private Instant completedDate;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isCompleted;

   @QField(possibleValueSourceName = CrmActivityPriority.NAME)
   private Integer priority;

   @QField()
   private Integer durationMinutes;

   @QField(possibleValueSourceName = ActivityOutcome.TABLE_NAME)
   private Integer outcomeId;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String location;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String conferenceLink;

   @QField(possibleValueSourceName = CrmDirection.NAME)
   private Integer direction;

   @QField(defaultValue = "false")
   private Boolean isReminderSet;

   @QField()
   private Instant reminderDate;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String externalCalendarId;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public Activity()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public Activity(QRecord record)
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
   public Activity withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for activityTypeId
    *******************************************************************************/
   public Integer getActivityTypeId()
   {
      return (this.activityTypeId);
   }



   /*******************************************************************************
    ** Setter for activityTypeId
    *******************************************************************************/
   public void setActivityTypeId(Integer activityTypeId)
   {
      this.activityTypeId = activityTypeId;
   }



   /*******************************************************************************
    ** Fluent setter for activityTypeId
    *******************************************************************************/
   public Activity withActivityTypeId(Integer activityTypeId)
   {
      this.activityTypeId = activityTypeId;
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
   public Activity withSubject(String subject)
   {
      this.subject = subject;
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
   public Activity withDescription(String description)
   {
      this.description = description;
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
   public Activity withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for companyId
    *******************************************************************************/
   public Integer getCompanyId()
   {
      return (this.companyId);
   }



   /*******************************************************************************
    ** Setter for companyId
    *******************************************************************************/
   public void setCompanyId(Integer companyId)
   {
      this.companyId = companyId;
   }



   /*******************************************************************************
    ** Fluent setter for companyId
    *******************************************************************************/
   public Activity withCompanyId(Integer companyId)
   {
      this.companyId = companyId;
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
   public Activity withDealId(Integer dealId)
   {
      this.dealId = dealId;
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
   public Activity withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for assignedToUserId
    *******************************************************************************/
   public String getAssignedToUserId()
   {
      return (this.assignedToUserId);
   }



   /*******************************************************************************
    ** Setter for assignedToUserId
    *******************************************************************************/
   public void setAssignedToUserId(String assignedToUserId)
   {
      this.assignedToUserId = assignedToUserId;
   }



   /*******************************************************************************
    ** Fluent setter for assignedToUserId
    *******************************************************************************/
   public Activity withAssignedToUserId(String assignedToUserId)
   {
      this.assignedToUserId = assignedToUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startDate
    *******************************************************************************/
   public Instant getStartDate()
   {
      return (this.startDate);
   }



   /*******************************************************************************
    ** Setter for startDate
    *******************************************************************************/
   public void setStartDate(Instant startDate)
   {
      this.startDate = startDate;
   }



   /*******************************************************************************
    ** Fluent setter for startDate
    *******************************************************************************/
   public Activity withStartDate(Instant startDate)
   {
      this.startDate = startDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for endDate
    *******************************************************************************/
   public Instant getEndDate()
   {
      return (this.endDate);
   }



   /*******************************************************************************
    ** Setter for endDate
    *******************************************************************************/
   public void setEndDate(Instant endDate)
   {
      this.endDate = endDate;
   }



   /*******************************************************************************
    ** Fluent setter for endDate
    *******************************************************************************/
   public Activity withEndDate(Instant endDate)
   {
      this.endDate = endDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dueDate
    *******************************************************************************/
   public Instant getDueDate()
   {
      return (this.dueDate);
   }



   /*******************************************************************************
    ** Setter for dueDate
    *******************************************************************************/
   public void setDueDate(Instant dueDate)
   {
      this.dueDate = dueDate;
   }



   /*******************************************************************************
    ** Fluent setter for dueDate
    *******************************************************************************/
   public Activity withDueDate(Instant dueDate)
   {
      this.dueDate = dueDate;
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
   public Activity withCompletedDate(Instant completedDate)
   {
      this.completedDate = completedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isCompleted
    *******************************************************************************/
   public Boolean getIsCompleted()
   {
      return (this.isCompleted);
   }



   /*******************************************************************************
    ** Setter for isCompleted
    *******************************************************************************/
   public void setIsCompleted(Boolean isCompleted)
   {
      this.isCompleted = isCompleted;
   }



   /*******************************************************************************
    ** Fluent setter for isCompleted
    *******************************************************************************/
   public Activity withIsCompleted(Boolean isCompleted)
   {
      this.isCompleted = isCompleted;
      return (this);
   }



   /*******************************************************************************
    ** Getter for priority
    *******************************************************************************/
   public Integer getPriority()
   {
      return (this.priority);
   }



   /*******************************************************************************
    ** Setter for priority
    *******************************************************************************/
   public void setPriority(Integer priority)
   {
      this.priority = priority;
   }



   /*******************************************************************************
    ** Fluent setter for priority
    *******************************************************************************/
   public Activity withPriority(Integer priority)
   {
      this.priority = priority;
      return (this);
   }



   /*******************************************************************************
    ** Getter for durationMinutes
    *******************************************************************************/
   public Integer getDurationMinutes()
   {
      return (this.durationMinutes);
   }



   /*******************************************************************************
    ** Setter for durationMinutes
    *******************************************************************************/
   public void setDurationMinutes(Integer durationMinutes)
   {
      this.durationMinutes = durationMinutes;
   }



   /*******************************************************************************
    ** Fluent setter for durationMinutes
    *******************************************************************************/
   public Activity withDurationMinutes(Integer durationMinutes)
   {
      this.durationMinutes = durationMinutes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for outcomeId
    *******************************************************************************/
   public Integer getOutcomeId()
   {
      return (this.outcomeId);
   }



   /*******************************************************************************
    ** Setter for outcomeId
    *******************************************************************************/
   public void setOutcomeId(Integer outcomeId)
   {
      this.outcomeId = outcomeId;
   }



   /*******************************************************************************
    ** Fluent setter for outcomeId
    *******************************************************************************/
   public Activity withOutcomeId(Integer outcomeId)
   {
      this.outcomeId = outcomeId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for location
    *******************************************************************************/
   public String getLocation()
   {
      return (this.location);
   }



   /*******************************************************************************
    ** Setter for location
    *******************************************************************************/
   public void setLocation(String location)
   {
      this.location = location;
   }



   /*******************************************************************************
    ** Fluent setter for location
    *******************************************************************************/
   public Activity withLocation(String location)
   {
      this.location = location;
      return (this);
   }



   /*******************************************************************************
    ** Getter for conferenceLink
    *******************************************************************************/
   public String getConferenceLink()
   {
      return (this.conferenceLink);
   }



   /*******************************************************************************
    ** Setter for conferenceLink
    *******************************************************************************/
   public void setConferenceLink(String conferenceLink)
   {
      this.conferenceLink = conferenceLink;
   }



   /*******************************************************************************
    ** Fluent setter for conferenceLink
    *******************************************************************************/
   public Activity withConferenceLink(String conferenceLink)
   {
      this.conferenceLink = conferenceLink;
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
   public Activity withDirection(Integer direction)
   {
      this.direction = direction;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isReminderSet
    *******************************************************************************/
   public Boolean getIsReminderSet()
   {
      return (this.isReminderSet);
   }



   /*******************************************************************************
    ** Setter for isReminderSet
    *******************************************************************************/
   public void setIsReminderSet(Boolean isReminderSet)
   {
      this.isReminderSet = isReminderSet;
   }



   /*******************************************************************************
    ** Fluent setter for isReminderSet
    *******************************************************************************/
   public Activity withIsReminderSet(Boolean isReminderSet)
   {
      this.isReminderSet = isReminderSet;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reminderDate
    *******************************************************************************/
   public Instant getReminderDate()
   {
      return (this.reminderDate);
   }



   /*******************************************************************************
    ** Setter for reminderDate
    *******************************************************************************/
   public void setReminderDate(Instant reminderDate)
   {
      this.reminderDate = reminderDate;
   }



   /*******************************************************************************
    ** Fluent setter for reminderDate
    *******************************************************************************/
   public Activity withReminderDate(Instant reminderDate)
   {
      this.reminderDate = reminderDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for externalCalendarId
    *******************************************************************************/
   public String getExternalCalendarId()
   {
      return (this.externalCalendarId);
   }



   /*******************************************************************************
    ** Setter for externalCalendarId
    *******************************************************************************/
   public void setExternalCalendarId(String externalCalendarId)
   {
      this.externalCalendarId = externalCalendarId;
   }



   /*******************************************************************************
    ** Fluent setter for externalCalendarId
    *******************************************************************************/
   public Activity withExternalCalendarId(String externalCalendarId)
   {
      this.externalCalendarId = externalCalendarId;
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
   public Activity withCreateDate(Instant createDate)
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
   public Activity withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
