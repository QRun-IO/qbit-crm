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

package com.kingsrook.qbits.crm.core.model;


import java.math.BigDecimal;
import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** crm_currency.rateLastUpdatedDate says when the exchange rate was last set or
 ** changed: stamped on insert, moved by a changed rate, left alone by an edit
 ** that does not touch the rate.
 *******************************************************************************/
class CurrencyRateDateTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void stampsOnInsertAndOnlyMovesWhenTheRateChanges() throws Exception
   {
      Instant before = Instant.now().minusSeconds(1);
      Integer id = new InsertAction().execute(new InsertInput("crm_currency").withRecord(new QRecord()
         .withValue("currencyCode", "EUR").withValue("name", "Euro").withValue("exchangeRateToBase", new BigDecimal("1.10"))))
         .getRecords().getFirst().getValueInteger("id");
      Instant stamped = GetAction.execute("crm_currency", id).getValueInstant("rateLastUpdatedDate");
      assertThat(stamped).as("insert stamps the date").isAfter(before);

      Thread.sleep(20);
      new UpdateAction().execute(new UpdateInput("crm_currency").withRecord(new QRecord().withValue("id", id).withValue("name", "Euro (EU)")));
      assertThat(GetAction.execute("crm_currency", id).getValueInstant("rateLastUpdatedDate")).as("a rename leaves the date").isEqualTo(stamped);

      new UpdateAction().execute(new UpdateInput("crm_currency").withRecord(new QRecord().withValue("id", id).withValue("exchangeRateToBase", new BigDecimal("1.100000"))));
      assertThat(GetAction.execute("crm_currency", id).getValueInstant("rateLastUpdatedDate")).as("the same rate leaves the date").isEqualTo(stamped);

      new UpdateAction().execute(new UpdateInput("crm_currency").withRecord(new QRecord().withValue("id", id).withValue("exchangeRateToBase", new BigDecimal("1.25"))));
      assertThat(GetAction.execute("crm_currency", id).getValueInstant("rateLastUpdatedDate")).as("a new rate moves the date").isAfter(stamped);
   }

}
