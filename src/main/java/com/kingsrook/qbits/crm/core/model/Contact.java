/*******************************************************************************
 ** QRecord Entity for Contact table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProductionContext;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;


/*******************************************************************************
 ** QRecord Entity for Contact table -- the primary person record in the CRM.
 ** Contacts are linked to companies, activities, deals, and tags.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = Contact.TableMetaDataCustomizer.class
)
public class Contact extends QRecordEntity
{
   public static final String TABLE_NAME = "crmContact";



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
         QFieldSection t1Section = SectionFactory.defaultT1("id", "firstName", "lastName", "email", "phone");

         table
            .withIcon(new QIcon().withName("person"))
            .withRecordLabelFormat("%s %s")
            .withRecordLabelFields("firstName", "lastName")
            .withSection(t1Section)
            .withSection(new QFieldSection("details", new QIcon().withName("info"), Tier.T2,
               List.of("companyId", "jobTitle", "department", "lifecycleStageId", "leadSourceId",
                  "ownerUserId", "leadScore", "timezone", "preferredLanguage")))
            .withSection(new QFieldSection("online", new QIcon().withName("language"), Tier.T2,
               List.of("secondaryEmail", "mobilePhone", "website", "linkedinUrl")))
            .withSection(new QFieldSection("address", new QIcon().withName("location_on"), Tier.T2,
               List.of("addressLine1", "addressLine2", "city", "state", "postalCode", "country")))
            .withSection(new QFieldSection("communication", new QIcon().withName("settings"), Tier.T2,
               List.of("doNotEmail", "doNotCall", "unsubscribeDate")))
            .withSection(SectionFactory.defaultT2("description"))
            .withSection(SectionFactory.defaultT3("lastActivityDate", "lastContactedDate", "createDate", "modifyDate"));

         ///////////////////////////////////////////////////////////////
         // inject security fields and locks from CrmQBitConfig      //
         ///////////////////////////////////////////////////////////////
         CrmQBitConfig config = (CrmQBitConfig) QBitProductionContext.peekQBitConfig();
         if(config != null)
         {
            if(config.getSecurityFields() != null)
            {
               t1Section.setFieldNames(new MutableList<>(t1Section.getFieldNames()));
               for(QFieldMetaData fieldMetaData : config.getSecurityFields())
               {
                  table.addField(fieldMetaData.clone());
                  t1Section.getFieldNames().add(fieldMetaData.getName());
               }
            }

            if(config.getRecordSecurityLocks() != null)
            {
               for(RecordSecurityLock lock : config.getRecordSecurityLocks())
               {
                  table.withRecordSecurityLock(lock);
               }
            }
         }

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String firstName;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String lastName;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String email;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String secondaryEmail;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String phone;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String mobilePhone;

   @QField(maxLength = 150, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String jobTitle;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String department;

   @QField(possibleValueSourceName = Company.TABLE_NAME)
   private Integer companyId;

   @QField(possibleValueSourceName = LifecycleStage.TABLE_NAME)
   private Integer lifecycleStageId;

   @QField(possibleValueSourceName = LeadSource.TABLE_NAME)
   private Integer leadSourceId;

   @QField(isRequired = true)
   private String ownerUserId;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String addressLine1;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String addressLine2;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String city;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String state;

   @QField(maxLength = 20, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String postalCode;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String country;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String timezone;

   @QField(maxLength = 10, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String preferredLanguage;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String website;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String linkedinUrl;

   @QField()
   private String description;

   @QField(defaultValue = "0")
   private Integer leadScore;

   @QField(defaultValue = "false")
   private Boolean doNotEmail;

   @QField(defaultValue = "false")
   private Boolean doNotCall;

   @QField()
   private Instant unsubscribeDate;

   @QField(isEditable = false)
   private Instant lastActivityDate;

   @QField(isEditable = false)
   private Instant lastContactedDate;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public Contact()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public Contact(QRecord record)
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
   public Contact withId(Integer id)
   {
      this.id = id;
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
   public Contact withFirstName(String firstName)
   {
      this.firstName = firstName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastName
    *******************************************************************************/
   public String getLastName()
   {
      return (this.lastName);
   }



   /*******************************************************************************
    ** Setter for lastName
    *******************************************************************************/
   public void setLastName(String lastName)
   {
      this.lastName = lastName;
   }



   /*******************************************************************************
    ** Fluent setter for lastName
    *******************************************************************************/
   public Contact withLastName(String lastName)
   {
      this.lastName = lastName;
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
   public Contact withEmail(String email)
   {
      this.email = email;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secondaryEmail
    *******************************************************************************/
   public String getSecondaryEmail()
   {
      return (this.secondaryEmail);
   }



   /*******************************************************************************
    ** Setter for secondaryEmail
    *******************************************************************************/
   public void setSecondaryEmail(String secondaryEmail)
   {
      this.secondaryEmail = secondaryEmail;
   }



   /*******************************************************************************
    ** Fluent setter for secondaryEmail
    *******************************************************************************/
   public Contact withSecondaryEmail(String secondaryEmail)
   {
      this.secondaryEmail = secondaryEmail;
      return (this);
   }



   /*******************************************************************************
    ** Getter for phone
    *******************************************************************************/
   public String getPhone()
   {
      return (this.phone);
   }



   /*******************************************************************************
    ** Setter for phone
    *******************************************************************************/
   public void setPhone(String phone)
   {
      this.phone = phone;
   }



   /*******************************************************************************
    ** Fluent setter for phone
    *******************************************************************************/
   public Contact withPhone(String phone)
   {
      this.phone = phone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mobilePhone
    *******************************************************************************/
   public String getMobilePhone()
   {
      return (this.mobilePhone);
   }



   /*******************************************************************************
    ** Setter for mobilePhone
    *******************************************************************************/
   public void setMobilePhone(String mobilePhone)
   {
      this.mobilePhone = mobilePhone;
   }



   /*******************************************************************************
    ** Fluent setter for mobilePhone
    *******************************************************************************/
   public Contact withMobilePhone(String mobilePhone)
   {
      this.mobilePhone = mobilePhone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobTitle
    *******************************************************************************/
   public String getJobTitle()
   {
      return (this.jobTitle);
   }



   /*******************************************************************************
    ** Setter for jobTitle
    *******************************************************************************/
   public void setJobTitle(String jobTitle)
   {
      this.jobTitle = jobTitle;
   }



   /*******************************************************************************
    ** Fluent setter for jobTitle
    *******************************************************************************/
   public Contact withJobTitle(String jobTitle)
   {
      this.jobTitle = jobTitle;
      return (this);
   }



   /*******************************************************************************
    ** Getter for department
    *******************************************************************************/
   public String getDepartment()
   {
      return (this.department);
   }



   /*******************************************************************************
    ** Setter for department
    *******************************************************************************/
   public void setDepartment(String department)
   {
      this.department = department;
   }



   /*******************************************************************************
    ** Fluent setter for department
    *******************************************************************************/
   public Contact withDepartment(String department)
   {
      this.department = department;
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
   public Contact withCompanyId(Integer companyId)
   {
      this.companyId = companyId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lifecycleStageId
    *******************************************************************************/
   public Integer getLifecycleStageId()
   {
      return (this.lifecycleStageId);
   }



   /*******************************************************************************
    ** Setter for lifecycleStageId
    *******************************************************************************/
   public void setLifecycleStageId(Integer lifecycleStageId)
   {
      this.lifecycleStageId = lifecycleStageId;
   }



   /*******************************************************************************
    ** Fluent setter for lifecycleStageId
    *******************************************************************************/
   public Contact withLifecycleStageId(Integer lifecycleStageId)
   {
      this.lifecycleStageId = lifecycleStageId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for leadSourceId
    *******************************************************************************/
   public Integer getLeadSourceId()
   {
      return (this.leadSourceId);
   }



   /*******************************************************************************
    ** Setter for leadSourceId
    *******************************************************************************/
   public void setLeadSourceId(Integer leadSourceId)
   {
      this.leadSourceId = leadSourceId;
   }



   /*******************************************************************************
    ** Fluent setter for leadSourceId
    *******************************************************************************/
   public Contact withLeadSourceId(Integer leadSourceId)
   {
      this.leadSourceId = leadSourceId;
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
   public Contact withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for addressLine1
    *******************************************************************************/
   public String getAddressLine1()
   {
      return (this.addressLine1);
   }



   /*******************************************************************************
    ** Setter for addressLine1
    *******************************************************************************/
   public void setAddressLine1(String addressLine1)
   {
      this.addressLine1 = addressLine1;
   }



   /*******************************************************************************
    ** Fluent setter for addressLine1
    *******************************************************************************/
   public Contact withAddressLine1(String addressLine1)
   {
      this.addressLine1 = addressLine1;
      return (this);
   }



   /*******************************************************************************
    ** Getter for addressLine2
    *******************************************************************************/
   public String getAddressLine2()
   {
      return (this.addressLine2);
   }



   /*******************************************************************************
    ** Setter for addressLine2
    *******************************************************************************/
   public void setAddressLine2(String addressLine2)
   {
      this.addressLine2 = addressLine2;
   }



   /*******************************************************************************
    ** Fluent setter for addressLine2
    *******************************************************************************/
   public Contact withAddressLine2(String addressLine2)
   {
      this.addressLine2 = addressLine2;
      return (this);
   }



   /*******************************************************************************
    ** Getter for city
    *******************************************************************************/
   public String getCity()
   {
      return (this.city);
   }



   /*******************************************************************************
    ** Setter for city
    *******************************************************************************/
   public void setCity(String city)
   {
      this.city = city;
   }



   /*******************************************************************************
    ** Fluent setter for city
    *******************************************************************************/
   public Contact withCity(String city)
   {
      this.city = city;
      return (this);
   }



   /*******************************************************************************
    ** Getter for state
    *******************************************************************************/
   public String getState()
   {
      return (this.state);
   }



   /*******************************************************************************
    ** Setter for state
    *******************************************************************************/
   public void setState(String state)
   {
      this.state = state;
   }



   /*******************************************************************************
    ** Fluent setter for state
    *******************************************************************************/
   public Contact withState(String state)
   {
      this.state = state;
      return (this);
   }



   /*******************************************************************************
    ** Getter for postalCode
    *******************************************************************************/
   public String getPostalCode()
   {
      return (this.postalCode);
   }



   /*******************************************************************************
    ** Setter for postalCode
    *******************************************************************************/
   public void setPostalCode(String postalCode)
   {
      this.postalCode = postalCode;
   }



   /*******************************************************************************
    ** Fluent setter for postalCode
    *******************************************************************************/
   public Contact withPostalCode(String postalCode)
   {
      this.postalCode = postalCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for country
    *******************************************************************************/
   public String getCountry()
   {
      return (this.country);
   }



   /*******************************************************************************
    ** Setter for country
    *******************************************************************************/
   public void setCountry(String country)
   {
      this.country = country;
   }



   /*******************************************************************************
    ** Fluent setter for country
    *******************************************************************************/
   public Contact withCountry(String country)
   {
      this.country = country;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timezone
    *******************************************************************************/
   public String getTimezone()
   {
      return (this.timezone);
   }



   /*******************************************************************************
    ** Setter for timezone
    *******************************************************************************/
   public void setTimezone(String timezone)
   {
      this.timezone = timezone;
   }



   /*******************************************************************************
    ** Fluent setter for timezone
    *******************************************************************************/
   public Contact withTimezone(String timezone)
   {
      this.timezone = timezone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for preferredLanguage
    *******************************************************************************/
   public String getPreferredLanguage()
   {
      return (this.preferredLanguage);
   }



   /*******************************************************************************
    ** Setter for preferredLanguage
    *******************************************************************************/
   public void setPreferredLanguage(String preferredLanguage)
   {
      this.preferredLanguage = preferredLanguage;
   }



   /*******************************************************************************
    ** Fluent setter for preferredLanguage
    *******************************************************************************/
   public Contact withPreferredLanguage(String preferredLanguage)
   {
      this.preferredLanguage = preferredLanguage;
      return (this);
   }



   /*******************************************************************************
    ** Getter for website
    *******************************************************************************/
   public String getWebsite()
   {
      return (this.website);
   }



   /*******************************************************************************
    ** Setter for website
    *******************************************************************************/
   public void setWebsite(String website)
   {
      this.website = website;
   }



   /*******************************************************************************
    ** Fluent setter for website
    *******************************************************************************/
   public Contact withWebsite(String website)
   {
      this.website = website;
      return (this);
   }



   /*******************************************************************************
    ** Getter for linkedinUrl
    *******************************************************************************/
   public String getLinkedinUrl()
   {
      return (this.linkedinUrl);
   }



   /*******************************************************************************
    ** Setter for linkedinUrl
    *******************************************************************************/
   public void setLinkedinUrl(String linkedinUrl)
   {
      this.linkedinUrl = linkedinUrl;
   }



   /*******************************************************************************
    ** Fluent setter for linkedinUrl
    *******************************************************************************/
   public Contact withLinkedinUrl(String linkedinUrl)
   {
      this.linkedinUrl = linkedinUrl;
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
   public Contact withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for leadScore
    *******************************************************************************/
   public Integer getLeadScore()
   {
      return (this.leadScore);
   }



   /*******************************************************************************
    ** Setter for leadScore
    *******************************************************************************/
   public void setLeadScore(Integer leadScore)
   {
      this.leadScore = leadScore;
   }



   /*******************************************************************************
    ** Fluent setter for leadScore
    *******************************************************************************/
   public Contact withLeadScore(Integer leadScore)
   {
      this.leadScore = leadScore;
      return (this);
   }



   /*******************************************************************************
    ** Getter for doNotEmail
    *******************************************************************************/
   public Boolean getDoNotEmail()
   {
      return (this.doNotEmail);
   }



   /*******************************************************************************
    ** Setter for doNotEmail
    *******************************************************************************/
   public void setDoNotEmail(Boolean doNotEmail)
   {
      this.doNotEmail = doNotEmail;
   }



   /*******************************************************************************
    ** Fluent setter for doNotEmail
    *******************************************************************************/
   public Contact withDoNotEmail(Boolean doNotEmail)
   {
      this.doNotEmail = doNotEmail;
      return (this);
   }



   /*******************************************************************************
    ** Getter for doNotCall
    *******************************************************************************/
   public Boolean getDoNotCall()
   {
      return (this.doNotCall);
   }



   /*******************************************************************************
    ** Setter for doNotCall
    *******************************************************************************/
   public void setDoNotCall(Boolean doNotCall)
   {
      this.doNotCall = doNotCall;
   }



   /*******************************************************************************
    ** Fluent setter for doNotCall
    *******************************************************************************/
   public Contact withDoNotCall(Boolean doNotCall)
   {
      this.doNotCall = doNotCall;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unsubscribeDate
    *******************************************************************************/
   public Instant getUnsubscribeDate()
   {
      return (this.unsubscribeDate);
   }



   /*******************************************************************************
    ** Setter for unsubscribeDate
    *******************************************************************************/
   public void setUnsubscribeDate(Instant unsubscribeDate)
   {
      this.unsubscribeDate = unsubscribeDate;
   }



   /*******************************************************************************
    ** Fluent setter for unsubscribeDate
    *******************************************************************************/
   public Contact withUnsubscribeDate(Instant unsubscribeDate)
   {
      this.unsubscribeDate = unsubscribeDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastActivityDate
    *******************************************************************************/
   public Instant getLastActivityDate()
   {
      return (this.lastActivityDate);
   }



   /*******************************************************************************
    ** Setter for lastActivityDate
    *******************************************************************************/
   public void setLastActivityDate(Instant lastActivityDate)
   {
      this.lastActivityDate = lastActivityDate;
   }



   /*******************************************************************************
    ** Fluent setter for lastActivityDate
    *******************************************************************************/
   public Contact withLastActivityDate(Instant lastActivityDate)
   {
      this.lastActivityDate = lastActivityDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastContactedDate
    *******************************************************************************/
   public Instant getLastContactedDate()
   {
      return (this.lastContactedDate);
   }



   /*******************************************************************************
    ** Setter for lastContactedDate
    *******************************************************************************/
   public void setLastContactedDate(Instant lastContactedDate)
   {
      this.lastContactedDate = lastContactedDate;
   }



   /*******************************************************************************
    ** Fluent setter for lastContactedDate
    *******************************************************************************/
   public Contact withLastContactedDate(Instant lastContactedDate)
   {
      this.lastContactedDate = lastContactedDate;
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
   public Contact withCreateDate(Instant createDate)
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
   public Contact withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
