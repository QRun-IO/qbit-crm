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


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ParentWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Seeded memory integration tests exercise the actual registered renderers.
 *******************************************************************************/
class CrmHomeWidgetsTest extends BaseTest
{
   /*******************************************************************************
    ** All five Today metrics respect status, time, assignment and decimal values.
    *******************************************************************************/
   @Test
   void today() throws Exception
   {
      seedStages();
      deal(1, 1, "1234.56", "100.25", LocalDate.now(ZoneOffset.UTC), TEST_USER_ID);
      deal(2, 2, "222.22", "222.22", LocalDate.now(ZoneOffset.UTC), TEST_USER_ID);
      deal(3, 3, "9999", "9999", LocalDate.now(ZoneOffset.UTC), TEST_USER_ID);
      seed("crm_contact", new QRecord().withValue("createDate", Instant.now().minus(5, ChronoUnit.DAYS)));
      seed("crm_contact", new QRecord().withValue("createDate", Instant.now().minus(31, ChronoUnit.DAYS)));
      seed("crm_form_submission", new QRecord().withValue("createDate", Instant.now().minus(6, ChronoUnit.DAYS)));
      seed("crm_form_submission", new QRecord().withValue("createDate", Instant.now().minus(8, ChronoUnit.DAYS)));
      activity(TEST_USER_ID, false, Instant.now().minus(1, ChronoUnit.DAYS));
      activity(TEST_USER_ID, true, Instant.now().minus(1, ChronoUnit.DAYS));
      activity("someone-else", false, Instant.now().minus(1, ChronoUnit.DAYS));
      activity(TEST_USER_ID, false, Instant.now().plus(1, ChronoUnit.DAYS));
      assertThat(((ParentWidgetData) render("crm_today")).getChildWidgetNameList()).containsExactlyElementsOf(CrmHomeWidgets.TODAY);
      assertStatistic("crm_open_pipeline", "1234.56");
      assertStatistic("crm_won_month", "222.22");
      assertStatistic("crm_new_contacts", "1");
      assertStatistic("crm_new_web_leads", "1");
      assertStatistic("crm_overdue_activities", "1");
      QContext.getQSession().setIdReference(null);
      assertStatistic("crm_overdue_activities", "0");
   }


   /*******************************************************************************
    ** Whole and fractional amounts retain grouping without artificial decimals.
    *******************************************************************************/
   @Test
   void amountFormatting()
   {
      assertThat(CrmHomeRenderer.formatAmount(new BigDecimal("72000.000000"))).isEqualTo("72,000");
      assertThat(CrmHomeRenderer.formatAmount(new BigDecimal("1234.560000"))).isEqualTo("1,234.56");
      assertThat(CrmHomeRenderer.formatAmount(new BigDecimal("-1234.005"))).isEqualTo("-1,234.005");
      assertThat(CrmHomeRenderer.formatAmount(null)).isEqualTo("0");
   }


   /*******************************************************************************
    ** A direct won-stage insert participates in actual-close reporting immediately.
    *******************************************************************************/
   @Test
   void directWonInsert() throws Exception
   {
      seedStages();
      seed("crm_currency", new QRecord().withValue("currencyCode", "USD").withValue("isBaseCurrency", true));
      var inserted = new com.kingsrook.qqq.backend.core.actions.tables.InsertAction().execute(new InsertInput("crm_deal")
         .withRecord(new QRecord().withValue("name", "Direct win").withValue("pipelineId", 1).withValue("pipelineStageId", 2)
            .withValue("amount", new BigDecimal("72000")).withValue("expectedCloseDate", LocalDate.now().plusYears(1))));
      assertThat(inserted.getRecords().getFirst().getErrors()).isNullOrEmpty();
      var deal = com.kingsrook.qqq.backend.core.actions.tables.GetAction.execute("crm_deal", inserted.getRecords().getFirst().getValue("id"));
      assertThat(deal.getValue("ownerUserId")).isNull();
      assertThat(deal.getValueLocalDate("actualCloseDate")).isEqualTo(LocalDate.now(ZoneOffset.UTC));
      assertThat(deal.getValueInstant("stageEnteredDate")).isNotNull();
      assertThat(((StatisticsData) render("crm_won_month")).getCount()).isEqualTo("USD 72,000");
      TableData leaderboard = (TableData) render("crm_sales_leaderboard");
      assertCell(leaderboard, 0, "first", "Unassigned");
      assertCell(leaderboard, 0, "second", "1");
      assertCell(leaderboard, 0, "third", "72,000");
   }


