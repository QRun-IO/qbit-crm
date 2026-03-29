/*******************************************************************************
 ** Unit tests for ImportContactsProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests for importing contacts: new inserts, duplicate skipping, validation.
 *******************************************************************************/
class ImportContactsProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Import new contacts and verify they are created with audit logs.
    *******************************************************************************/
   @Test
   void testImportNewContacts() throws QException
   {
      List<QRecord> records = new ArrayList<>();
      records.add(new QRecord()
         .withValue("firstName", "Alice")
         .withValue("lastName", "Smith")
         .withValue("email", "alice@example.com"));
      records.add(new QRecord()
         .withValue("firstName", "Bob")
         .withValue("lastName", "Jones")
         .withValue("email", "bob@example.com"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ImportContactsProcess.NAME);
      processInput.setRecords(records);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(2, processOutput.getValues().get("importedCount"));
      assertEquals(0, processOutput.getValues().get("skippedCount"));
      assertEquals(0, processOutput.getValues().get("errorCount"));

      QueryOutput contactQuery = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));
      assertEquals(2, contactQuery.getRecords().size());

      ///////////////////////////////////////////////////////////////////////////
      // 2 CREATED entries from CrmAuditLogCustomizer on contact insert + //
      // 2 IMPORTED entries from the import process itself = 4 total      //
      ///////////////////////////////////////////////////////////////////////////
      QueryOutput auditQuery = new QueryAction().execute(
         new QueryInput(AuditLog.TABLE_NAME));
      assertEquals(4, auditQuery.getRecords().size());
   }



   /*******************************************************************************
    ** Test duplicate skipping: pre-existing contact with same email is skipped.
    *******************************************************************************/
   @Test
   void testImportWithDuplicates() throws QException
   {
      /////////////////////////////////////////////
      // insert existing contact                 //
      /////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName("Existing")
               .withLastName("User")
               .withEmail("alice@example.com")
               .withOwnerUserId("user-001")));

      List<QRecord> records = new ArrayList<>();
      records.add(new QRecord()
         .withValue("firstName", "Alice")
         .withValue("lastName", "Smith")
         .withValue("email", "alice@example.com")); // duplicate
      records.add(new QRecord()
         .withValue("firstName", "Charlie")
         .withValue("lastName", "Brown")
         .withValue("email", "charlie@example.com")); // new

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ImportContactsProcess.NAME);
      processInput.setRecords(records);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("importedCount"));
      assertEquals(1, processOutput.getValues().get("skippedCount"));
      assertEquals(0, processOutput.getValues().get("errorCount"));
   }



   /*******************************************************************************
    ** Test validation: records missing required fields are counted as errors.
    *******************************************************************************/
   @Test
   void testImportWithValidationErrors() throws QException
   {
      List<QRecord> records = new ArrayList<>();
      records.add(new QRecord()
         .withValue("firstName", "")
         .withValue("lastName", "NoFirst")
         .withValue("email", "nofirst@example.com")); // missing firstName
      records.add(new QRecord()
         .withValue("firstName", "Valid")
         .withValue("lastName", "User")
         .withValue("email", "valid@example.com"));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(ImportContactsProcess.NAME);
      processInput.setRecords(records);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("importedCount"));
      assertEquals(0, processOutput.getValues().get("skippedCount"));
      assertEquals(1, processOutput.getValues().get("errorCount"));
   }

}
