/*******************************************************************************
 ** Unit tests for the DealProduct entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.deals.model.Product;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for DealProduct line items -- BigDecimal field round-trip, multiple
 ** line items per deal, and duplicate (dealId, productId) support.
 *******************************************************************************/
class DealProductTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with the correct icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(DealProduct.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("receipt_long");
   }



   /*******************************************************************************
    ** Insert a line item and verify BigDecimal fields round-trip correctly.
    *******************************************************************************/
   @Test
   void testInsertAndQueryBigDecimalFields() throws QException
   {
      Integer productId = insertProduct("Widget", "WDG-001", new BigDecimal("49.99"));

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(1)
               .withProductId(productId)
               .withQuantity(new BigDecimal("10"))
               .withUnitPrice(new BigDecimal("49.99"))
               .withDiscountPct(new BigDecimal("15"))));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealProduct.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      DealProduct fetched = new DealProduct(queryOutput.getRecords().get(0));
      assertNotNull(fetched.getId());
      assertEquals(1, fetched.getDealId());
      assertEquals(productId, fetched.getProductId());
      assertThat(fetched.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
      assertThat(fetched.getUnitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(fetched.getDiscountPct()).isEqualByComparingTo(new BigDecimal("15"));
   }



   /*******************************************************************************
    ** Verify multiple line items can be inserted for the same deal, including
    ** duplicate (dealId, productId) combinations.
    *******************************************************************************/
   @Test
   void testMultipleLineItemsSameDeal() throws QException
   {
      Integer widgetId = insertProduct("Widget", "WDG-002", new BigDecimal("25.00"));
      Integer serviceId = insertProduct("Service", "SVC-001", new BigDecimal("150.00"));

      ///////////////////////////////////////////
      // insert three line items for deal 100  //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(100)
               .withProductId(widgetId)
               .withQuantity(new BigDecimal("5"))
               .withUnitPrice(new BigDecimal("25.00"))
               .withDiscountPct(new BigDecimal("0"))));

      //////////////////////////////////////////////////////////////////
      // same product, different pricing -- duplicate is intentional   //
      //////////////////////////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(100)
               .withProductId(widgetId)
               .withQuantity(new BigDecimal("3"))
               .withUnitPrice(new BigDecimal("20.00"))
               .withDiscountPct(new BigDecimal("10"))));

      new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(100)
               .withProductId(serviceId)
               .withQuantity(new BigDecimal("1"))
               .withUnitPrice(new BigDecimal("150.00"))
               .withDiscountPct(new BigDecimal("0"))));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealProduct.TABLE_NAME));

      assertEquals(3, queryOutput.getRecords().size());

      //////////////////////////////////////////////////////////
      // verify both widget line items have different pricing  //
      //////////////////////////////////////////////////////////
      long widgetLines = queryOutput.getRecords().stream()
         .filter(r -> widgetId.equals(r.getValueInteger("productId")))
         .count();
      assertEquals(2, widgetLines);
   }



   /*******************************************************************************
    ** Verify a line item with zero discount.
    *******************************************************************************/
   @Test
   void testZeroDiscount() throws QException
   {
      Integer productId = insertProduct("No Discount Item", "ND-001", new BigDecimal("500.00"));

      new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(200)
               .withProductId(productId)
               .withQuantity(new BigDecimal("2"))
               .withUnitPrice(new BigDecimal("500.00"))
               .withDiscountPct(new BigDecimal("0"))));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealProduct.TABLE_NAME));

      DealProduct fetched = new DealProduct(queryOutput.getRecords().get(0));
      assertThat(fetched.getDiscountPct()).isEqualByComparingTo(BigDecimal.ZERO);
   }



   /***************************************************************************
    ** Helper: insert a Product and return its id.
    ***************************************************************************/
   private Integer insertProduct(String name, String sku, BigDecimal unitPrice) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecordEntity(new Product()
               .withName(name)
               .withSku(sku)
               .withUnitPrice(unitPrice)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
