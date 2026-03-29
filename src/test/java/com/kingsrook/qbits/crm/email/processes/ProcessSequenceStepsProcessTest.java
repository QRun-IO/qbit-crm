/*******************************************************************************
 ** Unit tests for ProcessSequenceStepsProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.processes;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
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
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests for ProcessSequenceStepsProcess: email step creates activity,
 ** failure counting, and enrollment completion.
 *******************************************************************************/
class ProcessSequenceStepsProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Email step should create an activity record.
    *******************************************************************************/
   @Test
   void testEmailStepCreatesActivity() throws QException
   {
      insertActivityType("Email");
      insertActivityType("Task");
      Integer sequenceId = insertSequence("Email Seq", true, 2);
      Integer contactId = insertContact("Alice", "Email", false);
      insertStep(sequenceId, 1, CrmSequenceStepType.EMAIL.getId(), 0, 0);
      insertStep(sequenceId, 2, CrmSequenceStepType.TASK.getId(), 1, 0);

      /////////////////////////////////////////////
      // create enrollment at step 0, due now    //
      /////////////////////////////////////////////
      Integer enrollmentId = insertEnrollment(sequenceId, contactId, 0,
         CrmEnrollmentStatus.ACTIVE.getId(), Instant.now().minus(1, ChronoUnit.MINUTES));

      /////////////////////////////////////////////
      // run the process                         //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ProcessSequenceStepsProcess.NAME);
      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify activity was created             //
      /////////////////////////////////////////////
      QueryOutput activityQuery = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      assertThat(activityQuery.getRecords()).isNotEmpty();
      assertThat(activityQuery.getRecords().get(0).getValueString("subject")).contains("Sequence email");

      /////////////////////////////////////////////
      // verify enrollment was advanced          //
      /////////////////////////////////////////////
      QueryOutput enrollmentQuery = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      SequenceEnrollment enrollment = new SequenceEnrollment(enrollmentQuery.getRecords().get(0));
      assertEquals(1, enrollment.getCurrentStepNumber());
   }



   /*******************************************************************************
    ** Enrollment should complete when all steps are processed.
    *******************************************************************************/
   @Test
   void testEnrollmentCompletion() throws QException
   {
      Integer sequenceId = insertSequence("Complete Seq", true, 1);
      Integer contactId = insertContact("Bob", "Complete", false);
      insertStep(sequenceId, 1, CrmSequenceStepType.WAIT.getId(), 0, 0);

      /////////////////////////////////////////////
      // enrollment at step 0, due now           //
      /////////////////////////////////////////////
      insertEnrollment(sequenceId, contactId, 0,
         CrmEnrollmentStatus.ACTIVE.getId(), Instant.now().minus(1, ChronoUnit.MINUTES));

      /////////////////////////////////////////////
      // run the process                         //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ProcessSequenceStepsProcess.NAME);
      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify enrollment is now COMPLETED      //
      /////////////////////////////////////////////
      QueryOutput enrollmentQuery = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      SequenceEnrollment enrollment = new SequenceEnrollment(enrollmentQuery.getRecords().get(0));
      assertEquals(CrmEnrollmentStatus.COMPLETED.getId(), enrollment.getStatus());
   }



   /*******************************************************************************
    ** After 3 failures, enrollment should be marked as FAILED.
    *******************************************************************************/
   @Test
   void testFailureCounting() throws QException
   {
      Integer sequenceId = insertSequence("Fail Seq", true, 2);
      Integer contactId = insertContact("Carol", "Fail", true); // doNotEmail = true
      insertStep(sequenceId, 1, CrmSequenceStepType.EMAIL.getId(), 0, 0);
      insertStep(sequenceId, 2, CrmSequenceStepType.EMAIL.getId(), 0, 0);

      /////////////////////////////////////////////
      // enrollment with failureCount=2          //
      /////////////////////////////////////////////
      InsertOutput enrollInsert = new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME)
            .withRecordEntity(new SequenceEnrollment()
               .withSequenceId(sequenceId)
               .withContactId(contactId)
               .withCurrentStepNumber(0)
               .withStatus(CrmEnrollmentStatus.ACTIVE.getId())
               .withEnrolledDate(Instant.now().minus(1, ChronoUnit.HOURS))
               .withEnrolledByUserId("user-001")
               .withNextStepDate(Instant.now().minus(1, ChronoUnit.MINUTES))
               .withFailureCount(2)));

      /////////////////////////////////////////////
      // run the process -- should fail again    //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ProcessSequenceStepsProcess.NAME);
      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify enrollment is FAILED             //
      /////////////////////////////////////////////
      QueryOutput enrollmentQuery = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      SequenceEnrollment enrollment = new SequenceEnrollment(enrollmentQuery.getRecords().get(0));
      assertEquals(CrmEnrollmentStatus.FAILED.getId(), enrollment.getStatus());
      assertEquals(3, enrollment.getFailureCount());
      assertThat(enrollment.getLastFailureMessage()).isNotNull();
   }



   /***************************************************************************
    ** Helper: insert an EmailSequence.
    ***************************************************************************/
   private Integer insertSequence(String name, boolean isActive, int totalSteps) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailSequence.TABLE_NAME)
            .withRecordEntity(new EmailSequence()
               .withName(name)
               .withOwnerUserId("user-001")
               .withIsActive(isActive)
               .withTotalSteps(totalSteps)
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



   /***************************************************************************
    ** Helper: insert a SequenceEnrollment.
    ***************************************************************************/
   private Integer insertEnrollment(Integer sequenceId, Integer contactId, Integer currentStep,
                                    Integer status, Instant nextStepDate) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME)
            .withRecordEntity(new SequenceEnrollment()
               .withSequenceId(sequenceId)
               .withContactId(contactId)
               .withCurrentStepNumber(currentStep)
               .withStatus(status)
               .withEnrolledDate(Instant.now().minus(1, ChronoUnit.HOURS))
               .withEnrolledByUserId("user-001")
               .withNextStepDate(nextStepDate)
               .withFailureCount(0)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an ActivityType and return its id.
    ***************************************************************************/
   private void insertActivityType(String name) throws QException
   {
      new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName("event")
               .withIsSystem(true)
               .withSortOrder(1)
               .withIsActive(true)));
   }

}
