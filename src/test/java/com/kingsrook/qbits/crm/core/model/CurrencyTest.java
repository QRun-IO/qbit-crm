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
 ** Unit test for Currency entity
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.model;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for Currency entity -- verifies field definitions, PVS, and
 ** exchange rate handling via insert/query round-trip.
 *******************************************************************************/
class CurrencyTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Currency.TABLE_NAME);
      assertThat(table).isNotNull();
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("currencyCode");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("symbol");
      assertThat(table.getFields()).containsKey("exchangeRateToBase");
      assertThat(table.getFields()).containsKey("isBaseCurrency");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("rateLastUpdatedDate");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
      assertThat(table.getIcon().getName()).isEqualTo("currency_exchange");
   }



   /*******************************************************************************
    ** Verify PVS is produced.
    *******************************************************************************/
   @Test
   void testPossibleValueSourceProduced()
   {
      assertThat(QContext.getQInstance().getPossibleValueSource(Currency.TABLE_NAME)).isNotNull();
   }



   /*******************************************************************************
    ** Verify insert and query round-trip with exchange rate data.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws Exception
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(Currency.TABLE_NAME);
      insertInput.setRecords(List.of(
         new Currency()
            .withCurrencyCode("USD")
            .withName("US Dollar")
            .withSymbol("$")
            .withExchangeRateToBase(new BigDecimal("1.0"))
            .withIsBaseCurrency(true)
            .withIsActive(true)
            .toQRecord(),
         new Currency()
            .withCurrencyCode("EUR")
            .withName("Euro")
            .withSymbol("EUR")
            .withExchangeRateToBase(new BigDecimal("0.92"))
            .withIsBaseCurrency(false)
            .withIsActive(true)
            .toQRecord()));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertThat(insertOutput.getRecords()).hasSize(2);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(Currency.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).hasSize(2);

      Currency usd = queryOutput.getRecords().stream()
         .map(Currency::new)
         .filter(c -> "USD".equals(c.getCurrencyCode()))
         .findFirst()
         .orElseThrow();

      assertThat(usd.getName()).isEqualTo("US Dollar");
      assertThat(usd.getSymbol()).isEqualTo("$");
      assertThat(usd.getIsBaseCurrency()).isTrue();
      assertThat(usd.getExchangeRateToBase()).isEqualByComparingTo(new BigDecimal("1.0"));

      Currency eur = queryOutput.getRecords().stream()
         .map(Currency::new)
         .filter(c -> "EUR".equals(c.getCurrencyCode()))
         .findFirst()
         .orElseThrow();

      assertThat(eur.getIsBaseCurrency()).isFalse();
      assertThat(eur.getExchangeRateToBase()).isEqualByComparingTo(new BigDecimal("0.92"));
   }

}
