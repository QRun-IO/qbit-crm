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
 ** Unit tests for CrmStageDurationRenderer.
 *******************************************************************************/

package com.kingsrook.qbits.crm.widgets;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the Stage Duration widget: with data, empty data, and null durations.
 *******************************************************************************/
class CrmStageDurationWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Test render with duration data produces rows.
    *******************************************************************************/
   @Test
   void testRenderWithData() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id   = insertPipelineStage(pipelineId, "Prospect", 1);
      Integer stage2Id   = insertPipelineStage(pipelineId, "Qualified", 2);

      // Insert history records with duration populated
      insertStageHistory(1, stage1Id, stage2Id, 5);
      insertStageHistory(2, stage1Id, stage2Id, 10);
      insertStageHistory(3, stage1Id, stage2Id, 15);

      CrmStageDurationRenderer renderer = new CrmStageDurationRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmStageDurationWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      assertNotNull(output);
      assertNotNull(output.getWidgetData());
      assertThat(output.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) output.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();
      assertThat(rows).isNotEmpty();
      assertThat(rows.get(0)).containsKey("stageName");
      assertThat(rows.get(0)).containsKey("avgDays");
      assertThat(rows.get(0)).containsKey("medianDays");
   }



   /*******************************************************************************
    ** Test render with no data produces empty rows.
    *******************************************************************************/
   @Test
   void testRenderEmptyData() throws QException
   {
      CrmStageDurationRenderer renderer = new CrmStageDurationRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmStageDurationWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      assertNotNull(output);
      TableData tableData = (TableData) output.getWidgetData();
      assertThat(tableData.getRows()).isEmpty();
   }



   /*******************************************************************************
    ** Test render with even number of durations for median calculation.
    *******************************************************************************/
   @Test
   void testRenderWithEvenDurations() throws QException
   {
      Integer pipelineId = insertPipeline("Sales");
      Integer stage1Id   = insertPipelineStage(pipelineId, "Prospect", 1);
      Integer stage2Id   = insertPipelineStage(pipelineId, "Qualified", 2);

      insertStageHistory(1, stage1Id, stage2Id, 4);
      insertStageHistory(2, stage1Id, stage2Id, 8);

      CrmStageDurationRenderer renderer = new CrmStageDurationRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmStageDurationWidgetProducer().produce(null));

      RenderWidgetOutput output = renderer.render(widgetInput);

      TableData tableData = (TableData) output.getWidgetData();
      assertThat(tableData.getRows()).hasSize(1);
      // median of [4, 8] = (4+8)/2 = 6
      assertThat(tableData.getRows().get(0).get("medianDays")).isEqualTo(6);
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
   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(sortOrder * 25)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Stage History.
    *******************************************************************************/
   private void insertStageHistory(Integer dealId, Integer fromStageId, Integer toStageId, Integer durationDays) throws QException
   {
      new InsertAction().execute(
         new InsertInput(DealStageHistory.TABLE_NAME).withRecordEntity(
            new DealStageHistory()
               .withDealId(dealId)
               .withFromStageId(fromStageId)
               .withToStageId(toStageId)
               .withDurationInFromStageDays(durationDays)
               .withUserId("user-001")
               .withTransitionDate(Instant.now())));
   }

}
