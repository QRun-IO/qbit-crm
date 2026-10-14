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
 ** Unit tests for the SalesGoal entity.
 *******************************************************************************/

package com.kingsrook.qbits.crm.scoring;


import java.math.BigDecimal;
import java.time.LocalDate;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmGoalPeriod;
import com.kingsrook.qbits.crm.scoring.model.SalesGoal;
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


/*******************************************************************************
 ** Tests for SalesGoal CRUD, field persistence, and unique key.
 *******************************************************************************/
class SalesGoalTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(SalesGoal.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("flag");
   }



   /*******************************************************************************
    ** Verify all fields are present.
    *******************************************************************************/
   @Test
   void testFieldsPresent()
   {
      QTableMetaData table = QContext.getQInstance().getTable(SalesGoal.TABLE_NAME);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("userId");
      assertThat(table.getFields()).containsKey("pipelineId");
      assertThat(table.getFields()).containsKey("periodType");
      assertThat(table.getFields()).containsKey("periodStart");
      assertThat(table.getFields()).containsKey("periodEnd");
      assertThat(table.getFields()).containsKey("targetAmount");
      assertThat(table.getFields()).containsKey("currencyCode");
   }



   /*******************************************************************************
    ** Insert a goal and verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(SalesGoal.TABLE_NAME)
            .withRecordEntity(new SalesGoal()
               .withUserId("user-001")
               .withPeriodType(CrmGoalPeriod.MONTHLY.getId())
               .withPeriodStart(LocalDate.of(2026, 3, 1))
               .withPeriodEnd(LocalDate.of(2026, 3, 31))
               .withTargetAmount(new BigDecimal("50000.00"))
               .withCurrencyCode("USD")));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(SalesGoal.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      SalesGoal fetched = new SalesGoal(queryOutput.getRecords().get(0));
      assertEquals("user-001", fetched.getUserId());
      assertEquals(CrmGoalPeriod.MONTHLY.getId(), fetched.getPeriodType());
      assertEquals(LocalDate.of(2026, 3, 1), fetched.getPeriodStart());
      assertEquals(LocalDate.of(2026, 3, 31), fetched.getPeriodEnd());
      assertThat(fetched.getTargetAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
      assertEquals("USD", fetched.getCurrencyCode());
   }



   /*******************************************************************************
    ** Verify unique key is defined on the table.
    *******************************************************************************/
   @Test
   void testUniqueKeyDefined()
   {
      QTableMetaData table = QContext.getQInstance().getTable(SalesGoal.TABLE_NAME);
      assertThat(table.getUniqueKeys()).isNotEmpty();
      assertThat(table.getUniqueKeys().get(0).getFieldNames())
         .containsExactly("userId", "pipelineId", "periodType", "periodStart");
   }

}
