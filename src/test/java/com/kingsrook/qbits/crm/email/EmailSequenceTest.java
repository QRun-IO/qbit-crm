/*******************************************************************************
 ** Unit tests for the EmailSequence entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmSequenceStepType;
import com.kingsrook.qbits.crm.email.model.EmailSequence;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qbits.crm.email.model.SequenceStep;
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
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for EmailSequence CRUD, field persistence, PVS registration,
 ** and child table relationships (steps and enrollments).
 *******************************************************************************/
class EmailSequenceTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has the expected icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailSequence.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("playlist_play");
   }



   /*******************************************************************************
    ** Verify PVS was produced.
    *******************************************************************************/
   @Test
   void testPvsRegistered()
   {
      assertNotNull(QContext.getQInstance().getPossibleValueSource(EmailSequence.TABLE_NAME));
   }



   /*******************************************************************************
    ** Verify all fields are present in the table metadata.
    *******************************************************************************/
   @Test
   void testFieldsPresent()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailSequence.TABLE_NAME);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("ownerUserId");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("totalSteps");
      assertThat(table.getFields()).containsKey("businessDaysOnly");
      assertThat(table.getFields()).containsKey("sendWindowStartHour");
      assertThat(table.getFields()).containsKey("sendWindowEndHour");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
   }



   /*******************************************************************************
    ** Insert a sequence and verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(EmailSequence.TABLE_NAME)
            .withRecordEntity(new EmailSequence()
               .withName("Onboarding Sequence")
               .withDescription("Welcome new contacts")
               .withOwnerUserId("user-001")
               .withIsActive(true)
               .withBusinessDaysOnly(true)
               .withSendWindowStartHour(9)
               .withSendWindowEndHour(17)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailSequence.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      EmailSequence fetched = new EmailSequence(queryOutput.getRecords().get(0));
      assertNotNull(fetched.getId());
      assertEquals("Onboarding Sequence", fetched.getName());
      assertEquals("Welcome new contacts", fetched.getDescription());
      assertEquals("user-001", fetched.getOwnerUserId());
      assertTrue(fetched.getIsActive());
      assertTrue(fetched.getBusinessDaysOnly());
      assertEquals(9, fetched.getSendWindowStartHour());
      assertEquals(17, fetched.getSendWindowEndHour());
   }



   /*******************************************************************************
    ** Verify child table SequenceStep can be inserted for a sequence.
    *******************************************************************************/
   @Test
   void testChildTableSteps() throws QException
   {
      Integer sequenceId = insertSequence("Test Seq");

      new InsertAction().execute(
         new InsertInput(SequenceStep.TABLE_NAME)
            .withRecordEntity(new SequenceStep()
               .withSequenceId(sequenceId)
               .withStepNumber(1)
               .withStepType(CrmSequenceStepType.EMAIL.getId())
               .withDelayDays(0)
               .withDelayHours(0)));

      new InsertAction().execute(
         new InsertInput(SequenceStep.TABLE_NAME)
            .withRecordEntity(new SequenceStep()
               .withSequenceId(sequenceId)
               .withStepNumber(2)
               .withStepType(CrmSequenceStepType.WAIT.getId())
               .withDelayDays(2)));

      QueryOutput stepQuery = new QueryAction().execute(
         new QueryInput(SequenceStep.TABLE_NAME));

      assertEquals(2, stepQuery.getRecords().size());
   }



   /*******************************************************************************
    ** Verify child table SequenceEnrollment can be inserted for a sequence.
    *******************************************************************************/
   @Test
   void testChildTableEnrollments() throws QException
   {
      Integer sequenceId = insertSequence("Enroll Seq");

      new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME)
            .withRecordEntity(new SequenceEnrollment()
               .withSequenceId(sequenceId)
               .withContactId(100)
               .withCurrentStepNumber(0)
               .withStatus(CrmEnrollmentStatus.ACTIVE.getId())
               .withEnrolledDate(Instant.now())
               .withEnrolledByUserId("user-001")));

      QueryOutput enrollmentQuery = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME));

      assertEquals(1, enrollmentQuery.getRecords().size());
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
