/*******************************************************************************
 ** QRecord Entity for PipelineStage table
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.model;


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
 ** QRecord Entity for PipelineStage table -- a single stage within a pipeline,
 ** defining probability, rot days, required fields, and terminal state flags.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = PipelineStage.TableMetaDataCustomizer.class
)
public class PipelineStage extends QRecordEntity
{
   public static final String TABLE_NAME = "crmPipelineStage";



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
            .withIcon(new QIcon().withName("stairs"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withUniqueKey(new UniqueKey("pipelineId", "name"))
            .withUniqueKey(new UniqueKey("pipelineId", "sortOrder"))
            .withSection(SectionFactory.defaultT1("id", "pipelineId", "name", "sortOrder", "probabilityPct"))
            .withSection(SectionFactory.defaultT2("rotDays", "requiredFieldsJson", "isClosedWon", "isClosedLost"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Pipeline.TABLE_NAME)
   private Integer pipelineId;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(isRequired = true)
   private Integer sortOrder;

   @QField(isRequired = true, defaultValue = "0")
   private Integer probabilityPct;

   @QField()
   private Integer rotDays;

   @QField()
   private String requiredFieldsJson;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isClosedWon;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isClosedLost;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public PipelineStage()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public PipelineStage(QRecord record)
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
   public PipelineStage withId(Integer id)
   {
      this.id = id;
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
   public PipelineStage withPipelineId(Integer pipelineId)
   {
      this.pipelineId = pipelineId;
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
   public PipelineStage withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sortOrder
    *******************************************************************************/
   public Integer getSortOrder()
   {
      return (this.sortOrder);
   }



   /*******************************************************************************
    ** Setter for sortOrder
    *******************************************************************************/
   public void setSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
   }



   /*******************************************************************************
    ** Fluent setter for sortOrder
    *******************************************************************************/
   public PipelineStage withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
      return (this);
   }



   /*******************************************************************************
    ** Getter for probabilityPct
    *******************************************************************************/
   public Integer getProbabilityPct()
   {
      return (this.probabilityPct);
   }



   /*******************************************************************************
    ** Setter for probabilityPct
    *******************************************************************************/
   public void setProbabilityPct(Integer probabilityPct)
   {
      this.probabilityPct = probabilityPct;
   }



   /*******************************************************************************
    ** Fluent setter for probabilityPct
    *******************************************************************************/
   public PipelineStage withProbabilityPct(Integer probabilityPct)
   {
      this.probabilityPct = probabilityPct;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rotDays
    *******************************************************************************/
   public Integer getRotDays()
   {
      return (this.rotDays);
   }



   /*******************************************************************************
    ** Setter for rotDays
    *******************************************************************************/
   public void setRotDays(Integer rotDays)
   {
      this.rotDays = rotDays;
   }



   /*******************************************************************************
    ** Fluent setter for rotDays
    *******************************************************************************/
   public PipelineStage withRotDays(Integer rotDays)
   {
      this.rotDays = rotDays;
      return (this);
   }



   /*******************************************************************************
    ** Getter for requiredFieldsJson
    *******************************************************************************/
   public String getRequiredFieldsJson()
   {
      return (this.requiredFieldsJson);
   }



   /*******************************************************************************
    ** Setter for requiredFieldsJson
    *******************************************************************************/
   public void setRequiredFieldsJson(String requiredFieldsJson)
   {
      this.requiredFieldsJson = requiredFieldsJson;
   }



   /*******************************************************************************
    ** Fluent setter for requiredFieldsJson
    *******************************************************************************/
   public PipelineStage withRequiredFieldsJson(String requiredFieldsJson)
   {
      this.requiredFieldsJson = requiredFieldsJson;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isClosedWon
    *******************************************************************************/
   public Boolean getIsClosedWon()
   {
      return (this.isClosedWon);
   }



   /*******************************************************************************
    ** Setter for isClosedWon
    *******************************************************************************/
   public void setIsClosedWon(Boolean isClosedWon)
   {
      this.isClosedWon = isClosedWon;
   }



   /*******************************************************************************
    ** Fluent setter for isClosedWon
    *******************************************************************************/
   public PipelineStage withIsClosedWon(Boolean isClosedWon)
   {
      this.isClosedWon = isClosedWon;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isClosedLost
    *******************************************************************************/
   public Boolean getIsClosedLost()
   {
      return (this.isClosedLost);
   }



   /*******************************************************************************
    ** Setter for isClosedLost
    *******************************************************************************/
   public void setIsClosedLost(Boolean isClosedLost)
   {
      this.isClosedLost = isClosedLost;
   }



   /*******************************************************************************
    ** Fluent setter for isClosedLost
    *******************************************************************************/
   public PipelineStage withIsClosedLost(Boolean isClosedLost)
   {
      this.isClosedLost = isClosedLost;
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
   public PipelineStage withCreateDate(Instant createDate)
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
   public PipelineStage withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
