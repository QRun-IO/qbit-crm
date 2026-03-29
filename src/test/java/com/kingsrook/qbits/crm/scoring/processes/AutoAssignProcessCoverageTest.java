/*******************************************************************************
 ** Additional coverage tests for AutoAssignProcess: LEAST_ACTIVE strategy,
 ** criteria JSON matching, invalid entity type, missing record.
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
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
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Additional tests for AutoAssignProcess.
 *******************************************************************************/
class AutoAssignProcessCoverageTest extends BaseTest
{

   /*******************************************************************************
    ** LEAST_ACTIVE strategy assigns to user with oldest lastAssignedDate.
    *******************************************************************************/
   @Test
   void testLeastActiveAssignment() throws QException
   {
      InsertOutput ruleInsert = new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Least Active")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.LEAST_ACTIVE.getId())
               .withIsActive(true)
               .withSortOrder(1)));

      Integer ruleId = ruleInsert.getRecords().get(0).getValueInteger("id");

      // user-A has a recent assignment, user-B has no assignment
      new InsertAction().execute(
         new InsertInput(AssignmentRuleMember.TABLE_NAME)
            .withRecordEntity(new AssignmentRuleMember()
               .withAssignmentRuleId(ruleId)
               .withUserId("user-A")
               .withSortOrder(1)
               .withIsActive(true)
               .withLastAssignedDate(Instant.now())));

      new InsertAction().execute(
         new InsertInput(AssignmentRuleMember.TABLE_NAME)
            .withRecordEntity(new AssignmentRuleMember()
               .withAssignmentRuleId(ruleId)
               .withUserId("user-B")
               .withSortOrder(2)
               .withIsActive(true)));

      Integer contactId = insertContact("Alice", "Test");

      RunProcessOutput output = runAutoAssign("CONTACT", contactId);
      assertEquals("ASSIGNED", output.getValues().get("action"));
      // user-B has null lastAssignedDate => treated as EPOCH => least active
      assertEquals("user-B", output.getValues().get("assignedUserId"));
   }



   /*******************************************************************************
    ** Criteria JSON matching: rule with criteria that matches.
    *******************************************************************************/
   @Test
   void testCriteriaJsonMatch() throws QException
   {
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("State CO Rule")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.SPECIFIC_USER.getId())
               .withAssignToUserId("co-rep")
               .withCriteriaJson("{\"state\":\"CO\"}")
               .withIsActive(true)
               .withSortOrder(1)));

      Integer contactId = insertContactWithState("Bob", "Test", "CO");

      RunProcessOutput output = runAutoAssign("CONTACT", contactId);
      assertEquals("ASSIGNED", output.getValues().get("action"));
      assertEquals("co-rep", output.getValues().get("assignedUserId"));
   }



   /*******************************************************************************
    ** Criteria JSON that does NOT match should skip the rule.
    *******************************************************************************/
   @Test
   void testCriteriaJsonNoMatch() throws QException
   {
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("State TX Rule")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.SPECIFIC_USER.getId())
               .withAssignToUserId("tx-rep")
               .withCriteriaJson("{\"state\":\"TX\"}")
               .withIsActive(true)
               .withSortOrder(1)));

      Integer contactId = insertContactWithState("Carol", "Test", "CO");

      RunProcessOutput output = runAutoAssign("CONTACT", contactId);
      assertEquals("NO_MATCH", output.getValues().get("action"));
   }



   /*******************************************************************************
    ** Invalid entity type returns NO_MATCH.
    *******************************************************************************/
   @Test
   void testInvalidEntityType() throws QException
   {
      Integer contactId = insertContact("Eve", "Invalid");

      RunProcessOutput output = runAutoAssign("NONEXISTENT", contactId);
      assertEquals("NO_MATCH", output.getValues().get("action"));
   }



   /*******************************************************************************
    ** Non-existent record returns RECORD_NOT_FOUND.
    *******************************************************************************/
   @Test
   void testRecordNotFound() throws QException
   {
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Find Contact")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.SPECIFIC_USER.getId())
               .withAssignToUserId("user-x")
               .withIsActive(true)
               .withSortOrder(1)));

      RunProcessOutput output = runAutoAssign("CONTACT", 99999);
      assertEquals("RECORD_NOT_FOUND", output.getValues().get("action"));
   }



   /*******************************************************************************
    ** Test assignment for DEAL entity type.
    *******************************************************************************/
   @Test
   void testDealEntityType() throws QException
   {
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Assign Deal")
               .withEntityType(CrmAssignableEntity.DEAL.getId())
               .withAssignStrategy(CrmAssignStrategy.SPECIFIC_USER.getId())
               .withAssignToUserId("deal-owner")
               .withIsActive(true)
               .withSortOrder(1)));

      Integer dealId = insertDeal("Test Deal");

      RunProcessOutput output = runAutoAssign("DEAL", dealId);
      assertEquals("ASSIGNED", output.getValues().get("action"));
      assertEquals("deal-owner", output.getValues().get("assignedUserId"));
   }



   /*******************************************************************************
    ** Test LEAST_ACTIVE with no members returns NO_MATCH.
    *******************************************************************************/
   @Test
   void testLeastActiveNoMembers() throws QException
   {
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Empty Least Active")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.LEAST_ACTIVE.getId())
               .withIsActive(true)
               .withSortOrder(1)));

      Integer contactId = insertContact("Lonely", "User");

      RunProcessOutput output = runAutoAssign("CONTACT", contactId);
      assertEquals("NO_MATCH", output.getValues().get("action"));
   }



   /*******************************************************************************
    ** Test using entity label for type resolution (e.g., "Contact" instead of "CONTACT").
    *******************************************************************************/
   @Test
   void testEntityTypeLabelResolution() throws QException
   {
      new InsertAction().execute(
         new InsertInput(AssignmentRule.TABLE_NAME)
            .withRecordEntity(new AssignmentRule()
               .withName("Label Test")
               .withEntityType(CrmAssignableEntity.CONTACT.getId())
               .withAssignStrategy(CrmAssignStrategy.SPECIFIC_USER.getId())
               .withAssignToUserId("label-user")
               .withIsActive(true)
               .withSortOrder(1)));

      Integer contactId = insertContact("Label", "Test");

      RunProcessOutput output = runAutoAssign(CrmAssignableEntity.CONTACT.getLabel(), contactId);
      assertEquals("ASSIGNED", output.getValues().get("action"));
   }



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



   private Integer insertContactWithState(String firstName, String lastName, String state) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(firstName.toLowerCase() + "@example.com")
               .withState(state)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertDeal(String name) throws QException
   {
      Integer pipelineId = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME).withRecordEntity(
            new Pipeline().withName("Sales").withIsActive(true).withIsDefault(false)))
         .getRecords().get(0).getValueInteger("id");

      Integer stageId = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME).withRecordEntity(
            new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Prospect")
               .withSortOrder(1)
               .withProbabilityPct(25)))
         .getRecords().get(0).getValueInteger("id");

      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(new BigDecimal("10000"))
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private RunProcessOutput runAutoAssign(String entityType, Integer recordId) throws QException
   {
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(AutoAssignProcess.NAME);
      processInput.addValue("entityType", entityType);
      processInput.addValue("recordId", recordId);
      return (new RunProcessAction().execute(processInput));
   }

}
