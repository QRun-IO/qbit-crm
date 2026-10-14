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
 ** Renderer for the Stage Duration widget.
 **
 ** Queries DealStageHistory where durationInFromStageDays is not null,
 ** computes average and median per fromStageId.
 *******************************************************************************/

package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Renders stage duration analytics as a table widget.
 *******************************************************************************/
public class CrmStageDurationRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // query history with durationInFromStageDays populated  //
      ///////////////////////////////////////////////////////////
      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("durationInFromStageDays", QCriteriaOperator.IS_NOT_BLANK))));

      ///////////////////////////////////////////////////////////
      // group durations by fromStageId                        //
      ///////////////////////////////////////////////////////////
      Map<Integer, List<Integer>> durationsByStage = new HashMap<>();
      for(QRecord record : historyOutput.getRecords())
      {
         Integer fromStageId = record.getValueInteger("fromStageId");
         Integer duration    = record.getValueInteger("durationInFromStageDays");
         if(fromStageId != null && duration != null)
         {
            durationsByStage.computeIfAbsent(fromStageId, k -> new ArrayList<>()).add(duration);
         }
      }

      ///////////////////////////////////////////////////////////
      // build stage name lookup                               //
      ///////////////////////////////////////////////////////////
      QueryOutput stageOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME));

      Map<Integer, String> stageNames = new HashMap<>();
      for(QRecord record : stageOutput.getRecords())
      {
         stageNames.put(record.getValueInteger("id"), record.getValueString("name"));
      }

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Stage", "stageName", "2fr", null),
         new TableData.Column("html", "Avg Days", "avgDays", "1fr", "right"),
         new TableData.Column("html", "Median Days", "medianDays", "1fr", "right")
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(Map.Entry<Integer, List<Integer>> entry : durationsByStage.entrySet())
      {
         Integer       stageId   = entry.getKey();
         List<Integer> durations = entry.getValue();

         BigDecimal avg = BigDecimal.valueOf(durations.stream().mapToInt(Integer::intValue).average().orElse(0))
            .setScale(1, RoundingMode.HALF_UP);

         Collections.sort(durations);
         int medianIndex = durations.size() / 2;
         int median = durations.size() % 2 == 0
            ? (durations.get(medianIndex - 1) + durations.get(medianIndex)) / 2
            : durations.get(medianIndex);

         String stageName = stageNames.getOrDefault(stageId, "Stage " + stageId);

         rows.add(MapBuilder.of(
            "stageName", stageName,
            "avgDays", avg,
            "medianDays", median
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(25)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
