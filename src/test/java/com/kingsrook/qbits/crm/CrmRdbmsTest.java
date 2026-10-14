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


package com.kingsrook.qbits.crm;


import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmFormRequest;
import com.kingsrook.qbits.crm.api.CrmIntakeApi;
import com.kingsrook.qbits.crm.api.CrmMarketingApi;
import com.kingsrook.qbits.crm.api.FormType;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Real QQQ RDBMS operations over the shipped PostgreSQL DDL, using H2 in
 ** PostgreSQL mode. No memory store or replacement SQL schema is involved.
 *******************************************************************************/
class CrmRdbmsTest
{
   private Connection connection;
   private QInstance qInstance;



   /*******************************************************************************
    ** Create the exact reference schema and register the RDBMS-backed QBit.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      String url = "jdbc:h2:mem:crm_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
      connection = DriverManager.getConnection(url, "sa", "");
      executeSql(Files.readString(Path.of("docs/ddl/postgres.sql")));
      qInstance = new QInstance();
      qInstance.addBackend(new RDBMSBackendMetaData().withName("rdbms").withVendor("h2")
         .withJdbcUrl(url).withUsername("sa").withPassword(""));
      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      qInstance.setEnforcePermissionsInCoreActions(true);
      new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables("rdbms"))
         .produce(qInstance).addSelfToInstance(qInstance);
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession().withPermissions(CrmPermissions.ALL));
   }



   /*******************************************************************************
    ** Release the database and request context even when an assertion fails.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
      if(connection != null)
      {
         try(Statement statement = connection.createStatement())
         {
            statement.execute("SHUTDOWN");
         }
         finally
         {
            connection.close();
         }
      }
   }



   /*******************************************************************************
    ** Persist the seven required entity types through QQQ, including customizers.
    *******************************************************************************/
   @Test
   void testPersistAndQueryCrmRecords() throws Exception
   {
      Integer company = insert("crm_company", new QRecord().withValue("name", "Example Company").withValue("ownerUserId", "rep"));
      Integer contact = insert("crm_contact", new QRecord().withValue("firstName", "Ada").withValue("lastName", "Lovelace")
         .withValue("email", "ada@example.test").withValue("ownerUserId", "rep").withValue("companyId", company));
      Integer pipeline = insert("crm_pipeline", new QRecord().withValue("name", "Sales"));
      Integer stage = insert("crm_pipeline_stage", new QRecord().withValue("name", "Qualified").withValue("pipelineId", pipeline)
         .withValue("sortOrder", 1).withValue("probabilityPct", 20));
      Integer deal = insert("crm_deal", new QRecord().withValue("name", "First sale").withValue("ownerUserId", "rep")
         .withValue("companyId", company).withValue("primaryContactId", contact).withValue("pipelineId", pipeline)
         .withValue("pipelineStageId", stage).withValue("amount", new BigDecimal("123.45")));
      Integer activityType = insert("crm_activity_type", new QRecord().withValue("name", "Call").withValue("sortOrder", 1));
      Integer activity = insert("crm_activity", new QRecord().withValue("activityTypeId", activityType).withValue("subject", "Discovery call")
         .withValue("ownerUserId", "rep").withValue("contactId", contact).withValue("companyId", company).withValue("dealId", deal));
      Integer submission = insert("crm_form_submission", new QRecord().withValue("formKey", "test-form").withValue("formType", 1).withValue("email", "ada@example.test")
         .withValue("firstName", "Ada").withValue("lastName", "Lovelace"));
      Integer list = insert("crm_email_list", new QRecord().withValue("name", "Newsletter").withValue("listKey", "newsletter").withValue("slug", "newsletter").withValue("sortOrder", 1));
      Integer member = insert("crm_email_list_member", new QRecord().withValue("emailListId", list).withValue("contactId", contact)
         .withValue("email", "ada@example.test").withValue("subscribedDate", Instant.parse("2026-10-14T12:00:00Z")).withValue("formSubmissionId", submission));
      Integer consent = insert("crm_consent_record", new QRecord().withValue("contactId", contact).withValue("consentType", 1)
         .withValue("status", 1).withValue("consentDate", Instant.parse("2026-10-14T12:00:00Z")).withValue("source", "RDBMS test"));

      assertThat(query("crm_company", company).getValueString("name")).isEqualTo("Example Company");
      assertThat(query("crm_contact", contact).getValueInteger("companyId")).isEqualTo(company);
      assertThat(query("crm_deal", deal).getValueBigDecimal("amount")).isEqualByComparingTo("123.45");
      assertThat(query("crm_deal", deal).getValueBigDecimal("weightedAmount")).isEqualByComparingTo("24.69");
      assertThat(query("crm_activity", activity).getValueInteger("dealId")).isEqualTo(deal);
      assertThat(query("crm_form_submission", submission).getValueInteger("contactId")).isEqualTo(contact);
      assertThat(query("crm_email_list_member", member).getValueInteger("formSubmissionId")).isEqualTo(submission);
      assertThat(query("crm_consent_record", consent).getValueString("source")).isEqualTo("RDBMS test");

      try(Statement statement = connection.createStatement())
      {
         SQLException duplicate = assertThrows(SQLException.class, () -> statement.executeUpdate(
            "INSERT INTO crm_email_list_member (email_list_id, email, status, subscribed_date) "
               + "SELECT email_list_id, email, status, subscribed_date FROM crm_email_list_member WHERE id = " + member));
         assertThat(duplicate.getSQLState()).isEqualTo("23505");
      }
   }



