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


package com.kingsrook.qbits.crm.widgets;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
import com.kingsrook.qbits.crm.sync.CrmCurrencyUtils;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ParentWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 ** Daily CRM views. Core actions retain the current session and record locks.
 ** Monetary totals use base currency; weighted amounts are grouped by currency
 ** before conversion. No query loads all deals, contacts or submissions.
 *******************************************************************************/
public class CrmHomeRenderer extends AbstractWidgetRenderer
{
   private static final String DEAL = "crm_deal";
   private static final String STAGE = "crm_pipeline_stage";
   private static final Aggregate COUNT = new Aggregate("id", AggregateOperator.COUNT);
   private static final Aggregate AMOUNT = new Aggregate("amountInBaseCurrency", AggregateOperator.SUM);
   private static final Aggregate WEIGHTED = new Aggregate("weightedAmount", AggregateOperator.SUM);
   private static final GroupBy CURRENCY = new GroupBy(QFieldType.STRING, "currencyCode");


   /*******************************************************************************
    ** Render only the requested home component using UTC reporting boundaries.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      switch(input.getWidgetMetaData().getName())
      {
         case "crm_today":
            return (new RenderWidgetOutput(new ParentWidgetData().withChildWidgetNameList(CrmHomeWidgets.TODAY).withLabel("Today")));
         case "crm_open_pipeline":
            return (new RenderWidgetOutput(statistic(sum(DEAL, dealStatus(false), AMOUNT), true)));
         case "crm_won_month":
            return (new RenderWidgetOutput(statistic(sum(DEAL, period(dealStatus(true), "actualCloseDate", today.withDayOfMonth(1), today.plusDays(1)), AMOUNT), true)));
         case "crm_new_contacts":
            return (new RenderWidgetOutput(statistic(countSince("crm_contact", 30), false)));
         case "crm_new_web_leads":
            return (new RenderWidgetOutput(statistic(countSince("crm_form_submission", 7), false)));
         case "crm_overdue_activities":
            return (new RenderWidgetOutput(overdue()));
         case "crm_pipeline_summary":
            return (new RenderWidgetOutput(pipeline(input)));
         case "crm_deal_forecast":
            return (new RenderWidgetOutput(forecast(today)));
         case "crm_rotting_deals":
            return (new RenderWidgetOutput(rotting()));
         case "crm_recent_web_leads":
            return (new RenderWidgetOutput(webLeads()));
         case "crm_goals":
            return (new RenderWidgetOutput(goals(today)));
         case "crm_sales_leaderboard":
            return (new RenderWidgetOutput(leaderboard(today)));
         default:
            throw new QException("Unknown CRM home widget: " + input.getWidgetMetaData().getName());
      }
   }


   /*******************************************************************************
    ** Preserve decimals and large counts in the standard statistics renderer.
    *******************************************************************************/
   private StatisticsData statistic(BigDecimal value, boolean currency)
   {
      return (new StatisticsData().withCount(value).withCountContext(currency ? "base currency" : null));
   }


   /*******************************************************************************
    ** Rolling counts include the present instant and exclude future records.
    *******************************************************************************/
   private BigDecimal countSince(String table, int days) throws QException
   {
      Instant now = Instant.now();
      return (sum(table, period(new QQueryFilter(), "createDate", now.minus(days, ChronoUnit.DAYS), now), COUNT));
   }


   /*******************************************************************************
    ** An unassigned activity is never treated as belonging to an anonymous user.
    *******************************************************************************/
   private StatisticsData overdue() throws QException
   {
      String user = QContext.getQSession().getIdReference();
      if(user == null || user.isBlank())
      {
         return (statistic(BigDecimal.ZERO, false));
      }
      QQueryFilter filter = equalsFilter("assignedToUserId", user)
         .withCriteria(new QFilterCriteria("isCompleted", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true))
         .withCriteria(new QFilterCriteria("dueDate", QCriteriaOperator.LESS_THAN, Instant.now()));
      return (statistic(sum("crm_activity", filter, COUNT), false));
   }


   /*******************************************************************************
    ** Open and won stages are resolved through the current session as well.
    *******************************************************************************/
   private QQueryFilter dealStatus(boolean won) throws QException
   {
      QQueryFilter filter = won ? equalsFilter("isClosedWon", true) : new QQueryFilter()
         .withCriteria(new QFilterCriteria("isClosedWon", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true))
         .withCriteria(new QFilterCriteria("isClosedLost", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true));
      List<Integer> ids = allStages(filter).stream().map(row -> row.getValueInteger("id")).toList();
      return (new QQueryFilter().withCriteria(new QFilterCriteria("pipelineStageId", QCriteriaOperator.IN, ids)));
   }


