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
 ** Unit tests for BulkAssignOwnerProcess.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests that BulkAssignOwnerProcess sets ownerUserId on multiple records.
 *******************************************************************************/
class BulkAssignOwnerProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Assign owner on multiple contacts via the transform step.
    *******************************************************************************/
   @Test
   void testBulkAssignOwner() throws QException
   {
      //////////////////////////////////
      // insert two contacts           //
      //////////////////////////////////
      new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("Alice").withLastName("A").withOwnerUserId("old-owner").withEmail("alice@test.com")));
      new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("Bob").withLastName("B").withOwnerUserId("old-owner").withEmail("bob@test.com")));

      //////////////////////////////////////////////////////////
      // query back the records                                //
      //////////////////////////////////////////////////////////
      QueryOutput queryBefore = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));
      assertEquals(2, queryBefore.getRecords().size());

      //////////////////////////////////////////////////////////
      // run the transform step directly                      //
      //////////////////////////////////////////////////////////
      BulkAssignOwnerProcess.BulkAssignOwnerTransformStep step = new BulkAssignOwnerProcess.BulkAssignOwnerTransformStep();

      RunBackendStepInput stepInput = new RunBackendStepInput();
      stepInput.addValue("newOwnerUserId", "new-owner");
      stepInput.setRecords(queryBefore.getRecords());

      RunBackendStepOutput stepOutput = new RunBackendStepOutput();
      step.runOnePage(stepInput, stepOutput);

      //////////////////////////////////////////////////////////
      // verify all records had ownerUserId updated            //
      //////////////////////////////////////////////////////////
      assertEquals(2, stepOutput.getRecords().size());
      for(QRecord record : stepOutput.getRecords())
      {
         assertEquals("new-owner", record.getValueString("ownerUserId"));
      }
   }

}
