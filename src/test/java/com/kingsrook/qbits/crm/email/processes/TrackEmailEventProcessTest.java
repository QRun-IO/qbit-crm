/*******************************************************************************
 ** Unit tests for TrackEmailEventProcess.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.processes;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmBounceType;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
import com.kingsrook.qbits.crm.email.model.EmailMessage;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for email event tracking: open/click increment, hard bounce and
 ** spam set doNotEmail on the contact.
 *******************************************************************************/
class TrackEmailEventProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Test OPEN event increments openCount and sets firstOpenedDate.
    *******************************************************************************/
   @Test
   void testOpenEvent() throws QException
   {
      Integer contactId = insertContact("open@example.com");
      Integer emailId   = insertEmailMessage(contactId);

      runTrackEvent(emailId, "OPEN", null);

      EmailMessage updated = new EmailMessage(GetAction.execute(EmailMessage.TABLE_NAME, emailId));
      assertEquals(1, updated.getOpenCount());
      assertNotNull(updated.getFirstOpenedDate());

      ///////////////////////////////////
      // second open should increment  //
      ///////////////////////////////////
      runTrackEvent(emailId, "OPEN", null);
      updated = new EmailMessage(GetAction.execute(EmailMessage.TABLE_NAME, emailId));
      assertEquals(2, updated.getOpenCount());
   }



   /*******************************************************************************
    ** Test CLICK event increments clickCount and sets firstClickedDate.
    *******************************************************************************/
   @Test
   void testClickEvent() throws QException
   {
      Integer contactId = insertContact("click@example.com");
      Integer emailId   = insertEmailMessage(contactId);

      runTrackEvent(emailId, "CLICK", null);

      EmailMessage updated = new EmailMessage(GetAction.execute(EmailMessage.TABLE_NAME, emailId));
      assertEquals(1, updated.getClickCount());
      assertNotNull(updated.getFirstClickedDate());
   }



   /*******************************************************************************
    ** Test BOUNCE HARD sets bounceType and contact doNotEmail.
    *******************************************************************************/
   @Test
   void testHardBounceEvent() throws QException
   {
      Integer contactId = insertContact("bounce@example.com");
      Integer emailId   = insertEmailMessage(contactId);

      runTrackEvent(emailId, "BOUNCE", CrmBounceType.HARD.getId());

      EmailMessage updated = new EmailMessage(GetAction.execute(EmailMessage.TABLE_NAME, emailId));
      assertEquals(CrmBounceType.HARD.getId(), updated.getBounceType());

      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertTrue(contact.getDoNotEmail());
   }



   /*******************************************************************************
    ** Test SPAM event sets isSpamComplaint and contact doNotEmail.
    *******************************************************************************/
   @Test
   void testSpamEvent() throws QException
   {
      Integer contactId = insertContact("spam@example.com");
      Integer emailId   = insertEmailMessage(contactId);

      runTrackEvent(emailId, "SPAM", null);

      EmailMessage updated = new EmailMessage(GetAction.execute(EmailMessage.TABLE_NAME, emailId));
      assertTrue(updated.getIsSpamComplaint());

      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertTrue(contact.getDoNotEmail());
   }



   /***************************************************************************
    ** Helper: run the track event process.
    ***************************************************************************/
   private void runTrackEvent(Integer emailId, String eventType, Integer bounceTypeId) throws QException
   {
      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(TrackEmailEventProcess.NAME);
      processInput.addValue("emailMessageId", emailId);
      processInput.addValue("eventType", eventType);
      if(bounceTypeId != null)
      {
         processInput.addValue("bounceTypeId", bounceTypeId);
      }
      new RunProcessAction().execute(processInput);
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String email) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME).withRecordEntity(
            new Contact()
               .withEmail(email)
               .withFirstName("Test")
               .withLastName("User")
               .withDoNotEmail(false)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an EmailMessage and return its id.
    ***************************************************************************/
   private Integer insertEmailMessage(Integer contactId) throws QException
   {
      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));

      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailMessage.TABLE_NAME).withRecordEntity(
            new EmailMessage()
               .withFromAddress("sender@example.com")
               .withToAddresses(contact.getEmail())
               .withSubject("Test Email")
               .withDirection(CrmDirection.OUTBOUND.getId())
               .withStatus(CrmEmailStatus.SENT.getId())
               .withIsTracked(true)
               .withOpenCount(0)
               .withClickCount(0)
               .withIsSpamComplaint(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
