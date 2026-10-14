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


import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The shipped starter defaults (db/liquibase/qbit-crm/crm-defaults.changelog.sql):
 ** they apply to a fresh schema built from the qbit's own changelog, re-running
 ** inserts nothing and keeps an administrator's edits, an existing setup is
 ** respected, and a tenant-enabled schema is left alone.
 *******************************************************************************/
class CrmDefaultsChangelogTest
{
   static final String SCHEMA    = "/db/liquibase/qbit-crm/crm.changelog.sql";
   static final String TENANCY   = "/db/liquibase/qbit-crm/crm-tenancy.changelog.sql";
   static final String CHANGELOG = "/db/liquibase/qbit-crm/crm-defaults.changelog.sql";

   /*******************************************************************************
    ** The tables the changelog seeds.
    *******************************************************************************/
   static final List<String> SEEDED_TABLES = List.of("crm_activity_type", "crm_activity_outcome", "crm_pipeline", "crm_pipeline_stage",
      "crm_lifecycle_stage", "crm_lead_source", "crm_industry", "crm_win_loss_reason", "crm_contact_role", "crm_currency");



   /*******************************************************************************
    ** Fresh schema, seed twice (ignoring preconditions, so the SQL guards
    ** themselves are tested): same rows; an edit survives.
    *******************************************************************************/
   @Test
   void seedsAFreshSchemaInsertOnly() throws Exception
   {
      try(Connection connection = freshSchema())
      {
         runChangelog(connection, true);
         Map<String, Integer> first = counts(connection);
         assertThat(first).containsEntry("crm_activity_type", 6).containsEntry("crm_activity_outcome", 5).containsEntry("crm_pipeline", 1)
            .containsEntry("crm_pipeline_stage", 6).containsEntry("crm_lifecycle_stage", 8).containsEntry("crm_lead_source", 8)
            .containsEntry("crm_win_loss_reason", 8).containsEntry("crm_contact_role", 6).containsEntry("crm_currency", 1);
         assertThat(first.get("crm_industry")).isGreaterThanOrEqualTo(8);

         try(Statement statement = connection.createStatement())
         {
            statement.execute("UPDATE crm_pipeline_stage SET probability_pct = 15 WHERE name = 'Qualification'");
            statement.execute("UPDATE crm_currency SET name = 'Dollar' WHERE currency_code = 'USD'");
         }

         runChangelog(connection, true);
         assertThat(counts(connection)).isEqualTo(first);
         assertThat(scalar(connection, "SELECT probability_pct FROM crm_pipeline_stage WHERE name = 'Qualification'")).isEqualTo("15");
         assertThat(scalar(connection, "SELECT name FROM crm_currency WHERE currency_code = 'USD'")).isEqualTo("Dollar");
      }
   }



