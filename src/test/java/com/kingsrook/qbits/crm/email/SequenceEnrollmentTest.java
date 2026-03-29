/*******************************************************************************
 ** Unit tests for the SequenceEnrollment entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.model.EmailSequence;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for SequenceEnrollment CRUD, unique key enforcement, and status
 ** transitions.
 *******************************************************************************/
class SequenceEnrollmentTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(SequenceEnrollment.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("person_play");
   }



   /*******************************************************************************
    ** Verify all fields are present.
    *******************************************************************************/
   @Test
   void testFieldsPresent()
   {
      QTableMetaData table = QContext.getQInstance().getTable(SequenceEnrollment.TABLE_NAME);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("sequenceId");
      assertThat(table.getFields()).containsKey("contactId");
      assertThat(table.getFields()).containsKey("dealId");
      assertThat(table.getFields()).containsKey("currentStepNumber");
      assertThat(table.getFields()).containsKey("status");
      assertThat(table.getFields()).containsKey("enrolledDate");
      assertThat(table.getFields()).containsKey("enrolledByUserId");
      assertThat(table.getFields()).containsKey("completedDate");
      assertThat(table.getFields()).containsKey("unenrolledDate");
      assertThat(table.getFields()).containsKey("unenrollReason");
      assertThat(table.getFields()).containsKey("nextStepDate");
      assertThat(table.getFields()).containsKey("failureCount");
      assertThat(table.getFields()).containsKey("lastFailureMessage");
   }



   /*******************************************************************************
    ** Insert an enrollment and verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      Integer sequenceId = insertSequence("Test");

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME)
            .withRecordEntity(new SequenceEnrollment()
               .withSequenceId(sequenceId)
               .withContactId(100)
               .withCurrentStepNumber(0)
               .withStatus(CrmEnrollmentStatus.ACTIVE.getId())
               .withEnrolledDate(Instant.now())
               .withEnrolledByUserId("user-001")
               .withFailureCount(0)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());
      SequenceEnrollment fetched = new SequenceEnrollment(queryOutput.getRecords().get(0));
      assertEquals(sequenceId, fetched.getSequenceId());
      assertEquals(100, fetched.getContactId());
      assertEquals(CrmEnrollmentStatus.ACTIVE.getId(), fetched.getStatus());
   }



   /*******************************************************************************
    ** Verify unique key (sequenceId, contactId) is defined on the table.
    *******************************************************************************/
   @Test
   void testUniqueKeyDefined()
   {
      QTableMetaData table = QContext.getQInstance().getTable(SequenceEnrollment.TABLE_NAME);
      assertThat(table.getUniqueKeys()).isNotEmpty();
      assertThat(table.getUniqueKeys().get(0).getFieldNames()).containsExactly("sequenceId", "contactId");
   }



   /*******************************************************************************
    ** Verify status transitions: ACTIVE -> COMPLETED -> verify dates set.
    *******************************************************************************/
   @Test
   void testStatusTransition() throws QException
   {
      Integer sequenceId = insertSequence("Trans Test");

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME)
            .withRecordEntity(new SequenceEnrollment()
               .withSequenceId(sequenceId)
               .withContactId(200)
               .withCurrentStepNumber(0)
               .withStatus(CrmEnrollmentStatus.ACTIVE.getId())
               .withEnrolledDate(Instant.now())
               .withEnrolledByUserId("user-001")));

      Integer enrollmentId = insertOutput.getRecords().get(0).getValueInteger("id");

      /////////////////////////////////////////////
      // transition to COMPLETED                 //
      /////////////////////////////////////////////
      Instant completedDate = Instant.now();
      new UpdateAction().execute(
         new UpdateInput(SequenceEnrollment.TABLE_NAME).withRecord(
            new QRecord()
               .withValue("id", enrollmentId)
               .withValue("status", CrmEnrollmentStatus.COMPLETED.getId())
               .withValue("completedDate", completedDate)));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      SequenceEnrollment fetched = new SequenceEnrollment(queryOutput.getRecords().get(0));
      assertEquals(CrmEnrollmentStatus.COMPLETED.getId(), fetched.getStatus());
      assertNotNull(fetched.getCompletedDate());
   }



   /***************************************************************************
    ** Helper: insert an EmailSequence and return its id.
    ***************************************************************************/
   private Integer insertSequence(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(EmailSequence.TABLE_NAME)
            .withRecordEntity(new EmailSequence()
               .withName(name)
               .withOwnerUserId("user-001")
               .withIsActive(true)
               .withBusinessDaysOnly(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
