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
 ** Unit tests for CrmSalesLeaderboardRenderer.
 *******************************************************************************/

package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.LocalDate;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for the Sales Leaderboard widget.
 *******************************************************************************/
class CrmSalesLeaderboardWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Test render with data.
    *******************************************************************************/
   @Test
   void testRenderWithData() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer wonStageId = insertPipelineStage(pipelineId, "Won", 3, true, false);

      insertDeal("Deal A", pipelineId, wonStageId, new BigDecimal("30000"), "user-A");
      insertDeal("Deal B", pipelineId, wonStageId, new BigDecimal("20000"), "user-B");
      insertDeal("Deal C", pipelineId, wonStageId, new BigDecimal("10000"), "user-A");

      CrmSalesLeaderboardRenderer renderer = new CrmSalesLeaderboardRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmSalesLeaderboardWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);
      TableData tableData = (TableData) output.getWidgetData();

      assertThat(tableData.getRows()).hasSize(2);
      // user-A should be first (40000 > 20000)
      assertThat(tableData.getRows().get(0).get("owner")).isEqualTo("user-A");
      assertThat(tableData.getRows().get(0).get("rank")).isEqualTo("1");
      assertThat(tableData.getRows().get(1).get("owner")).isEqualTo("user-B");
   }



   /*******************************************************************************
    ** Test render with no data.
    *******************************************************************************/
   @Test
   void testRenderEmpty() throws QException
   {
      CrmSalesLeaderboardRenderer renderer = new CrmSalesLeaderboardRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmSalesLeaderboardWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);
      TableData tableData = (TableData) output.getWidgetData();
      assertThat(tableData.getRows()).isEmpty();
   }



   /*******************************************************************************
    ** Insert Pipeline.
    *******************************************************************************/
   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName(name).withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Pipeline Stage.
    *******************************************************************************/
   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder,
                                        boolean isClosedWon, boolean isClosedLost) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(isClosedWon ? 100 : 0)
               .withIsClosedWon(isClosedWon)
               .withIsClosedLost(isClosedLost)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Deal.
    *******************************************************************************/
   private void insertDeal(String name, Integer pipelineId, Integer stageId,
                            BigDecimal amountInBase, String ownerUserId) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amountInBase)
               .withAmountInBaseCurrency(amountInBase)
               .withActualCloseDate(LocalDate.now())
               .withOwnerUserId(ownerUserId)));
   }

}