   /*******************************************************************************
    ** Stage dictionaries are paged; transactional facts are aggregated in SQL.
    *******************************************************************************/
   private List<QRecord> allStages(QQueryFilter filter) throws QException
   {
      return (dictionary(STAGE, filter));
   }


   /*******************************************************************************
    ** Read small settings dictionaries in deterministic keyset pages.
    *******************************************************************************/
   private List<QRecord> dictionary(String table, QQueryFilter filter) throws QException
   {
      List<QRecord> stages = new ArrayList<>();
      int lastId = 0;
      while(true)
      {
         QQueryFilter page = filter.clone().withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId));
         List<QRecord> records = query(table, page, "id", true, 200);
         stages.addAll(records);
         if(records.size() < 200)
         {
            return (stages);
         }
         lastId = records.getLast().getValueInteger("id");
      }
   }


   /*******************************************************************************
    ** Stage and deal count share one column, keeping both monetary columns.
    *******************************************************************************/
   private TableData pipeline(RenderWidgetInput input) throws QException
   {
      List<QRecord> pipelines = dictionary("crm_pipeline", equalsFilter("isActive", true));
      List<Map<String, Object>> rows = new ArrayList<>();
      String selected = pipelines.isEmpty() ? null : pipelines.getFirst().getValueString("id");
      String requested = input.getQueryParams().get("pipelineId");
      if(pipelines.stream().anyMatch(pipeline -> pipeline.getValueString("id").equals(requested)))
      {
         selected = requested;
      }
      if(!pipelines.isEmpty())
      {
         Integer pipelineId = Integer.valueOf(selected);
         List<QRecord> stages = allStages(equalsFilter("pipelineId", pipelineId));
         stages.sort(Comparator.comparing(row -> Objects.requireNonNullElse(row.getValueInteger("sortOrder"), 0)));
         GroupBy stageGroup = new GroupBy(QFieldType.INTEGER, "pipelineStageId");
         Map<Serializable, BigDecimal[]> totals = groupedTotals(equalsFilter("pipelineId", pipelineId), stageGroup);
         for(QRecord stage : stages)
         {
            BigDecimal[] values = totals.getOrDefault(stage.getValue("id"), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            String href = AiLinkBuilder.recordHref(STAGE, stage.getValue("id"));
            rows.add(row(href, stage.getValueString("name") + " (" + values[0].toPlainString() + " deals)", values[1], values[2]));
         }
      }
      TableData data = table(rows, "Stage / deals", "Amount (base)", "Weighted amount (base)");
      if(pipelines.size() > 1)
      {
         data.withDropdownNameList(List.of("pipelineId")).withDropdownLabelList(List.of("Pipeline"))
            .withDropdownDefaultValueList(List.of(selected))
            .withDropdownDataList(List.of(pipelines.stream().map(pipeline -> Map.of("id", pipeline.getValueString("id"),
               "label", Objects.toString(pipeline.getValue("name"), "Pipeline " + pipeline.getValueString("id")))).toList()));
      }
      return (data);
   }


   /*******************************************************************************
    ** Six calendar buckets, including the current month, with empty months kept.
    *******************************************************************************/
   private TableData forecast(LocalDate today) throws QException
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      for(int month = 0; month < 6; month++)
      {
         LocalDate start = today.withDayOfMonth(1).plusMonths(month);
         QQueryFilter filter = period(dealStatus(false), "expectedCloseDate", start, start.plusMonths(1));
         BigDecimal[] totals = totals(filter);
         rows.add(row(AiLinkBuilder.filteredTableHref(DEAL, filter), start.toString().substring(0, 7), totals[0], totals[2]));
      }
      return (table(rows, "Month", "Deals", "Weighted amount (base)"));
   }


   /*******************************************************************************
    ** Limit the candidates in each stage in SQL, then select the oldest ten.
    *******************************************************************************/
   private TableData rotting() throws QException
   {
      List<QRecord> candidates = new ArrayList<>();
      Map<Integer, String> stages = new LinkedHashMap<>();
      Instant now = Instant.now();
      QQueryFilter stageFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("isClosedWon", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true))
         .withCriteria(new QFilterCriteria("isClosedLost", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true))
         .withCriteria(new QFilterCriteria("rotDays", QCriteriaOperator.GREATER_THAN, 0));
      for(QRecord stage : allStages(stageFilter))
      {
         Integer id = stage.getValueInteger("id");
         stages.put(id, stage.getValueString("name"));
         QQueryFilter filter = equalsFilter("pipelineStageId", id)
            .withCriteria(new QFilterCriteria("stageEnteredDate", QCriteriaOperator.LESS_THAN, now.minus(stage.getValueInteger("rotDays"), ChronoUnit.DAYS)));
         candidates.addAll(query(DEAL, filter, "stageEnteredDate", true, 10));
      }
      candidates.sort(Comparator.comparing(row -> row.getValueInstant("stageEnteredDate")));
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord deal : candidates.stream().limit(10).toList())
      {
         rows.add(row(AiLinkBuilder.recordHref(DEAL, deal.getValue("id")), deal.getValueString("name"),
            stages.get(deal.getValueInteger("pipelineStageId")), ChronoUnit.DAYS.between(deal.getValueInstant("stageEnteredDate"), now)));
      }
      return (table(rows, "Deal", "Stage", "Days in stage"));
   }


   /*******************************************************************************
    ** Latest ten submissions, with escaped labels and links on every cell.
    *******************************************************************************/
   private TableData webLeads() throws QException
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord lead : query("crm_form_submission", new QQueryFilter(), "createDate", false, 10))
      {
         String name = (Objects.toString(lead.getValue("firstName"), "") + " " + Objects.toString(lead.getValue("lastName"), "")).trim();
         if(name.isBlank())
         {
            name = Objects.toString(lead.getValue("email"), "Web lead");
         }
         String form = "Unknown";
         for(CrmFormType value : CrmFormType.values())
         {
            if(value.getId().equals(lead.getValueInteger("formType")))
            {
               form = value.getLabel();
            }
         }
         String status = "New";
         for(CrmFormConversionStatus value : CrmFormConversionStatus.values())
         {
            if(value.getId().equals(lead.getValueInteger("conversionStatus")))
            {
               status = value.getLabel();
            }
         }
         rows.add(row(AiLinkBuilder.recordHref("crm_form_submission", lead.getValue("id")), name, form, status));
      }
      return (table(rows, "Name or email", "Form", "Status"));
   }


   /*******************************************************************************
    ** Current goals only; metadata visibility hides the widget when none exist.
    *******************************************************************************/
   private TableData goals(LocalDate today) throws QException
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("periodStart", QCriteriaOperator.LESS_THAN_OR_EQUALS, today))
         .withCriteria(new QFilterCriteria("periodEnd", QCriteriaOperator.GREATER_THAN_OR_EQUALS, today));
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord goal : query("crm_sales_goal", filter, "id", true, 50))
      {
         QQueryFilter deals = period(dealStatus(true), "actualCloseDate", goal.getValueLocalDate("periodStart"), goal.getValueLocalDate("periodEnd").plusDays(1));
         deals.withCriteria(new QFilterCriteria("ownerUserId", QCriteriaOperator.EQUALS, goal.getValue("userId")));
         if(goal.getValue("pipelineId") != null)
         {
            deals.withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, goal.getValue("pipelineId")));
         }
         BigDecimal actual = sum(DEAL, deals, AMOUNT);
         BigDecimal target = CrmCurrencyUtils.convertToBaseCurrency(goal.getValueBigDecimal("targetAmount"), goal.getValueString("currencyCode"));
         String attainment = target != null && target.signum() > 0 ? actual.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP) + "%" : "No target";
         rows.add(row(AiLinkBuilder.recordHref("crm_sales_goal", goal.getValue("id")),
            goal.getValueString("userId") + " (" + attainment + ")", target, actual));
      }
      TableData data = table(rows, "Owner / attainment", "Target (base)", "Won (base)");
      return (data);
   }


   /*******************************************************************************
    ** Quarter-to-date won revenue grouped by owner, linking to matching deals.
    *******************************************************************************/
   private TableData leaderboard(LocalDate today) throws QException
   {
      LocalDate start = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1);
      QQueryFilter filter = period(dealStatus(true), "actualCloseDate", start, today.plusDays(1));
      GroupBy owner = new GroupBy(QFieldType.STRING, "ownerUserId");
      List<AggregateResult> results = aggregate(DEAL, filter, List.of(COUNT, AMOUNT), List.of(owner));
      results.sort(Comparator.comparing((AggregateResult result) -> number(result.getAggregateValue(AMOUNT))).reversed());
      List<Map<String, Object>> rows = new ArrayList<>();
      for(AggregateResult result : results)
      {
         Serializable user = result.getGroupByValue(owner);
         QQueryFilter ownerFilter = filter.clone().withCriteria(new QFilterCriteria("ownerUserId", user == null ? QCriteriaOperator.IS_BLANK : QCriteriaOperator.EQUALS, user));
         rows.add(row(AiLinkBuilder.filteredTableHref(DEAL, ownerFilter), Objects.toString(user, "Unassigned"),
            number(result.getAggregateValue(COUNT)), number(result.getAggregateValue(AMOUNT))));
      }
      return (table(rows, "Owner", "Won deals", "Won amount (base)"));
   }


   /*******************************************************************************
    ** Aggregate currency groups instead of loading the underlying deals.
    *******************************************************************************/
   private Map<Serializable, BigDecimal[]> groupedTotals(QQueryFilter filter, GroupBy group) throws QException
   {
      List<GroupBy> groups = new ArrayList<>(List.of(CURRENCY));
      if(group != null)
      {
         groups.add(group);
      }
      Map<Serializable, BigDecimal[]> totals = new LinkedHashMap<>();
      for(AggregateResult result : aggregate(DEAL, filter, List.of(COUNT, AMOUNT, WEIGHTED), groups))
      {
         Serializable key = group == null ? "total" : result.getGroupByValue(group);
         BigDecimal[] values = totals.computeIfAbsent(key, ignored -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
         values[0] = values[0].add(number(result.getAggregateValue(COUNT)));
         values[1] = values[1].add(number(result.getAggregateValue(AMOUNT)));
         values[2] = values[2].add(CrmCurrencyUtils.convertToBaseCurrency(number(result.getAggregateValue(WEIGHTED)), (String) result.getGroupByValue(CURRENCY)));
      }
      return (totals);
   }


   /*******************************************************************************
    ** Totals across all currency buckets.
    *******************************************************************************/
   private BigDecimal[] totals(QQueryFilter filter) throws QException
   {
      return (groupedTotals(filter, null).getOrDefault("total", new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO}));
   }


   /*******************************************************************************
    ** A scalar aggregate with an explicit zero for an empty selection.
    *******************************************************************************/
   private BigDecimal sum(String table, QQueryFilter filter, Aggregate aggregate) throws QException
   {
      List<AggregateResult> results = aggregate(table, filter, List.of(aggregate), List.of());
      return (results.isEmpty() ? BigDecimal.ZERO : number(results.getFirst().getAggregateValue(aggregate)));
   }


   /*******************************************************************************
    ** Delegate all permission and record-lock filtering to the core action.
    *******************************************************************************/
   private List<AggregateResult> aggregate(String table, QQueryFilter filter, List<Aggregate> aggregates, List<GroupBy> groups) throws QException
   {
      return (new AggregateAction().execute(new AggregateInput(table).withFilter(filter).withAggregates(aggregates).withGroupBys(groups)).getResults());
   }


   /*******************************************************************************
    ** Normalize aggregate values without lossy integer conversions.
    *******************************************************************************/
   private BigDecimal number(Serializable value)
   {
      return (value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()));
   }


   /*******************************************************************************
    ** A bounded query with deterministic ordering for tied timestamps.
    *******************************************************************************/
   static List<QRecord> query(String table, QQueryFilter filter, String order, boolean ascending, int limit) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(filter.withOrderBy(new QFilterOrderBy(order, ascending))
         .withOrderBy(new QFilterOrderBy("id", ascending)).withLimit(limit))).getRecords());
   }


   /*******************************************************************************
    ** Equality filter.
    *******************************************************************************/
   static QQueryFilter equalsFilter(String field, Serializable value)
   {
      return (new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value)));
   }


   /*******************************************************************************
    ** Half-open reporting interval, avoiding end-of-month time rounding.
    *******************************************************************************/
   private QQueryFilter period(QQueryFilter filter, String field, Serializable start, Serializable end)
   {
      return (filter.withCriteria(new QFilterCriteria(field, QCriteriaOperator.GREATER_THAN_OR_EQUALS, start))
         .withCriteria(new QFilterCriteria(field, QCriteriaOperator.LESS_THAN, end)));
   }


   /*******************************************************************************
    ** Every cell navigates to its record, or to the records in an aggregate.
    *******************************************************************************/
   static Map<String, Object> row(String href, Object first, Object second, Object third)
   {
      return (Map.of("first", link(href, first), "second", link(href, second), "third", link(href, third)));
   }


   /*******************************************************************************
    ** Standard table cells may contain links; user text is always escaped.
    *******************************************************************************/
   private static String link(String href, Object label)
   {
      return ("<a href=\"" + escape(href) + "\">" + escape(Objects.toString(label, "")) + "</a>");
   }


   /*******************************************************************************
    ** Escape text and attributes in links.
    *******************************************************************************/
   private static String escape(String value)
   {
      return (value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"));
   }


   /*******************************************************************************
    ** Three columns, full-width metadata, and the standard paginated table.
    *******************************************************************************/
   static TableData table(List<Map<String, Object>> rows, String first, String second, String third)
   {
      return (new TableData(null, List.of(new TableData.Column("html", first, "first", "2fr", "left"),
         new TableData.Column("html", second, "second", "1fr", "left"), new TableData.Column("html", third, "third", "1fr", "left")), rows)
         .withRowsPerPage(10).withHidePaginationDropdown(true));
   }
}
