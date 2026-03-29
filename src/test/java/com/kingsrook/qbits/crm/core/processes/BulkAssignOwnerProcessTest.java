/*******************************************************************************
 ** Unit tests for BulkAssignOwnerProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.List;
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
