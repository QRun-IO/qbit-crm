/*******************************************************************************
 ** QRecord Entity for Deal table
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.LeadSource;
import com.kingsrook.qbits.crm.core.model.enums.CrmDealPriority;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildJoin;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildRecordListWidget;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildTable;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProductionContext;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;


/*******************************************************************************
 ** QRecord Entity for Deal table -- the primary revenue-tracking record in the
 ** CRM. Deals move through pipeline stages and are linked to contacts, companies,
 ** products, activities, and tags.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   producePossibleValueSource = true,
   tableMetaDataCustomizer = Deal.TableMetaDataCustomizer.class,
   childTables = {
      @ChildTable(
         childTableEntityClass = DealContact.class,
         joinFieldName = "dealId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Contacts", enabled = true, maxRows = 50)),
      @ChildTable(
         childTableEntityClass = DealProduct.class,
         joinFieldName = "dealId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Products", enabled = true, maxRows = 50)),
      @ChildTable(
         childTableEntityClass = DealTag.class,
         joinFieldName = "dealId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Tags", enabled = true, maxRows = 50)),
      @ChildTable(
         childTableEntityClass = DealStageHistory.class,
         joinFieldName = "dealId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Stage History", enabled = true, maxRows = 50)),
      @ChildTable(
         childTableEntityClass = Activity.class,
         joinFieldName = "dealId",
         childJoin = @ChildJoin(enabled = true),
         childRecordListWidget = @ChildRecordListWidget(label = "Activities", enabled = true, maxRows = 50))
   }
)
public class Deal extends QRecordEntity
{
   public static final String TABLE_NAME = "crmDeal";



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
         QFieldSection t1Section = SectionFactory.defaultT1("id", "name", "pipelineId", "pipelineStageId", "amount", "currencyCode");

         table
            .withIcon(new QIcon().withName("handshake"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withSection(t1Section)
            .withSection(new QFieldSection("details", new QIcon().withName("info"), Tier.T2,
               List.of("companyId", "ownerUserId", "leadSourceId", "priority",
                  "probabilityOverridePct", "expectedCloseDate", "actualCloseDate",
                  "winLossReasonId", "weightedAmount", "amountInBaseCurrency")))
            .withSection(SectionFactory.defaultT2("description"))
            .withSection(SectionFactory.defaultT3("stageEnteredDate", "lastActivityDate", "createDate", "modifyDate"));

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

   @QField(isRequired = true, possibleValueSourceName = Pipeline.TABLE_NAME)
   private Integer pipelineId;

   @QField(isRequired = true, possibleValueSourceName = PipelineStage.TABLE_NAME)
   private Integer pipelineStageId;

   @QField()
   private BigDecimal amount;

   @QField(maxLength = 3, valueTooLongBehavior = ValueTooLongBehavior.ERROR, defaultValue = "USD")
   private String currencyCode;

   @QField(isEditable = false)
   private BigDecimal amountInBaseCurrency;

   @QField()
   private Integer probabilityOverridePct;

   @QField(isEditable = false)
   private BigDecimal weightedAmount;

   @QField(possibleValueSourceName = Company.TABLE_NAME)
   private Integer companyId;

   @QField(isRequired = true)
   private String ownerUserId;

   @QField(possibleValueSourceName = LeadSource.TABLE_NAME)
   private Integer leadSourceId;

   @QField()
   private LocalDate expectedCloseDate;

   @QField(isEditable = false)
   private LocalDate actualCloseDate;

   @QField(possibleValueSourceName = WinLossReason.TABLE_NAME)
   private Integer winLossReasonId;

   @QField(isEditable = false)
   private Instant stageEnteredDate;

   @QField()
   private String description;

   @QField(possibleValueSourceName = CrmDealPriority.NAME)
   private Integer priority;

   @QField(isEditable = false)
   private Instant lastActivityDate;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public Deal()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public Deal(QRecord record)
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
   public Deal withId(Integer id)
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
   public Deal withName(String name)
   {
      this.name = name;
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
   public Deal withPipelineId(Integer pipelineId)
   {
      this.pipelineId = pipelineId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pipelineStageId
    *******************************************************************************/
   public Integer getPipelineStageId()
   {
      return (this.pipelineStageId);
   }



   /*******************************************************************************
    ** Setter for pipelineStageId
    *******************************************************************************/
   public void setPipelineStageId(Integer pipelineStageId)
   {
      this.pipelineStageId = pipelineStageId;
   }



   /*******************************************************************************
    ** Fluent setter for pipelineStageId
    *******************************************************************************/
   public Deal withPipelineStageId(Integer pipelineStageId)
   {
      this.pipelineStageId = pipelineStageId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for amount
    *******************************************************************************/
   public BigDecimal getAmount()
   {
      return (this.amount);
   }



   /*******************************************************************************
    ** Setter for amount
    *******************************************************************************/
   public void setAmount(BigDecimal amount)
   {
      this.amount = amount;
   }



   /*******************************************************************************
    ** Fluent setter for amount
    *******************************************************************************/
   public Deal withAmount(BigDecimal amount)
   {
      this.amount = amount;
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
   public Deal withCurrencyCode(String currencyCode)
   {
      this.currencyCode = currencyCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for amountInBaseCurrency
    *******************************************************************************/
   public BigDecimal getAmountInBaseCurrency()
   {
      return (this.amountInBaseCurrency);
   }



   /*******************************************************************************
    ** Setter for amountInBaseCurrency
    *******************************************************************************/
   public void setAmountInBaseCurrency(BigDecimal amountInBaseCurrency)
   {
      this.amountInBaseCurrency = amountInBaseCurrency;
   }



   /*******************************************************************************
    ** Fluent setter for amountInBaseCurrency
    *******************************************************************************/
   public Deal withAmountInBaseCurrency(BigDecimal amountInBaseCurrency)
   {
      this.amountInBaseCurrency = amountInBaseCurrency;
      return (this);
   }



   /*******************************************************************************
    ** Getter for probabilityOverridePct
    *******************************************************************************/
   public Integer getProbabilityOverridePct()
   {
      return (this.probabilityOverridePct);
   }



   /*******************************************************************************
    ** Setter for probabilityOverridePct
    *******************************************************************************/
   public void setProbabilityOverridePct(Integer probabilityOverridePct)
   {
      this.probabilityOverridePct = probabilityOverridePct;
   }



   /*******************************************************************************
    ** Fluent setter for probabilityOverridePct
    *******************************************************************************/
   public Deal withProbabilityOverridePct(Integer probabilityOverridePct)
   {
      this.probabilityOverridePct = probabilityOverridePct;
      return (this);
   }



   /*******************************************************************************
    ** Getter for weightedAmount
    *******************************************************************************/
   public BigDecimal getWeightedAmount()
   {
      return (this.weightedAmount);
   }



   /*******************************************************************************
    ** Setter for weightedAmount
    *******************************************************************************/
   public void setWeightedAmount(BigDecimal weightedAmount)
   {
      this.weightedAmount = weightedAmount;
   }



   /*******************************************************************************
    ** Fluent setter for weightedAmount
    *******************************************************************************/
   public Deal withWeightedAmount(BigDecimal weightedAmount)
   {
      this.weightedAmount = weightedAmount;
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
   public Deal withCompanyId(Integer companyId)
   {
      this.companyId = companyId;
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
   public Deal withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
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
   public Deal withLeadSourceId(Integer leadSourceId)
   {
      this.leadSourceId = leadSourceId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expectedCloseDate
    *******************************************************************************/
   public LocalDate getExpectedCloseDate()
   {
      return (this.expectedCloseDate);
   }



   /*******************************************************************************
    ** Setter for expectedCloseDate
    *******************************************************************************/
   public void setExpectedCloseDate(LocalDate expectedCloseDate)
   {
      this.expectedCloseDate = expectedCloseDate;
   }



   /*******************************************************************************
    ** Fluent setter for expectedCloseDate
    *******************************************************************************/
   public Deal withExpectedCloseDate(LocalDate expectedCloseDate)
   {
      this.expectedCloseDate = expectedCloseDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for actualCloseDate
    *******************************************************************************/
   public LocalDate getActualCloseDate()
   {
      return (this.actualCloseDate);
   }



   /*******************************************************************************
    ** Setter for actualCloseDate
    *******************************************************************************/
   public void setActualCloseDate(LocalDate actualCloseDate)
   {
      this.actualCloseDate = actualCloseDate;
   }



   /*******************************************************************************
    ** Fluent setter for actualCloseDate
    *******************************************************************************/
   public Deal withActualCloseDate(LocalDate actualCloseDate)
   {
      this.actualCloseDate = actualCloseDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for winLossReasonId
    *******************************************************************************/
   public Integer getWinLossReasonId()
   {
      return (this.winLossReasonId);
   }



   /*******************************************************************************
    ** Setter for winLossReasonId
    *******************************************************************************/
   public void setWinLossReasonId(Integer winLossReasonId)
   {
      this.winLossReasonId = winLossReasonId;
   }



   /*******************************************************************************
    ** Fluent setter for winLossReasonId
    *******************************************************************************/
   public Deal withWinLossReasonId(Integer winLossReasonId)
   {
      this.winLossReasonId = winLossReasonId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stageEnteredDate
    *******************************************************************************/
   public Instant getStageEnteredDate()
   {
      return (this.stageEnteredDate);
   }



   /*******************************************************************************
    ** Setter for stageEnteredDate
    *******************************************************************************/
   public void setStageEnteredDate(Instant stageEnteredDate)
   {
      this.stageEnteredDate = stageEnteredDate;
   }



   /*******************************************************************************
    ** Fluent setter for stageEnteredDate
    *******************************************************************************/
   public Deal withStageEnteredDate(Instant stageEnteredDate)
   {
      this.stageEnteredDate = stageEnteredDate;
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
   public Deal withDescription(String description)
   {
      this.description = description;
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
   public Deal withPriority(Integer priority)
   {
      this.priority = priority;
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
   public Deal withLastActivityDate(Instant lastActivityDate)
   {
      this.lastActivityDate = lastActivityDate;
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
   public Deal withCreateDate(Instant createDate)
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
   public Deal withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
