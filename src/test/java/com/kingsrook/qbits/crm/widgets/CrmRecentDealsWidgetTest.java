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
 ** Unit tests for CrmRecentDealsWidget.
 *******************************************************************************/

package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for the Recent Deals widget.
 *******************************************************************************/
class CrmRecentDealsWidgetTest extends BaseTest
{

   /*******************************************************************************
    ** Insert closed deals, render widget, verify rows.
    *******************************************************************************/
   @Test
   void testRenderRecentDeals() throws QException
   {
      ///////////////////////////////////////
      // set up pipeline and stage         //
      ///////////////////////////////////////
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId    = insertPipelineStage(pipelineId, "Closed Won", 1);

      ///////////////////////////////////////
      // insert deals with close dates     //
      ///////////////////////////////////////
      insertClosedDeal("Deal A", pipelineId, stageId, new BigDecimal("10000"), LocalDate.of(2026, 3, 1));
      insertClosedDeal("Deal B", pipelineId, stageId, new BigDecimal("25000"), LocalDate.of(2026, 3, 15));
      insertClosedDeal("Deal C", pipelineId, stageId, new BigDecimal("5000"), LocalDate.of(2026, 3, 20));

      ///////////////////////////////////////
      // render the widget                 //
      ///////////////////////////////////////
      CrmRecentDealsRenderer renderer = new CrmRecentDealsRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmRecentDealsWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      assertNotNull(widgetOutput);
      assertNotNull(widgetOutput.getWidgetData());
      assertThat(widgetOutput.getWidgetData()).isInstanceOf(TableData.class);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
      List<Map<String, Object>> rows = tableData.getRows();

      assertEquals(3, rows.size());

      /////////////////////////////////////////////////////////////
      // verify ordering: most recent first                      //
      /////////////////////////////////////////////////////////////
      assertEquals("Deal C", rows.get(0).get("name"));
      assertEquals("Deal B", rows.get(1).get("name"));
      assertEquals("Deal A", rows.get(2).get("name"));

      /////////////////////////////////////////////////////////////
      // verify columns present                                  //
      /////////////////////////////////////////////////////////////
      assertThat(rows.get(0)).containsKey("name");
      assertThat(rows.get(0)).containsKey("amount");
      assertThat(rows.get(0)).containsKey("ownerUserId");
      assertThat(rows.get(0)).containsKey("closedDate");
      assertThat(rows.get(0)).containsKey("winLossReason");
   }



   /*******************************************************************************
    ** Render with no deals returns empty rows.
    *******************************************************************************/
   @Test
   void testRenderEmptyRecentDeals() throws QException
   {
      CrmRecentDealsRenderer renderer = new CrmRecentDealsRenderer();
      RenderWidgetInput widgetInput = new RenderWidgetInput();
      widgetInput.setWidgetMetaData(new CrmRecentDealsWidgetProducer().produce(null));

      RenderWidgetOutput widgetOutput = renderer.render(widgetInput);

      TableData tableData = (TableData) widgetOutput.getWidgetData();
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
   private Integer insertPipelineStage(Integer pipelineId, String name, int sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(100)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Closed Deal.
    *******************************************************************************/
   private void insertClosedDeal(String name, Integer pipelineId, Integer stageId, BigDecimal amount, LocalDate closeDate) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecord(
            new QRecord()
               .withValue("name", name)
               .withValue("pipelineId", pipelineId)
               .withValue("pipelineStageId", stageId)
               .withValue("amount", amount)
               .withValue("actualCloseDate", closeDate)
               .withValue("ownerUserId", "user-001")));
   }

}
