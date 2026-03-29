/*******************************************************************************
 ** Unit tests for the Deal entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.deals.model.WinLossReason;
import com.kingsrook.qbits.crm.core.model.enums.CrmDealPriority;
import com.kingsrook.qbits.crm.core.model.enums.CrmWinLossType;
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
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for Deal CRUD operations, field persistence, BigDecimal amounts,
 ** and linked entity references.
 *******************************************************************************/
class DealTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the deal table is registered and has all expected fields.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Deal.TABLE_NAME);
      assertNotNull(table);

      //////////////////////////
      // identity fields      //
      //////////////////////////
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("pipelineId");
      assertThat(table.getFields()).containsKey("pipelineStageId");
      assertThat(table.getFields()).containsKey("amount");
      assertThat(table.getFields()).containsKey("currencyCode");

      //////////////////////////
      // detail fields        //
      //////////////////////////
      assertThat(table.getFields()).containsKey("amountInBaseCurrency");
      assertThat(table.getFields()).containsKey("probabilityOverridePct");
      assertThat(table.getFields()).containsKey("weightedAmount");
      assertThat(table.getFields()).containsKey("companyId");
      assertThat(table.getFields()).containsKey("ownerUserId");
      assertThat(table.getFields()).containsKey("leadSourceId");
      assertThat(table.getFields()).containsKey("expectedCloseDate");
      assertThat(table.getFields()).containsKey("actualCloseDate");
      assertThat(table.getFields()).containsKey("winLossReasonId");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("priority");

      //////////////////////////
      // system fields        //
      //////////////////////////
      assertThat(table.getFields()).containsKey("stageEnteredDate");
      assertThat(table.getFields()).containsKey("lastActivityDate");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");

      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("handshake");
   }



   /*******************************************************************************
    ** Verify the table has sections defined.
    *******************************************************************************/
   @Test
   void testSections()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Deal.TABLE_NAME);
      assertThat(table.getSections()).isNotNull();
      assertThat(table.getSections().size()).isGreaterThanOrEqualTo(4);

      ///////////////////////////////////
      // verify T1 identity section    //
      ///////////////////////////////////
      assertThat(table.getSections().get(0).getFieldNames())
         .contains("id", "name", "pipelineId", "pipelineStageId", "amount", "currencyCode");
   }



   /*******************************************************************************
    ** Insert a deal with all fields and verify round-trip, including BigDecimal.
    *******************************************************************************/
   @Test
   void testInsertWithAllFields() throws QException
   {
      ////////////////////////////////////
      // set up prerequisite data       //
      ////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales Pipeline");
      Integer stageId = insertPipelineStage(pipelineId, "Prospecting", 1, 10);
      Integer reasonId = insertWinLossReason("Better Fit", CrmWinLossType.WIN.getId(), 1);

      Deal deal = new Deal()
         .withName("Acme Corp Renewal Q2")
         .withPipelineId(pipelineId)
         .withPipelineStageId(stageId)
         .withAmount(new BigDecimal("75000.50"))
         .withCurrencyCode("USD")
         .withProbabilityOverridePct(60)
         .withCompanyId(100)
         .withOwnerUserId("user-001")
         .withLeadSourceId(null)
         .withExpectedCloseDate(LocalDate.of(2026, 6, 30))
         .withWinLossReasonId(reasonId)
         .withDescription("Q2 contract renewal with expanded license seats.")
         .withPriority(CrmDealPriority.HIGH.getId());

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(deal));
      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME));
      assertEquals(1, queryOutput.getRecords().size());

      Deal fetched = new Deal(queryOutput.getRecords().get(0));
      assertEquals("Acme Corp Renewal Q2", fetched.getName());
      assertEquals(pipelineId, fetched.getPipelineId());
      assertEquals(stageId, fetched.getPipelineStageId());
      assertThat(fetched.getAmount()).isEqualByComparingTo(new BigDecimal("75000.50"));
      assertEquals("USD", fetched.getCurrencyCode());
      assertEquals(60, fetched.getProbabilityOverridePct());
      assertEquals(100, fetched.getCompanyId());
      assertEquals("user-001", fetched.getOwnerUserId());
      assertEquals(LocalDate.of(2026, 6, 30), fetched.getExpectedCloseDate());
      assertEquals(reasonId, fetched.getWinLossReasonId());
      assertEquals("Q2 contract renewal with expanded license seats.", fetched.getDescription());
      assertEquals(CrmDealPriority.HIGH.getId(), fetched.getPriority());
   }



   /*******************************************************************************
    ** Insert a minimal deal with only required fields and verify defaults.
    *******************************************************************************/
   @Test
   void testInsertMinimalDeal() throws QException
   {
      Integer pipelineId = insertPipeline("Simple Pipeline");
      Integer stageId = insertPipelineStage(pipelineId, "New", 1, 0);

      Deal deal = new Deal()
         .withName("Quick Deal")
         .withPipelineId(pipelineId)
         .withPipelineStageId(stageId)
         .withOwnerUserId("user-001");

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(deal));
      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME));
      Deal fetched = new Deal(queryOutput.getRecords().get(0));

      assertEquals("Quick Deal", fetched.getName());
      assertEquals("user-001", fetched.getOwnerUserId());
      assertNull(fetched.getAmount());
      assertNull(fetched.getCompanyId());
      assertNull(fetched.getExpectedCloseDate());
   }



   /*******************************************************************************
    ** Insert multiple deals and verify query returns all.
    *******************************************************************************/
   @Test
   void testMultipleDeals() throws QException
   {
      Integer pipelineId = insertPipeline("Multi Pipeline");
      Integer stage1Id = insertPipelineStage(pipelineId, "Stage 1", 1, 25);
      Integer stage2Id = insertPipelineStage(pipelineId, "Stage 2", 2, 50);

      new InsertAction().execute(new InsertInput(Deal.TABLE_NAME)
         .withRecordEntity(new Deal()
            .withName("Deal A")
            .withPipelineId(pipelineId)
            .withPipelineStageId(stage1Id)
            .withAmount(new BigDecimal("10000.00"))
            .withOwnerUserId("user-001")));

      new InsertAction().execute(new InsertInput(Deal.TABLE_NAME)
         .withRecordEntity(new Deal()
            .withName("Deal B")
            .withPipelineId(pipelineId)
            .withPipelineStageId(stage2Id)
            .withAmount(new BigDecimal("25000.99"))
            .withOwnerUserId("user-002")));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME));
      assertEquals(2, queryOutput.getRecords().size());

      ///////////////////////////////////////////////
      // verify BigDecimal precision round-trips   //
      ///////////////////////////////////////////////
      Deal dealB = new Deal(queryOutput.getRecords().stream()
         .filter(r -> "Deal B".equals(r.getValueString("name")))
         .findFirst()
         .orElseThrow());
      assertThat(dealB.getAmount()).isEqualByComparingTo(new BigDecimal("25000.99"));
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
    ** Helper: insert a WinLossReason and return its id.
    ***************************************************************************/
   private Integer insertWinLossReason(String name, Integer type, Integer sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(WinLossReason.TABLE_NAME)
            .withRecordEntity(new WinLossReason()
               .withName(name)
               .withType(type)
               .withSortOrder(sortOrder)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
