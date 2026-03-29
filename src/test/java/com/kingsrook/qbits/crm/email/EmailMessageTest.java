/*******************************************************************************
 ** Unit tests for the EmailMessage entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmBounceType;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
import com.kingsrook.qbits.crm.email.model.EmailMessage;
import com.kingsrook.qbits.crm.email.model.EmailTemplate;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for EmailMessage CRUD, all-fields persistence, self-referential
 ** threading, and enum PVS references.
 *******************************************************************************/
class EmailMessageTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with the correct icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailMessage.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("email");
   }



   /*******************************************************************************
    ** Insert an email message with all fields populated and verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertWithAllFields() throws QException
   {
      //////////////////////////////////
      // insert a template to link to //
      //////////////////////////////////
      Integer templateId = insertEmailTemplate("Follow-up", "Re: Meeting", "<p>Hi</p>", "user-001");

      Instant now = Instant.now();

      EmailMessage email = new EmailMessage()
         .withActivityId(42)
         .withFromAddress("rep@company.com")
         .withFromName("Sales Rep")
         .withToAddresses("[\"prospect@example.com\"]")
         .withCcAddresses("[\"manager@company.com\"]")
         .withBccAddresses("[\"archive@company.com\"]")
         .withSubject("Q2 Contract Discussion")
         .withBodyHtml("<h1>Hello</h1><p>Let's discuss the contract.</p>")
         .withBodyText("Hello\nLet's discuss the contract.")
         .withSentDate(now)
         .withThreadId("thread-abc-123")
         .withEmailTemplateId(templateId)
         .withSequenceEnrollmentId(99)
         .withDirection(CrmDirection.OUTBOUND.getId())
         .withStatus(CrmEmailStatus.SENT.getId())
         .withIsTracked(true)
         .withOpenCount(0)
         .withClickCount(0)
         .withBounceType(null)
         .withIsSpamComplaint(false)
         .withReplyToEmailId(null);

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(EmailMessage.TABLE_NAME).withRecordEntity(email));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailMessage.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      EmailMessage fetched = new EmailMessage(queryOutput.getRecords().get(0));
      assertEquals(42, fetched.getActivityId());
      assertEquals("rep@company.com", fetched.getFromAddress());
      assertEquals("Sales Rep", fetched.getFromName());
      assertEquals("[\"prospect@example.com\"]", fetched.getToAddresses());
      assertEquals("[\"manager@company.com\"]", fetched.getCcAddresses());
      assertEquals("[\"archive@company.com\"]", fetched.getBccAddresses());
      assertEquals("Q2 Contract Discussion", fetched.getSubject());
      assertNotNull(fetched.getBodyHtml());
      assertNotNull(fetched.getBodyText());
      assertNotNull(fetched.getSentDate());
      assertEquals("thread-abc-123", fetched.getThreadId());
      assertEquals(templateId, fetched.getEmailTemplateId());
      assertEquals(99, fetched.getSequenceEnrollmentId());
      assertEquals(CrmDirection.OUTBOUND.getId(), fetched.getDirection());
      assertEquals(CrmEmailStatus.SENT.getId(), fetched.getStatus());
      assertTrue(fetched.getIsTracked());
      assertEquals(0, fetched.getOpenCount());
      assertEquals(0, fetched.getClickCount());
      assertNull(fetched.getFirstOpenedDate());
      assertNull(fetched.getFirstClickedDate());
      assertNull(fetched.getBounceType());
      assertFalse(fetched.getIsSpamComplaint());
      assertNull(fetched.getReplyToEmailId());
   }



   /*******************************************************************************
    ** Verify self-referential threading via replyToEmailId.
    *******************************************************************************/
   @Test
   void testSelfRefThreading() throws QException
   {
      /////////////////////////////////////////
      // insert original outbound email      //
      /////////////////////////////////////////
      InsertOutput originalOutput = new InsertAction().execute(
         new InsertInput(EmailMessage.TABLE_NAME)
            .withRecordEntity(new EmailMessage()
               .withFromAddress("rep@company.com")
               .withToAddresses("[\"prospect@example.com\"]")
               .withSubject("Initial outreach")
               .withDirection(CrmDirection.OUTBOUND.getId())
               .withStatus(CrmEmailStatus.DELIVERED.getId())
               .withIsTracked(true)
               .withIsSpamComplaint(false)
               .withThreadId("thread-001")));

      Integer originalId = originalOutput.getRecords().get(0).getValueInteger("id");
      assertNotNull(originalId);

      ///////////////////////////////////////////////
      // insert reply referencing the original     //
      ///////////////////////////////////////////////
      InsertOutput replyOutput = new InsertAction().execute(
         new InsertInput(EmailMessage.TABLE_NAME)
            .withRecordEntity(new EmailMessage()
               .withFromAddress("prospect@example.com")
               .withToAddresses("[\"rep@company.com\"]")
               .withSubject("Re: Initial outreach")
               .withDirection(CrmDirection.INBOUND.getId())
               .withStatus(CrmEmailStatus.DELIVERED.getId())
               .withIsTracked(false)
               .withIsSpamComplaint(false)
               .withThreadId("thread-001")
               .withReplyToEmailId(originalId)));

      Integer replyId = replyOutput.getRecords().get(0).getValueInteger("id");
      assertNotNull(replyId);

      ///////////////////////////////////
      // query and verify the chain    //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailMessage.TABLE_NAME));

      assertEquals(2, queryOutput.getRecords().size());

      EmailMessage reply = new EmailMessage(queryOutput.getRecords().stream()
         .filter(r -> "Re: Initial outreach".equals(r.getValueString("subject")))
         .findFirst()
         .orElseThrow());

      assertEquals(originalId, reply.getReplyToEmailId());
      assertEquals("thread-001", reply.getThreadId());
      assertEquals(CrmDirection.INBOUND.getId(), reply.getDirection());
   }



   /*******************************************************************************
    ** Verify bounced email with bounce type.
    *******************************************************************************/
   @Test
   void testBouncedEmail() throws QException
   {
      new InsertAction().execute(
         new InsertInput(EmailMessage.TABLE_NAME)
            .withRecordEntity(new EmailMessage()
               .withFromAddress("rep@company.com")
               .withToAddresses("[\"invalid@nowhere.com\"]")
               .withSubject("Bounced email test")
               .withDirection(CrmDirection.OUTBOUND.getId())
               .withStatus(CrmEmailStatus.FAILED.getId())
               .withBounceType(CrmBounceType.HARD.getId())
               .withIsSpamComplaint(false)
               .withIsTracked(true)));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailMessage.TABLE_NAME));

      EmailMessage fetched = new EmailMessage(queryOutput.getRecords().get(0));
      assertEquals(CrmEmailStatus.FAILED.getId(), fetched.getStatus());
      assertEquals(CrmBounceType.HARD.getId(), fetched.getBounceType());
   }



   /***************************************************************************
    ** Helper: insert an EmailTemplate and return its id.
    ***************************************************************************/
   private Integer insertEmailTemplate(String name, String subject, String bodyHtml, String ownerUserId) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailTemplate.TABLE_NAME)
            .withRecordEntity(new EmailTemplate()
               .withName(name)
               .withSubject(subject)
               .withBodyHtml(bodyHtml)
               .withOwnerUserId(ownerUserId)
               .withIsShared(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
