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

package com.kingsrook.qbits.crm;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Relationship validation must cover insert and either side of partial updates.
 *******************************************************************************/
class CrmRelationshipValidationTest extends BaseTest
{
   /*******************************************************************************
    ** Rejects wrong pipeline on insert and partial updates.
    *******************************************************************************/
   @Test
   void rejectsWrongPipelineOnInsertAndPartialUpdates() throws Exception
   {
      P4TestSupport.seed("crm_pipeline_stage", new QRecord().withValue("id", 10).withValue("pipelineId", 1),
         new QRecord().withValue("id", 20).withValue("pipelineId", 2));
      var inserted = new InsertAction().execute(new InsertInput("crm_deal").withRecord(new QRecord().withValue("name", "Wrong stage")
         .withValue("pipelineId", 1).withValue("pipelineStageId", 20)));
      assertThat(inserted.getRecords().getFirst().getErrors()).isNotEmpty();
      P4TestSupport.seed("crm_deal", new QRecord().withValue("id", 1).withValue("name", "Existing").withValue("pipelineId", 1).withValue("pipelineStageId", 10));
      for(QRecord change : List.of(new QRecord().withValue("id", 1).withValue("pipelineId", 2),
         new QRecord().withValue("id", 1).withValue("pipelineStageId", 20)))
      {
         assertThat(new UpdateAction().execute(new UpdateInput("crm_deal").withRecord(change)).getRecords().getFirst().getErrors()).isNotEmpty();
      }
   }


   /*******************************************************************************
    ** Validates activity outcome and allows null outcome.
    *******************************************************************************/
   @Test
   void validatesActivityOutcomeAndAllowsNullOutcome() throws Exception
   {
      P4TestSupport.seed("crm_activity_outcome", new QRecord().withValue("id", 10).withValue("activityTypeId", 1));
      var bad = new InsertAction().execute(new InsertInput("crm_activity").withRecord(new QRecord().withValue("subject", "Wrong outcome")
         .withValue("activityTypeId", 2).withValue("outcomeId", 10)));
      assertThat(bad.getRecords().getFirst().getErrors()).isNotEmpty();
      var good = new InsertAction().execute(new InsertInput("crm_activity").withRecord(new QRecord().withValue("subject", "Call").withValue("ownerUserId", TEST_USER_ID)
         .withValue("activityTypeId", 1).withValue("outcomeId", 10)));
      assertThat(good.getRecords().getFirst().getErrors()).isNullOrEmpty();
      var cleared = new UpdateAction().execute(new UpdateInput("crm_activity").withRecord(new QRecord()
         .withValue("id", good.getRecords().getFirst().getValue("id")).withValue("outcomeId", null)));
      assertThat(cleared.getRecords().getFirst().getErrors()).isNullOrEmpty();
   }
}
