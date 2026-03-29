/*******************************************************************************
 ** Unit tests for TransferOwnershipProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for transferring ownership of contacts, companies, and deals.
 *******************************************************************************/
class TransferOwnershipProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Transfer records from one user to another and verify updates + audit logs.
    *******************************************************************************/
   @Test
   void testTransferOwnership() throws QException
   {
      ///////////////////////////////////////
      // set up records owned by user-old  //
      ///////////////////////////////////////
      Integer contactId = insertContact("user-old");
      Integer companyId = insertCompany("user-old");

      Integer pipelineId = insertPipeline();
      Integer stageId    = insertPipelineStage(pipelineId);
      Integer dealId     = insertDeal("user-old", pipelineId, stageId);

      ////////////////////////////////////
      // run transfer process           //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(TransferOwnershipProcess.NAME);
      processInput.addValue("fromUserId", "user-old");
      processInput.addValue("toUserId", "user-new");

      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify contact updated                  //
      /////////////////////////////////////////////
      Contact updatedContact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertEquals("user-new", updatedContact.getOwnerUserId());

      /////////////////////////////////////////////
      // verify company updated                  //
      /////////////////////////////////////////////
      Company updatedCompany = new Company(GetAction.execute(Company.TABLE_NAME, companyId));
      assertEquals("user-new", updatedCompany.getOwnerUserId());

      /////////////////////////////////////////////
      // verify deal updated                     //
      /////////////////////////////////////////////
      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals("user-new", updatedDeal.getOwnerUserId());

      /////////////////////////////////////////////
      // verify audit log entries created        //
      /////////////////////////////////////////////
      QueryOutput auditQuery = new QueryAction().execute(
         new QueryInput(AuditLog.TABLE_NAME));
      assertTrue(auditQuery.getRecords().size() >= 3);
   }



   /***************************************************************************
    ** Helper: insert a Contact.
    ***************************************************************************/
   private Integer insertContact(String ownerUserId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("Test")
               .withLastName("Contact")
               .withEmail("test@example.com")
               .withOwnerUserId(ownerUserId)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Company.
    ***************************************************************************/
   private Integer insertCompany(String ownerUserId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Company.TABLE_NAME).withRecordEntity(
            new Company()
               .withName("Test Corp")
               .withDomain("test.com")
               .withOwnerUserId(ownerUserId)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertPipeline() throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName("Sales").withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertPipelineStage(Integer pipelineId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Prospect")
               .withSortOrder(1)
               .withProbabilityPct(10)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertDeal(String ownerUserId, Integer pipelineId, Integer stageId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName("Test Deal")
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(new BigDecimal("5000"))
               .withOwnerUserId(ownerUserId)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
