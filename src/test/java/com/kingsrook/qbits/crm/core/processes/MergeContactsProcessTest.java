/*******************************************************************************
 ** Unit tests for MergeContactsProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.ContactTag;
import com.kingsrook.qbits.crm.core.model.Tag;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for merging contacts: activities re-linked, tags de-duped,
 ** secondary deleted, audit logged.
 *******************************************************************************/
class MergeContactsProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Merge two contacts and verify activities are re-linked and secondary deleted.
    *******************************************************************************/
   @Test
   void testMergeContacts() throws QException
   {
      ///////////////////////////////////////
      // set up contacts                   //
      ///////////////////////////////////////
      Integer primaryId   = insertContact("primary@example.com", "Primary", "Contact");
      Integer secondaryId = insertContact("secondary@example.com", "Secondary", "Contact");

      ///////////////////////////////////////
      // create activity on secondary      //
      ///////////////////////////////////////
      Integer typeId = insertActivityType("Note");
      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(typeId)
               .withSubject("Note on secondary")
               .withContactId(secondaryId)
               .withOwnerUserId("user-001")
               .withIsCompleted(false)));

      ///////////////////////////////////////
      // create tags on both contacts      //
      ///////////////////////////////////////
      Integer tag1Id = insertTag("VIP");
      Integer tag2Id = insertTag("Prospect");

      insertContactTag(primaryId, tag1Id);
      insertContactTag(secondaryId, tag1Id); // duplicate
      insertContactTag(secondaryId, tag2Id); // unique

      ////////////////////////////////////
      // run merge process              //
      ////////////////////////////////////
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(MergeContactsProcess.NAME);
      processInput.addValue("primaryContactId", primaryId);
      processInput.addValue("secondaryContactId", secondaryId);

      new RunProcessAction().execute(processInput);

      /////////////////////////////////////////////
      // verify secondary contact is deleted     //
      /////////////////////////////////////////////
      assertNull(GetAction.execute(Contact.TABLE_NAME, secondaryId));

      /////////////////////////////////////////////
      // verify activity re-linked to primary    //
      /////////////////////////////////////////////
      QueryOutput activities = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, primaryId))));
      assertEquals(1, activities.getRecords().size());

      /////////////////////////////////////////////
      // verify tags: primary should have both   //
      /////////////////////////////////////////////
      QueryOutput tags = new QueryAction().execute(
         new QueryInput(ContactTag.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, primaryId))));
      assertEquals(2, tags.getRecords().size());

      /////////////////////////////////////////////
      // verify audit logs created               //
      /////////////////////////////////////////////
      QueryOutput auditQuery = new QueryAction().execute(
         new QueryInput(AuditLog.TABLE_NAME));
      assertTrue(auditQuery.getRecords().size() >= 2);
   }



   /***************************************************************************
    ** Helper: insert a Contact.
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



   private Integer insertActivityType(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName("note")
               .withIsSystem(false)
               .withSortOrder(1)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private Integer insertTag(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Tag.TABLE_NAME).withRecordEntity(
            new Tag().withName(name)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   private void insertContactTag(Integer contactId, Integer tagId) throws QException
   {
      new InsertAction().execute(
         new InsertInput(ContactTag.TABLE_NAME).withRecord(
            new QRecord()
               .withValue("contactId", contactId)
               .withValue("tagId", tagId)));
   }

}
