/*******************************************************************************
 ** Unit tests for BulkAddTagProcess and BulkRemoveTagProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.ContactTag;
import com.kingsrook.qbits.crm.core.model.Tag;
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
 ** Tests for BulkAddTag and BulkRemoveTag processes: adding tags, skipping
 ** duplicates, and removing tags.
 *******************************************************************************/
class BulkAddTagProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Add a tag to multiple contacts and verify junction rows are created.
    *******************************************************************************/
   @Test
   void testBulkAddTag() throws QException
   {
      ///////////////////////////////////////
      // set up contacts and a tag         //
      ///////////////////////////////////////
      Integer contactId1 = insertContact("Alice", "A");
      Integer contactId2 = insertContact("Bob", "B");
      Integer tagId = insertTag("VIP");

      ///////////////////////////////////////
      // run the BulkAddTag process        //
      ///////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(BulkAddTagProcess.NAME);
      processInput.addValue("tagId", tagId);
      processInput.addValue("recordIds", contactId1 + "," + contactId2);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(2, processOutput.getValues().get("addedCount"));
      assertEquals(0, processOutput.getValues().get("skippedCount"));

      ///////////////////////////////////////
      // verify junction rows created      //
      ///////////////////////////////////////
      QueryOutput tagQuery = new QueryAction().execute(
         new QueryInput(ContactTag.TABLE_NAME));
      assertEquals(2, tagQuery.getRecords().size());
   }



   /*******************************************************************************
    ** Skip duplicate when the tag already exists on a contact.
    *******************************************************************************/
   @Test
   void testBulkAddTagSkipsDuplicates() throws QException
   {
      Integer contactId = insertContact("Alice", "A");
      Integer tagId = insertTag("VIP");

      /////////////////////////////////////////////
      // manually insert the junction row first  //
      /////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(ContactTag.TABLE_NAME).withRecordEntity(
            new ContactTag()
               .withContactId(contactId)
               .withTagId(tagId)));

      /////////////////////////////////////////////
      // run the process - should skip           //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(BulkAddTagProcess.NAME);
      processInput.addValue("tagId", tagId);
      processInput.addValue("recordIds", String.valueOf(contactId));

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(0, processOutput.getValues().get("addedCount"));
      assertEquals(1, processOutput.getValues().get("skippedCount"));

      /////////////////////////////////////////////
      // verify still only 1 junction row        //
      /////////////////////////////////////////////
      QueryOutput tagQuery = new QueryAction().execute(
         new QueryInput(ContactTag.TABLE_NAME));
      assertEquals(1, tagQuery.getRecords().size());
   }



   /*******************************************************************************
    ** Remove a tag from contacts via the BulkRemoveTag process.
    *******************************************************************************/
   @Test
   void testBulkRemoveTag() throws QException
   {
      Integer contactId = insertContact("Alice", "A");
      Integer tagId = insertTag("VIP");

      /////////////////////////////////////////////
      // insert the junction row                 //
      /////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(ContactTag.TABLE_NAME).withRecordEntity(
            new ContactTag()
               .withContactId(contactId)
               .withTagId(tagId)));

      /////////////////////////////////////////////
      // verify junction row exists              //
      /////////////////////////////////////////////
      assertEquals(1, new QueryAction().execute(
         new QueryInput(ContactTag.TABLE_NAME)).getRecords().size());

      /////////////////////////////////////////////
      // run BulkRemoveTag                       //
      /////////////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(BulkRemoveTagProcess.NAME);
      processInput.addValue("tagId", tagId);
      processInput.addValue("recordIds", String.valueOf(contactId));

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);

      assertEquals(1, processOutput.getValues().get("removedCount"));
      assertEquals(0, processOutput.getValues().get("skippedCount"));

      /////////////////////////////////////////////
      // verify junction row was deleted         //
      /////////////////////////////////////////////
      assertEquals(0, new QueryAction().execute(
         new QueryInput(ContactTag.TABLE_NAME)).getRecords().size());
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Tag and return its id.
    ***************************************************************************/
   private Integer insertTag(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Tag.TABLE_NAME).withRecordEntity(
            new Tag()
               .withName(name)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
