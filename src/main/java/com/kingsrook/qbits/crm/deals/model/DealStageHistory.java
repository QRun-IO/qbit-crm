/*******************************************************************************
 ** QRecord Entity for DealStageHistory table
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.model;


import java.time.Instant;
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
 ** QRecord Entity for DealStageHistory table -- append-only log of deal stage
 ** transitions for pipeline analytics and conversion rate reporting.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = DealStageHistory.TableMetaDataCustomizer.class
)
public class DealStageHistory extends QRecordEntity
{
   public static final String TABLE_NAME = "crmDealStageHistory";



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
            .withIcon(new QIcon().withName("history"))
            .withRecordLabelFormat("Deal %s Stage Transition")
            .withRecordLabelFields("dealId")
            .withSection(SectionFactory.defaultT1("id", "dealId", "fromStageId", "toStageId", "userId", "transitionDate", "durationInFromStageDays"))
            .withSection(SectionFactory.defaultT3("createDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Deal.TABLE_NAME)
   private Integer dealId;

   @QField(possibleValueSourceName = PipelineStage.TABLE_NAME)
   private Integer fromStageId;

   @QField(isRequired = true, possibleValueSourceName = PipelineStage.TABLE_NAME)
   private Integer toStageId;

   @QField(isRequired = true)
   private String userId;

   @QField(isRequired = true)
   private Instant transitionDate;

   @QField()
   private Integer durationInFromStageDays;

   @QField(isEditable = false)
   private Instant createDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public DealStageHistory()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public DealStageHistory(QRecord record)
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
   public DealStageHistory withId(Integer id)
   {
      this.id = id;
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
   public DealStageHistory withDealId(Integer dealId)
   {
      this.dealId = dealId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fromStageId
    *******************************************************************************/
   public Integer getFromStageId()
   {
      return (this.fromStageId);
   }



   /*******************************************************************************
    ** Setter for fromStageId
    *******************************************************************************/
   public void setFromStageId(Integer fromStageId)
   {
      this.fromStageId = fromStageId;
   }



   /*******************************************************************************
    ** Fluent setter for fromStageId
    *******************************************************************************/
   public DealStageHistory withFromStageId(Integer fromStageId)
   {
      this.fromStageId = fromStageId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for toStageId
    *******************************************************************************/
   public Integer getToStageId()
   {
      return (this.toStageId);
   }



   /*******************************************************************************
    ** Setter for toStageId
    *******************************************************************************/
   public void setToStageId(Integer toStageId)
   {
      this.toStageId = toStageId;
   }



   /*******************************************************************************
    ** Fluent setter for toStageId
    *******************************************************************************/
   public DealStageHistory withToStageId(Integer toStageId)
   {
      this.toStageId = toStageId;
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
   public DealStageHistory withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for transitionDate
    *******************************************************************************/
   public Instant getTransitionDate()
   {
      return (this.transitionDate);
   }



   /*******************************************************************************
    ** Setter for transitionDate
    *******************************************************************************/
   public void setTransitionDate(Instant transitionDate)
   {
      this.transitionDate = transitionDate;
   }



   /*******************************************************************************
    ** Fluent setter for transitionDate
    *******************************************************************************/
   public DealStageHistory withTransitionDate(Instant transitionDate)
   {
      this.transitionDate = transitionDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for durationInFromStageDays
    *******************************************************************************/
   public Integer getDurationInFromStageDays()
   {
      return (this.durationInFromStageDays);
   }



   /*******************************************************************************
    ** Setter for durationInFromStageDays
    *******************************************************************************/
   public void setDurationInFromStageDays(Integer durationInFromStageDays)
   {
      this.durationInFromStageDays = durationInFromStageDays;
   }



   /*******************************************************************************
    ** Fluent setter for durationInFromStageDays
    *******************************************************************************/
   public DealStageHistory withDurationInFromStageDays(Integer durationInFromStageDays)
   {
      this.durationInFromStageDays = durationInFromStageDays;
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
   public DealStageHistory withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
