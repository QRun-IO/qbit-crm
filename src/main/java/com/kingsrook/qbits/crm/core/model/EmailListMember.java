/*******************************************************************************
 ** QRecord Entity for EmailListMember table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
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
 ** QRecord Entity for EmailListMember table -- subscribers on email lists.
 ** Supports standalone subscribers (no CRM contact) and linked subscribers.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = EmailListMember.TableMetaDataCustomizer.class
)
public class EmailListMember extends QRecordEntity
{
   public static final String TABLE_NAME = "crmEmailListMember";



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
            .withIcon(new QIcon().withName("person_add"))
            .withRecordLabelFormat("%s - %s")
            .withRecordLabelFields("emailListId", "email")
            .withUniqueKey(new UniqueKey("emailListId", "email"))
            .withSection(SectionFactory.defaultT1("id", "emailListId", "contactId", "email", "firstName", "status"))
            .withSection(SectionFactory.defaultT2("subscribedDate", "unsubscribedDate", "subscriptionSource", "formSubmissionId"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = EmailList.TABLE_NAME)
   private Integer emailListId;

   @QField(possibleValueSourceName = Contact.TABLE_NAME)
   private Integer contactId;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String email;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String firstName;

   @QField(isRequired = true, possibleValueSourceName = CrmListMemberStatus.NAME, defaultValue = "1")
   private Integer status;

   @QField(isRequired = true)
   private Instant subscribedDate;

   @QField()
   private Instant unsubscribedDate;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String subscriptionSource;

   @QField(possibleValueSourceName = FormSubmission.TABLE_NAME)
   private Integer formSubmissionId;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public EmailListMember()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public EmailListMember(QRecord record)
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
   public EmailListMember withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for emailListId
    *******************************************************************************/
   public Integer getEmailListId()
   {
      return (this.emailListId);
   }



   /*******************************************************************************
    ** Setter for emailListId
    *******************************************************************************/
   public void setEmailListId(Integer emailListId)
   {
      this.emailListId = emailListId;
   }



   /*******************************************************************************
    ** Fluent setter for emailListId
    *******************************************************************************/
   public EmailListMember withEmailListId(Integer emailListId)
   {
      this.emailListId = emailListId;
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
   public EmailListMember withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for email
    *******************************************************************************/
   public String getEmail()
   {
      return (this.email);
   }



   /*******************************************************************************
    ** Setter for email
    *******************************************************************************/
   public void setEmail(String email)
   {
      this.email = email;
   }



   /*******************************************************************************
    ** Fluent setter for email
    *******************************************************************************/
   public EmailListMember withEmail(String email)
   {
      this.email = email;
      return (this);
   }



   /*******************************************************************************
    ** Getter for firstName
    *******************************************************************************/
   public String getFirstName()
   {
      return (this.firstName);
   }



   /*******************************************************************************
    ** Setter for firstName
    *******************************************************************************/
   public void setFirstName(String firstName)
   {
      this.firstName = firstName;
   }



   /*******************************************************************************
    ** Fluent setter for firstName
    *******************************************************************************/
   public EmailListMember withFirstName(String firstName)
   {
      this.firstName = firstName;
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
   public EmailListMember withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subscribedDate
    *******************************************************************************/
   public Instant getSubscribedDate()
   {
      return (this.subscribedDate);
   }



   /*******************************************************************************
    ** Setter for subscribedDate
    *******************************************************************************/
   public void setSubscribedDate(Instant subscribedDate)
   {
      this.subscribedDate = subscribedDate;
   }



   /*******************************************************************************
    ** Fluent setter for subscribedDate
    *******************************************************************************/
   public EmailListMember withSubscribedDate(Instant subscribedDate)
   {
      this.subscribedDate = subscribedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unsubscribedDate
    *******************************************************************************/
   public Instant getUnsubscribedDate()
   {
      return (this.unsubscribedDate);
   }



   /*******************************************************************************
    ** Setter for unsubscribedDate
    *******************************************************************************/
   public void setUnsubscribedDate(Instant unsubscribedDate)
   {
      this.unsubscribedDate = unsubscribedDate;
   }



   /*******************************************************************************
    ** Fluent setter for unsubscribedDate
    *******************************************************************************/
   public EmailListMember withUnsubscribedDate(Instant unsubscribedDate)
   {
      this.unsubscribedDate = unsubscribedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subscriptionSource
    *******************************************************************************/
   public String getSubscriptionSource()
   {
      return (this.subscriptionSource);
   }



   /*******************************************************************************
    ** Setter for subscriptionSource
    *******************************************************************************/
   public void setSubscriptionSource(String subscriptionSource)
   {
      this.subscriptionSource = subscriptionSource;
   }



   /*******************************************************************************
    ** Fluent setter for subscriptionSource
    *******************************************************************************/
   public EmailListMember withSubscriptionSource(String subscriptionSource)
   {
      this.subscriptionSource = subscriptionSource;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formSubmissionId
    *******************************************************************************/
   public Integer getFormSubmissionId()
   {
      return (this.formSubmissionId);
   }



   /*******************************************************************************
    ** Setter for formSubmissionId
    *******************************************************************************/
   public void setFormSubmissionId(Integer formSubmissionId)
   {
      this.formSubmissionId = formSubmissionId;
   }



   /*******************************************************************************
    ** Fluent setter for formSubmissionId
    *******************************************************************************/
   public EmailListMember withFormSubmissionId(Integer formSubmissionId)
   {
      this.formSubmissionId = formSubmissionId;
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
   public EmailListMember withCreateDate(Instant createDate)
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
   public EmailListMember withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
