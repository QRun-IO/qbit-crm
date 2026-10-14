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
 ** Unit tests for the LeadScoreRule entity.
 *******************************************************************************/

package com.kingsrook.qbits.crm.scoring;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmScorableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
import com.kingsrook.qbits.crm.scoring.model.LeadScoreRule;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for LeadScoreRule CRUD, field persistence, and score operators.
 *******************************************************************************/
class LeadScoreRuleTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with the expected icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(LeadScoreRule.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("score");
   }



   /*******************************************************************************
    ** Verify all fields are present.
    *******************************************************************************/
   @Test
   void testFieldsPresent()
   {
      QTableMetaData table = QContext.getQInstance().getTable(LeadScoreRule.TABLE_NAME);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("entityType");
      assertThat(table.getFields()).containsKey("fieldPath");
      assertThat(table.getFields()).containsKey("operator");
      assertThat(table.getFields()).containsKey("fieldValue");
      assertThat(table.getFields()).containsKey("scoreAdjustment");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("sortOrder");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
   }



   /*******************************************************************************
    ** Insert a rule and verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(LeadScoreRule.TABLE_NAME)
            .withRecordEntity(new LeadScoreRule()
               .withName("Has Email")
               .withDescription("Contacts with email get +10")
               .withEntityType(CrmScorableEntity.CONTACT.getId())
               .withFieldPath("email")
               .withOperator(CrmScoreOperator.IS_SET.getId())
               .withScoreAdjustment(10)
               .withIsActive(true)
               .withSortOrder(1)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(LeadScoreRule.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      LeadScoreRule fetched = new LeadScoreRule(queryOutput.getRecords().get(0));
      assertEquals("Has Email", fetched.getName());
      assertEquals(CrmScorableEntity.CONTACT.getId(), fetched.getEntityType());
      assertEquals("email", fetched.getFieldPath());
      assertEquals(CrmScoreOperator.IS_SET.getId(), fetched.getOperator());
      assertEquals(10, fetched.getScoreAdjustment());
      assertTrue(fetched.getIsActive());
   }



   /*******************************************************************************
    ** Verify score operators PVS is available.
    *******************************************************************************/
   @Test
   void testScoreOperatorsPvs()
   {
      assertNotNull(QContext.getQInstance().getPossibleValueSource(CrmScoreOperator.NAME));
   }

}
