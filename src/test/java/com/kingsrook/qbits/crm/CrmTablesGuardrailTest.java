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


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Guardrail (owner requirement 2026-10-14: every table populated, used and
 ** self-explaining). For every crm_* table:
 ** - runtime code (not just its model, metadata or setup card) reads or writes
 **   it, or the table is listed here as having no runtime code and why;
 ** - it is either seeded by the shipped defaults changelog, or listed here
 **   with why it starts empty;
 ** - it has bundle help that says how its rows get created (shown on the table
 **   screen and when it is empty).
 ** A new table fails this test until it is wired, seeded or explained.
 *******************************************************************************/
class CrmTablesGuardrailTest extends BaseTest
{
   private static final Path MAIN = Path.of("src", "main", "java", "com", "kingsrook", "qbits", "crm");

   /*******************************************************************************
    ** Metadata and presentation files: naming a table here is not using it.
    *******************************************************************************/
   private static final Set<String> NOT_RUNTIME = Set.of("CrmAppMetaData.java", "CrmPresentation.java", "CrmPermissions.java",
      "CrmDataPolicy.java", "CrmSetupStatus.java", "CrmIntegrations.java");

   /*******************************************************************************
    ** Tables that legitimately start empty, and where their rows come from.
    *******************************************************************************/
   static final Map<String, String> STARTS_EMPTY = Map.ofEntries(
      Map.entry("crm_contact", "user content: people, imports, converted web leads"),
      Map.entry("crm_company", "user content"),
      Map.entry("crm_deal", "user content: New, or Create Deal on a contact"),
      Map.entry("crm_deal_contact", "user content: Add Contact on a deal"),
      Map.entry("crm_deal_product", "user content: product lines on a deal"),
      Map.entry("crm_deal_stage_history", "runtime: a deal is created or changes stage"),
      Map.entry("crm_activity", "user content: Log Activity"),
      Map.entry("crm_activity_participant", "user content: people added to an activity"),
      Map.entry("crm_form_submission", "runtime: the host's public intake call"),
      Map.entry("crm_consent_record", "runtime: evidence written when a consent decision is made"),
      Map.entry("crm_email_list", "user content: lists are the business's own"),
      Map.entry("crm_email_list_member", "runtime: subscribing a contact or address"),
      Map.entry("crm_email_message", "runtime: every email sent or received"),
      Map.entry("crm_email_template", "user content: the wording is the business's own"),
      Map.entry("crm_email_sequence", "user content: the outreach is the business's own"),
      Map.entry("crm_sequence_step", "user content: steps of a sequence"),
      Map.entry("crm_sequence_enrollment", "runtime: Enroll in Sequence"),
      Map.entry("crm_product", "user content: the catalogue is the business's own"),
      Map.entry("crm_sales_goal", "user content: targets per user and period"),
      Map.entry("crm_lead_score_rule", "business-specific: good scoring depends on the business"),
      Map.entry("crm_assignment_rule", "host-specific: rules name the host's users"),
      Map.entry("crm_assignment_rule_member", "host-specific: the host's users"),
      Map.entry("crm_job_state", "runtime: a scheduled maintenance run saves its checkpoint"),
      Map.entry("crm_external_mapping", "not wired: nothing in the CRM writes it (reserved for a host integration)"));

   /*******************************************************************************
    ** Tables no CRM code reads or writes: their rows are the user's own, added
    ** and edited through the standard table screens and the published API.
    *******************************************************************************/
   static final Map<String, String> USER_OWNED_NO_CODE = Map.of(
      "crm_activity_participant", "user-owned child rows added on an activity's record",
      "crm_product", "user-owned catalogue; deal product lines pick from it (possible values, line item screens)",
      "crm_industry", "reference list consumed only through the Industry picker on a company (possible values)");



   /*******************************************************************************
    ** Tables with no writer in the CRM; their help must say so.
    *******************************************************************************/
   static final Set<String> NOT_WRITTEN_YET = Set.of("crm_external_mapping");



   /*******************************************************************************
    ** Every table is used by runtime code.
    *******************************************************************************/
   @Test
   void everyTableIsUsedAtRuntime() throws Exception
   {
      Map<String, String> entityByTable = entityByTable();
      List<String>        runtimeSource;
      try(Stream<Path> files = Files.walk(MAIN))
      {
         runtimeSource = files.filter(p -> p.toString().endsWith(".java")).filter(p -> !p.toString().replace('\\', '/').contains("/model/"))
            .filter(p -> !NOT_RUNTIME.contains(p.getFileName().toString())).map(CrmTablesGuardrailTest::read).toList();
      }
      for(String table : crmTables())
      {
         if(USER_OWNED_NO_CODE.containsKey(table))
         {
            continue;
         }
         String  entity    = entityByTable.get(table);
         Pattern entityUse = Pattern.compile("\\b" + entity + "(\\.TABLE_NAME|\\.class|\\(|::)");
         assertThat(entity).as("entity class for " + table).isNotNull();
         assertThat(runtimeSource.stream().anyMatch(source -> source.contains("\"" + table + "\"") || entityUse.matcher(source).find()))
            .as(table + " (" + entity + ") must be read or written by runtime code, not only declared").isTrue();
      }
   }



