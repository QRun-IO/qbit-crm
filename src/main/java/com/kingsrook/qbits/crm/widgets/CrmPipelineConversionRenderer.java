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
 ** Renderer for the Pipeline Conversion widget.
 **
 ** Queries DealStageHistory, counts transitions into each stage (entered)
 ** versus transitions out (progressed), and computes a conversion rate.
 *******************************************************************************/

package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Renders pipeline conversion data as a table widget.
 *******************************************************************************/
public class CrmPipelineConversionRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // query all deal stage history                          //
      ///////////////////////////////////////////////////////////
      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME));

      ///////////////////////////////////////////////////////////
      // count entered and progressed for each toStageId       //
      ///////////////////////////////////////////////////////////
      Map<Integer, Integer> enteredByStage    = new HashMap<>();
      Map<Integer, Integer> progressedByStage = new HashMap<>();

      for(QRecord record : historyOutput.getRecords())
      {
         Integer toStageId   = record.getValueInteger("toStageId");
         Integer fromStageId = record.getValueInteger("fromStageId");

         if(toStageId != null)
         {
            enteredByStage.merge(toStageId, 1, Integer::sum);
         }
         if(fromStageId != null)
         {
            progressedByStage.merge(fromStageId, 1, Integer::sum);
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
         new TableData.Column("html", "Entered", "entered", "1fr", "right"),
         new TableData.Column("html", "Progressed", "progressed", "1fr", "right"),
         new TableData.Column("html", "Conversion %", "conversionRate", "1fr", "right")
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(Map.Entry<Integer, Integer> entry : enteredByStage.entrySet())
      {
         Integer stageId    = entry.getKey();
         int     entered    = entry.getValue();
         int     progressed = progressedByStage.getOrDefault(stageId, 0);

         BigDecimal conversionRate = entered > 0
            ? BigDecimal.valueOf(progressed).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(entered), 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

         String stageName = stageNames.getOrDefault(stageId, "Stage " + stageId);

         rows.add(MapBuilder.of(
            "stageName", stageName,
            "entered", entered,
            "progressed", progressed,
            "conversionRate", conversionRate + "%"
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(25)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
