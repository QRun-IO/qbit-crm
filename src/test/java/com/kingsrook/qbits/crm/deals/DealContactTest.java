/*******************************************************************************
 ** Unit tests for the DealContact entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.deals.model.ContactRole;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for DealContact junction entity, unique key on (dealId, contactId),
 ** and linked entity references.
 *******************************************************************************/
class DealContactTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the deal contact table is registered.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(DealContact.TABLE_NAME);
      assertNotNull(table);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("dealId");
      assertThat(table.getFields()).containsKey("contactId");
      assertThat(table.getFields()).containsKey("contactRoleId");
      assertThat(table.getFields()).containsKey("isPrimary");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
   }



   /*******************************************************************************
    ** Verify unique key on (dealId, contactId).
    *******************************************************************************/
   @Test
   void testUniqueKey()
   {
      QTableMetaData table = QContext.getQInstance().getTable(DealContact.TABLE_NAME);
      assertThat(table.getUniqueKeys()).isNotNull();
      assertThat(table.getUniqueKeys()).hasSize(1);
      assertThat(table.getUniqueKeys().get(0).getFieldNames()).containsExactly("dealId", "contactId");
   }



   /*******************************************************************************
    ** Insert deal contacts with role and primary flag, verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      ////////////////////////////////////
      // set up prerequisite data       //
      ////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales Pipeline");
      Integer stageId = insertPipelineStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("Test Deal", pipelineId, stageId);
      Integer contactId = insertContact("Jane", "Smith");
      Integer roleId = insertContactRole("Decision Maker", 1);

      //////////////////////////////////
      // insert deal-contact link     //
      //////////////////////////////////
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(DealContact.TABLE_NAME)
            .withRecordEntity(new DealContact()
               .withDealId(dealId)
               .withContactId(contactId)
               .withContactRoleId(roleId)
               .withIsPrimary(true)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME));
      assertEquals(1, queryOutput.getRecords().size());

      DealContact fetched = new DealContact(queryOutput.getRecords().get(0));
      assertEquals(dealId, fetched.getDealId());
      assertEquals(contactId, fetched.getContactId());
      assertEquals(roleId, fetched.getContactRoleId());
      assertTrue(fetched.getIsPrimary());
   }



   /*******************************************************************************
    ** Insert multiple contacts on a deal, verify all persist.
    *******************************************************************************/
   @Test
   void testMultipleContactsOnDeal() throws QException
   {
      Integer pipelineId = insertPipeline("Pipeline");
      Integer stageId = insertPipelineStage(pipelineId, "Stage 1", 1, 10);
      Integer dealId = insertDeal("Multi Contact Deal", pipelineId, stageId);
      Integer contact1Id = insertContact("Alice", "Johnson");
      Integer contact2Id = insertContact("Bob", "Williams");

      new InsertAction().execute(new InsertInput(DealContact.TABLE_NAME)
         .withRecordEntity(new DealContact()
            .withDealId(dealId)
            .withContactId(contact1Id)
            .withIsPrimary(true)));

      new InsertAction().execute(new InsertInput(DealContact.TABLE_NAME)
         .withRecordEntity(new DealContact()
            .withDealId(dealId)
            .withContactId(contact2Id)
            .withIsPrimary(false)));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME));
      assertEquals(2, queryOutput.getRecords().size());
   }



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
   private Integer insertPipelineStage(Integer pipelineId, String name, Integer sortOrder, Integer probabilityPct) throws QException
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
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
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



   /***************************************************************************
    ** Helper: insert a ContactRole and return its id.
    ***************************************************************************/
   private Integer insertContactRole(String name, Integer sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ContactRole.TABLE_NAME)
            .withRecordEntity(new ContactRole()
               .withName(name)
               .withSortOrder(sortOrder)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
