/*******************************************************************************
 ** QRecord Entity for CompanyTag table
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


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
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** QRecord Entity for CompanyTag junction table -- many-to-many link between
 ** companies and tags.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = CompanyTag.TableMetaDataCustomizer.class
)
public class CompanyTag extends QRecordEntity
{
   public static final String TABLE_NAME = "crmCompanyTag";



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
            .withIcon(new QIcon().withName("label"))
            .withRecordLabelFormat("Company %s - Tag %s")
            .withRecordLabelFields("companyId", "tagId")
            .withUniqueKey(new UniqueKey("companyId", "tagId"))
            .withSection(SectionFactory.defaultT1("id", "companyId", "tagId"))
            .withSection(SectionFactory.defaultT3("createDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Company.TABLE_NAME)
   private Integer companyId;

   @QField(isRequired = true, possibleValueSourceName = Tag.TABLE_NAME)
   private Integer tagId;

   @QField(isEditable = false)
   private Instant createDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public CompanyTag()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public CompanyTag(QRecord record)
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
   public CompanyTag withId(Integer id)
   {
      this.id = id;
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
   public CompanyTag withCompanyId(Integer companyId)
   {
      this.companyId = companyId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tagId
    *******************************************************************************/
   public Integer getTagId()
   {
      return (this.tagId);
   }



   /*******************************************************************************
    ** Setter for tagId
    *******************************************************************************/
   public void setTagId(Integer tagId)
   {
      this.tagId = tagId;
   }



   /*******************************************************************************
    ** Fluent setter for tagId
    *******************************************************************************/
   public CompanyTag withTagId(Integer tagId)
   {
      this.tagId = tagId;
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
   public CompanyTag withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
