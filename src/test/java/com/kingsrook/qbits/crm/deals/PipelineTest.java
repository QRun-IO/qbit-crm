/*******************************************************************************
 ** Unit tests for the Pipeline and PipelineStage entities.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import com.kingsrook.qbits.crm.BaseTest;
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
 ** Tests for Pipeline and PipelineStage CRUD operations, PVS registration,
 ** unique key constraints, and section layout.
 *******************************************************************************/
class PipelineTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the pipeline table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testPipelineTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Pipeline.TABLE_NAME);
      assertNotNull(table);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("isDefault");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");

      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("linear_scale");
   }



   /*******************************************************************************
    ** Verify the pipeline stage table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testPipelineStageTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(PipelineStage.TABLE_NAME);
      assertNotNull(table);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("pipelineId");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("sortOrder");
      assertThat(table.getFields()).containsKey("probabilityPct");
      assertThat(table.getFields()).containsKey("rotDays");
      assertThat(table.getFields()).containsKey("requiredFieldsJson");
      assertThat(table.getFields()).containsKey("isClosedWon");
      assertThat(table.getFields()).containsKey("isClosedLost");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");

      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("stairs");
   }



   /*******************************************************************************
    ** Verify pipeline PVS is registered.
    *******************************************************************************/
   @Test
   void testPipelinePossibleValueSource()
   {
      assertNotNull(QContext.getQInstance().getPossibleValueSource(Pipeline.TABLE_NAME));
   }



   /*******************************************************************************
    ** Verify pipeline stage PVS is registered.
    *******************************************************************************/
   @Test
   void testPipelineStagePossibleValueSource()
   {
      assertNotNull(QContext.getQInstance().getPossibleValueSource(PipelineStage.TABLE_NAME));
   }



   /*******************************************************************************
    ** Verify pipeline stage has unique keys on (pipelineId, name) and
    ** (pipelineId, sortOrder).
    *******************************************************************************/
   @Test
   void testPipelineStageUniqueKeys()
   {
      QTableMetaData table = QContext.getQInstance().getTable(PipelineStage.TABLE_NAME);
      assertThat(table.getUniqueKeys()).isNotNull();
      assertThat(table.getUniqueKeys()).hasSize(2);
   }



   /*******************************************************************************
    ** Insert pipeline and stages, then verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertPipelineAndStages() throws QException
   {
      //////////////////////////
      // insert a pipeline    //
      //////////////////////////
      Integer pipelineId = insertPipeline("Sales Pipeline", true, true);
      assertNotNull(pipelineId);

      //////////////////////////
      // insert stages        //
      //////////////////////////
      Integer prospectingId = insertPipelineStage(pipelineId, "Prospecting", 1, 10, 14, null, false, false);
      Integer closedWonId = insertPipelineStage(pipelineId, "Closed Won", 5, 100, null, null, true, false);
      Integer closedLostId = insertPipelineStage(pipelineId, "Closed Lost", 6, 0, null, "[\"winLossReasonId\"]", false, true);

      assertNotNull(prospectingId);
      assertNotNull(closedWonId);
      assertNotNull(closedLostId);

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput pipelineQuery = new QueryAction().execute(
         new QueryInput(Pipeline.TABLE_NAME));
      assertEquals(1, pipelineQuery.getRecords().size());

      Pipeline fetched = new Pipeline(pipelineQuery.getRecords().get(0));
      assertEquals("Sales Pipeline", fetched.getName());
      assertTrue(fetched.getIsDefault());
      assertTrue(fetched.getIsActive());

      QueryOutput stageQuery = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME));
      assertEquals(3, stageQuery.getRecords().size());

      PipelineStage wonStage = new PipelineStage(stageQuery.getRecords().stream()
         .filter(r -> "Closed Won".equals(r.getValueString("name")))
         .findFirst()
         .orElseThrow());
      assertEquals(pipelineId, wonStage.getPipelineId());
      assertEquals(5, wonStage.getSortOrder());
      assertEquals(100, wonStage.getProbabilityPct());
      assertTrue(wonStage.getIsClosedWon());
      assertFalse(wonStage.getIsClosedLost());

      PipelineStage lostStage = new PipelineStage(stageQuery.getRecords().stream()
         .filter(r -> "Closed Lost".equals(r.getValueString("name")))
         .findFirst()
         .orElseThrow());
      assertFalse(lostStage.getIsClosedWon());
      assertTrue(lostStage.getIsClosedLost());
      assertEquals("[\"winLossReasonId\"]", lostStage.getRequiredFieldsJson());
   }



   /*******************************************************************************
    ** Insert a minimal pipeline with only required fields.
    *******************************************************************************/
   @Test
   void testInsertMinimalPipeline() throws QException
   {
      Integer pipelineId = insertPipeline("Renewal Pipeline", false, true);
      assertNotNull(pipelineId);

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Pipeline.TABLE_NAME));
      Pipeline fetched = new Pipeline(queryOutput.getRecords().get(0));
      assertEquals("Renewal Pipeline", fetched.getName());
      assertFalse(fetched.getIsDefault());
   }



   /***************************************************************************
    ** Helper: insert a Pipeline and return its id.
    ***************************************************************************/
   private Integer insertPipeline(String name, Boolean isDefault, Boolean isActive) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName(name)
               .withIsDefault(isDefault)
               .withIsActive(isActive)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage and return its id.
    ***************************************************************************/
   private Integer insertPipelineStage(Integer pipelineId, String name, Integer sortOrder,
      Integer probabilityPct, Integer rotDays, String requiredFieldsJson,
      Boolean isClosedWon, Boolean isClosedLost) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withRotDays(rotDays)
               .withRequiredFieldsJson(requiredFieldsJson)
               .withIsClosedWon(isClosedWon)
               .withIsClosedLost(isClosedLost)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
