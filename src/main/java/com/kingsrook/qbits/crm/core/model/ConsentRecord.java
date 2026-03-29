/*******************************************************************************
 ** QRecord Entity for ConsentRecord table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmLegalBasis;
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


/*******************************************************************************
 ** QRecord Entity for ConsentRecord table -- GDPR/CAN-SPAM consent tracking.
 ** Append-only: new consent states are recorded as new rows.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = ConsentRecord.TableMetaDataCustomizer.class
)
public class ConsentRecord extends QRecordEntity
{
   public static final String TABLE_NAME = "crmConsentRecord";



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
            .withIcon(new QIcon().withName("verified_user"))
            .withRecordLabelFormat("Consent %s - %s")
            .withRecordLabelFields("contactId", "consentType")
            .withSection(SectionFactory.defaultT1("id", "contactId", "consentType", "status"))
            .withSection(SectionFactory.defaultT2("legalBasis", "source", "consentDate", "expiryDate", "ipAddress"))
            .withSection(SectionFactory.defaultT3("createDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Contact.TABLE_NAME)
   private Integer contactId;

   @QField(isRequired = true, possibleValueSourceName = CrmConsentType.NAME)
   private Integer consentType;

   @QField(isRequired = true, possibleValueSourceName = CrmConsentStatus.NAME)
   private Integer status;

   @QField(possibleValueSourceName = CrmLegalBasis.NAME)
   private Integer legalBasis;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String source;

   @QField(isRequired = true)
   private Instant consentDate;

   @QField()
   private Instant expiryDate;

   @QField(maxLength = 45, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String ipAddress;

   @QField(isEditable = false)
   private Instant createDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public ConsentRecord()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public ConsentRecord(QRecord record)
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
   public ConsentRecord withId(Integer id)
   {
      this.id = id;
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
   public ConsentRecord withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for consentType
    *******************************************************************************/
   public Integer getConsentType()
   {
      return (this.consentType);
   }



   /*******************************************************************************
    ** Setter for consentType
    *******************************************************************************/
   public void setConsentType(Integer consentType)
   {
      this.consentType = consentType;
   }



   /*******************************************************************************
    ** Fluent setter for consentType
    *******************************************************************************/
   public ConsentRecord withConsentType(Integer consentType)
   {
      this.consentType = consentType;
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
   public ConsentRecord withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for legalBasis
    *******************************************************************************/
   public Integer getLegalBasis()
   {
      return (this.legalBasis);
   }



   /*******************************************************************************
    ** Setter for legalBasis
    *******************************************************************************/
   public void setLegalBasis(Integer legalBasis)
   {
      this.legalBasis = legalBasis;
   }



   /*******************************************************************************
    ** Fluent setter for legalBasis
    *******************************************************************************/
   public ConsentRecord withLegalBasis(Integer legalBasis)
   {
      this.legalBasis = legalBasis;
      return (this);
   }



   /*******************************************************************************
    ** Getter for source
    *******************************************************************************/
   public String getSource()
   {
      return (this.source);
   }



   /*******************************************************************************
    ** Setter for source
    *******************************************************************************/
   public void setSource(String source)
   {
      this.source = source;
   }



   /*******************************************************************************
    ** Fluent setter for source
    *******************************************************************************/
   public ConsentRecord withSource(String source)
   {
      this.source = source;
      return (this);
   }



   /*******************************************************************************
    ** Getter for consentDate
    *******************************************************************************/
   public Instant getConsentDate()
   {
      return (this.consentDate);
   }



   /*******************************************************************************
    ** Setter for consentDate
    *******************************************************************************/
   public void setConsentDate(Instant consentDate)
   {
      this.consentDate = consentDate;
   }



   /*******************************************************************************
    ** Fluent setter for consentDate
    *******************************************************************************/
   public ConsentRecord withConsentDate(Instant consentDate)
   {
      this.consentDate = consentDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expiryDate
    *******************************************************************************/
   public Instant getExpiryDate()
   {
      return (this.expiryDate);
   }



   /*******************************************************************************
    ** Setter for expiryDate
    *******************************************************************************/
   public void setExpiryDate(Instant expiryDate)
   {
      this.expiryDate = expiryDate;
   }



   /*******************************************************************************
    ** Fluent setter for expiryDate
    *******************************************************************************/
   public ConsentRecord withExpiryDate(Instant expiryDate)
   {
      this.expiryDate = expiryDate;
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
   public ConsentRecord withIpAddress(String ipAddress)
   {
      this.ipAddress = ipAddress;
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
   public ConsentRecord withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
