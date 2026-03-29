/*******************************************************************************
 ** Unit tests for ConvertFormToContactProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.core.model.LeadSource;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for form-to-contact conversion: new contact creation, duplicate
 ** detection, form update, and note activity creation.
 *******************************************************************************/
class ConvertFormToContactProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Submit a form, run conversion, verify contact created and form updated.
    *******************************************************************************/
   @Test
   void testConvertNewForm() throws QException
   {
      ///////////////////////////////////////////////
      // set up reference data                     //
      ///////////////////////////////////////////////
      insertLeadSource("Website");
      insertActivityType("Note");

      ///////////////////////////////////////////////
      // insert a form submission                  //
      ///////////////////////////////////////////////
      Integer formId = insertFormSubmission("john@example.com", "John", "Doe", "555-1234", "I need a quote");

      ///////////////////////////////////////////////
      // run the conversion process               //
      ///////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ConvertFormToContactProcess.NAME);
      processInput.addValue("formSubmissionId", formId);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      ///////////////////////////////////////////////
      // verify a new contact was created          //
      ///////////////////////////////////////////////
      QueryOutput contactQuery = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));

      assertEquals(1, contactQuery.getRecords().size());
      Contact contact = new Contact(contactQuery.getRecords().get(0));
      assertEquals("john@example.com", contact.getEmail());
      assertEquals("John", contact.getFirstName());
      assertEquals("Doe", contact.getLastName());
      assertEquals("555-1234", contact.getPhone());

      ///////////////////////////////////////////////
      // verify the form was updated               //
      ///////////////////////////////////////////////
      QueryOutput formQuery = new QueryAction().execute(
         new QueryInput(FormSubmission.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, formId))));

      FormSubmission updatedForm = new FormSubmission(formQuery.getRecords().get(0));
      assertEquals(CrmFormConversionStatus.CONVERTED_TO_CONTACT.getId(), updatedForm.getConversionStatus());
      assertNotNull(updatedForm.getConvertedDate());
      assertEquals(contact.getId(), updatedForm.getContactId());

      ///////////////////////////////////////////////
      // verify a note activity was created        //
      ///////////////////////////////////////////////
      QueryOutput activityQuery = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      assertEquals(1, activityQuery.getRecords().size());
      Activity activity = new Activity(activityQuery.getRecords().get(0));
      assertEquals("Form Submission Converted", activity.getSubject());
      assertEquals("I need a quote", activity.getDescription());
      assertEquals(contact.getId(), activity.getContactId());
   }



   /*******************************************************************************
    ** Test that converting a form with an existing contact uses that contact.
    *******************************************************************************/
   @Test
   void testConvertFormWithExistingContact() throws QException
   {
      insertLeadSource("Website");
      insertActivityType("Note");

      /////////////////////////////////////////////////
      // insert an existing contact with same email  //
      /////////////////////////////////////////////////
      InsertOutput contactInsert = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail("existing@example.com")
               .withFirstName("Existing")
               .withLastName("User")
               .withOwnerUserId("user-001")));
      Integer existingContactId = contactInsert.getRecords().get(0).getValueInteger("id");

      /////////////////////////////////////////////
      // insert a form with the same email       //
      /////////////////////////////////////////////
      Integer formId = insertFormSubmission("existing@example.com", "Existing", "User", null, "Hello again");

      /////////////////////////////////////////////
      // run conversion                          //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ConvertFormToContactProcess.NAME);
      processInput.addValue("formSubmissionId", formId);

      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify no new contact was created       //
      /////////////////////////////////////////////
      QueryOutput contactQuery = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));
      assertEquals(1, contactQuery.getRecords().size());

      /////////////////////////////////////////////
      // verify the form links to existing       //
      /////////////////////////////////////////////
      QueryOutput formQuery = new QueryAction().execute(
         new QueryInput(FormSubmission.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, formId))));

      FormSubmission updatedForm = new FormSubmission(formQuery.getRecords().get(0));
      assertEquals(existingContactId, updatedForm.getContactId());
      assertEquals(CrmFormConversionStatus.CONVERTED_TO_CONTACT.getId(), updatedForm.getConversionStatus());
   }



   /***************************************************************************
    ** Helper: insert a FormSubmission and return its id.
    ***************************************************************************/
   private Integer insertFormSubmission(String email, String firstName, String lastName, String phone, String message) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(FormSubmission.TABLE_NAME).withRecordEntity(
            new FormSubmission()
               .withFormType(1)
               .withEmail(email)
               .withFirstName(firstName)
               .withLastName(lastName)
               .withPhone(phone)
               .withMessage(message)
               .withConversionStatus(CrmFormConversionStatus.NEW.getId())));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a LeadSource.
    ***************************************************************************/
   private void insertLeadSource(String name) throws QException
   {
      new InsertAction().execute(
         new InsertInput(LeadSource.TABLE_NAME).withRecordEntity(
            new LeadSource()
               .withName(name)
               .withSortOrder(1)
               .withIsActive(true)));
   }



   /***************************************************************************
    ** Helper: insert an ActivityType.
    ***************************************************************************/
   private void insertActivityType(String name) throws QException
   {
      new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME).withRecordEntity(
            new ActivityType()
               .withName(name)
               .withIconName("note")
               .withIsSystem(false)
               .withSortOrder(1)
               .withIsActive(true)));
   }

}
