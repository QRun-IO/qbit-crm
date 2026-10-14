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

package com.kingsrook.qbits.crm.deals;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.scoring.JobState;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Deal age notifications with a persistent once-per-deal-and-stage checkpoint.
 *******************************************************************************/
public final class CrmDealNotifications
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmDealNotifications()
   {
   }

   /*******************************************************************************
    ** Scan open deals in keyset pages, resolving stage metadata once per run.
    *******************************************************************************/
   public static void checkRotting() throws QException
   {
      Map<Integer, QRecord> stages = new HashMap<>();
      for(QRecord stage : new QueryAction().execute(new QueryInput("crm_pipeline_stage")).getRecords())
      {
         stages.put(stage.getValueInteger("id"), stage);
      }
      Integer lastId = null;
      Instant now = Instant.now();
      while(true)
      {
         QQueryFilter filter = new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withLimit(200);
         if(lastId != null)
         {
            filter.withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId));
         }
         List<QRecord> page = new QueryAction().execute(new QueryInput("crm_deal").withFilter(filter)).getRecords();
         for(QRecord deal : page)
         {
            lastId = deal.getValueInteger("id");
            QRecord stage = stages.get(deal.getValueInteger("pipelineStageId"));
            Instant entered = deal.getValueInstant("stageEnteredDate");
            if(stage == null || entered == null || deal.getValueString("ownerUserId") == null
               || Boolean.TRUE.equals(stage.getValueBoolean("isClosedWon")) || Boolean.TRUE.equals(stage.getValueBoolean("isClosedLost")))
            {
               continue;
            }
            Integer days = stage.getValueInteger("rotDays");
            if(days == null || days <= 0 || !entered.isBefore(now.minus(days, ChronoUnit.DAYS)))
            {
               continue;
            }
            String key = "deal:" + lastId + ":stage:" + stage.getValue("id");
            String checkpoint = "crm.deal.rotting:" + key;
            if(new QueryAction().execute(new QueryInput(JobState.TABLE_NAME)
               .withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, checkpoint)))).getRecords().isEmpty())
            {
               QAlerts.notify(new NotificationSignal("Deal needs attention").withSourceName("crm.deal.rotting").withSourceKey(key)
                  .withSourceTableName("crm_deal").withSourceRecordId(lastId.toString())
                  .withAudience(NotificationAudience.user(deal.getValueString("ownerUserId"))));
               CrmRecords.insert(JobState.TABLE_NAME, new QRecord().withValue("name", checkpoint).withValue("lastSuccess", now));
            }
         }
         if(page.size() < 200)
         {
            break;
         }
      }
   }
}
