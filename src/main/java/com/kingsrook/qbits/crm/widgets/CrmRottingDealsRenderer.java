/*******************************************************************************
 ** Renderer for the CRM Rotting Deals widget.
 **
 ** Queries deals and their pipeline stages, finds deals where the number of
 ** days since stageEnteredDate exceeds the stage's rotDays threshold.
 ** Returns a table with columns: dealName, stageName, daysInStage, rotDays,
 ** ownerUserId, amount.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.deals.model.Deal;
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
 ** Renders the rotting deals table showing deals past their stage rot threshold.
 *******************************************************************************/
public class CrmRottingDealsRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      Instant now = Instant.now();

      ///////////////////////////////////////////////////////////////
      // query all stages that have a rotDays value set            //
      ///////////////////////////////////////////////////////////////
      QueryOutput stageOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("rotDays", QCriteriaOperator.IS_NOT_BLANK))));

      ///////////////////////////////////////////////////////////////
      // build a map of stageId -> PipelineStage for lookup        //
      ///////////////////////////////////////////////////////////////
      Map<Integer, PipelineStage> stageMap = new HashMap<>();
      for(QRecord stageRecord : stageOutput.getRecords())
      {
         PipelineStage stage = new PipelineStage(stageRecord);
         stageMap.put(stage.getId(), stage);
      }

      if(stageMap.isEmpty())
      {
         //////////////////////////////////////////////////////////
         // no stages with rot days -- return empty table        //
         //////////////////////////////////////////////////////////
         return (buildEmptyOutput());
      }

      ///////////////////////////////////////////////////////////////
      // query deals in those stages                               //
      ///////////////////////////////////////////////////////////////
      QueryOutput dealOutput = new QueryAction().execute(
         new QueryInput(Deal.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("pipelineStageId", QCriteriaOperator.IN, new ArrayList<>(stageMap.keySet())))));

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Deal", "dealName", "2fr", null),
         new TableData.Column("html", "Stage", "stageName", "1fr", null),
         new TableData.Column("html", "Days in Stage", "daysInStage", "1fr", "right"),
         new TableData.Column("html", "Rot Days", "rotDays", "1fr", "right"),
         new TableData.Column("html", "Owner", "ownerUserId", "1fr", null),
         new TableData.Column("html", "Amount", "amount", "1fr", "right")
      );

      //////////////////////////////////////////////////////////
      // filter to only deals past the rot threshold          //
      //////////////////////////////////////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord dealRecord : dealOutput.getRecords())
      {
         Integer stageId = dealRecord.getValueInteger("pipelineStageId");
         PipelineStage stage = stageMap.get(stageId);

         if(stage == null || stage.getRotDays() == null)
         {
            continue;
         }

         Instant stageEnteredDate = dealRecord.getValueInstant("stageEnteredDate");
         if(stageEnteredDate == null)
         {
            continue;
         }

         long daysInStage = ChronoUnit.DAYS.between(stageEnteredDate, now);
         if(daysInStage > stage.getRotDays())
         {
            String dealName    = dealRecord.getValueString("name");
            String ownerUserId = dealRecord.getValueString("ownerUserId");
            BigDecimal amount  = dealRecord.getValueBigDecimal("amount");

            rows.add(MapBuilder.of(
               "dealName", dealName != null ? dealName : "",
               "stageName", stage.getName(),
               "daysInStage", (int) daysInStage,
               "rotDays", stage.getRotDays(),
               "ownerUserId", ownerUserId != null ? ownerUserId : "",
               "amount", amount != null ? amount.toPlainString() : "0"
            ));
         }
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(50)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }



   /***************************************************************************
    ** Build an empty table output when there are no rotting deals.
    ***************************************************************************/
   private RenderWidgetOutput buildEmptyOutput()
   {
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Deal", "dealName", "2fr", null),
         new TableData.Column("html", "Stage", "stageName", "1fr", null),
         new TableData.Column("html", "Days in Stage", "daysInStage", "1fr", "right"),
         new TableData.Column("html", "Rot Days", "rotDays", "1fr", "right"),
         new TableData.Column("html", "Owner", "ownerUserId", "1fr", null),
         new TableData.Column("html", "Amount", "amount", "1fr", "right")
      );

      TableData tableData = new TableData(null, columns, new ArrayList<>())
         .withRowsPerPage(50)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
