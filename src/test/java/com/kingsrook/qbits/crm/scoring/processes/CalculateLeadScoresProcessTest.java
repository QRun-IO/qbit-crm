/*******************************************************************************
 ** Unit tests for CalculateLeadScoresProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmScorableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
import com.kingsrook.qbits.crm.scoring.model.LeadScoreRule;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests for CalculateLeadScoresProcess: rule evaluation, score update,
 ** and unchanged contacts skipped.
 *******************************************************************************/
class CalculateLeadScoresProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Contacts matching rules should have their score updated.
    *******************************************************************************/
   @Test
   void testRulesEvaluation() throws QException
   {
      /////////////////////////////////////////////
      // create rules                            //
      /////////////////////////////////////////////
      insertRule("Has Email", "email", CrmScoreOperator.IS_SET.getId(), null, 10, 1);
      insertRule("Job Title CEO", "jobTitle", CrmScoreOperator.EQUALS.getId(), "CEO", 25, 2);

      /////////////////////////////////////////////
      // create contacts                         //
      /////////////////////////////////////////////
      Integer contactWithEmail = insertContact("Alice", "Test", "alice@example.com", null);
      Integer contactCeo = insertContact("Bob", "Boss", "bob@example.com", "CEO");
      Integer contactNoEmail = insertContact("Carol", "NoEmail", null, null);

      /////////////////////////////////////////////
      // run the process                         //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(CalculateLeadScoresProcess.NAME);
      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify scores                           //
      /////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));

      for(var record : queryOutput.getRecords())
      {
         Contact contact = new Contact(record);
         if(contact.getId().equals(contactWithEmail))
         {
            assertEquals(10, contact.getLeadScore()); // has email: +10
         }
         else if(contact.getId().equals(contactCeo))
         {
            assertEquals(35, contact.getLeadScore()); // has email: +10, CEO: +25
         }
         else if(contact.getId().equals(contactNoEmail))
         {
            assertEquals(0, contact.getLeadScore()); // no rules match
         }
      }

      /////////////////////////////////////////////
      // verify output counts                    //
      /////////////////////////////////////////////
      assertEquals(3, processOutput.getValues().get("totalContacts"));
      assertEquals(2, processOutput.getValues().get("totalRules"));
   }



   /*******************************************************************************
    ** Contacts whose score does not change should not be updated.
    *******************************************************************************/
   @Test
   void testUnchangedScoresSkipped() throws QException
   {
      insertRule("Has Email", "email", CrmScoreOperator.IS_SET.getId(), null, 0, 1);

      insertContact("Dave", "Zero", "dave@example.com", null);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(CalculateLeadScoresProcess.NAME);
      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // score is 0 -> 0, should not be counted  //
      /////////////////////////////////////////////
      assertEquals(0, processOutput.getValues().get("updatedCount"));
   }



   /***************************************************************************
    ** Helper: insert a LeadScoreRule.
    ***************************************************************************/
   private void insertRule(String name, String fieldPath, Integer operator,
                           String fieldValue, Integer scoreAdj, Integer sortOrder) throws QException
   {
      new InsertAction().execute(
         new InsertInput(LeadScoreRule.TABLE_NAME)
            .withRecordEntity(new LeadScoreRule()
               .withName(name)
               .withEntityType(CrmScorableEntity.CONTACT.getId())
               .withFieldPath(fieldPath)
               .withOperator(operator)
               .withFieldValue(fieldValue)
               .withScoreAdjustment(scoreAdj)
               .withIsActive(true)
               .withSortOrder(sortOrder)));
   }



   /***************************************************************************
    ** Helper: insert a Contact.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName, String email, String jobTitle) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(email)
               .withJobTitle(jobTitle)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
