/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*******************************************************************************
 ** Unit tests for DealInitializationCustomizer -- verifies that inserting a
 ** deal sets stageEnteredDate, weightedAmount, and creates the initial
 ** DealStageHistory row.
 *******************************************************************************/

package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for DealInitializationCustomizer: verifies computed field
 ** initialization and initial stage history creation on deal insert.
 *******************************************************************************/
class DealInitializationCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Insert a deal via InsertAction. Get it back. Verify stageEnteredDate is set.
    *******************************************************************************/
   @Test
   void testNewDealGetsStageEnteredDate() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("New Deal", pipelineId, stageId, new BigDecimal("5000"));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getStageEnteredDate());
   }



   /*******************************************************************************
    ** Insert deal with amount=1000 at a stage with probabilityPct=50. Verify
    ** weightedAmount=500.
    *******************************************************************************/
   @Test
   void testNewDealGetsWeightedAmount() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Qualified", 1, 50);
      Integer dealId = insertDeal("Weighted Deal", pipelineId, stageId, new BigDecimal("1000"));

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getWeightedAmount());
      assertEquals(0, new BigDecimal("500.00").compareTo(deal.getWeightedAmount()));
   }



   /*******************************************************************************
    ** Insert deal. Query DealStageHistory for that dealId. Verify one row with
    ** fromStageId=null and toStageId matching the deal's stage.
    *******************************************************************************/
   @Test
   void testNewDealCreatesInitialHistory() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("History Deal", pipelineId, stageId, new BigDecimal("3000"));

      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

      assertEquals(1, historyOutput.getRecords().size());

      DealStageHistory history = new DealStageHistory(historyOutput.getRecords().get(0));
      assertNull(history.getFromStageId());
      assertEquals(stageId, history.getToStageId());
      assertNotNull(history.getTransitionDate());
      assertEquals(dealId, history.getDealId());
   }



   /*******************************************************************************
    ** Lost-stage inserts close immediately while imported dates are preserved.
    *******************************************************************************/
   @Test
   void closedStageDates() throws QException
   {
      Integer pipeline = insertPipeline("Closed imports");
      Integer stage = new InsertAction().execute(new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(new PipelineStage()
         .withPipelineId(pipeline).withName("Lost").withSortOrder(1).withProbabilityPct(0).withIsClosedLost(true)))
         .getRecords().getFirst().getValueInteger("id");
      Deal lost = new Deal(GetAction.execute(Deal.TABLE_NAME, insertDeal("Lost", pipeline, stage, BigDecimal.TEN)));
      assertEquals(java.time.LocalDate.now(java.time.ZoneOffset.UTC), lost.getActualCloseDate());
      assertNotNull(lost.getStageEnteredDate());
      java.time.LocalDate closeDate = java.time.LocalDate.of(2025, 1, 2);
      java.time.Instant entered = java.time.Instant.parse("2025-01-01T10:00:00Z");
      Integer imported = new InsertAction().execute(new InsertInput(Deal.TABLE_NAME).withRecordEntity(new Deal()
         .withName("Imported").withPipelineId(pipeline).withPipelineStageId(stage).withActualCloseDate(closeDate).withStageEnteredDate(entered)))
         .getRecords().getFirst().getValueInteger("id");
      Deal saved = new Deal(GetAction.execute(Deal.TABLE_NAME, imported));
      assertEquals(closeDate, saved.getActualCloseDate());
      assertEquals(entered, saved.getStageEnteredDate());
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

}
