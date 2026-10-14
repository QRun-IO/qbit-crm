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
 ** Process to unsubscribe an email address from an email list. Sets the member
 ** status to UNSUBSCRIBED and records the unsubscribedDate.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to unsubscribe an email from a list.
 ** Queries by emailListId + email, sets status to UNSUBSCRIBED and
 ** unsubscribedDate to now.
 *******************************************************************************/
public class UnsubscribeFromListProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_unsubscribe_from_list";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Unsubscribe from List")
         .withIcon(new QIcon().withName("unsubscribe"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(UnsubscribeFromListProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("email", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("emailListId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the unsubscription.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String  email       = input.getValueString("email");
      Integer emailListId = input.getValueInteger("emailListId");

      ////////////////////////////////////////////////////
      // query the member by emailListId + email        //
      ////////////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("emailListId", QCriteriaOperator.EQUALS, emailListId))
               .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email))));

      if(queryOutput.getRecords().isEmpty())
      {
         output.addValue("action", "NOT_FOUND");
         return;
      }

      QRecord memberRecord = queryOutput.getRecords().get(0);

      ////////////////////////////////////////////////////
      // update status to UNSUBSCRIBED                  //
      ////////////////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", memberRecord.getValueInteger("id"))
         .withValue("status", CrmListMemberStatus.UNSUBSCRIBED.getPossibleValueId())
         .withValue("unsubscribedDate", Instant.now());

      new UpdateAction().execute(
         new UpdateInput(EmailListMember.TABLE_NAME).withRecord(updateRecord));

      output.addValue("action", "UNSUBSCRIBED");
      output.addValue("emailListMemberId", memberRecord.getValueInteger("id"));
   }

}
