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

package com.kingsrook.qbits.crm.core.customizers;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Stamps crm_currency.rateLastUpdatedDate whenever a rate is set or changed, so
 ** the date says when the exchange rate was last touched. Editing only the name,
 ** symbol or active flag leaves it alone.
 *******************************************************************************/
public final class CurrencyRateDateCustomizer
{
   public static final String RATE_FIELD = "exchangeRateToBase";
   public static final String DATE_FIELD = "rateLastUpdatedDate";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CurrencyRateDateCustomizer()
   {
   }



   /*******************************************************************************
    ** A new currency has a rate from the moment it exists.
    *******************************************************************************/
   public static class OnInsert extends AbstractPreInsertCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         Instant now = Instant.now();
         for(QRecord record : records)
         {
            record.setValue(DATE_FIELD, now);
         }
         return (records);
      }
   }



   /*******************************************************************************
    ** An update that carries a different rate than the stored one moves the date.
    *******************************************************************************/
   public static class OnUpdate extends AbstractPreUpdateCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         if(isPreview || oldRecordList == null)
         {
            return (records);
         }

         Instant now = Instant.now();
         for(QRecord record : records)
         {
            QRecord oldRecord = getOldRecordMap().get(record.getValue("id"));
            if(oldRecord != null && record.getValues().containsKey(RATE_FIELD)
               && !sameRate(ValueUtils.getValueAsBigDecimal(record.getValue(RATE_FIELD)), ValueUtils.getValueAsBigDecimal(oldRecord.getValue(RATE_FIELD))))
            {
               record.setValue(DATE_FIELD, now);
            }
         }
         return (records);
      }
   }



   /*******************************************************************************
    ** Numeric equality, so 1 and 1.000000 are the same rate.
    *******************************************************************************/
   static boolean sameRate(BigDecimal a, BigDecimal b)
   {
      return (a == null ? b == null : b != null && a.compareTo(b) == 0);
   }

}
