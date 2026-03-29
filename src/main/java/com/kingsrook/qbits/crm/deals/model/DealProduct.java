/*******************************************************************************
 ** QRecord Entity for the crm_deal_product table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** Deal line-item entity. Links a product to a deal with quantity, price, and
 ** discount. Duplicate (dealId, productId) is intentional -- the same product
 ** can appear as multiple line items with different pricing.
 *******************************************************************************/
@QMetaDataProducingEntity(
   produceTableMetaData = true,
   tableMetaDataCustomizer = DealProduct.TableMetaDataCustomizer.class
)
public class DealProduct extends QRecordEntity
{
   public static final String TABLE_NAME = "crmDealProduct";



   /***************************************************************************
    ** Customizer that sets icon and section layout.
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
            .withIcon(new QIcon().withName("receipt_long"))
            .withSection(SectionFactory.defaultT1("id", "dealId", "productId"))
            .withSection(new QFieldSection("pricing", new QIcon("payments"), Tier.T2,
               List.of("quantity", "unitPrice", "discountPct", "totalAmount")))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = Deal.TABLE_NAME)
   private Integer dealId;

   @QField(isRequired = true, possibleValueSourceName = Product.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, defaultValue = "1")
   private BigDecimal quantity;

   @QField(isRequired = true)
   private BigDecimal unitPrice;

   @QField(defaultValue = "0")
   private BigDecimal discountPct;

   @QField(isEditable = false)
   private BigDecimal totalAmount;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public DealProduct()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public DealProduct(QRecord record)
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
   public DealProduct withId(Integer id)
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
   public DealProduct withDealId(Integer dealId)
   {
      this.dealId = dealId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for productId
    *******************************************************************************/
   public Integer getProductId()
   {
      return (this.productId);
   }



   /*******************************************************************************
    ** Setter for productId
    *******************************************************************************/
   public void setProductId(Integer productId)
   {
      this.productId = productId;
   }



   /*******************************************************************************
    ** Fluent setter for productId
    *******************************************************************************/
   public DealProduct withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for quantity
    *******************************************************************************/
   public BigDecimal getQuantity()
   {
      return (this.quantity);
   }



   /*******************************************************************************
    ** Setter for quantity
    *******************************************************************************/
   public void setQuantity(BigDecimal quantity)
   {
      this.quantity = quantity;
   }



   /*******************************************************************************
    ** Fluent setter for quantity
    *******************************************************************************/
   public DealProduct withQuantity(BigDecimal quantity)
   {
      this.quantity = quantity;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unitPrice
    *******************************************************************************/
   public BigDecimal getUnitPrice()
   {
      return (this.unitPrice);
   }



   /*******************************************************************************
    ** Setter for unitPrice
    *******************************************************************************/
   public void setUnitPrice(BigDecimal unitPrice)
   {
      this.unitPrice = unitPrice;
   }



   /*******************************************************************************
    ** Fluent setter for unitPrice
    *******************************************************************************/
   public DealProduct withUnitPrice(BigDecimal unitPrice)
   {
      this.unitPrice = unitPrice;
      return (this);
   }



   /*******************************************************************************
    ** Getter for discountPct
    *******************************************************************************/
   public BigDecimal getDiscountPct()
   {
      return (this.discountPct);
   }



   /*******************************************************************************
    ** Setter for discountPct
    *******************************************************************************/
   public void setDiscountPct(BigDecimal discountPct)
   {
      this.discountPct = discountPct;
   }



   /*******************************************************************************
    ** Fluent setter for discountPct
    *******************************************************************************/
   public DealProduct withDiscountPct(BigDecimal discountPct)
   {
      this.discountPct = discountPct;
      return (this);
   }



   /*******************************************************************************
    ** Getter for totalAmount
    *******************************************************************************/
   public BigDecimal getTotalAmount()
   {
      return (this.totalAmount);
   }



   /*******************************************************************************
    ** Setter for totalAmount
    *******************************************************************************/
   public void setTotalAmount(BigDecimal totalAmount)
   {
      this.totalAmount = totalAmount;
   }



   /*******************************************************************************
    ** Fluent setter for totalAmount
    *******************************************************************************/
   public DealProduct withTotalAmount(BigDecimal totalAmount)
   {
      this.totalAmount = totalAmount;
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
   public DealProduct withCreateDate(Instant createDate)
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
   public DealProduct withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