   /*******************************************************************************
    ** A partial submission name uses the email to retain identifying context.
    *******************************************************************************/
   @Test
   void partialLeadName() throws Exception
   {
      seed("crm_form_submission", new QRecord().withValue("firstName", "Lee").withValue("email", "lee@example.test"));
      assertCell((TableData) render("crm_recent_web_leads"), 0, "first", "lee@example.test");
   }


   /*******************************************************************************
    ** Empty data renders zero statistics, six months and no goals card.
    *******************************************************************************/
   @Test
   void emptyHome() throws Exception
   {
      for(String name : CrmHomeWidgets.TODAY)
      {
         assertStatistic(name, "0");
      }
      for(String name : CrmHomeWidgets.HOME.subList(1, CrmHomeWidgets.HOME.size()).stream().filter(name -> !name.equals("crm_setup")).toList())
      {
         TableData data = (TableData) render(name);
         assertThat(data.getRows()).hasSize(name.equals("crm_deal_forecast") ? 6 : 0);
      }
      assertThat(PermissionsHelper.hasWidgetPermission(new RenderWidgetInput(), "crm_goals")).isFalse();
   }


   /*******************************************************************************
    ** The first active pipeline is selected, empty stages remain, currencies sum.
    *******************************************************************************/
   @Test
   void pipeline() throws Exception
   {
      seed("crm_pipeline", new QRecord().withValue("id", 0).withValue("name", "Inactive").withValue("isActive", false));
      seedStages();
      seed("crm_pipeline", new QRecord().withValue("id", 2).withValue("isActive", true));
      seed("crm_pipeline_stage", new QRecord().withValue("id", 4).withValue("pipelineId", 2).withValue("name", "Other"));
      seed("crm_currency", new QRecord().withValue("currencyCode", "EUR").withValue("exchangeRateToBase", new BigDecimal("2")));
      deal(1, 1, "100.00", "20.50", LocalDate.now(), TEST_USER_ID);
      seed("crm_deal", new QRecord().withValue("pipelineId", 1).withValue("pipelineStageId", 1).withValue("currencyCode", "EUR")
         .withValue("amountInBaseCurrency", new BigDecimal("200")).withValue("weightedAmount", new BigDecimal("10")));
      seed("crm_deal", new QRecord().withValue("pipelineId", 2).withValue("pipelineStageId", 4).withValue("amountInBaseCurrency", new BigDecimal("9999")));
      TableData data = (TableData) render("crm_pipeline_summary");
      assertThat(data.getColumns()).hasSize(3);
      assertThat(data.getRows()).hasSize(3);
      assertCell(data, 0, "first", "Open");
      assertCell(data, 0, "second", "2");
      assertCell(data, 0, "third", "40.5");
      assertCell(data, 1, "first", "Won");
      assertThat(data.getDropdownNameList()).containsExactly("pipelineId");
      RenderWidgetInput selected = new RenderWidgetInput().withWidgetMetaData(QContext.getQInstance().getWidget("crm_pipeline_summary"));
      selected.addQueryParam("pipelineId", "2");
      TableData other = (TableData) new CrmHomeRenderer().render(selected).getWidgetData();
      assertThat(other.getRows()).hasSize(1);
      assertCell(other, 0, "first", "Other");
      selected.addQueryParam("pipelineId", "999");
      assertThat(((TableData) new CrmHomeRenderer().render(selected).getWidgetData()).getRows()).hasSize(3);
      assertLinks(data);
   }


   /*******************************************************************************
    ** Forecast includes month five, excludes month six and closed deals.
    *******************************************************************************/
   @Test
   void forecast() throws Exception
   {
      seedStages();
      LocalDate month = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
      deal(1, 1, "100", "25.25", month, TEST_USER_ID);
      deal(2, 1, "100", "75.75", month.plusMonths(5), TEST_USER_ID);
      deal(3, 1, "100", "99", month.plusMonths(6), TEST_USER_ID);
      deal(4, 2, "100", "99", month, TEST_USER_ID);
      TableData data = (TableData) render("crm_deal_forecast");
      assertThat(data.getRows()).hasSize(6);
      assertCell(data, 0, "second", "1");
      assertCell(data, 0, "third", "25.25");
      assertCell(data, 1, "second", "0");
      assertCell(data, 5, "third", "75.75");
      assertLinks(data);
   }


