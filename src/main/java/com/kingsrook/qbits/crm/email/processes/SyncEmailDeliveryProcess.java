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
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
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
 ** Scheduled CRM maintenance: crm_sync_email_delivery.
 *******************************************************************************/
public class SyncEmailDeliveryProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_sync_email_delivery";

   @Override
   public QProcessMetaData produce(QInstance instance)
   {
      return (new QProcessMetaData().withName(NAME).withLabel("SyncEmailDelivery").withIsHidden(true)
         .withStep(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(SyncEmailDeliveryProcess.class))));
   }

   /*******************************************************************************
    ** Resolve the host transport; an absent qbit-email yields empty lookups.
    *******************************************************************************/
   protected CrmEmailTransport emailTransport() throws QException
   {
      return (new CrmEmailTransport(CrmApi.config()));
   }

   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      CrmEmailTransport transport = emailTransport();
      Integer lastId = null;
      int updated = 0;
      Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
      while(true)
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since))
            .withOrderBy(new QFilterOrderBy("id")).withLimit(200);
         if(lastId != null)
         {
            filter.addCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId));
         }
         List<QRecord> page = new QueryAction().execute(new QueryInput("crm_email_message").withFilter(filter)).getRecords();
         for(QRecord message : page)
         {
            lastId = message.getValueInteger("id");
            if(message.getValueInteger("status") != null && List.of(CrmEmailStatus.BOUNCED.getId(), CrmEmailStatus.COMPLAINED.getId(), CrmEmailStatus.FAILED.getId(),
               CrmEmailStatus.SUPPRESSED.getId()).contains(message.getValueInteger("status")))
            {
               continue;
            }
            var delivery = transport.lookup(message.getValueString("idempotencyKey"));
            if(delivery.isEmpty())
            {
               continue;
            }
            CrmEmailTransport.Delivery state = delivery.get();
            QRecord change = new QRecord().withValue("id", lastId).withValue("status", state.status().getId());
            date(change, "sentDate", state.sentDate());
            date(change, "deliveredDate", state.deliveredDate());
            date(change, "firstOpenedDate", state.openedDate());
            date(change, "firstClickedDate", state.clickedDate());
            date(change, "bouncedDate", state.bouncedDate());
            boolean complaint = state.status() == CrmEmailStatus.COMPLAINED;
            if(complaint || state.status() == CrmEmailStatus.BOUNCED)
            {
               Integer contactId = message.getValueInteger("contactId");
               if(contactId != null)
               {
                  CrmRecords.update("crm_contact", new QRecord().withValue("id", contactId).withValue("doNotEmail", true));
                  String source = complaint ? "Complaint" : "Hard bounce";
                  // The terminal message checkpoint is written last. A retry must not duplicate consent evidence.
                  String evidence = "Email: " + message.getValueString("idempotencyKey");
                  List<QRecord> recorded = new QueryAction().execute(new QueryInput("crm_consent_record").withFilter(new QQueryFilter(
                     new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId),
                     new QFilterCriteria("source", QCriteriaOperator.EQUALS, source),
                     new QFilterCriteria("consentText", QCriteriaOperator.EQUALS, evidence)))).getRecords();
                  if(recorded.isEmpty())
                  {
                     CrmRecords.insert("crm_consent_record", new QRecord().withValue("contactId", contactId)
                        .withValue("consentType", CrmConsentType.EMAIL.getId()).withValue("status", CrmConsentStatus.WITHDRAWN.getId())
                        .withValue("consentDate", Instant.now()).withValue("source", source).withValue("consentText", evidence));
                  }
               }
               change.withValue("isSpamComplaint", complaint);
            }
            CrmRecords.update("crm_email_message", change);
            updated++;
         }
         if(page.size() < 200)
         {
            break;
         }
      }
      output.addValue("updatedCount", updated);
   }

   /*******************************************************************************
    ** Never clear a timestamp when the provider has not exposed it.
    *******************************************************************************/
   private void date(QRecord row, String field, Instant value)
   {
      if(value != null)
      {
         row.setValue(field, value);
      }
   }
}
