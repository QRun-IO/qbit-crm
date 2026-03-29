/*******************************************************************************
 ** QRecord Entity for DealContact table
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.core.model.Contact;
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
 ** QRecord Entity for DealContact junction table -- many-to-many link between
 ** deals and contacts, with an optional contact role and primary flag.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = DealContact.TableMetaDataCustomizer.class
)
public class DealContact extends QRecordEntity
{
   public static final String TABLE_NAME = "crmDealContact";



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
            .withIcon(new QIcon().withName("people"))
            .withRecordLabelFormat("Deal %s - Contact %s")
            .withRecordLabelFields("dealId", "contactId")
            .withUniqueKey(new UniqueKey("dealId", "contactId"))
            .withSection(SectionFactory.defaultT1("id", "dealId", "contactId", "contactRoleId", "isPrimary"))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Deal.TABLE_NAME)
   private Integer dealId;

   @QField(isRequired = true, possibleValueSourceName = Contact.TABLE_NAME)
   private Integer contactId;

   @QField(possibleValueSourceName = ContactRole.TABLE_NAME)
   private Integer contactRoleId;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isPrimary;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public DealContact()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public DealContact(QRecord record)
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
   public DealContact withId(Integer id)
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
   public DealContact withDealId(Integer dealId)
   {
      this.dealId = dealId;
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
   public DealContact withContactId(Integer contactId)
   {
      this.contactId = contactId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contactRoleId
    *******************************************************************************/
   public Integer getContactRoleId()
   {
      return (this.contactRoleId);
   }



   /*******************************************************************************
    ** Setter for contactRoleId
    *******************************************************************************/
   public void setContactRoleId(Integer contactRoleId)
   {
      this.contactRoleId = contactRoleId;
   }



   /*******************************************************************************
    ** Fluent setter for contactRoleId
    *******************************************************************************/
   public DealContact withContactRoleId(Integer contactRoleId)
   {
      this.contactRoleId = contactRoleId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isPrimary
    *******************************************************************************/
   public Boolean getIsPrimary()
   {
      return (this.isPrimary);
   }



   /*******************************************************************************
    ** Setter for isPrimary
    *******************************************************************************/
   public void setIsPrimary(Boolean isPrimary)
   {
      this.isPrimary = isPrimary;
   }



   /*******************************************************************************
    ** Fluent setter for isPrimary
    *******************************************************************************/
   public DealContact withIsPrimary(Boolean isPrimary)
   {
      this.isPrimary = isPrimary;
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
   public DealContact withCreateDate(Instant createDate)
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
   public DealContact withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
