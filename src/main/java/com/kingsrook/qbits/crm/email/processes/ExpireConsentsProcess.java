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

package com.kingsrook.qbits.crm.email.processes;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Scheduled CRM maintenance: crm_expire_consents.
 *******************************************************************************/
public class ExpireConsentsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_expire_consents";

   @Override
   public QProcessMetaData produce(QInstance instance)
   {
      return (new QProcessMetaData().withName(NAME).withLabel("ExpireConsents").withIsHidden(true)
         .withStep(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(ExpireConsentsProcess.class))));
   }

   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Instant now = Instant.now();
      Instant cutoff = now.minus(14, ChronoUnit.DAYS);
      Integer lastId = null;
      int expired = 0;
      while(true)
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmListMemberStatus.PENDING_CONFIRMATION.getId()))
            .withOrderBy(new QFilterOrderBy("id")).withLimit(200);
         if(lastId != null)
         {
            filter.addCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId));
         }
         List<QRecord> page = new QueryAction().execute(new QueryInput("crm_email_list_member").withFilter(filter)).getRecords();
         for(QRecord member : page)
         {
            lastId = member.getValueInteger("id");
            Instant pendingSince = member.getValueInstant("confirmationSentDate");
            if(pendingSince == null)
            {
               pendingSince = member.getValueInstant("createDate");
            }
            if(pendingSince != null && !pendingSince.isAfter(cutoff))
            {
               CrmRecords.update("crm_email_list_member", new QRecord().withValue("id", lastId)
                  .withValue("status", CrmListMemberStatus.UNSUBSCRIBED.getId()).withValue("confirmationTokenHash", null)
                  .withValue("unsubscribedDate", now));
               expired++;
            }
         }
         if(page.size() < 200)
         {
            break;
         }
      }
      output.addValue("expiredPendingCount", expired);
      output.addValue("expiredConsentCount", new CountAction().execute(new CountInput("crm_consent_record")
         .withFilter(new QQueryFilter(new QFilterCriteria("expiryDate", QCriteriaOperator.LESS_THAN_OR_EQUALS, now)))).getCount());
   }
}