   /*******************************************************************************
    ** Every table is seeded or explained.
    *******************************************************************************/
   @Test
   void everyTableIsSeededOrExplained() throws Exception
   {
      String changelog = CrmDefaultsChangelogTest.resource(CrmDefaultsChangelogTest.CHANGELOG);
      for(String table : crmTables())
      {
         boolean seeded = CrmDefaultsChangelogTest.SEEDED_TABLES.contains(table);
         assertThat(seeded || STARTS_EMPTY.containsKey(table)).as(table + " must be seeded by the defaults changelog or listed in STARTS_EMPTY").isTrue();
         assertThat(seeded && STARTS_EMPTY.containsKey(table)).as(table + " cannot be both seeded and listed as starting empty").isFalse();
         assertThat(changelog.contains("INSERT INTO " + table + " ")).as(table + " seeded iff the changelog inserts it").isEqualTo(seeded);
      }
      assertThat(STARTS_EMPTY.keySet()).allMatch(table -> qInstance().getTable(table) != null, "STARTS_EMPTY names only real tables");
   }



   /*******************************************************************************
    ** Every table and the app have bundle help that says how rows get created.
    *******************************************************************************/
   @Test
   void everyTableHasEmptyStateHelp() throws Exception
   {
      HelpBundles.of(qInstance()).getBundles().getFirst().getEntries()
         .forEach(entry -> assertThat(entry.getSummary()).as(entry.getKey()).hasSizeLessThanOrEqualTo(120));
      for(String table : crmTables())
      {
         assertThat(tableHelp(table)).as(table + " help says how rows get created").contains("**How rows get created:**");
      }
      for(String table : NOT_WRITTEN_YET)
      {
         assertThat(tableHelp(table)).as(table + " says it is not used yet").contains("not used yet");
      }
      assertThat(HelpBundles.of(qInstance()).resolveWinningEntries()).containsKey(new HelpBundles.EntryKey("app:crm;slot:help", "ALL_SCREENS"));
   }



   /*******************************************************************************
    ** The tables seeded by the changelog are the ones the help says are seeded.
    *******************************************************************************/
   @Test
   void helpOfSeededTablesSaysSo() throws Exception
   {
      for(String table : CrmDefaultsChangelogTest.SEEDED_TABLES)
      {
         assertThat(tableHelp(table)).as(table).contains("seeds");
      }
   }



   /*******************************************************************************
    ** Read the winning table entry from the registered native bundle.
    *******************************************************************************/
   private String tableHelp(String table)
   {
      var entry = HelpBundles.of(qInstance()).resolveWinningEntries().get(new HelpBundles.EntryKey("table:" + table + ";slot:help", "ALL_SCREENS"));
      assertThat(entry).as(table + " table help").isNotNull();
      return (entry.entry().getContent());
   }



   /*******************************************************************************
    ** Test instance after CRM production.
    *******************************************************************************/
   private com.kingsrook.qqq.backend.core.model.metadata.QInstance qInstance()
   {
      return (com.kingsrook.qqq.backend.core.context.QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> crmTables()
   {
      List<String> tables = qInstance().getTables().values().stream().map(QTableMetaData::getName).filter(n -> n.startsWith("crm_")).sorted().toList();
      assertThat(tables).hasSize(34);
      return (tables);
   }



   /*******************************************************************************
    ** model/X.java declares TABLE_NAME = "x".
    *******************************************************************************/
   private static Map<String, String> entityByTable() throws Exception
   {
      Map<String, String> rs = new HashMap<>();
      Pattern             p  = Pattern.compile("TABLE_NAME\\s*=\\s*\"([a-z_]+)\"");
      try(Stream<Path> files = Files.walk(MAIN))
      {
         for(Path file : files.filter(f -> f.toString().endsWith(".java")).toList())
         {
            Matcher m = p.matcher(read(file));
            if(m.find())
            {
               rs.put(m.group(1), file.getFileName().toString().replace(".java", ""));
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String read(Path path)
   {
      try
      {
         return (Files.readString(path, StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
