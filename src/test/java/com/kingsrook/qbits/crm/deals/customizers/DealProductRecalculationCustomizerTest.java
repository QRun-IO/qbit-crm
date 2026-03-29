/*******************************************************************************
 ** Unit tests for DealProductRecalculationCustomizer -- verifies that
 ** inserting, updating, and deleting DealProduct rows correctly recalculates
 ** totalAmount on the line item and rolls up to the parent Deal's amount.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.deals.model.Product;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for DealProductRecalculationCustomizer: verifies totalAmount
 ** calculation on line items and rollup to parent deal amount.
 *******************************************************************************/
class DealProductRecalculationCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Insert a DealProduct with quantity=2, unitPrice=100, discountPct=10.
    ** Get it back. Verify totalAmount=180.
    *******************************************************************************/
   @Test
   void testInsertProductCalculatesTotalAmount() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Open", 1, 10);
      Integer dealId = insertDeal("Product Deal", pipelineId, stageId, new BigDecimal("0"));
      Integer productId = insertProduct("Widget");

      Integer dpId = insertDealProduct(dealId, productId, new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("10"));

      DealProduct dp = new DealProduct(GetAction.execute(DealProduct.TABLE_NAME, dpId));
      assertNotNull(dp.getTotalAmount());

      //////////////////////////////////////////////////////////////////
      // totalAmount = 2 * 100 * (1 - 10/100) = 200 * 0.9 = 180.00  //
      //////////////////////////////////////////////////////////////////
      assertEquals(0, new BigDecimal("180.00").compareTo(dp.getTotalAmount()));
   }



   /*******************************************************************************
    ** Insert a deal, then insert 2 DealProduct rows. Get the deal back. Verify
    ** deal.amount equals the sum of line item totalAmounts.
    *******************************************************************************/
   @Test
   void testInsertProductUpdatesDealAmount() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Open", 1, 10);
      Integer dealId = insertDeal("Sum Deal", pipelineId, stageId, new BigDecimal("0"));
      Integer productId1 = insertProduct("Widget A");
      Integer productId2 = insertProduct("Widget B");

      ///////////////////////////////////////////
      // product 1: 3 * 50 * (1-0) = 150      //
      ///////////////////////////////////////////
      insertDealProduct(dealId, productId1, new BigDecimal("3"), new BigDecimal("50"), new BigDecimal("0"));

      ///////////////////////////////////////////
      // product 2: 1 * 200 * (1-0.25) = 150  //
      ///////////////////////////////////////////
      insertDealProduct(dealId, productId2, new BigDecimal("1"), new BigDecimal("200"), new BigDecimal("25"));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));

      //////////////////////////////////////////////////////////////////
      // deal.amount = 150.00 + 150.00 = 300.00                      //
      //////////////////////////////////////////////////////////////////
      assertEquals(0, new BigDecimal("300.00").compareTo(deal.getAmount()));
   }



   /*******************************************************************************
    ** Insert a DealProduct, then update its quantity. Verify totalAmount and
    ** deal.amount both recalculate.
    *******************************************************************************/
   @Test
   void testUpdateProductRecalculates() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Open", 1, 10);
      Integer dealId = insertDeal("Update Deal", pipelineId, stageId, new BigDecimal("0"));
      Integer productId = insertProduct("Gadget");

      ///////////////////////////////////////////
      // initial: 2 * 100 * (1-0) = 200       //
      ///////////////////////////////////////////
      Integer dpId = insertDealProduct(dealId, productId, new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("0"));

      ///////////////////////////////////////////
      // update quantity to 5                  //
      ///////////////////////////////////////////
      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dpId);
      updateRecord.setValue("quantity", new BigDecimal("5"));
      updateRecord.setValue("unitPrice", new BigDecimal("100"));
      updateRecord.setValue("discountPct", new BigDecimal("0"));
      updateRecord.setValue("dealId", dealId);
      new UpdateAction().execute(new UpdateInput(DealProduct.TABLE_NAME).withRecord(updateRecord));

      DealProduct dp = new DealProduct(GetAction.execute(DealProduct.TABLE_NAME, dpId));
      assertEquals(0, new BigDecimal("500.00").compareTo(dp.getTotalAmount()));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(0, new BigDecimal("500.00").compareTo(deal.getAmount()));
   }



   /*******************************************************************************
    ** Insert 2 DealProducts, delete one. Verify deal.amount equals the remaining
    ** line item's totalAmount.
    *******************************************************************************/
   @Test
   void testDeleteProductRecalculatesDealAmount() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Open", 1, 10);
      Integer dealId = insertDeal("Delete Deal", pipelineId, stageId, new BigDecimal("0"));
      Integer productId1 = insertProduct("Alpha");
      Integer productId2 = insertProduct("Beta");

      ///////////////////////////////////////////
      // product 1: 1 * 300 * (1-0) = 300     //
      ///////////////////////////////////////////
      Integer dp1Id = insertDealProduct(dealId, productId1, new BigDecimal("1"), new BigDecimal("300"), new BigDecimal("0"));

      ///////////////////////////////////////////
      // product 2: 2 * 100 * (1-0) = 200     //
      ///////////////////////////////////////////
      Integer dp2Id = insertDealProduct(dealId, productId2, new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("0"));

      ///////////////////////////////////////////
      // verify deal.amount = 500 before delete//
      ///////////////////////////////////////////
      Deal dealBefore = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(0, new BigDecimal("500.00").compareTo(dealBefore.getAmount()));

      ///////////////////////////////////////////
      // delete product 1                      //
      ///////////////////////////////////////////
      DeleteInput deleteInput = new DeleteInput(DealProduct.TABLE_NAME);
      deleteInput.setPrimaryKeys(List.of(dp1Id));
      new DeleteAction().execute(deleteInput);

      Deal dealAfter = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(0, new BigDecimal("200.00").compareTo(dealAfter.getAmount()));
   }



   /////////////////////////////////////////////////////////////////////////////
   // Helper methods                                                          //
   /////////////////////////////////////////////////////////////////////////////

   /***************************************************************************
    ** Helper: insert a Pipeline and return its id.
    ***************************************************************************/
   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName(name)
               .withIsDefault(true)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage and return its id.
    ***************************************************************************/
   private Integer insertStage(Integer pipelineId, String name, Integer sortOrder, Integer probabilityPct) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(false)
               .withIsClosedLost(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Deal and return its id.
    ***************************************************************************/
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId, BigDecimal amount) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Product and return its id.
    ***************************************************************************/
   private Integer insertProduct(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecordEntity(new Product()
               .withName(name)
               .withUnitPrice(new BigDecimal("0"))
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a DealProduct and return its id.
    ***************************************************************************/
   private Integer insertDealProduct(Integer dealId, Integer productId, BigDecimal quantity,
                                     BigDecimal unitPrice, BigDecimal discountPct) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(dealId)
               .withProductId(productId)
               .withQuantity(quantity)
               .withUnitPrice(unitPrice)
               .withDiscountPct(discountPct)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
