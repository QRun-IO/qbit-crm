/*******************************************************************************
 ** Unit tests for DetectDuplicatesProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for detecting duplicate contacts and companies.
 *******************************************************************************/
class DetectDuplicatesProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Insert contacts with duplicate emails and detect them.
    *******************************************************************************/
   @Test
   void testDetectContactDuplicates() throws QException
   {
      ///////////////////////////////////////
      // insert contacts with same email   //
      ///////////////////////////////////////
      insertContact("dupe@example.com", "Alice", "Smith");
      insertContact("dupe@example.com", "Alice2", "Smith2");
      insertContact("unique@example.com", "Bob", "Jones");

      ////////////////////////////////////
      // run detect duplicates          //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(DetectDuplicatesProcess.NAME);
      processInput.addValue("entityType", "CONTACT");

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("duplicateGroupCount"));
      assertEquals(1, processOutput.getRecords().size());
      assertEquals(2, processOutput.getRecords().get(0).getValueInteger("count"));
   }



   /*******************************************************************************
    ** Insert companies with duplicate domains and detect them.
    *******************************************************************************/
   @Test
   void testDetectCompanyDuplicates() throws QException
   {
      insertCompany("Corp A", "dupe.com");
      insertCompany("Corp B", "dupe.com");
      insertCompany("Corp C", "unique.com");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(DetectDuplicatesProcess.NAME);
      processInput.addValue("entityType", "COMPANY");

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("duplicateGroupCount"));
   }



   /*******************************************************************************
    ** No duplicates should return empty results.
    *******************************************************************************/
   @Test
   void testNoDuplicates() throws QException
   {
      insertContact("one@example.com", "One", "User");
      insertContact("two@example.com", "Two", "User");

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(DetectDuplicatesProcess.NAME);
      processInput.addValue("entityType", "CONTACT");

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(0, processOutput.getValues().get("duplicateGroupCount"));
      assertTrue(processOutput.getRecords().isEmpty());
   }



   private void insertContact(String email, String firstName, String lastName) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail(email)
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
   }



   private void insertCompany(String name, String domain) throws QException
   {
      new InsertAction().execute(
         new InsertInput(Company.TABLE_NAME).withRecordEntity(
            new Company()
               .withName(name)
               .withDomain(domain)
               .withOwnerUserId("user-001")));
   }

}
