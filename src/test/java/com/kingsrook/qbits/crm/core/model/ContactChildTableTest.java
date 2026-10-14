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
 ** Unit tests verifying Contact child table joins for Activity, DealContact,
 ** and ConsentRecord.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests that Contact entity has child joins for Activity, DealContact,
 ** and ConsentRecord, and that child records can be inserted
 ** and queried against a parent contact.
 *******************************************************************************/
class ContactChildTableTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the join from Contact to Activity exists in the QInstance.
    *******************************************************************************/
   @Test
   void testContactActivityJoinExists()
   {
      String joinName = QJoinMetaData.makeInferredJoinName(Contact.TABLE_NAME, Activity.TABLE_NAME).replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
      QJoinMetaData join = QContext.getQInstance().getJoin(joinName);
      assertNotNull(join, "Expected join from Contact to Activity to exist");
   }



   /*******************************************************************************
    ** Verify the join from Contact to DealContact exists in the QInstance.
    *******************************************************************************/
   @Test
   void testContactDealContactJoinExists()
   {
      String joinName = QJoinMetaData.makeInferredJoinName(Contact.TABLE_NAME, DealContact.TABLE_NAME).replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
      QJoinMetaData join = QContext.getQInstance().getJoin(joinName);
      assertNotNull(join, "Expected join from Contact to DealContact to exist");
   }



   /*******************************************************************************
    ** Verify the join from Contact to ConsentRecord exists in the QInstance.
    *******************************************************************************/
   @Test
   void testContactConsentRecordJoinExists()
   {
      String joinName = QJoinMetaData.makeInferredJoinName(Contact.TABLE_NAME, ConsentRecord.TABLE_NAME).replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
      QJoinMetaData join = QContext.getQInstance().getJoin(joinName);
      assertNotNull(join, "Expected join from Contact to ConsentRecord to exist");
   }



   /*******************************************************************************
    ** Insert a contact, then insert child Activity, DealContact,
    ** and ConsentRecord records referencing it. Verify all persist and can be
    ** queried by contactId.
    *******************************************************************************/
   @Test
   void testInsertChildRecords() throws QException
   {
      //////////////////////////
      // insert a contact     //
      //////////////////////////
      Integer contactId = insertContact("Alice", "Johnson");

      ///////////////////////////
      // insert an activity    //
      ///////////////////////////
      Integer activityTypeId = insertActivityType("Call");
      new InsertAction().execute(new InsertInput(Activity.TABLE_NAME)
         .withRecordEntity(new Activity()
            .withActivityTypeId(activityTypeId)
            .withSubject("Follow-up call")
            .withContactId(contactId)
            .withOwnerUserId("user-001")));

      QueryOutput activityQuery = new QueryAction().execute(new QueryInput(Activity.TABLE_NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))));
      assertEquals(1, activityQuery.getRecords().size());

      //////////////////////////////
      // insert a deal contact    //
      //////////////////////////////
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertPipelineStage(pipelineId, "Prospecting", 1, 10);
      Integer dealId = insertDeal("Test Deal", pipelineId, stageId);

      new InsertAction().execute(new InsertInput(DealContact.TABLE_NAME)
         .withRecordEntity(new DealContact()
            .withDealId(dealId)
            .withContactId(contactId)
            .withIsPrimary(true)));

      QueryOutput dealContactQuery = new QueryAction().execute(new QueryInput(DealContact.TABLE_NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))));
      assertEquals(1, dealContactQuery.getRecords().size());

      ////////////////////////////////
      // insert a consent record    //
      ////////////////////////////////
      new InsertAction().execute(new InsertInput(ConsentRecord.TABLE_NAME)
         .withRecordEntity(new ConsentRecord()
            .withContactId(contactId)
            .withConsentType(CrmConsentType.EMAIL.getId())
            .withStatus(CrmConsentStatus.GRANTED.getId())
            .withConsentDate(Instant.now())));

      QueryOutput consentQuery = new QueryAction().execute(new QueryInput(ConsentRecord.TABLE_NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))));
      assertEquals(1, consentQuery.getRecords().size());
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an ActivityType and return its id.
    ***************************************************************************/
   private Integer insertActivityType(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName("phone")
               .withIsActive(true)
               .withSortOrder(1)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Pipeline and return its id.
    ***************************************************************************/
   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName(name)
               .withIsDefault(true)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage and return its id.
    ***************************************************************************/
   private Integer insertPipelineStage(Integer pipelineId, String name, Integer sortOrder, Integer probabilityPct) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(false)
               .withIsClosedLost(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Deal and return its id.
    ***************************************************************************/
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



}
