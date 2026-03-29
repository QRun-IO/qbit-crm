/*******************************************************************************
 ** QRecord Entity for Company table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.core.model.enums.CrmCompanyType;
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
 ** QRecord Entity for Company table -- an organization/business entity in the
 ** CRM. Supports self-referential parent-child hierarchy via parentCompanyId.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = Company.TableMetaDataCustomizer.class
)
public class Company extends QRecordEntity
{
   public static final String TABLE_NAME = "crmCompany";



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
         QFieldSection t1Section = SectionFactory.defaultT1("id", "name", "domain", "companyType");

         table
            .withIcon(new QIcon().withName("business"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withSection(t1Section)
            .withSection(new QFieldSection("details", new QIcon().withName("info"), Tier.T2,
               List.of("industryId", "employeeCount", "annualRevenue", "annualRevenueCurrencyCode",
                  "phone", "website", "ownerUserId", "parentCompanyId")))
            .withSection(new QFieldSection("address", new QIcon().withName("location_on"), Tier.T2,
               List.of("addressLine1", "addressLine2", "city", "state", "postalCode", "country")))
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

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String domain;

   @QField(possibleValueSourceName = CrmCompanyType.NAME)
   private Integer companyType;

   @QField(possibleValueSourceName = Industry.TABLE_NAME)
   private Integer industryId;

   @QField()
   private Integer employeeCount;

   @QField()
   private BigDecimal annualRevenue;

   @QField(maxLength = 3, valueTooLongBehavior = ValueTooLongBehavior.ERROR, defaultValue = "USD")
   private String annualRevenueCurrencyCode;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String phone;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String website;

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

   @QField(isRequired = true)
   private String ownerUserId;

   @QField(possibleValueSourceName = Company.TABLE_NAME)
   private Integer parentCompanyId;

   @QField()
   private String description;

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
   public Company()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public Company(QRecord record)
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
   public Company withId(Integer id)
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
   public Company withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for domain
    *******************************************************************************/
   public String getDomain()
   {
      return (this.domain);
   }



   /*******************************************************************************
    ** Setter for domain
    *******************************************************************************/
   public void setDomain(String domain)
   {
      this.domain = domain;
   }



   /*******************************************************************************
    ** Fluent setter for domain
    *******************************************************************************/
   public Company withDomain(String domain)
   {
      this.domain = domain;
      return (this);
   }



   /*******************************************************************************
    ** Getter for companyType
    *******************************************************************************/
   public Integer getCompanyType()
   {
      return (this.companyType);
   }



   /*******************************************************************************
    ** Setter for companyType
    *******************************************************************************/
   public void setCompanyType(Integer companyType)
   {
      this.companyType = companyType;
   }



   /*******************************************************************************
    ** Fluent setter for companyType
    *******************************************************************************/
   public Company withCompanyType(Integer companyType)
   {
      this.companyType = companyType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for industryId
    *******************************************************************************/
   public Integer getIndustryId()
   {
      return (this.industryId);
   }



   /*******************************************************************************
    ** Setter for industryId
    *******************************************************************************/
   public void setIndustryId(Integer industryId)
   {
      this.industryId = industryId;
   }



   /*******************************************************************************
    ** Fluent setter for industryId
    *******************************************************************************/
   public Company withIndustryId(Integer industryId)
   {
      this.industryId = industryId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for employeeCount
    *******************************************************************************/
   public Integer getEmployeeCount()
   {
      return (this.employeeCount);
   }



   /*******************************************************************************
    ** Setter for employeeCount
    *******************************************************************************/
   public void setEmployeeCount(Integer employeeCount)
   {
      this.employeeCount = employeeCount;
   }



   /*******************************************************************************
    ** Fluent setter for employeeCount
    *******************************************************************************/
   public Company withEmployeeCount(Integer employeeCount)
   {
      this.employeeCount = employeeCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for annualRevenue
    *******************************************************************************/
   public BigDecimal getAnnualRevenue()
   {
      return (this.annualRevenue);
   }



   /*******************************************************************************
    ** Setter for annualRevenue
    *******************************************************************************/
   public void setAnnualRevenue(BigDecimal annualRevenue)
   {
      this.annualRevenue = annualRevenue;
   }



   /*******************************************************************************
    ** Fluent setter for annualRevenue
    *******************************************************************************/
   public Company withAnnualRevenue(BigDecimal annualRevenue)
   {
      this.annualRevenue = annualRevenue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for annualRevenueCurrencyCode
    *******************************************************************************/
   public String getAnnualRevenueCurrencyCode()
   {
      return (this.annualRevenueCurrencyCode);
   }



   /*******************************************************************************
    ** Setter for annualRevenueCurrencyCode
    *******************************************************************************/
   public void setAnnualRevenueCurrencyCode(String annualRevenueCurrencyCode)
   {
      this.annualRevenueCurrencyCode = annualRevenueCurrencyCode;
   }



   /*******************************************************************************
    ** Fluent setter for annualRevenueCurrencyCode
    *******************************************************************************/
   public Company withAnnualRevenueCurrencyCode(String annualRevenueCurrencyCode)
   {
      this.annualRevenueCurrencyCode = annualRevenueCurrencyCode;
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
   public Company withPhone(String phone)
   {
      this.phone = phone;
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
   public Company withWebsite(String website)
   {
      this.website = website;
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
   public Company withAddressLine1(String addressLine1)
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
   public Company withAddressLine2(String addressLine2)
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
   public Company withCity(String city)
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
   public Company withState(String state)
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
   public Company withPostalCode(String postalCode)
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
   public Company withCountry(String country)
   {
      this.country = country;
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
   public Company withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentCompanyId
    *******************************************************************************/
   public Integer getParentCompanyId()
   {
      return (this.parentCompanyId);
   }



   /*******************************************************************************
    ** Setter for parentCompanyId
    *******************************************************************************/
   public void setParentCompanyId(Integer parentCompanyId)
   {
      this.parentCompanyId = parentCompanyId;
   }



   /*******************************************************************************
    ** Fluent setter for parentCompanyId
    *******************************************************************************/
   public Company withParentCompanyId(Integer parentCompanyId)
   {
      this.parentCompanyId = parentCompanyId;
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
   public Company withDescription(String description)
   {
      this.description = description;
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
   public Company withLastActivityDate(Instant lastActivityDate)
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
   public Company withLastContactedDate(Instant lastContactedDate)
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
   public Company withCreateDate(Instant createDate)
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
   public Company withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
