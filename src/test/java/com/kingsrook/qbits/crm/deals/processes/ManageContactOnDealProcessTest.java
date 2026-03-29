/*******************************************************************************
 ** Unit tests for ManageContactOnDealProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for adding and removing contacts on a deal: basic add, isPrimary
 ** enforcement, basic remove, and removing a non-linked contact.
 *******************************************************************************/
class ManageContactOnDealProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Add a contact to a deal and verify the junction record.
    *******************************************************************************/
   @Test
   void testAddContactToDeal() throws QException
   {
      Integer dealId    = setupDeal();
      Integer contactId = insertContact("Alice", "Test");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ManageContactOnDealProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("contactId", contactId);
      processInput.addValue("action", "ADD");
      processInput.addValue("isPrimary", false);

      new RunProcessAction().execute(processInput);

      ///////////////////////////////////////////
      // verify DealContact junction created   //
      ///////////////////////////////////////////
      QueryOutput junctionOutput = queryDealContacts(dealId);
      assertEquals(1, junctionOutput.getRecords().size());
      DealContact dc = new DealContact(junctionOutput.getRecords().get(0));
      assertEquals(contactId, dc.getContactId());
      assertFalse(dc.getIsPrimary());
   }



   /*******************************************************************************
    ** Add two contacts, then add a third as primary, verify the first two lose
    ** isPrimary.
    *******************************************************************************/
   @Test
   void testIsPrimaryEnforcement() throws QException
   {
      Integer dealId     = setupDeal();
      Integer contact1Id = insertContact("First", "Primary");
      Integer contact2Id = insertContact("Second", "Contact");
      Integer contact3Id = insertContact("Third", "NewPrimary");

      ///////////////////////////////////////////
      // add first contact as primary          //
      ///////////////////////////////////////////
      runManageContact(dealId, contact1Id, "ADD", true);

      ///////////////////////////////////////////
      // add second contact as non-primary     //
      ///////////////////////////////////////////
      runManageContact(dealId, contact2Id, "ADD", false);

      ///////////////////////////////////////////
      // verify first is still primary         //
      ///////////////////////////////////////////
      QueryOutput beforeOutput = queryDealContacts(dealId);
      assertEquals(2, beforeOutput.getRecords().size());

      ///////////////////////////////////////////
      // add third contact as primary          //
      ///////////////////////////////////////////
      runManageContact(dealId, contact3Id, "ADD", true);

      ///////////////////////////////////////////
      // verify first is no longer primary     //
      ///////////////////////////////////////////
      QueryOutput afterOutput = queryDealContacts(dealId);
      assertEquals(3, afterOutput.getRecords().size());

      int primaryCount = 0;
      Integer primaryContactId = null;
      for(var record : afterOutput.getRecords())
      {
         DealContact dc = new DealContact(record);
         if(Boolean.TRUE.equals(dc.getIsPrimary()))
         {
            primaryCount++;
            primaryContactId = dc.getContactId();
         }
      }

      assertEquals(1, primaryCount, "Only one contact should be primary");
      assertEquals(contact3Id, primaryContactId, "Third contact should be the primary");
   }



   /*******************************************************************************
    ** Remove a contact from a deal.
    *******************************************************************************/
   @Test
   void testRemoveContactFromDeal() throws QException
   {
      Integer dealId    = setupDeal();
      Integer contactId = insertContact("Remove", "Me");

      ///////////////////////////////////////////
      // add and then remove                   //
      ///////////////////////////////////////////
      runManageContact(dealId, contactId, "ADD", false);
      assertEquals(1, queryDealContacts(dealId).getRecords().size());

      runManageContact(dealId, contactId, "REMOVE", null);
      assertEquals(0, queryDealContacts(dealId).getRecords().size());
   }



   /*******************************************************************************
    ** Removing a contact not linked to the deal should throw.
    *******************************************************************************/
   @Test
   void testRemoveNonLinkedContactThrows() throws QException
   {
      Integer dealId    = setupDeal();
      Integer contactId = insertContact("Not", "Linked");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ManageContactOnDealProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("contactId", contactId);
      processInput.addValue("action", "REMOVE");

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



   /***************************************************************************
    ** Helper: run the manage contact process.
    ***************************************************************************/
   private void runManageContact(Integer dealId, Integer contactId, String action, Boolean isPrimary) throws QException
   {
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ManageContactOnDealProcess.NAME);
      processInput.addValue("dealId", dealId);
      processInput.addValue("contactId", contactId);
      processInput.addValue("action", action);
      if(isPrimary != null)
      {
         processInput.addValue("isPrimary", isPrimary);
      }

      new RunProcessAction().execute(processInput);
   }



   /***************************************************************************
    ** Helper: query DealContact for a given deal.
    ***************************************************************************/
   private QueryOutput queryDealContacts(Integer dealId) throws QException
   {
      return new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));
   }



   /***************************************************************************
    ** Helper: set up pipeline + stage + deal for tests.
    ***************************************************************************/
   private Integer setupDeal() throws QException
   {
      InsertOutput pipelineInsert = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName("Test Pipeline")
               .withIsDefault(true)
               .withIsActive(true)));
      Integer pipelineId = pipelineInsert.getRecords().get(0).getValueInteger("id");

      InsertOutput stageInsert = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Open")
               .withSortOrder(1)
               .withProbabilityPct(10)
               .withIsClosedWon(false)
               .withIsClosedLost(false)));
      Integer stageId = stageInsert.getRecords().get(0).getValueInteger("id");

      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName("Test Deal")
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(new BigDecimal("5000"))
               .withOwnerUserId("user-001")
               .withStageEnteredDate(Instant.now())));
      return (dealInsert.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