   /*******************************************************************************
    ** Rotting deals are old enough, open, sorted oldest first and capped at ten.
    *******************************************************************************/
   @Test
   void needsAttention() throws Exception
   {
      seedStages();
      for(int i = 1; i <= 12; i++)
      {
         seed("crm_deal", new QRecord().withValue("name", "Deal " + i).withValue("pipelineStageId", 1)
            .withValue("stageEnteredDate", Instant.now().minus(i + 10, ChronoUnit.DAYS)));
      }
      seed("crm_deal", new QRecord().withValue("name", "Fresh").withValue("pipelineStageId", 1).withValue("stageEnteredDate", Instant.now()));
      seed("crm_deal", new QRecord().withValue("name", "Won").withValue("pipelineStageId", 2).withValue("stageEnteredDate", Instant.now().minus(90, ChronoUnit.DAYS)));
      TableData data = (TableData) render("crm_rotting_deals");
      assertThat(data.getRows()).hasSize(10);
      assertCell(data, 0, "first", "Deal 12");
      assertCell(data, 0, "second", "Open");
      assertCell(data, 0, "third", "22");
      assertLinks(data);
   }


   /*******************************************************************************
    ** Recent leads have enum labels, newest-ten limits and safe record links.
    *******************************************************************************/
   @Test
   void recentWebLeads() throws Exception
   {
      for(int i = 1; i <= 12; i++)
      {
         seed("crm_form_submission", new QRecord().withValue("id", i).withValue("email", "lead" + i + "@example.test")
            .withValue("formType", 2).withValue("conversionStatus", 3).withValue("createDate", Instant.now().minus(i, ChronoUnit.DAYS)));
      }
      seed("crm_form_submission", new QRecord().withValue("firstName", "<script>").withValue("lastName", "A&B")
         .withValue("createDate", Instant.now()));
      TableData data = (TableData) render("crm_recent_web_leads");
      assertThat(data.getRows()).hasSize(10);
      assertCell(data, 0, "first", "&lt;script&gt; A&amp;B");
      assertCell(data, 1, "first", "lead1@example.test");
      assertCell(data, 1, "second", "Demo Request");
      assertCell(data, 1, "third", "Spam");
      assertLinks(data);
   }


   /*******************************************************************************
    ** Each current goal uses its owner, inclusive period and optional pipeline.
    *******************************************************************************/
   @Test
   void goals() throws Exception
   {
      seedStages();
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      deal(1, 2, "250.25", "250.25", today, TEST_USER_ID);
      deal(2, 2, "900", "900", today, "other");
      seed("crm_sales_goal", new QRecord().withValue("userId", TEST_USER_ID).withValue("pipelineId", 1)
         .withValue("periodStart", today).withValue("periodEnd", today).withValue("targetAmount", new BigDecimal("500.50")));
      seed("crm_sales_goal", new QRecord().withValue("userId", "other").withValue("periodStart", today.minusDays(5))
         .withValue("periodEnd", today.minusDays(1)).withValue("targetAmount", new BigDecimal("1")));
      TableData data = (TableData) render("crm_goals");
      assertThat(PermissionsHelper.hasWidgetPermission(new RenderWidgetInput(), "crm_goals")).isTrue();
      QContext.getQSession().setPermissions(java.util.Set.of());
      assertThat(PermissionsHelper.hasWidgetPermission(new RenderWidgetInput(), "crm_goals")).isFalse();
      assertThat(data.getRows()).hasSize(1);
      assertCell(data, 0, "first", TEST_USER_ID + " (50.0%)");
      assertCell(data, 0, "second", "500.5");
      assertCell(data, 0, "third", "250.25");
      assertLinks(data);
   }


