/*******************************************************************************
 ** Additional coverage tests for CalculateLeadScoresProcess: operators
 ** CONTAINS, GT, LT, NOT_EQUALS, IS_NOT_SET, and invalid operator.
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmScorableEntity;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
import com.kingsrook.qbits.crm.scoring.model.LeadScoreRule;
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


/*******************************************************************************
 ** Additional operator tests for CalculateLeadScoresProcess.
 *******************************************************************************/
class CalculateLeadScoresCoverageTest extends BaseTest
{

   /*******************************************************************************
    ** Test CONTAINS operator.
    *******************************************************************************/
   @Test
   void testContainsOperator() throws QException
   {
      insertRule("Contains gmail", "email", CrmScoreOperator.CONTAINS.getId(), "gmail", 15, 1);

      Integer contactId = insertContact("Alice", "Test", "alice@gmail.com", null, null);

      runScoring();

      Contact contact = getContact(contactId);
      assertEquals(15, contact.getLeadScore());
   }



   /*******************************************************************************
    ** Test NOT_EQUALS operator.
    *******************************************************************************/
   @Test
   void testNotEqualsOperator() throws QException
   {
      insertRule("Not CEO", "jobTitle", CrmScoreOperator.NOT_EQUALS.getId(), "CEO", 5, 1);

      Integer ctoId = insertContact("Bob", "NotCEO", "bob@example.com", "CTO", null);
      Integer ceoId = insertContact("Carol", "IsCEO", "carol@example.com", "CEO", null);

      runScoring();

      Contact cto = getContact(ctoId);
      assertEquals(5, cto.getLeadScore()); // not CEO: +5

      Contact ceo = getContact(ceoId);
      assertEquals(0, ceo.getLeadScore()); // is CEO: no match
   }



   /*******************************************************************************
    ** Test GT operator (leadScore as string).
    *******************************************************************************/
   @Test
   void testGtOperator() throws QException
   {
      insertRule("Score > 50", "leadScore", CrmScoreOperator.GT.getId(), "50", 20, 1);

      Integer highId = insertContact("Dave", "High", "dave@example.com", null, 75);
      Integer lowId  = insertContact("Eve", "Low", "eve@example.com", null, 25);

      runScoring();

      Contact high = getContact(highId);
      assertEquals(20, high.getLeadScore()); // 75 > 50 => +20

      Contact low = getContact(lowId);
      assertEquals(0, low.getLeadScore()); // 25 > 50 => false
   }



   /*******************************************************************************
    ** Test LT operator.
    *******************************************************************************/
   @Test
   void testLtOperator() throws QException
   {
      insertRule("Score < 30", "leadScore", CrmScoreOperator.LT.getId(), "30", 10, 1);

      Integer lowId  = insertContact("Frank", "Low", "frank@example.com", null, 10);
      Integer highId = insertContact("Grace", "High", "grace@example.com", null, 50);

      runScoring();

      Contact low = getContact(lowId);
      assertEquals(10, low.getLeadScore()); // 10 < 30 => +10

      Contact high = getContact(highId);
      assertEquals(0, high.getLeadScore()); // 50 < 30 => false
   }



   /*******************************************************************************
    ** Test IS_NOT_SET operator.
    *******************************************************************************/
   @Test
   void testIsNotSetOperator() throws QException
   {
      insertRule("No phone", "phone", CrmScoreOperator.IS_NOT_SET.getId(), null, -5, 1);

      Integer noPhoneId   = insertContact("Hank", "NoPhone", "hank@example.com", null, null);
      Integer withPhoneId = insertContactWithPhone("Ivy", "HasPhone", "ivy@example.com", "555-1234");

      runScoring();

      Contact noPhone = getContact(noPhoneId);
      assertEquals(-5, noPhone.getLeadScore()); // no phone => -5

      Contact withPhone = getContact(withPhoneId);
      assertEquals(0, withPhone.getLeadScore()); // has phone => no match
   }



   /*******************************************************************************
    ** Test invalid (null) operator returns false.
    *******************************************************************************/
   @Test
   void testInvalidOperator() throws QException
   {
      // Use id 999 which does not map to any CrmScoreOperator
      insertRule("Bad Operator", "email", 999, null, 100, 1);

      Integer contactId = insertContact("Jim", "Test", "jim@example.com", null, null);

      RunProcessOutput output = runScoring();

      Contact contact = getContact(contactId);
      assertEquals(0, contact.getLeadScore()); // invalid operator => no match
   }



   /*******************************************************************************
    ** Test CONTAINS with null actual value.
    *******************************************************************************/
   @Test
   void testContainsNullActual() throws QException
   {
      insertRule("Contains test", "email", CrmScoreOperator.CONTAINS.getId(), "test", 10, 1);

      Integer contactId = insertContact("Kim", "Null", null, null, null);

      runScoring();

      Contact contact = getContact(contactId);
      assertEquals(0, contact.getLeadScore()); // null email => CONTAINS is false
   }



   /*******************************************************************************
    ** Test GT with non-numeric values.
    *******************************************************************************/
   @Test
   void testGtNonNumeric() throws QException
   {
      insertRule("Name GT", "firstName", CrmScoreOperator.GT.getId(), "100", 10, 1);

      Integer contactId = insertContact("Alice", "Test", "alice@example.com", null, null);

      runScoring();

      Contact contact = getContact(contactId);
      assertEquals(0, contact.getLeadScore()); // non-numeric => compareNumeric returns 0
   }



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



   private Integer insertContact(String firstName, String lastName, String email,
                                   String jobTitle, Integer leadScore) throws QException
   {
      Contact contact = new Contact()
         .withFirstName(firstName)
         .withLastName(lastName)
         .withEmail(email)
         .withJobTitle(jobTitle)
         .withOwnerUserId("user-001");

      if(leadScore != null)
      {
         contact.withLeadScore(leadScore);
      }

      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(contact));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertContactWithPhone(String firstName, String lastName,
                                            String email, String phone) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(email)
               .withPhone(phone)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private RunProcessOutput runScoring() throws QException
   {
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(CalculateLeadScoresProcess.NAME);
      return (new RunProcessAction().execute(processInput));
   }



   private Contact getContact(Integer id) throws QException
   {
      GetOutput getOutput = new GetAction().execute(
         new GetInput(Contact.TABLE_NAME).withPrimaryKey(id));
      return (new Contact(getOutput.getRecord()));
   }

}
