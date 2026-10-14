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
 ** Unit tests for MergeCompaniesProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.math.BigDecimal;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for merging companies: contacts/deals re-linked, secondary deleted.
 *******************************************************************************/
class MergeCompaniesProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Merge two companies and verify child contacts and deals are re-linked.
    *******************************************************************************/
   @Test
   void testMergeCompanies() throws QException
   {
      ///////////////////////////////////////
      // set up companies                  //
      ///////////////////////////////////////
      Integer primaryId   = insertCompany("Primary Corp", "primary.com");
      Integer secondaryId = insertCompany("Secondary Corp", "secondary.com");

      ///////////////////////////////////////
      // create contact on secondary       //
      ///////////////////////////////////////
      Integer contactId = insertContact(secondaryId);

      ///////////////////////////////////////
      // create deal on secondary          //
      ///////////////////////////////////////
      Integer pipelineId = insertPipeline();
      Integer stageId    = insertPipelineStage(pipelineId);
      Integer dealId     = insertDeal(secondaryId, pipelineId, stageId);

      ////////////////////////////////////
      // run merge process              //
      ////////////////////////////////////
      RunBackendStepInput processInput = new RunBackendStepInput();
      processInput.setProcessName(MergeCompaniesProcess.NAME);
      processInput.addValue("primaryCompanyId", primaryId);
      processInput.addValue("secondaryCompanyId", secondaryId);

      runBusinessStep(processInput);

      /////////////////////////////////////////////
      // verify secondary company is deleted     //
      /////////////////////////////////////////////
      assertNull(GetAction.execute(Company.TABLE_NAME, secondaryId));

      /////////////////////////////////////////////
      // verify contact re-linked to primary     //
      /////////////////////////////////////////////
      Contact updatedContact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertEquals(primaryId, updatedContact.getCompanyId());

      /////////////////////////////////////////////
      // verify deal re-linked to primary        //
      /////////////////////////////////////////////
      Deal updatedDeal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertEquals(primaryId, updatedDeal.getCompanyId());

   }



   /*******************************************************************************
    ** Insert Company.
    *******************************************************************************/
   private Integer insertCompany(String name, String domain) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Company.TABLE_NAME).withRecordEntity(
            new Company()
               .withName(name)
               .withDomain(domain)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Contact.
    *******************************************************************************/
   private Integer insertContact(Integer companyId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("Test")
               .withLastName("Contact")
               .withEmail("test@example.com")
               .withCompanyId(companyId)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Pipeline.
    *******************************************************************************/
   private Integer insertPipeline() throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName("Sales").withIsActive(true).withIsDefault(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Pipeline Stage.
    *******************************************************************************/
   private Integer insertPipelineStage(Integer pipelineId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Prospect")
               .withSortOrder(1)
               .withProbabilityPct(10)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Insert Deal.
    *******************************************************************************/
   private Integer insertDeal(Integer companyId, Integer pipelineId, Integer stageId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(
            new Deal()
               .withName("Test Deal")
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withCompanyId(companyId)
               .withAmount(new BigDecimal("5000"))
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