   /*******************************************************************************
    ** Quarter boundaries, decimal revenue, owner grouping and ordering.
    *******************************************************************************/
   @Test
   void leaderboard() throws Exception
   {
      seedStages();
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      LocalDate quarter = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1);
      deal(1, 2, "125.50", "125.50", today, "Ada");
      deal(2, 2, "125.50", "125.50", quarter, "Ada");
      deal(3, 2, "10", "10", today, "Bob");
      deal(4, 2, "9999", "9999", quarter.minusDays(1), "Bob");
      deal(5, 1, "9999", "9999", today, "Bob");
      TableData data = (TableData) render("crm_sales_leaderboard");
      assertThat(data.getRows()).hasSize(2);
      assertCell(data, 0, "first", "Ada");
      assertCell(data, 0, "second", "2");
      assertCell(data, 0, "third", "251");
      assertLinks(data);
   }


   /*******************************************************************************
    ** Record locks apply to aggregate totals and ordinary table queries alike.
    *******************************************************************************/
   @Test
   void sessionLocks() throws Exception
   {
      seedStages();
      seed("crm_deal", new QRecord().withValue("id", 1).withValue("pipelineStageId", 1).withValue("ownerUserId", TEST_USER_ID)
         .withValue("amountInBaseCurrency", new BigDecimal("12.34")).withValue("stageEnteredDate", Instant.now().minus(30, ChronoUnit.DAYS)));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("owner"));
      QContext.getQInstance().getTable("crm_deal").withRecordSecurityLock(new RecordSecurityLock()
         .withFieldName("ownerUserId").withSecurityKeyType("owner").withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      assertStatistic("crm_open_pipeline", "0");
      assertThat(((TableData) render("crm_rotting_deals")).getRows()).isEmpty();
      QContext.getQSession().withSecurityKeyValue("owner", TEST_USER_ID);
      assertStatistic("crm_open_pipeline", "12.34");
      assertThat(((TableData) render("crm_rotting_deals")).getRows()).hasSize(1);
   }


   /*******************************************************************************
    ** Seed stable stage IDs without invoking unrelated business customizers.
    *******************************************************************************/
   private void seedStages() throws QException
   {
      seed("crm_pipeline", new QRecord().withValue("id", 1).withValue("isActive", true));
      for(int id = 1; id <= 3; id++)
      {
         seed("crm_pipeline_stage", new QRecord().withValue("id", id).withValue("pipelineId", 1)
            .withValue("name", List.of("Open", "Won", "Lost").get(id - 1)).withValue("sortOrder", id)
            .withValue("isClosedWon", id == 2).withValue("isClosedLost", id == 3).withValue("rotDays", 5));
      }
   }


   /*******************************************************************************
    ** Direct fixtures preserve dates and computed amounts under test.
    *******************************************************************************/
   static void seed(String table, QRecord record) throws QException
   {
      MemoryRecordStore.getInstance().insert(new InsertInput(table).withRecord(record), true);
   }


   /*******************************************************************************
    ** A deal with both expected and actual close dates for boundary tests.
    *******************************************************************************/
   private void deal(int id, int stage, String amount, String weighted, LocalDate date, String owner) throws QException
   {
      seed("crm_deal", new QRecord().withValue("id", id).withValue("name", "Deal " + id).withValue("pipelineId", 1)
         .withValue("pipelineStageId", stage).withValue("amountInBaseCurrency", new BigDecimal(amount))
         .withValue("weightedAmount", new BigDecimal(weighted)).withValue("actualCloseDate", date).withValue("expectedCloseDate", date)
         .withValue("ownerUserId", owner));
   }


   /*******************************************************************************
    ** Activity fixture.
    *******************************************************************************/
   private void activity(String owner, boolean completed, Instant due) throws QException
   {
      seed("crm_activity", new QRecord().withValue("assignedToUserId", owner).withValue("isCompleted", completed).withValue("dueDate", due));
   }


   /*******************************************************************************
    ** Exercise metadata lookup and the standard render action.
    *******************************************************************************/
   static QWidgetData render(String name) throws QException
   {
      return (new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(QContext.getQInstance().getWidget(name))).getWidgetData());
   }


   /*******************************************************************************
    ** Assert decimal statistics exactly, independent of BigDecimal scale.
    *******************************************************************************/
   private void assertStatistic(String name, String expected) throws QException
   {
      assertThat(new BigDecimal(((StatisticsData) render(name)).getCount().toString().replace(",", ""))).isEqualByComparingTo(expected);
   }


   /*******************************************************************************
    ** Standard HTML cell with one linked label.
    *******************************************************************************/
   static void assertCell(TableData data, int index, String column, String text)
   {
      assertThat(data.getRows().get(index).get(column).toString()).endsWith(">" + text + "</a>");
   }


   /*******************************************************************************
    ** Every visible cell links to a record or a filtered record list.
    *******************************************************************************/
   private void assertLinks(TableData data)
   {
      for(Map<String, Object> row : data.getRows())
      {
         assertThat(row.values()).allSatisfy(value -> assertThat(value.toString()).startsWith("<a href=\"/app/crm_").endsWith("</a>"));
      }
   }
}
