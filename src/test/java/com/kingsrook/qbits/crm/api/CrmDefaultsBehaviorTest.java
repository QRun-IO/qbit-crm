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

package com.kingsrook.qbits.crm.api;


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.CrmDefaultsChangelogTest;
import com.kingsrook.qbits.crm.deals.processes.ConvertLeadProcess;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.widgets.CrmSetupStatus;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The shipped defaults change behavior: with exactly the rows the changelog
 ** inserts (applied to H2, then copied into the test backend), Convert lead
 ** works and moves the contact to Opportunity, intake gives a new lead the Lead
 ** stage, and the Setup card has nothing left to do.
 *******************************************************************************/
class CrmDefaultsBehaviorTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void seededRowsMakeTheCrmWork() throws Exception
   {
      loadSeededRows();

      ////////////////////////////////////////////////////////////////
      // intake: a new web lead is assigned the seeded "Lead" stage //
      ////////////////////////////////////////////////////////////////
      CrmIntakeApi intake = new CrmIntakeApi(CrmApi.config(), new CrmEmailTransport(CrmApi.config()));
      Integer contactId = intake.submitForm(new CrmFormRequest("contact", FormType.CONTACT, "Ada", "Lovelace", "ada@example.test", "555-0100",
         "Example", "Hello", false, "Reply", "search", "organic", "c", "banner", "crm", "https://example.test/form", "https://search.example.test",
         "a".repeat(64), "b".repeat(64))).contactId();
      QRecord lead = CrmStore.asUser().get("crm_contact", contactId);
      Integer leadStageId = CrmStore.asUser().by("crm_lifecycle_stage", "name", "Lead").getFirst().getValueInteger("id");
      assertThat(lead.getValueInteger("lifecycleStageId")).isEqualTo(leadStageId);

      ////////////////////////////////////////////////////////////////////////////////
      // Convert lead: finds the default pipeline and its first stage, and moves the //
      // contact to the seeded "Opportunity" stage                                  //
      ////////////////////////////////////////////////////////////////////////////////
      RunBackendStepInput input = new RunBackendStepInput();
      input.setProcessName(ConvertLeadProcess.NAME);
      input.addValue("contactId", contactId);
      input.addValue("dealName", "Ada deal");
      input.addValue("dealAmount", new BigDecimal("1000"));
      RunBackendStepOutput output = runBusinessStep(input);
      QRecord deal = CrmStore.asUser().get("crm_deal", output.getValueInteger("dealId"));
      assertThat(deal).isNotNull();
      QRecord firstStage = CrmStore.asUser().by("crm_pipeline_stage", "name", "Qualification").getFirst();
      assertThat(deal.getValueInteger("pipelineStageId")).isEqualTo(firstStage.getValueInteger("id"));
      Integer opportunityId = CrmStore.asUser().by("crm_lifecycle_stage", "name", "Opportunity").getFirst().getValueInteger("id");
      assertThat(CrmStore.asUser().get("crm_contact", contactId).getValueInteger("lifecycleStageId")).isEqualTo(opportunityId);
      assertThat(deal.getValueBigDecimal("amountInBaseCurrency")).isEqualByComparingTo("1000");

      ///////////////////////////////////////////////////////////////
      // setup card: nothing left to do for the five tracked items //
      ///////////////////////////////////////////////////////////////
      List<CrmSetupStatus.Item> items = CrmSetupStatus.load();
      assertThat(items).extracting(CrmSetupStatus.Item::state).doesNotContain(CrmSetupStatus.State.TODO);
      assertThat(items).filteredOn(item -> !item.label().equals("Email delivery")).extracting(CrmSetupStatus.Item::state).containsOnly(CrmSetupStatus.State.OK);
   }



   /*******************************************************************************
    ** Apply the defaults to a fresh H2 schema and insert every row it created
    ** into the test backend, preserving ids.
    *******************************************************************************/
   private void loadSeededRows() throws Exception
   {
      try(Connection connection = CrmDefaultsChangelogTest.freshSchema())
      {
         CrmDefaultsChangelogTest.runChangelog(connection, false);
         for(String table : CrmDefaultsChangelogTest.SEEDED_TABLES)
         {
            try(Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
            {
               ResultSetMetaData meta = rs.getMetaData();
               while(rs.next())
               {
                  QRecord record = new QRecord();
                  for(int i = 1; i <= meta.getColumnCount(); i++)
                  {
                     String column = meta.getColumnLabel(i).toLowerCase();
                     if(column.equals("create_date") || column.equals("modify_date") || rs.getObject(i) == null)
                     {
                        continue;
                     }
                     record.setValue(camel(column), (java.io.Serializable) rs.getObject(i));
                  }
                  var result = new InsertAction().execute(new InsertInput(table).withRecord(record));
                  assertThat(result.getRecords().getFirst().getErrors()).as(table).isNullOrEmpty();
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String camel(String snake)
   {
      StringBuilder sb    = new StringBuilder();
      boolean       upper = false;
      for(char c : snake.toCharArray())
      {
         if(c == '_')
         {
            upper = true;
         }
         else
         {
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
         }
      }
      return (sb.toString());
   }

}
