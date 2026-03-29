/*******************************************************************************
 ** QRecord Entity for FormSubmission table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
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
 ** QRecord Entity for FormSubmission table -- raw website form submissions.
 ** Captures web-specific metadata (UTM, referrer, IP) that does not belong on
 ** crm_contact. Submissions are converted to contacts via a process.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = FormSubmission.TableMetaDataCustomizer.class
)
public class FormSubmission extends QRecordEntity
{
   public static final String TABLE_NAME = "crmFormSubmission";



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
            .withIcon(new QIcon().withName("dynamic_form"))
            .withRecordLabelFormat("%s - %s %s")
            .withRecordLabelFields("formType", "firstName", "email")
            .withSection(SectionFactory.defaultT1("id", "formType", "email", "firstName", "lastName"))
            .withSection(SectionFactory.defaultT2("company", "phone", "message"))
            .withSection(new QFieldSection("utm", new QIcon().withName("campaign"), Tier.T2,
               List.of("sourceUrl", "utmSource", "utmMedium", "utmCampaign", "utmTerm", "utmContent", "referrer")))
            .withSection(new QFieldSection("conversion", new QIcon().withName("swap_horiz"), Tier.T2,
               List.of("contactId", "conversionStatus", "convertedDate", "convertedByUserId")))
            .withSection(new QFieldSection("technical", new QIcon().withName("code"), Tier.T2,
               List.of("ipAddress", "userAgent", "metadata")))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = CrmFormType.NAME)
   private Integer formType;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String email;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String firstName;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String lastName;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String company;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String phone;

   @QField()
   private String message;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String sourceUrl;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String utmSource;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String utmMedium;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String utmCampaign;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String utmTerm;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String utmContent;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String referrer;

   @QField(possibleValueSourceName = Contact.TABLE_NAME)
   private Integer contactId;

   @QField(isRequired = true, possibleValueSourceName = CrmFormConversionStatus.NAME, defaultValue = "1")
   private Integer conversionStatus;

   @QField(isEditable = false)
   private Instant convertedDate;

   @QField()
   private String convertedByUserId;

   @QField(maxLength = 45, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String ipAddress;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String userAgent;

   @QField()
   private String metadata;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public FormSubmission()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public FormSubmission(QRecord record)
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
   public FormSubmission withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formType
    *******************************************************************************/
   public Integer getFormType()
   {
      return (this.formType);
   }



   /*******************************************************************************
    ** Setter for formType
    *******************************************************************************/
   public void setFormType(Integer formType)
   {
      this.formType = formType;
   }



   /*******************************************************************************
    ** Fluent setter for formType
    *******************************************************************************/
   public FormSubmission withFormType(Integer formType)
   {
      this.formType = formType;
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
   public FormSubmission withEmail(String email)
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
   public FormSubmission withFirstName(String firstName)
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
   public FormSubmission withLastName(String lastName)
   {
      this.lastName = lastName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for company
    *******************************************************************************/
   public String getCompany()
   {
      return (this.company);
   }



   /*******************************************************************************
    ** Setter for company
    *******************************************************************************/
   public void setCompany(String company)
   {
      this.company = company;
   }



   /*******************************************************************************
    ** Fluent setter for company
    *******************************************************************************/
   public FormSubmission withCompany(String company)
   {
      this.company = company;
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
   public FormSubmission withPhone(String phone)
   {
      this.phone = phone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public FormSubmission withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceUrl
    *******************************************************************************/
   public String getSourceUrl()
   {
      return (this.sourceUrl);
   }



   /*******************************************************************************
    ** Setter for sourceUrl
    *******************************************************************************/
   public void setSourceUrl(String sourceUrl)
   {
      this.sourceUrl = sourceUrl;
   }



   /*******************************************************************************
    ** Fluent setter for sourceUrl
    *******************************************************************************/
   public FormSubmission withSourceUrl(String sourceUrl)
   {
      this.sourceUrl = sourceUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for utmSource
    *******************************************************************************/
   public String getUtmSource()
   {
      return (this.utmSource);
   }



   /*******************************************************************************
    ** Setter for utmSource
    *******************************************************************************/
   public void setUtmSource(String utmSource)
   {
      this.utmSource = utmSource;
   }



   /*******************************************************************************
    ** Fluent setter for utmSource
    *******************************************************************************/
   public FormSubmission withUtmSource(String utmSource)
   {
      this.utmSource = utmSource;
      return (this);
   }



   /*******************************************************************************
    ** Getter for utmMedium
    *******************************************************************************/
   public String getUtmMedium()
   {
      return (this.utmMedium);
   }



   /*******************************************************************************
    ** Setter for utmMedium
    *******************************************************************************/
   public void setUtmMedium(String utmMedium)
   {
      this.utmMedium = utmMedium;
   }



   /*******************************************************************************
    ** Fluent setter for utmMedium
    *******************************************************************************/
   public FormSubmission withUtmMedium(String utmMedium)
   {
      this.utmMedium = utmMedium;
      return (this);
   }



   /*******************************************************************************
    ** Getter for utmCampaign
    *******************************************************************************/
   public String getUtmCampaign()
   {
      return (this.utmCampaign);
   }



   /*******************************************************************************
    ** Setter for utmCampaign
    *******************************************************************************/
   public void setUtmCampaign(String utmCampaign)
   {
      this.utmCampaign = utmCampaign;
   }



   /*******************************************************************************
    ** Fluent setter for utmCampaign
    *******************************************************************************/
   public FormSubmission withUtmCampaign(String utmCampaign)
   {
      this.utmCampaign = utmCampaign;
      return (this);
   }



   /*******************************************************************************
    ** Getter for utmTerm
    *******************************************************************************/
   public String getUtmTerm()
   {
      return (this.utmTerm);
   }



   /*******************************************************************************
    ** Setter for utmTerm
    *******************************************************************************/
   public void setUtmTerm(String utmTerm)
   {
      this.utmTerm = utmTerm;
   }



   /*******************************************************************************
    ** Fluent setter for utmTerm
    *******************************************************************************/
   public FormSubmission withUtmTerm(String utmTerm)
   {
      this.utmTerm = utmTerm;
      return (this);
   }



   /*******************************************************************************
    ** Getter for utmContent
    *******************************************************************************/
   public String getUtmContent()
   {
      return (this.utmContent);
   }



   /*******************************************************************************
    ** Setter for utmContent
    *******************************************************************************/
   public void setUtmContent(String utmContent)
   {
      this.utmContent = utmContent;
   }



   /*******************************************************************************
    ** Fluent setter for utmContent
    *******************************************************************************/
   public FormSubmission withUtmContent(String utmContent)
   {
      this.utmContent = utmContent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for referrer
    *******************************************************************************/
   public String getReferrer()
   {
      return (this.referrer);
   }



   /*******************************************************************************
    ** Setter for referrer
    *******************************************************************************/
   public void setReferrer(String referrer)
   {
      this.referrer = referrer;
   }



   /*******************************************************************************
    ** Fluent setter for referrer
    *******************************************************************************/
   public FormSubmission withReferrer(String referrer)
   {
      this.referrer = referrer;
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
   public FormSubmission withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for conversionStatus
    *******************************************************************************/
   public Integer getConversionStatus()
   {
      return (this.conversionStatus);
   }



   /*******************************************************************************
    ** Setter for conversionStatus
    *******************************************************************************/
   public void setConversionStatus(Integer conversionStatus)
   {
      this.conversionStatus = conversionStatus;
   }



   /*******************************************************************************
    ** Fluent setter for conversionStatus
    *******************************************************************************/
   public FormSubmission withConversionStatus(Integer conversionStatus)
   {
      this.conversionStatus = conversionStatus;
      return (this);
   }



   /*******************************************************************************
    ** Getter for convertedDate
    *******************************************************************************/
   public Instant getConvertedDate()
   {
      return (this.convertedDate);
   }



   /*******************************************************************************
    ** Setter for convertedDate
    *******************************************************************************/
   public void setConvertedDate(Instant convertedDate)
   {
      this.convertedDate = convertedDate;
   }



   /*******************************************************************************
    ** Fluent setter for convertedDate
    *******************************************************************************/
   public FormSubmission withConvertedDate(Instant convertedDate)
   {
      this.convertedDate = convertedDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for convertedByUserId
    *******************************************************************************/
   public String getConvertedByUserId()
   {
      return (this.convertedByUserId);
   }



   /*******************************************************************************
    ** Setter for convertedByUserId
    *******************************************************************************/
   public void setConvertedByUserId(String convertedByUserId)
   {
      this.convertedByUserId = convertedByUserId;
   }



   /*******************************************************************************
    ** Fluent setter for convertedByUserId
    *******************************************************************************/
   public FormSubmission withConvertedByUserId(String convertedByUserId)
   {
      this.convertedByUserId = convertedByUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ipAddress
    *******************************************************************************/
   public String getIpAddress()
   {
      return (this.ipAddress);
   }



   /*******************************************************************************
    ** Setter for ipAddress
    *******************************************************************************/
   public void setIpAddress(String ipAddress)
   {
      this.ipAddress = ipAddress;
   }



   /*******************************************************************************
    ** Fluent setter for ipAddress
    *******************************************************************************/
   public FormSubmission withIpAddress(String ipAddress)
   {
      this.ipAddress = ipAddress;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userAgent
    *******************************************************************************/
   public String getUserAgent()
   {
      return (this.userAgent);
   }



   /*******************************************************************************
    ** Setter for userAgent
    *******************************************************************************/
   public void setUserAgent(String userAgent)
   {
      this.userAgent = userAgent;
   }



   /*******************************************************************************
    ** Fluent setter for userAgent
    *******************************************************************************/
   public FormSubmission withUserAgent(String userAgent)
   {
      this.userAgent = userAgent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for metadata
    *******************************************************************************/
   public String getMetadata()
   {
      return (this.metadata);
   }



   /*******************************************************************************
    ** Setter for metadata
    *******************************************************************************/
   public void setMetadata(String metadata)
   {
      this.metadata = metadata;
   }



   /*******************************************************************************
    ** Fluent setter for metadata
    *******************************************************************************/
   public FormSubmission withMetadata(String metadata)
   {
      this.metadata = metadata;
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
   public FormSubmission withCreateDate(Instant createDate)
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
   public FormSubmission withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
