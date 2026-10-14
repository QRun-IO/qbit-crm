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
 ** Process to transfer ownership of all contacts, companies, and deals from
 ** one user to another, using core table auditing for each change.
 *******************************************************************************/

package com.kingsrook.qbits.crm.deals.processes;


import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
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


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions for transferring ownership of contacts,
 ** companies, and deals from one user to another.
 *******************************************************************************/
public class TransferOwnershipProcess implements BackendStep
{
   public static final String NAME = "crm_transfer_ownership";



   /*******************************************************************************
    ** Execute the transfer ownership process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String fromUserId    = input.getValueString("fromUserId");
      String toUserId      = input.getValueString("toUserId");
      String sessionUserId = QContext.getQSession().getIdReference();

      ///////////////////////////////////
      // Transfer contacts             //
      ///////////////////////////////////
      transferRecords(Contact.TABLE_NAME, CrmEntityType.CONTACT, fromUserId, toUserId, sessionUserId);

      ///////////////////////////////////
      // Transfer companies            //
      ///////////////////////////////////
      transferRecords(Company.TABLE_NAME, CrmEntityType.COMPANY, fromUserId, toUserId, sessionUserId);

      ///////////////////////////////////
      // Transfer deals                //
      ///////////////////////////////////
      transferRecords(Deal.TABLE_NAME, CrmEntityType.DEAL, fromUserId, toUserId, sessionUserId);
   }



   /***************************************************************************
    ** Query records owned by fromUserId, update to toUserId, and log audit.
    ***************************************************************************/
   private void transferRecords(String tableName, CrmEntityType entityType, String fromUserId, String toUserId, String sessionUserId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("ownerUserId", QCriteriaOperator.EQUALS, fromUserId))));

      for(QRecord record : queryOutput.getRecords())
      {
         Integer recordId = record.getValueInteger("id");

         //////////////////////////////////
         // update the owner             //
         //////////////////////////////////
         QRecord updateRecord = new QRecord()
            .withValue("id", recordId)
            .withValue("ownerUserId", toUserId);

         new UpdateAction().execute(
            new UpdateInput(tableName).withRecord(updateRecord));


      }
   }

}
