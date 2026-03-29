/*******************************************************************************
 ** Unit tests for EnrollInSequenceProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmSequenceStepType;
import com.kingsrook.qbits.crm.email.model.EmailSequence;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qbits.crm.email.model.SequenceStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Tests for EnrollInSequenceProcess: successful enrollment, duplicate
 ** rejection, and doNotEmail rejection.
 *******************************************************************************/
class EnrollInSequenceProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Enroll a contact and verify enrollment record is created.
    *******************************************************************************/
   @Test
   void testEnrollContact() throws QException
   {
      Integer sequenceId = insertSequence("Welcome", true);
      Integer contactId = insertContact("Alice", "Test", false);
      insertStep(sequenceId, 1, CrmSequenceStepType.EMAIL.getId(), 1, 0);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(EnrollInSequenceProcess.NAME);
      processInput.addValue("sequenceId", sequenceId);
      processInput.addValue("contactId", contactId);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals("ENROLLED", processOutput.getValues().get("action"));

      /////////////////////////////////////////////
      // verify enrollment record was created    //
      /////////////////////////////////////////////
      QueryOutput enrollmentQuery = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      assertEquals(1, enrollmentQuery.getRecords().size());
      SequenceEnrollment enrollment = new SequenceEnrollment(enrollmentQuery.getRecords().get(0));
      assertEquals(sequenceId, enrollment.getSequenceId());
      assertEquals(contactId, enrollment.getContactId());
      assertEquals(CrmEnrollmentStatus.ACTIVE.getId(), enrollment.getStatus());
      assertEquals(0, enrollment.getCurrentStepNumber());
      assertNotNull(enrollment.getEnrolledDate());
      assertNotNull(enrollment.getNextStepDate());
   }



   /*******************************************************************************
    ** Duplicate enrollment should be rejected.
    *******************************************************************************/
   @Test
   void testDuplicateEnrollmentRejected() throws QException
   {
      Integer sequenceId = insertSequence("Dup Test", true);
      Integer contactId = insertContact("Bob", "Dup", false);

      /////////////////////////////////////////////
      // first enrollment                        //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(EnrollInSequenceProcess.NAME);
      processInput.addValue("sequenceId", sequenceId);
      processInput.addValue("contactId", contactId);

      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // second enrollment should throw          //
      /////////////////////////////////////////////
      RunProcessInput processInput2 = new RunProcessInput();
      processInput2.setProcessName(EnrollInSequenceProcess.NAME);
      processInput2.addValue("sequenceId", sequenceId);
      processInput2.addValue("contactId", contactId);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput2));
   }



   /*******************************************************************************
    ** doNotEmail contact should be rejected.
    *******************************************************************************/
   @Test
   void testDoNotEmailRejected() throws QException
   {
      Integer sequenceId = insertSequence("DNE Test", true);
      Integer contactId = insertContact("Carol", "NoEmail", true);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(EnrollInSequenceProcess.NAME);
      processInput.addValue("sequenceId", sequenceId);
      processInput.addValue("contactId", contactId);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



   /*******************************************************************************
    ** Inactive sequence should be rejected.
    *******************************************************************************/
   @Test
   void testInactiveSequenceRejected() throws QException
   {
      Integer sequenceId = insertSequence("Inactive", false);
      Integer contactId = insertContact("Dave", "Test", false);

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(EnrollInSequenceProcess.NAME);
      processInput.addValue("sequenceId", sequenceId);
      processInput.addValue("contactId", contactId);

      assertThrows(QUserFacingException.class, () ->
         new RunProcessAction().execute(processInput));
   }



   /***************************************************************************
    ** Helper: insert an EmailSequence.
    ***************************************************************************/
   private Integer insertSequence(String name, boolean isActive) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailSequence.TABLE_NAME)
            .withRecordEntity(new EmailSequence()
               .withName(name)
               .withOwnerUserId("user-001")
               .withIsActive(isActive)
               .withBusinessDaysOnly(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Contact.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName, boolean doNotEmail) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(firstName.toLowerCase() + "@example.com")
               .withOwnerUserId("user-001")
               .withDoNotEmail(doNotEmail)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a SequenceStep.
    ***************************************************************************/
   private void insertStep(Integer sequenceId, Integer stepNumber, Integer stepType,
                           Integer delayDays, Integer delayHours) throws QException
   {
      new InsertAction().execute(
         new InsertInput(SequenceStep.TABLE_NAME)
            .withRecordEntity(new SequenceStep()
               .withSequenceId(sequenceId)
               .withStepNumber(stepNumber)
               .withStepType(stepType)
               .withDelayDays(delayDays)
               .withDelayHours(delayHours)));
   }

}
