/*******************************************************************************
 ** Renderer for the Win Rate widget.
 **
 ** Queries DealStageHistory for transitions to closedWon and closedLost stages,
 ** groups by month, and computes win rate percentage.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
 ** Renders win rate analytics as a table widget.
 *******************************************************************************/
public class CrmWinRateRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ///////////////////////////////////////////////////////////
      // find closedWon and closedLost stage IDs               //
      ///////////////////////////////////////////////////////////
      Set<Integer> closedWonStageIds  = new HashSet<>();
      Set<Integer> closedLostStageIds = new HashSet<>();

      QueryOutput stageOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME));

      for(QRecord record : stageOutput.getRecords())
      {
         if(Boolean.TRUE.equals(record.getValueBoolean("isClosedWon")))
         {
            closedWonStageIds.add(record.getValueInteger("id"));
         }
         if(Boolean.TRUE.equals(record.getValueBoolean("isClosedLost")))
         {
            closedLostStageIds.add(record.getValueInteger("id"));
         }
      }

      ///////////////////////////////////////////////////////////
      // query all deal stage history                          //
      ///////////////////////////////////////////////////////////
      QueryOutput historyOutput = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME));

      ///////////////////////////////////////////////////////////
      // group by month                                        //
      ///////////////////////////////////////////////////////////
      Map<YearMonth, int[]> monthData = new TreeMap<>(); // [won, lost]

      for(QRecord record : historyOutput.getRecords())
      {
         Integer toStageId      = record.getValueInteger("toStageId");
         Instant transitionDate = record.getValueInstant("transitionDate");

         if(toStageId == null || transitionDate == null)
         {
            continue;
         }

         boolean isWon  = closedWonStageIds.contains(toStageId);
         boolean isLost = closedLostStageIds.contains(toStageId);

         if(!isWon && !isLost)
         {
            continue;
         }

         YearMonth month = YearMonth.from(transitionDate.atZone(ZoneId.systemDefault()));
         int[] counts = monthData.computeIfAbsent(month, k -> new int[2]);
         if(isWon)
         {
            counts[0]++;
         }
         if(isLost)
         {
            counts[1]++;
         }
      }

      ///////////////////////////
      // build table columns   //
      ///////////////////////////
      List<TableData.Column> columns = List.of(
         new TableData.Column("html", "Month", "month", "1fr", null),
         new TableData.Column("html", "Won", "won", "1fr", "right"),
         new TableData.Column("html", "Lost", "lost", "1fr", "right"),
         new TableData.Column("html", "Win Rate %", "winRate", "1fr", "right")
      );

      //////////////////////////
      // build table rows     //
      //////////////////////////
      List<Map<String, Object>> rows = new ArrayList<>();
      for(Map.Entry<YearMonth, int[]> entry : monthData.entrySet())
      {
         int won   = entry.getValue()[0];
         int lost  = entry.getValue()[1];
         int total = won + lost;

         BigDecimal winRate = total > 0
            ? BigDecimal.valueOf(won).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

         rows.add(MapBuilder.of(
            "month", entry.getKey().toString(),
            "won", won,
            "lost", lost,
            "winRate", winRate + "%"
         ));
      }

      TableData tableData = new TableData(null, columns, rows)
         .withRowsPerPage(25)
         .withHidePaginationDropdown(true);

      return (new RenderWidgetOutput(tableData));
   }

}
