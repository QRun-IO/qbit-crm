/*******************************************************************************
 ** Unit tests for CloneDealProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.deals.model.DealTag;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for cloning a deal with its contacts, products, and tags.
 *******************************************************************************/
class CloneDealProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Clone a deal and verify the copy has correct name, stage, and child records.
    *******************************************************************************/
   @Test
   void testCloneDeal() throws QException
   {
      ///////////////////////////////////////
      // set up pipeline and stages        //
      ///////////////////////////////////////
      Integer pipelineId   = insertPipeline("Sales");
      Integer stage1Id     = insertPipelineStage(pipelineId, "Prospecting", 1);
      Integer stage2Id     = insertPipelineStage(pipelineId, "Negotiation", 2);

      ///////////////////////////////////////
      // create a deal at stage 2          //
      ///////////////////////////////////////
      Integer dealId = insertDeal("Big Deal", pipelineId, stage2Id, new BigDecimal("10000"));

      ///////////////////////////////////////
      // add child records                 //
      ///////////////////////////////////////
      insertDealContact(dealId, 42);
      insertDealProduct(dealId, 100, 2, new BigDecimal("5000"));
      insertDealTag(dealId, 10);

      ////////////////////////////////////
      // run the CloneDeal process      //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(CloneDealProcess.NAME);
      processInput.addValue("dealId", dealId);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);
      Integer newDealId = (Integer) processOutput.getValues().get("dealId");

      /////////////////////////////////////////////
      // verify the new deal                     //
      /////////////////////////////////////////////
      assertNotNull(newDealId);
      assertNotEquals(dealId, newDealId);

      Deal newDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, newDealId));
      assertEquals("Big Deal (Copy)", newDeal.getName());
      assertEquals(stage1Id, newDeal.getPipelineStageId()); // reset to first stage
      assertEquals(pipelineId, newDeal.getPipelineId());
      assertEquals(0, new BigDecimal("10000").compareTo(newDeal.getAmount()));

      /////////////////////////////////////////////
      // verify child records were cloned        //
      /////////////////////////////////////////////
      QueryOutput dealContacts = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, newDealId))));
      assertEquals(1, dealContacts.getRecords().size());

      QueryOutput dealProducts = new QueryAction().execute(
         new QueryInput(DealProduct.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, newDealId))));
      assertEquals(1, dealProducts.getRecords().size());

      QueryOutput dealTags = new QueryAction().execute(
         new QueryInput(DealTag.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, newDealId))));
      assertEquals(1, dealTags.getRecords().size());
   }



   /***************************************************************************
    ** Helper: insert a Pipeline.
    ***************************************************************************/
   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName(name).withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage.
    ***************************************************************************/
   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(50)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Deal.
    ***************************************************************************/
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId, BigDecimal amount) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a DealContact.
    ***************************************************************************/
   private void insertDealContact(Integer dealId, Integer contactId) throws QException
   {
      new InsertAction().execute(
         new InsertInput(DealContact.TABLE_NAME).withRecord(
            new QRecord()
               .withValue("dealId", dealId)
               .withValue("contactId", contactId)));
   }



   /***************************************************************************
    ** Helper: insert a DealProduct.
    ***************************************************************************/
   private void insertDealProduct(Integer dealId, Integer productId, int quantity, BigDecimal unitPrice) throws QException
   {
      new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME).withRecord(
            new QRecord()
               .withValue("dealId", dealId)
               .withValue("productId", productId)
               .withValue("quantity", quantity)
               .withValue("unitPrice", unitPrice)));
   }



   /***************************************************************************
    ** Helper: insert a DealTag.
    ***************************************************************************/
   private void insertDealTag(Integer dealId, Integer tagId) throws QException
   {
      new InsertAction().execute(
         new InsertInput(DealTag.TABLE_NAME).withRecord(
            new QRecord()
               .withValue("dealId", dealId)
               .withValue("tagId", tagId)));
   }

}
