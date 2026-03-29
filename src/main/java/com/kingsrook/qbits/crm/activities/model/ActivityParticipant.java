/*******************************************************************************
 ** QRecord Entity for the crm_activity_participant table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmParticipantRole;
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


/*******************************************************************************
 ** Tracks contacts and/or internal users participating in an activity.
 ** Insert-only (no modifyDate). Validation that at least one of contactId or
 ** userId must be set will be added via a future customizer.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = ActivityParticipant.TableMetaDataCustomizer.class
)
public class ActivityParticipant extends QRecordEntity
{
   public static final String TABLE_NAME = "crmActivityParticipant";



   /***************************************************************************
    ** Customizer that sets icon, record label, and sections.
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
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("id")
            .withSection(SectionFactory.defaultT1("id", "activityId", "contactId", "userId", "role"))
            .withSection(SectionFactory.defaultT3("createDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Activity.TABLE_NAME)
   private Integer activityId;

   @QField()
   private Integer contactId;

   @QField()
   private String userId;

   @QField(possibleValueSourceName = CrmParticipantRole.NAME)
   private Integer role;

   @QField(isEditable = false)
   private Instant createDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public ActivityParticipant()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public ActivityParticipant(QRecord record)
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
   public ActivityParticipant withId(Integer id)
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
   public ActivityParticipant withActivityId(Integer activityId)
   {
      this.activityId = activityId;
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
   public ActivityParticipant withContactId(Integer contactId)
   {
      this.contactId = contactId;
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
   public ActivityParticipant withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for role
    *******************************************************************************/
   public Integer getRole()
   {
      return (this.role);
   }



   /*******************************************************************************
    ** Setter for role
    *******************************************************************************/
   public void setRole(Integer role)
   {
      this.role = role;
   }



   /*******************************************************************************
    ** Fluent setter for role
    *******************************************************************************/
   public ActivityParticipant withRole(Integer role)
   {
      this.role = role;
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
   public ActivityParticipant withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
