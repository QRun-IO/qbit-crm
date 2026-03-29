/*******************************************************************************
 ** Unit tests for ConvertLeadProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.LifecycleStage;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for lead conversion: contact is created, lifecycle stage is updated
 ** to Opportunity, a deal is created with the first pipeline stage, and a
 ** DealContact junction is created with isPrimary=true.
 *******************************************************************************/
class ConvertLeadProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Convert a contact to a deal and verify deal + junction created.
    *******************************************************************************/
   @Test
   void testConvertLeadCreatesDeal() throws QException
   {
      ///////////////////////////////////////////
      // set up reference data                 //
      ///////////////////////////////////////////
      insertLifecycleStage("Opportunity");
      Integer pipelineId = insertPipeline("Default Pipeline", true);
      Integer firstStage = insertStage(pipelineId, "New", 1, 10);

      ///////////////////////////////////////////
      // create a contact (lead)               //
      ///////////////////////////////////////////
      Integer contactId = insertContact("Jane", "Lead", "jane@example.com", 100);

      ///////////////////////////////////////////
      // run the conversion process            //
      ///////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ConvertLeadProcess.NAME);
      processInput.addValue("contactId", contactId);
      processInput.addValue("dealName", "Jane's Opportunity");
      processInput.addValue("dealAmount", new BigDecimal("25000"));

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      Integer dealId = processOutput.getValueInteger("dealId");
      assertNotNull(dealId);

      ///////////////////////////////////////////
      // verify deal was created               //
      ///////////////////////////////////////////
      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals("Jane's Opportunity", deal.getName());
      assertEquals(pipelineId, deal.getPipelineId());
      assertEquals(firstStage, deal.getPipelineStageId());
      assertEquals(0, new BigDecimal("25000").compareTo(deal.getAmount()));
      assertEquals(100, deal.getCompanyId());
      assertNotNull(deal.getStageEnteredDate());

      ///////////////////////////////////////////
      // verify DealContact junction created   //
      ///////////////////////////////////////////
      QueryOutput junctionOutput = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

      assertEquals(1, junctionOutput.getRecords().size());
      DealContact dc = new DealContact(junctionOutput.getRecords().get(0));
      assertEquals(contactId, dc.getContactId());
      assertTrue(dc.getIsPrimary());

      ///////////////////////////////////////////
      // verify contact lifecycle updated      //
      ///////////////////////////////////////////
      Contact updatedContact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertNotNull(updatedContact.getLifecycleStageId());
   }



   /*******************************************************************************
    ** Convert a lead with an explicit pipelineId instead of using default.
    *******************************************************************************/
   @Test
   void testConvertLeadWithExplicitPipeline() throws QException
   {
      insertLifecycleStage("Opportunity");

      Integer defaultPipelineId = insertPipeline("Default Pipeline", true);
      insertStage(defaultPipelineId, "Default Stage", 1, 10);

      Integer customPipelineId = insertPipeline("Custom Pipeline", false);
      Integer customStage      = insertStage(customPipelineId, "Custom First", 1, 20);

      Integer contactId = insertContact("Bob", "Lead", "bob@example.com", null);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ConvertLeadProcess.NAME);
      processInput.addValue("contactId", contactId);
      processInput.addValue("pipelineId", customPipelineId);
      processInput.addValue("dealName", "Bob's Custom Deal");

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      Integer dealId = processOutput.getValueInteger("dealId");
      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(customPipelineId, deal.getPipelineId());
      assertEquals(customStage, deal.getPipelineStageId());
   }



   /*******************************************************************************
    ** Convert a lead with no amount to verify nullable amounts work.
    *******************************************************************************/
   @Test
   void testConvertLeadNoAmount() throws QException
   {
      insertLifecycleStage("Opportunity");
      Integer pipelineId = insertPipeline("Pipeline", true);
      insertStage(pipelineId, "First", 1, 10);

      Integer contactId = insertContact("Alice", "NoAmount", "alice@example.com", null);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ConvertLeadProcess.NAME);
      processInput.addValue("contactId", contactId);
      processInput.addValue("dealName", "Alice Deal");

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      Integer dealId = processOutput.getValueInteger("dealId");
      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals("Alice Deal", deal.getName());
   }



   /***************************************************************************
    ** Helper: insert a Pipeline and return its id.
    ***************************************************************************/
   private Integer insertPipeline(String name, Boolean isDefault) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName(name)
               .withIsDefault(isDefault)
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
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName, String email, Integer companyId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(email)
               .withCompanyId(companyId)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a LifecycleStage.
    ***************************************************************************/
   private void insertLifecycleStage(String name) throws QException
   {
      new InsertAction().execute(
         new InsertInput(LifecycleStage.TABLE_NAME)
            .withRecordEntity(new LifecycleStage()
               .withName(name)
               .withSortOrder(1)
               .withIsActive(true)));
   }

}
