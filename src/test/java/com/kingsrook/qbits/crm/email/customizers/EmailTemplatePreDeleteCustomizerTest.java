/*******************************************************************************
 ** Unit tests for EmailTemplatePreDeleteCustomizer.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.customizers;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmSequenceStepType;
import com.kingsrook.qbits.crm.email.model.EmailSequence;
import com.kingsrook.qbits.crm.email.model.EmailTemplate;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qbits.crm.email.model.SequenceStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for EmailTemplatePreDeleteCustomizer: verifies that templates
 ** referenced by active sequence enrollments cannot be deleted, while
 ** unreferenced templates and those without active enrollments can.
 *******************************************************************************/
class EmailTemplatePreDeleteCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Delete a template that has no referencing sequence steps -- should succeed.
    *******************************************************************************/
   @Test
   void testDeleteTemplateWithNoSequencesSucceeds() throws QException
   {
      Integer templateId = insertTemplate("Standalone Template");

      DeleteOutput deleteOutput = new DeleteAction().execute(
         new DeleteInput(EmailTemplate.TABLE_NAME).withPrimaryKeys(java.util.List.of(templateId)));

      /////////////////////////////////////////////
      // verify template is deleted              //
      /////////////////////////////////////////////
      assertNull(GetAction.execute(EmailTemplate.TABLE_NAME, templateId));
      assertTrue(deleteOutput.getRecordsWithErrors().isEmpty());
   }



   /*******************************************************************************
    ** Delete a template referenced by a sequence step with an active enrollment
    ** -- should be blocked with an error on the record.
    *******************************************************************************/
   @Test
   void testDeleteTemplateWithActiveSequenceBlocked() throws QException
   {
      Integer templateId  = insertTemplate("Active Sequence Template");
      Integer sequenceId  = insertSequence("Onboarding Sequence");
      insertSequenceStep(sequenceId, 1, templateId);

      Integer contactId = insertContact("active@example.com", "Active", "User");
      insertActiveEnrollment(sequenceId, contactId);

      DeleteOutput deleteOutput = new DeleteAction().execute(
         new DeleteInput(EmailTemplate.TABLE_NAME).withPrimaryKeys(java.util.List.of(templateId)));

      /////////////////////////////////////////////
      // verify template still exists            //
      /////////////////////////////////////////////
      assertNotNull(GetAction.execute(EmailTemplate.TABLE_NAME, templateId));

      /////////////////////////////////////////////////////
      // verify delete output contains errors             //
      /////////////////////////////////////////////////////
      assertTrue(!deleteOutput.getRecordsWithErrors().isEmpty());
   }



   /*******************************************************************************
    ** Delete a template referenced by a sequence step with no active enrollments
    ** -- should succeed because there are no active enrollments.
    *******************************************************************************/
   @Test
   void testDeleteTemplateWithNoActiveEnrollmentsSucceeds() throws QException
   {
      Integer templateId = insertTemplate("Inactive Sequence Template");
      Integer sequenceId = insertSequence("Old Sequence");
      insertSequenceStep(sequenceId, 1, templateId);

      /////////////////////////////////////////////
      // no enrollments at all for this sequence //
      /////////////////////////////////////////////
      DeleteOutput deleteOutput = new DeleteAction().execute(
         new DeleteInput(EmailTemplate.TABLE_NAME).withPrimaryKeys(java.util.List.of(templateId)));

      /////////////////////////////////////////////
      // verify template is deleted              //
      /////////////////////////////////////////////
      assertNull(GetAction.execute(EmailTemplate.TABLE_NAME, templateId));
      assertTrue(deleteOutput.getRecordsWithErrors().isEmpty());
   }



   /***************************************************************************
    ** Helper: insert an EmailTemplate and return its id.
    ***************************************************************************/
   private Integer insertTemplate(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailTemplate.TABLE_NAME).withRecordEntity(
            new EmailTemplate()
               .withName(name)
               .withSubject("Subject for " + name)
               .withBodyHtml("<p>Body</p>")
               .withBodyText("Body")
               .withOwnerUserId("user-001")
               .withIsShared(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an EmailSequence and return its id.
    ***************************************************************************/
   private Integer insertSequence(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailSequence.TABLE_NAME).withRecordEntity(
            new EmailSequence()
               .withName(name)
               .withOwnerUserId("user-001")
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a SequenceStep referencing a template.
    ***************************************************************************/
   private Integer insertSequenceStep(Integer sequenceId, Integer stepNumber, Integer templateId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(SequenceStep.TABLE_NAME).withRecordEntity(
            new SequenceStep()
               .withSequenceId(sequenceId)
               .withStepNumber(stepNumber)
               .withStepType(CrmSequenceStepType.EMAIL.getPossibleValueId())
               .withDelayDays(1)
               .withEmailTemplateId(templateId)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an active SequenceEnrollment.
    ***************************************************************************/
   private Integer insertActiveEnrollment(Integer sequenceId, Integer contactId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME).withRecordEntity(
            new SequenceEnrollment()
               .withSequenceId(sequenceId)
               .withContactId(contactId)
               .withCurrentStepNumber(0)
               .withStatus(CrmEnrollmentStatus.ACTIVE.getPossibleValueId())
               .withEnrolledDate(Instant.now())
               .withEnrolledByUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String email, String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail(email)
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
