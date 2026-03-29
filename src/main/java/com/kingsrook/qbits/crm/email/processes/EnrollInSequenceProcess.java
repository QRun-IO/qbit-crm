/*******************************************************************************
 ** Process to enroll a contact in an email sequence.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.processes;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.model.EmailSequence;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qbits.crm.email.model.SequenceStep;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to enroll a contact in an email sequence.
 ** Validates that the contact is not already enrolled, the sequence is active,
 ** and the contact has not opted out of email.
 *******************************************************************************/
public class EnrollInSequenceProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "enrollInSequence";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Enroll in Sequence")
         .withIcon(new QIcon().withName("playlist_add"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(EnrollInSequenceProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("sequenceId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("contactId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("dealId", QFieldType.INTEGER)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the enrollment logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer sequenceId = input.getValueInteger("sequenceId");
      Integer contactId  = input.getValueInteger("contactId");
      Integer dealId     = input.getValueInteger("dealId");

      /////////////////////////////////////////////
      // validate: sequence exists and is active //
      /////////////////////////////////////////////
      GetOutput sequenceGet = new GetAction().execute(
         new GetInput(EmailSequence.TABLE_NAME).withPrimaryKey(sequenceId));

      if(sequenceGet.getRecord() == null)
      {
         throw (new QUserFacingException("Sequence not found"));
      }

      EmailSequence sequence = new EmailSequence(sequenceGet.getRecord());
      if(!Boolean.TRUE.equals(sequence.getIsActive()))
      {
         throw (new QUserFacingException("Sequence is not active"));
      }

      /////////////////////////////////////////////
      // validate: contact exists                //
      /////////////////////////////////////////////
      GetOutput contactGet = new GetAction().execute(
         new GetInput(Contact.TABLE_NAME).withPrimaryKey(contactId));

      if(contactGet.getRecord() == null)
      {
         throw (new QUserFacingException("Contact not found"));
      }

      Contact contact = new Contact(contactGet.getRecord());

      /////////////////////////////////////////////
      // validate: doNotEmail is not true        //
      /////////////////////////////////////////////
      if(Boolean.TRUE.equals(contact.getDoNotEmail()))
      {
         throw (new QUserFacingException("Contact has opted out of email"));
      }

      /////////////////////////////////////////////
      // validate: not already enrolled          //
      /////////////////////////////////////////////
      QueryOutput existingEnrollments = new QueryAction().execute(
         new QueryInput(SequenceEnrollment.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("sequenceId", QCriteriaOperator.EQUALS, sequenceId))
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))
               .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getId()))));

      if(!existingEnrollments.getRecords().isEmpty())
      {
         throw (new QUserFacingException("Contact is already enrolled in this sequence"));
      }

      /////////////////////////////////////////////
      // query first step to calculate next date //
      /////////////////////////////////////////////
      QueryOutput firstStepQuery = new QueryAction().execute(
         new QueryInput(SequenceStep.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("sequenceId", QCriteriaOperator.EQUALS, sequenceId))
               .withCriteria(new QFilterCriteria("stepNumber", QCriteriaOperator.EQUALS, 1))));

      Instant nextStepDate = Instant.now();
      if(!firstStepQuery.getRecords().isEmpty())
      {
         SequenceStep firstStep = new SequenceStep(firstStepQuery.getRecords().get(0));
         Integer delayDays = firstStep.getDelayDays() != null ? firstStep.getDelayDays() : 0;
         Integer delayHours = firstStep.getDelayHours() != null ? firstStep.getDelayHours() : 0;
         nextStepDate = Instant.now()
            .plus(delayDays, ChronoUnit.DAYS)
            .plus(delayHours, ChronoUnit.HOURS);
      }

      /////////////////////////////////////////////
      // insert enrollment                       //
      /////////////////////////////////////////////
      SequenceEnrollment enrollment = new SequenceEnrollment()
         .withSequenceId(sequenceId)
         .withContactId(contactId)
         .withDealId(dealId)
         .withCurrentStepNumber(0)
         .withStatus(CrmEnrollmentStatus.ACTIVE.getId())
         .withEnrolledDate(Instant.now())
         .withEnrolledByUserId(input.getSession() != null ? input.getSession().getIdReference() : "system")
         .withNextStepDate(nextStepDate)
         .withFailureCount(0);

      new InsertAction().execute(
         new InsertInput(SequenceEnrollment.TABLE_NAME).withRecordEntity(enrollment));

      output.addValue("action", "ENROLLED");
      output.addValue("sequenceId", sequenceId);
      output.addValue("contactId", contactId);
   }

}
