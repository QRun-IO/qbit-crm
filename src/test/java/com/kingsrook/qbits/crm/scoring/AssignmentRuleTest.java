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
 ** Unit tests for the AssignmentRule entity.
 *******************************************************************************/

package com.kingsrook.qbits.crm.scoring;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRule;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRuleMember;
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
 ** Tests for AssignmentRule CRUD, field persistence, and child member table.
 *******************************************************************************/
class AssignmentRuleTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(AssignmentRule.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("assignment_ind");
   }



   /*******************************************************************************
    ** Verify all fields are present.
    *******************************************************************************/
   @Test
   void testFieldsPresent()
   {
      QTableMetaData table = QContext.getQInstance().getTable(AssignmentRule.TABLE_NAME);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("entityType");
      assertThat(table.getFields()).containsKey("criteriaJson");
      assertThat(table.getFields()).containsKey("assignStrategy");
      assertThat(table.getFields()).containsKey("assignToUserId");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("sortOrder");
   }



   /*******************************************************************************
    ** Insert a rule and verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Default Contact Assign")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.ROUND_ROBIN.getId())
               .withIsActive(true)
               .withSortOrder(1)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(AssignmentRule.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      AssignmentRule fetched = new AssignmentRule(queryOutput.getRecords().get(0));
      assertEquals("Default Contact Assign", fetched.getName());
      assertEquals(CrmAssignableEntity.CONTACT.getId(), fetched.getEntityType());
      assertEquals(CrmAssignStrategy.ROUND_ROBIN.getId(), fetched.getAssignStrategy());
      assertTrue(fetched.getIsActive());
   }



   /*******************************************************************************
    ** Verify child table AssignmentRuleMember can be inserted.
    *******************************************************************************/
   @Test
   void testChildMembers() throws QException
   {
      InsertOutput ruleInsert = new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("RR Rule")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.ROUND_ROBIN.getId())
               .withIsActive(true)
               .withSortOrder(1)));

      Integer ruleId = ruleInsert.getRecords().get(0).getValueInteger("id");

      new InsertAction().execute(
         new InsertInput(AssignmentRuleMember.TABLE_NAME)
            .withRecordEntity(new AssignmentRuleMember()
               .withAssignmentRuleId(ruleId)
               .withUserId("user-001")
               .withSortOrder(1)
               .withIsActive(true)));

      new InsertAction().execute(
         new InsertInput(AssignmentRuleMember.TABLE_NAME)
            .withRecordEntity(new AssignmentRuleMember()
               .withAssignmentRuleId(ruleId)
               .withUserId("user-002")
               .withSortOrder(2)
               .withIsActive(true)));

      QueryOutput memberQuery = new QueryAction().execute(
         new QueryInput(AssignmentRuleMember.TABLE_NAME));

      assertEquals(2, memberQuery.getRecords().size());
   }



   /*******************************************************************************
    ** Verify unique key on AssignmentRuleMember.
    *******************************************************************************/
   @Test
   void testMemberUniqueKey()
   {
      QTableMetaData table = QContext.getQInstance().getTable(AssignmentRuleMember.TABLE_NAME);
      assertThat(table.getUniqueKeys()).isNotEmpty();
      assertThat(table.getUniqueKeys().get(0).getFieldNames()).containsExactly("assignmentRuleId", "userId");
   }

}