   /*******************************************************************************
    ** The seeded rows are what the CRM's code looks up and relies on.
    *******************************************************************************/
   @Test
   void seededRowsMatchWhatTheCodeNeeds() throws Exception
   {
      try(Connection connection = freshSchema())
      {
         runChangelog(connection, false);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline WHERE is_default = TRUE")).isEqualTo("1");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline_stage s JOIN crm_pipeline p ON p.id = s.pipeline_id WHERE p.is_default = TRUE")).isEqualTo("6");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline_stage WHERE is_closed_won = TRUE AND probability_pct = 100 AND name = 'Closed Won'")).isEqualTo("1");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline_stage WHERE is_closed_lost = TRUE AND probability_pct = 0 AND name = 'Closed Lost'")).isEqualTo("1");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_lifecycle_stage WHERE name IN ('Lead', 'Opportunity')")).isEqualTo("2");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_lead_source WHERE name = 'Website'")).isEqualTo("1");
         assertThat(scalar(connection, "SELECT exchange_rate_to_base FROM crm_currency WHERE is_base_currency = TRUE AND currency_code = 'USD'")).isEqualTo("1");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_win_loss_reason WHERE type = 1")).isEqualTo("3");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_win_loss_reason WHERE type = 2")).isEqualTo("5");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_activity_outcome o JOIN crm_activity_type t ON t.id = o.activity_type_id WHERE t.code = 'CALL'")).isEqualTo("3");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_activity_outcome o JOIN crm_activity_type t ON t.id = o.activity_type_id WHERE t.code = 'MEETING'")).isEqualTo("2");
      }
   }



   /*******************************************************************************
    ** A host that already has its own pipeline or base currency keeps it: the
    ** default pipeline is not added, and a second base currency never appears.
    *******************************************************************************/
   @Test
   void keepsAnExistingSetup() throws Exception
   {
      try(Connection connection = freshSchema(); Statement statement = connection.createStatement())
      {
         statement.execute("INSERT INTO crm_pipeline (name, is_default, is_active) VALUES ('Mine', TRUE, TRUE)");
         statement.execute("INSERT INTO crm_currency (currency_code, name, is_base_currency) VALUES ('EUR', 'Euro', TRUE)");
         runChangelog(connection, true);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline")).isEqualTo("1");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline_stage")).isEqualTo("0");
         assertThat(scalar(connection, "SELECT currency_code FROM crm_currency")).isEqualTo("EUR");
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_currency")).isEqualTo("1");
      }
   }



   /*******************************************************************************
    ** The real thing: Liquibase applies the schema and then the defaults on H2;
    ** a second update runs nothing; the rows are what the SQL-level tests saw.
    *******************************************************************************/
   @Test
   void liquibaseAppliesTheDefaultsOnce() throws Exception
   {
      try(Connection connection = DriverManager.getConnection(url()))
      {
         liquibase(connection, SCHEMA);
         liquibase(connection, CHANGELOG);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM databasechangelog WHERE filename LIKE '%crm-defaults%' AND exectype = 'EXECUTED'")).isEqualTo("10");
         Map<String, Integer> first = counts(connection);
         assertThat(first).containsEntry("crm_pipeline", 1).containsEntry("crm_pipeline_stage", 6).containsEntry("crm_lifecycle_stage", 8);

         connection.createStatement().execute("UPDATE crm_pipeline_stage SET probability_pct = 15 WHERE name = 'Qualification'");
         liquibase(connection, CHANGELOG);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM databasechangelog WHERE filename LIKE '%crm-defaults%'")).isEqualTo("10");
         assertThat(counts(connection)).isEqualTo(first);
         assertThat(scalar(connection, "SELECT probability_pct FROM crm_pipeline_stage WHERE name = 'Qualification'")).isEqualTo("15");
      }
   }



   /*******************************************************************************
    ** A host that has not created the CRM tables yet does not fail: every
    ** changeset is marked ran. A tenant-enabled schema is marked ran too.
    *******************************************************************************/
   @Test
   void liquibaseMarksRanWhenTablesAreMissingOrTenanted() throws Exception
   {
      try(Connection connection = DriverManager.getConnection(url()))
      {
         liquibase(connection, CHANGELOG);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM databasechangelog WHERE exectype = 'MARK_RAN'")).isEqualTo("10");
      }
      try(Connection connection = DriverManager.getConnection(url()))
      {
         liquibase(connection, SCHEMA);
         liquibase(connection, TENANCY);
         liquibase(connection, CHANGELOG);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM databasechangelog WHERE filename LIKE '%crm-defaults%' AND exectype = 'MARK_RAN'")).isEqualTo("10");
         for(String table : SEEDED_TABLES)
         {
            assertThat(scalar(connection, "SELECT COUNT(*) FROM " + table)).as(table).isEqualTo("0");
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String url()
   {
      return ("jdbc:h2:mem:crmlb" + System.nanoTime() + ";MODE=PostgreSQL;DEFAULT_NULL_ORDERING=HIGH");
   }



   /*******************************************************************************
    ** Apply one changelog (resource path with a leading slash) through Liquibase.
    *******************************************************************************/
   private static void liquibase(Connection connection, String path) throws Exception
   {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
      new Liquibase(path.substring(1), new ClassLoaderResourceAccessor(), database).update("crm-tenancy");
   }



   /*******************************************************************************
    ** A tenant-enabled schema (tenant_key columns added) is skipped, honoring each
    ** changeset's precondition: seeding without a tenant key would create rows no
    ** tenant can see. A plain schema is not skipped.
    *******************************************************************************/
   @Test
   void skipsTenantEnabledTables() throws Exception
   {
      try(Connection connection = freshSchema(); Statement statement = connection.createStatement())
      {
         for(String sql : statements(resource(TENANCY)))
         {
            statement.execute(sql);
         }
         runChangelog(connection, false);
         for(String table : SEEDED_TABLES)
         {
            assertThat(scalar(connection, "SELECT COUNT(*) FROM " + table)).as(table).isEqualTo("0");
         }
      }
      try(Connection connection = freshSchema())
      {
         runChangelog(connection, false);
         assertThat(scalar(connection, "SELECT COUNT(*) FROM crm_pipeline")).isEqualTo("1");
      }
   }



   /*******************************************************************************
    ** Every changeset guards itself: a precondition (a host without the table
    ** marks it ran), an insert that skips rows that exist, and a rollback.
    *******************************************************************************/
   @Test
   void everyChangesetIsGuarded() throws Exception
   {
      List<String> changesets = changesets(resource(CHANGELOG));
      assertThat(changesets).hasSizeGreaterThanOrEqualTo(SEEDED_TABLES.size());
      for(String changeset : changesets)
      {
         assertThat(changeset).contains("-- preconditions onFail:MARK_RAN onError:MARK_RAN").contains("-- rollback ").contains("NOT EXISTS")
            .contains("-- precondition-sql-check expectedResult:0 ").doesNotContain("UPDATE ");
      }
      for(String table : SEEDED_TABLES)
      {
         assertThat(resource(CHANGELOG)).contains("INSERT INTO " + table + " ");
      }
   }



   /*******************************************************************************
    ** A fresh H2 (PostgreSQL mode) with the qbit's own schema.
    *******************************************************************************/
   static Connection freshSchema() throws Exception
   {
      Connection connection = DriverManager.getConnection("jdbc:h2:mem:crmdefaults" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
      try(Statement statement = connection.createStatement())
      {
         for(String sql : statements(resource(SCHEMA)))
         {
            statement.execute(sql);
         }
      }
      return (connection);
   }



   /*******************************************************************************
    ** Run each changeset's statements. With ignorePreconditions, the SQL runs
    ** whatever the preconditions say; otherwise a changeset whose
    ** precondition-sql-check does not give its expected result is skipped, as
    ** Liquibase would (MARK_RAN).
    *******************************************************************************/
   static void runChangelog(Connection connection, boolean ignorePreconditions) throws Exception
   {
      try(Statement statement = connection.createStatement())
      {
         for(String changeset : changesets(resource(CHANGELOG)))
         {
            if(!ignorePreconditions && !preconditionsHold(connection, changeset))
            {
               continue;
            }
            for(String sql : statements(changeset))
            {
               statement.execute(sql);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean preconditionsHold(Connection connection, String changeset) throws Exception
   {
      Matcher m = Pattern.compile("(?m)^-- precondition-sql-check expectedResult:(\\d+) (.*)$").matcher(changeset);
      while(m.find())
      {
         if(!scalar(connection, m.group(2)).equals(m.group(1)))
         {
            return (false);
         }
      }
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String resource(String path) throws Exception
   {
      try(InputStream stream = CrmDefaultsChangelogTest.class.getResourceAsStream(path))
      {
         assertThat(stream).as(path + " is on the classpath").isNotNull();
         return (new String(stream.readAllBytes(), StandardCharsets.UTF_8));
      }
   }



   /*******************************************************************************
    ** The text of each changeset (from its "-- changeset" line).
    *******************************************************************************/
   static List<String> changesets(String changelog)
   {
      List<String> rs = new ArrayList<>();
      for(String part : changelog.split("(?m)^(?=-- changeset )"))
      {
         if(part.startsWith("-- changeset "))
         {
            rs.add(part);
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** SQL statements, without comment lines, split at a ';' that ends a line.
    *******************************************************************************/
   static List<String> statements(String sql)
   {
      StringBuilder text = new StringBuilder();
      for(String line : sql.split("\\R"))
      {
         if(!line.strip().startsWith("--"))
         {
            text.append(line).append("\n");
         }
      }
      List<String> rs = new ArrayList<>();
      for(String statement : text.toString().split(";\\s*\\n"))
      {
         if(!statement.isBlank())
         {
            rs.add(statement.strip().replaceAll(";$", ""));
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Integer> counts(Connection connection) throws Exception
   {
      Map<String, Integer> rs = new LinkedHashMap<>();
      for(String table : SEEDED_TABLES)
      {
         rs.put(table, Integer.valueOf(scalar(connection, "SELECT COUNT(*) FROM " + table)));
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String scalar(Connection connection, String sql) throws Exception
   {
      try(Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql))
      {
         rs.next();
         Object value = rs.getObject(1);
         return (value instanceof BigDecimal bd ? bd.stripTrailingZeros().toPlainString() : String.valueOf(value));
      }
   }

}
