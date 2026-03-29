/*******************************************************************************
 ** Renderer for the CRM Pipeline Summary widget.
 **
 ** Queries deals grouped by pipelineStageId for a given pipelineId and returns
 ** a table with columns: stageName, dealCount, totalAmount, weightedAmount.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Renders the pipeline summary as a table widget showing deal counts and
 ** amounts grouped by pipeline stage.
 *******************************************************************************/
public class CrmPipelineSummaryRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////////
      // get pipelineId from widget input query params             //
      ///////////////////////////////////////////////////////////////
      Integer pipelineId = null;
      if(input.getQueryParams() != null && input.getQueryParams().containsKey("pipelineId"))
      {
         pipelineId = Integer.valueOf(input.getQueryParams().get("pipelineId"));
      }

      ///////////////////////////////////////////////////////////////
      // query all stages for the pipeline, ordered by sortOrder   //
      ///////////////////////////////////////////////////////////////
      QQueryFilter stageFilter = new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("sortOrder", true));
      if(pipelineId != null)
      {
         stageFilter.withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, pipelineId));
      }

      QueryOutput stageOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME).withFilter(stageFilter));

      //////////////////////////////////////////////////////////
      // build a map of stageId -> stageName for display      //
      //////////////////////////////////////////////////////////
      Map<Integer, String> stageNames = new LinkedHashMap<>();
      for(QRecord stageRecord : stageOutput.getRecords())
      {
         stageNames.put(stageRecord.getValueInteger("id"), stageRecord.getValueString("name"));
      }

      ///////////////////////////////////////////////////////////////
      // query deals for the pipeline                              //
      ///////////////////////////////////////////////////////////////
      QQueryFilter dealFilter = new QQueryFilter();
      if(pipelineId != null)
      {
         dealFilter.withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, pipelineId));
      }

      QueryOutput dealOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME).withFilter(dealFilter));

      //////////////////////////////////////////////////////////
      // aggregate deals by stage                             //
      //////////////////////////////////////////////////////////
      Map<Integer, Integer> countByStage          = new LinkedHashMap<>();
      Map<Integer, BigDecimal> totalAmountByStage = new LinkedHashMap<>();
      Map<Integer, BigDecimal> weightedByStage    = new LinkedHashMap<>();

      for(Integer stageId : stageNames.keySet())
      {
         countByStage.put(stageId, 0);
         totalAmountByStage.put(stageId, BigDecimal.ZERO);
         weightedByStage.put(stageId, BigDecimal.ZERO);
      }

      for(QRecord dealRecord : dealOutput.getRecords())
      {
         Integer stageId = dealRecord.getValueInteger("pipelineStageId");
         if(stageId == null || !stageNames.containsKey(stageId))
         {
            continue;
         }

         countByStage.merge(stageId, 1, Integer::sum);

         BigDecimal amount = dealRecord.getValueBigDecimal("amount");
         if(amount != null)
         {
            totalAmountByStage.merge(stageId, amount, BigDecimal::add);
         }

         BigDecimal weighted = dealRecord.getValueBigDecimal("weightedAmount");
         if(weighted != null)
         {
            weightedByStage.merge(stageId, weighted, BigDecimal::add);
         }
      }

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Stage", "stageName", "2fr", null),
         new TableData.Column("html", "Deal Count", "dealCount", "1fr", "right"),
         new TableData.Column("html", "Total Amount", "totalAmount", "1fr", "right"),
         new TableData.Column("html", "Weighted Amount", "weightedAmount", "1fr", "right")
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(Integer stageId : stageNames.keySet())
      {
         rows.add(MapBuilder.of(
            "stageName", stageNames.get(stageId),
            "dealCount", countByStage.getOrDefault(stageId, 0),
            "totalAmount", totalAmountByStage.getOrDefault(stageId, BigDecimal.ZERO).toPlainString(),
            "weightedAmount", weightedByStage.getOrDefault(stageId, BigDecimal.ZERO).toPlainString()
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(50)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