   /*******************************************************************************
    ** Intake persists attribution and immutable evidence against the shipped SQL schema.
    *******************************************************************************/
   @Test
   void testIntakeAndFreshInstallConstraints() throws Exception
   {
      CrmApi.config().withTokenSecret("h2-test-secret").withPublicBaseUrl("https://example.test");
      Integer contactId = insert("crm_contact", new QRecord().withValue("firstName", "Ada").withValue("lastName", "Lovelace")
         .withValue("email", "ADA@EXAMPLE.TEST").withValue("ownerUserId", "rep"));
      CrmIntakeApi intake = new CrmIntakeApi();
      var result = intake.submitForm(new CrmFormRequest("contact", FormType.CONTACT, "Ada", "Lovelace", "ada@example.test",
         null, "Example", "Please reply", false, "Reply only", "search", "organic", "launch", "hero", "crm",
         "https://example.test/contact", "https://search.example.test", "a".repeat(64), "b".repeat(64)));
      assertThat(result.contactCreated()).isFalse();
      assertThat(result.contactId()).isEqualTo(contactId);
      assertThat(query("crm_contact", contactId).getValueString("firstTouchUtmContent")).isEqualTo("hero");
      assertThat(query("crm_contact", contactId).getValueString("lastTouchUtmTerm")).isEqualTo("crm");
      assertThat(query("crm_form_submission", result.submissionId()).getValueString("clientHash")).isEqualTo("b".repeat(64));
      var consents = new QueryAction().execute(new QueryInput("crm_consent_record")).getRecords();
      assertThat(consents).hasSize(1);
      assertThat(consents.get(0).getValueInteger("evidenceFormSubmissionId")).isEqualTo(result.submissionId());
      assertThat(consents.get(0).getValueString("consentText")).isEqualTo("Reply only");
      assertThat(new CrmMarketingApi().checkEmailEligibility(contactId).eligible()).isFalse();
      try(Statement statement = connection.createStatement())
      {
         statement.executeUpdate("INSERT INTO crm_email_list (name, list_key, slug, sort_order) VALUES ('News', 'news', 'news', 1)");
         try(ResultSet rows = statement.executeQuery("SELECT require_double_opt_in FROM crm_email_list WHERE list_key = 'news'"))
         {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getBoolean(1)).isTrue();
         }
         assertThat(assertThrows(SQLException.class, () -> statement.executeUpdate(
            "INSERT INTO crm_email_list (name, list_key, slug, sort_order) VALUES ('Other', 'news', 'other', 2)")).getSQLState()).isEqualTo("23505");
         assertThat(assertThrows(SQLException.class, () -> statement.executeUpdate(
            "INSERT INTO crm_activity_type (name, code, sort_order) VALUES ('Another note', 'NOTE', 2)")).getSQLState()).isEqualTo("23505");
         assertThat(assertThrows(SQLException.class, () -> statement.executeUpdate(
            "INSERT INTO crm_form_submission (form_type, email) VALUES (1, 'missing-key@example.test')")).getSQLState()).isEqualTo("23502");
         statement.executeUpdate("INSERT INTO crm_activity_type (name, sort_order) VALUES ('Custom A', 10), ('Custom B', 11)");
      }
   }



   /*******************************************************************************
    ** All columns, metadata unique keys and relationship indexes exist in SQL.
    *******************************************************************************/
   @Test
   void testSchemaMatchesMetadata() throws Exception
   {
      assertThat(qInstance.getTables()).hasSize(33);
      for(QTableMetaData table : qInstance.getTables().values())
      {
         Set<String> columns = new HashSet<>();
         try(ResultSet result = connection.getMetaData().getColumns(null, "public", table.getName(), null))
         {
            while(result.next())
            {
               columns.add(result.getString("COLUMN_NAME"));
            }
         }
         assertThat(columns).as(table.getName()).containsExactlyInAnyOrderElementsOf(
            table.getFields().values().stream().map(field -> field.getBackendName()).toList());
         Map<String, List<String>> uniqueIndexes = new HashMap<>();
         Set<String> indexedColumns = new HashSet<>();
         try(ResultSet result = connection.getMetaData().getIndexInfo(null, "public", table.getName(), false, false))
         {
            while(result.next())
            {
               String column = result.getString("COLUMN_NAME");
               indexedColumns.add(column);
               if(!result.getBoolean("NON_UNIQUE"))
               {
                  uniqueIndexes.computeIfAbsent(result.getString("INDEX_NAME"), key -> new ArrayList<>()).add(column);
               }
            }
         }
         if(table.getUniqueKeys() != null)
         {
            for(UniqueKey key : table.getUniqueKeys())
            {
               List<String> expected = key.getFieldNames().stream().map(field -> table.getField(field).getBackendName()).toList();
               assertThat(uniqueIndexes.values()).as(table.getName() + " unique key " + expected).contains(expected);
            }
         }
         table.getFields().values().stream().filter(field -> field.getName().endsWith("Id")).forEach(field ->
            assertThat(indexedColumns).as(table.getName() + "." + field.getName()).contains(field.getBackendName()));
         assertThat(new QueryAction().execute(new QueryInput(table.getName())).getRecords()).isEmpty();
      }
   }



   /*******************************************************************************
    ** Both install paths contain identical SQL, with one Liquibase changeset per table.
    *******************************************************************************/
   @Test
   void testLiquibaseMatchesReferenceDdl() throws Exception
   {
      String ddl = Files.readString(Path.of("docs/ddl/postgres.sql"));
      String changelog = Files.readString(Path.of("src/main/resources/db/liquibase/qbit-crm/crm.changelog.sql"));
      assertThat(changelog).startsWith("--liquibase formatted sql");
      assertThat(changelog.lines().filter(line -> line.startsWith("--changeset qbit-crm:")).count()).isEqualTo(33);
      assertThat(stripComments(changelog)).isEqualTo(stripComments(ddl));
   }



   /*******************************************************************************
    ** Insert through QQQ and fail on record-level validation errors.
    *******************************************************************************/
   private Integer insert(String table, QRecord record) throws QException
   {
      QRecord inserted = new InsertAction().execute(new InsertInput(table).withRecord(record)).getRecords().get(0);
      assertThat(inserted.getErrors()).isNullOrEmpty();
      assertThat(inserted.getValueInteger("id")).isNotNull();
      return (inserted.getValueInteger("id"));
   }



   /*******************************************************************************
    ** Query by id through the real backend, verifying one persisted record.
    *******************************************************************************/
   private QRecord query(String table, Integer id) throws QException
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput(table)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id)))).getRecords();
      assertThat(records).hasSize(1);
      return (records.get(0));
   }



   /*******************************************************************************
    ** Run the simple formatted SQL statements used by this schema.
    *******************************************************************************/
   private void executeSql(String sql) throws SQLException
   {
      try(Statement statement = connection.createStatement())
      {
         for(String part : stripComments(sql).split(";"))
         {
            if(!part.isBlank())
            {
               statement.execute(part);
            }
         }
      }
   }



   /*******************************************************************************
    ** Normalize SQL independently of descriptive or Liquibase comment lines.
    *******************************************************************************/
   private String stripComments(String sql)
   {
      return (sql.lines().filter(line -> !line.stripLeading().startsWith("--")).filter(line -> !line.isBlank())
         .collect(java.util.stream.Collectors.joining("\n")));
   }


   /*******************************************************************************
    ** Home sums and GROUP BY execute against the shipped SQL schema, too.
    *******************************************************************************/
   @Test
   void testHomeAggregates() throws Exception
   {
      try(Statement statement = connection.createStatement())
      {
         statement.executeUpdate("INSERT INTO crm_pipeline (id, name) VALUES (1, 'Sales')");
         statement.executeUpdate("INSERT INTO crm_pipeline_stage (id, pipeline_id, name, sort_order, probability_pct, is_closed_won, is_closed_lost) VALUES (1, 1, 'Open', 1, 50, FALSE, FALSE), (2, 1, 'Won', 2, 100, TRUE, FALSE)");
         statement.executeUpdate("INSERT INTO crm_deal (name, pipeline_id, pipeline_stage_id, owner_user_id, amount_in_base_currency, weighted_amount, expected_close_date, actual_close_date) VALUES ('Open deal', 1, 1, 'Ada', 123.45, 61.73, CURRENT_DATE, NULL), ('Won deal', 1, 2, 'Ada', 20.25, 20.25, NULL, CURRENT_DATE)");
      }
      RenderWidgetAction renderer = new RenderWidgetAction();
      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(qInstance.getWidget("crm_open_pipeline"));
      StatisticsData statistics = (StatisticsData) renderer.execute(input).getWidgetData();
      assertThat(new BigDecimal(statistics.getCount().toString())).isEqualByComparingTo("123.45");
      input.setWidgetMetaData(qInstance.getWidget("crm_pipeline_summary"));
      TableData pipeline = (TableData) renderer.execute(input).getWidgetData();
      assertThat(pipeline.getRows()).hasSize(2);
      assertThat(pipeline.getRows().getFirst().get("first").toString()).endsWith(">Open (1 deals)</a>");
      input.setWidgetMetaData(qInstance.getWidget("crm_deal_forecast"));
      TableData forecast = (TableData) renderer.execute(input).getWidgetData();
      assertThat(forecast.getRows()).hasSize(6);
      assertThat(forecast.getRows().getFirst().get("second").toString()).endsWith(">1</a>");
      input.setWidgetMetaData(qInstance.getWidget("crm_sales_leaderboard"));
      TableData leaders = (TableData) renderer.execute(input).getWidgetData();
      assertThat(leaders.getRows()).hasSize(1);
      assertThat(leaders.getRows().getFirst().get("first").toString()).endsWith(">Ada</a>");
   }

}
