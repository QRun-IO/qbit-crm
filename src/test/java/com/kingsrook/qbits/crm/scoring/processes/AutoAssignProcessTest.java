/*******************************************************************************
 ** Unit tests for AutoAssignProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRule;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRuleMember;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for AutoAssignProcess: specific user assignment, round robin
 ** assignment, and no-match fallback.
 *******************************************************************************/
class AutoAssignProcessTest extends BaseTest
{

   /*******************************************************************************
    ** SPECIFIC_USER strategy assigns to the configured user.
    *******************************************************************************/
   @Test
   void testSpecificUserAssignment() throws QException
   {
      /////////////////////////////////////////////
      // create a specific-user rule             //
      /////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Assign to Sales Lead")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.SPECIFIC_USER.getId())
               .withAssignToUserId("sales-lead-001")
               .withIsActive(true)
               .withSortOrder(1)));

      /////////////////////////////////////////////
      // create contact                          //
      /////////////////////////////////////////////
      Integer contactId = insertContact("Alice", "Test");

      /////////////////////////////////////////////
      // run auto-assign                         //
      /////////////////////////////////////////////
      RunProcessOutput processOutput = runAutoAssign("CONTACT", contactId);

      assertEquals("ASSIGNED", processOutput.getValues().get("action"));
      assertEquals("sales-lead-001", processOutput.getValues().get("assignedUserId"));

      /////////////////////////////////////////////
      // verify contact owner was updated        //
      /////////////////////////////////////////////
      GetOutput contactGet = new GetAction().execute(
         new GetInput(Contact.TABLE_NAME).withPrimaryKey(contactId));
      Contact contact = new Contact(contactGet.getRecord());
      assertEquals("sales-lead-001", contact.getOwnerUserId());
   }



   /*******************************************************************************
    ** ROUND_ROBIN strategy cycles through active members.
    *******************************************************************************/
   @Test
   void testRoundRobinAssignment() throws QException
   {
      /////////////////////////////////////////////
      // create a round-robin rule with members  //
      /////////////////////////////////////////////
      InsertOutput ruleInsert = new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Round Robin")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.ROUND_ROBIN.getId())
               .withIsActive(true)
               .withSortOrder(1)));

      Integer ruleId = ruleInsert.getRecords().get(0).getValueInteger("id");

      new InsertAction().execute(
         new InsertInput(AssignmentRuleMember.TABLE_NAME)
            .withRecordEntity(new AssignmentRuleMember()
               .withAssignmentRuleId(ruleId)
               .withUserId("user-A")
               .withSortOrder(1)
               .withIsActive(true)));

      new InsertAction().execute(
         new InsertInput(AssignmentRuleMember.TABLE_NAME)
            .withRecordEntity(new AssignmentRuleMember()
               .withAssignmentRuleId(ruleId)
               .withUserId("user-B")
               .withSortOrder(2)
               .withIsActive(true)));

      /////////////////////////////////////////////
      // first contact should go to user-A       //
      /////////////////////////////////////////////
      Integer contactId1 = insertContact("Bob", "First");
      RunProcessOutput output1 = runAutoAssign("CONTACT", contactId1);
      assertEquals("ASSIGNED", output1.getValues().get("action"));
      assertEquals("user-A", output1.getValues().get("assignedUserId"));

      /////////////////////////////////////////////
      // second contact should go to user-B      //
      /////////////////////////////////////////////
      Integer contactId2 = insertContact("Carol", "Second");
      RunProcessOutput output2 = runAutoAssign("CONTACT", contactId2);
      assertEquals("ASSIGNED", output2.getValues().get("action"));
      assertEquals("user-B", output2.getValues().get("assignedUserId"));
   }



   /*******************************************************************************
    ** No matching rules should leave the existing owner unchanged.
    *******************************************************************************/
   @Test
   void testNoMatchFallback() throws QException
   {
      Integer contactId = insertContact("Eve", "NoRule");

      RunProcessOutput processOutput = runAutoAssign("CONTACT", contactId);

      assertEquals("NO_MATCH", processOutput.getValues().get("action"));

      /////////////////////////////////////////////
      // verify owner is unchanged               //
      /////////////////////////////////////////////
      GetOutput contactGet = new GetAction().execute(
         new GetInput(Contact.TABLE_NAME).withPrimaryKey(contactId));
      Contact contact = new Contact(contactGet.getRecord());
      assertEquals("user-001", contact.getOwnerUserId());
   }



   /***************************************************************************
    ** Helper: insert a Contact.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(firstName.toLowerCase() + "@example.com")
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: run the auto-assign process.
    ***************************************************************************/
   private RunProcessOutput runAutoAssign(String entityType, Integer recordId) throws QException
   {
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(AutoAssignProcess.NAME);
      processInput.addValue("entityType", entityType);
      processInput.addValue("recordId", recordId);
      return (new RunProcessAction().execute(processInput));
   }

}
