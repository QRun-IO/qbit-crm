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
 ** Process to merge two companies. Re-links contacts, deals, activities, and
 ** related records from the secondary to the primary, then deletes the secondary company
 ** with core table auditing.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.util.List;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
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
 ** MetaDataProducer + BackendStep for merging two companies into one.
 *******************************************************************************/
public class MergeCompaniesProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_merge_companies";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Merge Companies")
         .withTableName(Company.TABLE_NAME)
         .withIcon(new QIcon().withName("merge"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(MergeCompaniesProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("primaryCompanyId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("secondaryCompanyId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the merge companies process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer primaryCompanyId   = input.getValueInteger("primaryCompanyId");
      Integer secondaryCompanyId = input.getValueInteger("secondaryCompanyId");
      String  sessionUserId      = CrmSessionUtils.getCurrentUserId();

      /////////////////////////////////////////
      // validate both companies exist       //
      /////////////////////////////////////////
      if(GetAction.execute(Company.TABLE_NAME, primaryCompanyId) == null)
      {
         throw new QException("Primary company not found: " + primaryCompanyId);
      }
      if(GetAction.execute(Company.TABLE_NAME, secondaryCompanyId) == null)
      {
         throw new QException("Secondary company not found: " + secondaryCompanyId);
      }

      ///////////////////////////////////////////////
      // Re-link contacts from secondary           //
      ///////////////////////////////////////////////
      relinkRecords(Contact.TABLE_NAME, "companyId", secondaryCompanyId, primaryCompanyId);

      ///////////////////////////////////////////////
      // Re-link deals from secondary              //
      ///////////////////////////////////////////////
      relinkRecords(Deal.TABLE_NAME, "companyId", secondaryCompanyId, primaryCompanyId);

      ///////////////////////////////////////////////
      // Re-link activities from secondary         //
      ///////////////////////////////////////////////
      relinkRecords(Activity.TABLE_NAME, "companyId", secondaryCompanyId, primaryCompanyId);


      ///////////////////////////////////////////////
      // Delete the secondary company              //
      ///////////////////////////////////////////////
      new DeleteAction().execute(
         new DeleteInput(Company.TABLE_NAME).withPrimaryKeys(List.of(secondaryCompanyId)));

      output.addValue("primaryCompanyId", primaryCompanyId);
   }



   /***************************************************************************
    ** Re-link simple FK records from secondary to primary.
    ***************************************************************************/
   private void relinkRecords(String tableName, String fkField, Integer fromId, Integer toId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fkField, QCriteriaOperator.EQUALS, fromId))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue(fkField, toId);

         new UpdateAction().execute(
            new UpdateInput(tableName).withRecord(updateRecord));
      }
   }



}
