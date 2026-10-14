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
 ** Process to clone a deal including its contacts and products.
 ** The cloned deal gets a "(Copy)" suffix, resets to the first pipeline stage,
 ** and clears close-related fields.
 *******************************************************************************/

package com.kingsrook.qbits.crm.deals.processes;


import java.time.Instant;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions for cloning a deal and its child records.
 *******************************************************************************/
public class CloneDealProcess implements BackendStep
{
   public static final String NAME = "crm_clone_deal";



   /*******************************************************************************
    ** Execute the clone deal process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer dealId = input.getValueInteger("dealId");

      ///////////////////////////////////
      // Step 1: load the source deal  //
      ///////////////////////////////////
      QRecord dealRecord = GetAction.execute(Deal.TABLE_NAME, dealId);
      if(dealRecord == null)
      {
         throw new QException("Deal not found: " + dealId);
      }

      Deal sourceDeal = new Deal(dealRecord);

      /////////////////////////////////////////////////////////
      // Step 2: find the first stage of the deal's pipeline //
      /////////////////////////////////////////////////////////
      Integer firstStageId = lookupFirstStageId(sourceDeal.getPipelineId());

      /////////////////////////////////////////
      // Step 3: create the cloned deal      //
      /////////////////////////////////////////
      QRecord newDealRecord = new QRecord()
         .withValue("name", sourceDeal.getName() + " (Copy)")
         .withValue("pipelineId", sourceDeal.getPipelineId())
         .withValue("pipelineStageId", firstStageId != null ? firstStageId : sourceDeal.getPipelineStageId())
         .withValue("amount", sourceDeal.getAmount())
         .withValue("currencyCode", sourceDeal.getCurrencyCode())
         .withValue("companyId", sourceDeal.getCompanyId())
         .withValue("primaryContactId", sourceDeal.getPrimaryContactId())
         .withValue("ownerUserId", sourceDeal.getOwnerUserId())
         .withValue("leadSourceId", sourceDeal.getLeadSourceId())
         .withValue("expectedCloseDate", sourceDeal.getExpectedCloseDate())
         .withValue("description", sourceDeal.getDescription())
         .withValue("priority", sourceDeal.getPriority())
         .withValue("stageEnteredDate", Instant.now());

      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecord(newDealRecord));
      Integer newDealId = dealInsert.getRecords().get(0).getValueInteger("id");

      /////////////////////////////////////////////
      // Step 4: clone DealContact rows          //
      /////////////////////////////////////////////
      cloneChildRecords(DealContact.TABLE_NAME, "dealId", dealId, newDealId);

      /////////////////////////////////////////////
      // Step 5: clone DealProduct rows          //
      /////////////////////////////////////////////
      cloneChildRecords(DealProduct.TABLE_NAME, "dealId", dealId, newDealId);


      output.addValue("dealId", newDealId);
   }



   /***************************************************************************
    ** Look up the first pipeline stage (lowest sortOrder) for a pipeline.
    ***************************************************************************/
   private Integer lookupFirstStageId(Integer pipelineId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, pipelineId))
               .withOrderBy(new QFilterOrderBy("sortOrder", true))
               .withLimit(1)));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }



   /***************************************************************************
    ** Clone child records from one deal to another.
    ***************************************************************************/
   private void cloneChildRecords(String tableName, String fkField, Integer oldDealId, Integer newDealId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fkField, QCriteriaOperator.EQUALS, oldDealId))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord clone = new QRecord(record);
         clone.removeValue("id");
         clone.setValue(fkField, newDealId);
         clone.removeValue("createDate");
         clone.removeValue("modifyDate");

         new InsertAction().execute(
            new InsertInput(tableName).withRecord(clone));
      }
   }

}
