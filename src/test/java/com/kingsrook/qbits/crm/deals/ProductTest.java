/*******************************************************************************
 ** Unit tests for the Product entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Product;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for Product CRUD, PVS registration, unique SKU constraint, and
 ** BigDecimal field round-trip.
 *******************************************************************************/
class ProductTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table and PVS are registered.
    *******************************************************************************/
   @Test
   void testTableAndPvsRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Product.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("inventory_2");

      assertNotNull(QContext.getQInstance().getPossibleValueSource(Product.TABLE_NAME));
   }



   /*******************************************************************************
    ** Insert a product and verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecordEntity(new Product()
               .withName("Enterprise License")
               .withSku("ENT-001")
               .withDescription("Annual enterprise software license")
               .withUnitPrice(new BigDecimal("9999.99"))
               .withCurrencyCode("USD")
               .withIsActive(true)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Product.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      Product fetched = new Product(queryOutput.getRecords().get(0));
      assertNotNull(fetched.getId());
      assertEquals("Enterprise License", fetched.getName());
      assertEquals("ENT-001", fetched.getSku());
      assertEquals("Annual enterprise software license", fetched.getDescription());
      assertThat(fetched.getUnitPrice()).isEqualByComparingTo(new BigDecimal("9999.99"));
      assertEquals("USD", fetched.getCurrencyCode());
      assertTrue(fetched.getIsActive());
   }



   /*******************************************************************************
    ** Verify the unique key on sku rejects duplicate non-null values.
    *******************************************************************************/
   @Test
   void testUniqueSkuConstraint() throws QException
   {
      ///////////////////////////////////////////
      // insert first product with SKU         //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecordEntity(new Product()
               .withName("Product A")
               .withSku("SKU-DUPE")
               .withUnitPrice(new BigDecimal("100.00"))
               .withIsActive(true)));

      ///////////////////////////////////////////////////
      // insert second product with same SKU -- expect  //
      // error on the record from unique key violation  //
      ///////////////////////////////////////////////////
      InsertOutput dupeOutput = new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecordEntity(new Product()
               .withName("Product B")
               .withSku("SKU-DUPE")
               .withUnitPrice(new BigDecimal("200.00"))
               .withIsActive(true)));

      QRecord dupeRecord = dupeOutput.getRecords().get(0);
      assertThat(dupeRecord.getErrors()).isNotEmpty();
   }



   /*******************************************************************************
    ** Insert multiple products and verify query returns all.
    *******************************************************************************/
   @Test
   void testInsertMultiple() throws QException
   {
      new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecords(List.of(
               new Product()
                  .withName("Basic Plan")
                  .withSku("BASIC-001")
                  .withUnitPrice(new BigDecimal("29.99"))
                  .withIsActive(true)
                  .toQRecord(),
               new Product()
                  .withName("Pro Plan")
                  .withSku("PRO-001")
                  .withUnitPrice(new BigDecimal("99.99"))
                  .withIsActive(true)
                  .toQRecord(),
               new Product()
                  .withName("Custom Integration")
                  .withUnitPrice(new BigDecimal("5000.00"))
                  .withCurrencyCode("EUR")
                  .withIsActive(false)
                  .toQRecord()
            )));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Product.TABLE_NAME));

      assertEquals(3, queryOutput.getRecords().size());
   }

}
