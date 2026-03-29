/*******************************************************************************
 ** QRecord Entity for Currency table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.math.BigDecimal;
import java.time.Instant;
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
 ** QRecord Entity for Currency table -- exchange rate reference table for
 ** multi-currency support. Stores ISO 4217 currency codes and conversion rates
 ** relative to the system base currency.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = Currency.TableMetaDataCustomizer.class
)
public class Currency extends QRecordEntity
{
   public static final String TABLE_NAME = "crmCurrency";



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
            .withIcon(new QIcon().withName("currency_exchange"))
            .withRecordLabelFormat("%s - %s")
            .withRecordLabelFields("currencyCode", "name")
            .withUniqueKey(new UniqueKey("currencyCode"))
            .withSection(SectionFactory.defaultT1("id", "currencyCode", "name", "symbol"))
            .withSection(SectionFactory.defaultT2("exchangeRateToBase", "isBaseCurrency", "isActive", "rateLastUpdatedDate"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 3, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String currencyCode;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(maxLength = 5, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String symbol;

   @QField(isRequired = true, defaultValue = "1.0")
   private BigDecimal exchangeRateToBase;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isBaseCurrency;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField()
   private Instant rateLastUpdatedDate;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public Currency()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public Currency(QRecord record)
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
   public Currency withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for currencyCode
    *******************************************************************************/
   public String getCurrencyCode()
   {
      return (this.currencyCode);
   }



   /*******************************************************************************
    ** Setter for currencyCode
    *******************************************************************************/
   public void setCurrencyCode(String currencyCode)
   {
      this.currencyCode = currencyCode;
   }



   /*******************************************************************************
    ** Fluent setter for currencyCode
    *******************************************************************************/
   public Currency withCurrencyCode(String currencyCode)
   {
      this.currencyCode = currencyCode;
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
   public Currency withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for symbol
    *******************************************************************************/
   public String getSymbol()
   {
      return (this.symbol);
   }



   /*******************************************************************************
    ** Setter for symbol
    *******************************************************************************/
   public void setSymbol(String symbol)
   {
      this.symbol = symbol;
   }



   /*******************************************************************************
    ** Fluent setter for symbol
    *******************************************************************************/
   public Currency withSymbol(String symbol)
   {
      this.symbol = symbol;
      return (this);
   }



   /*******************************************************************************
    ** Getter for exchangeRateToBase
    *******************************************************************************/
   public BigDecimal getExchangeRateToBase()
   {
      return (this.exchangeRateToBase);
   }



   /*******************************************************************************
    ** Setter for exchangeRateToBase
    *******************************************************************************/
   public void setExchangeRateToBase(BigDecimal exchangeRateToBase)
   {
      this.exchangeRateToBase = exchangeRateToBase;
   }



   /*******************************************************************************
    ** Fluent setter for exchangeRateToBase
    *******************************************************************************/
   public Currency withExchangeRateToBase(BigDecimal exchangeRateToBase)
   {
      this.exchangeRateToBase = exchangeRateToBase;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isBaseCurrency
    *******************************************************************************/
   public Boolean getIsBaseCurrency()
   {
      return (this.isBaseCurrency);
   }



   /*******************************************************************************
    ** Setter for isBaseCurrency
    *******************************************************************************/
   public void setIsBaseCurrency(Boolean isBaseCurrency)
   {
      this.isBaseCurrency = isBaseCurrency;
   }



   /*******************************************************************************
    ** Fluent setter for isBaseCurrency
    *******************************************************************************/
   public Currency withIsBaseCurrency(Boolean isBaseCurrency)
   {
      this.isBaseCurrency = isBaseCurrency;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isActive
    *******************************************************************************/
   public Boolean getIsActive()
   {
      return (this.isActive);
   }



   /*******************************************************************************
    ** Setter for isActive
    *******************************************************************************/
   public void setIsActive(Boolean isActive)
   {
      this.isActive = isActive;
   }



   /*******************************************************************************
    ** Fluent setter for isActive
    *******************************************************************************/
   public Currency withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rateLastUpdatedDate
    *******************************************************************************/
   public Instant getRateLastUpdatedDate()
   {
      return (this.rateLastUpdatedDate);
   }



   /*******************************************************************************
    ** Setter for rateLastUpdatedDate
    *******************************************************************************/
   public void setRateLastUpdatedDate(Instant rateLastUpdatedDate)
   {
      this.rateLastUpdatedDate = rateLastUpdatedDate;
   }



   /*******************************************************************************
    ** Fluent setter for rateLastUpdatedDate
    *******************************************************************************/
   public Currency withRateLastUpdatedDate(Instant rateLastUpdatedDate)
   {
      this.rateLastUpdatedDate = rateLastUpdatedDate;
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
   public Currency withCreateDate(Instant createDate)
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
   public Currency withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
