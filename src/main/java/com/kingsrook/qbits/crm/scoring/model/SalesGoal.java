/*******************************************************************************
 ** QRecord Entity for SalesGoal table
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.kingsrook.qbits.crm.core.model.enums.CrmGoalPeriod;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
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
 ** QRecord Entity for SalesGoal table -- a revenue target for a user over a
 ** defined period, optionally scoped to a specific pipeline.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = SalesGoal.TableMetaDataCustomizer.class
)
public class SalesGoal extends QRecordEntity
{
   public static final String TABLE_NAME = "crmSalesGoal";



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
            .withIcon(new QIcon().withName("flag"))
            .withRecordLabelFormat("Goal #%s")
            .withRecordLabelFields("id")
            .withUniqueKey(new UniqueKey("userId", "pipelineId", "periodType", "periodStart"))
            .withSection(SectionFactory.defaultT1("id", "userId", "pipelineId", "periodType"))
            .withSection(SectionFactory.defaultT2("periodStart", "periodEnd", "targetAmount", "currencyCode"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true)
   private String userId;

   @QField(possibleValueSourceName = Pipeline.TABLE_NAME)
   private Integer pipelineId;

   @QField(isRequired = true, possibleValueSourceName = CrmGoalPeriod.NAME)
   private Integer periodType;

   @QField(isRequired = true)
   private LocalDate periodStart;

   @QField(isRequired = true)
   private LocalDate periodEnd;

   @QField(isRequired = true)
   private BigDecimal targetAmount;

   @QField(maxLength = 3, valueTooLongBehavior = ValueTooLongBehavior.ERROR, defaultValue = "USD")
   private String currencyCode;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public SalesGoal()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public SalesGoal(QRecord record)
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
   public SalesGoal withId(Integer id)
   {
      this.id = id;
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
   public SalesGoal withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pipelineId
    *******************************************************************************/
   public Integer getPipelineId()
   {
      return (this.pipelineId);
   }



   /*******************************************************************************
    ** Setter for pipelineId
    *******************************************************************************/
   public void setPipelineId(Integer pipelineId)
   {
      this.pipelineId = pipelineId;
   }



   /*******************************************************************************
    ** Fluent setter for pipelineId
    *******************************************************************************/
   public SalesGoal withPipelineId(Integer pipelineId)
   {
      this.pipelineId = pipelineId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for periodType
    *******************************************************************************/
   public Integer getPeriodType()
   {
      return (this.periodType);
   }



   /*******************************************************************************
    ** Setter for periodType
    *******************************************************************************/
   public void setPeriodType(Integer periodType)
   {
      this.periodType = periodType;
   }



   /*******************************************************************************
    ** Fluent setter for periodType
    *******************************************************************************/
   public SalesGoal withPeriodType(Integer periodType)
   {
      this.periodType = periodType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for periodStart
    *******************************************************************************/
   public LocalDate getPeriodStart()
   {
      return (this.periodStart);
   }



   /*******************************************************************************
    ** Setter for periodStart
    *******************************************************************************/
   public void setPeriodStart(LocalDate periodStart)
   {
      this.periodStart = periodStart;
   }



   /*******************************************************************************
    ** Fluent setter for periodStart
    *******************************************************************************/
   public SalesGoal withPeriodStart(LocalDate periodStart)
   {
      this.periodStart = periodStart;
      return (this);
   }



   /*******************************************************************************
    ** Getter for periodEnd
    *******************************************************************************/
   public LocalDate getPeriodEnd()
   {
      return (this.periodEnd);
   }



   /*******************************************************************************
    ** Setter for periodEnd
    *******************************************************************************/
   public void setPeriodEnd(LocalDate periodEnd)
   {
      this.periodEnd = periodEnd;
   }



   /*******************************************************************************
    ** Fluent setter for periodEnd
    *******************************************************************************/
   public SalesGoal withPeriodEnd(LocalDate periodEnd)
   {
      this.periodEnd = periodEnd;
      return (this);
   }



   /*******************************************************************************
    ** Getter for targetAmount
    *******************************************************************************/
   public BigDecimal getTargetAmount()
   {
      return (this.targetAmount);
   }



   /*******************************************************************************
    ** Setter for targetAmount
    *******************************************************************************/
   public void setTargetAmount(BigDecimal targetAmount)
   {
      this.targetAmount = targetAmount;
   }



   /*******************************************************************************
    ** Fluent setter for targetAmount
    *******************************************************************************/
   public SalesGoal withTargetAmount(BigDecimal targetAmount)
   {
      this.targetAmount = targetAmount;
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
   public SalesGoal withCurrencyCode(String currencyCode)
   {
      this.currencyCode = currencyCode;
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
   public SalesGoal withCreateDate(Instant createDate)
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
   public SalesGoal withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
