/*******************************************************************************
 ** GDPR right-to-erasure process. Anonymizes PII on a contact, scrubs audit
 ** log entries, deletes attachments, unenrolls from sequences, creates a
 ** consent-withdrawn record, and logs the anonymization event.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Attachment;
import com.kingsrook.qbits.crm.core.model.ConsentRecord;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmConsentType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
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
 ** MetaDataProducer + BackendStep for GDPR anonymization of a contact.
 *******************************************************************************/
public class GdprAnonymizeContactProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "gdprAnonymizeContact";

   private static final String ANONYMIZED = "[ANONYMIZED]";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("GDPR Anonymize Contact")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("privacy_tip"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(GdprAnonymizeContactProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("contactId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the GDPR anonymization process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer contactId     = input.getValueInteger("contactId");
      String  sessionUserId = QContext.getQSession().getIdReference();

      ///////////////////////////////////////////
      // Step 1: load and validate contact     //
      ///////////////////////////////////////////
      QRecord contactRecord = GetAction.execute(Contact.TABLE_NAME, contactId);
      if(contactRecord == null)
      {
         throw new QException("Contact not found: " + contactId);
      }

      ///////////////////////////////////////////
      // Step 2: anonymize PII fields          //
      ///////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", contactId)
         .withValue("firstName", ANONYMIZED)
         .withValue("lastName", ANONYMIZED)
         .withValue("email", "anonymized-" + contactId + "@removed.invalid")
         .withValue("phone", null)
         .withValue("mobilePhone", null)
         .withValue("secondaryEmail", null)
         .withValue("addressLine1", null)
         .withValue("addressLine2", null)
         .withValue("city", null)
         .withValue("state", null)
         .withValue("postalCode", null)
         .withValue("country", null)
         .withValue("website", null)
         .withValue("linkedinUrl", null)
         .withValue("description", null)
         .withValue("doNotEmail", true)
         .withValue("doNotCall", true);

      new UpdateAction().execute(
         new UpdateInput(Contact.TABLE_NAME).withRecord(updateRecord));

      ///////////////////////////////////////////
      // Step 3: scrub audit log entries       //
      ///////////////////////////////////////////
      scrubAuditLogEntries(contactId);

      ///////////////////////////////////////////
      // Step 4: delete attachments            //
      ///////////////////////////////////////////
      deleteAttachments(contactId);

      ///////////////////////////////////////////
      // Step 5: unenroll from sequences       //
      ///////////////////////////////////////////
      unenrollFromSequences(contactId);

      ///////////////////////////////////////////
      // Step 6: create consent record         //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(ConsentRecord.TABLE_NAME).withRecordEntity(
            new ConsentRecord()
               .withContactId(contactId)
               .withConsentType(CrmConsentType.DATA_PROCESSING.getId())
               .withStatus(CrmConsentStatus.WITHDRAWN.getId())
               .withSource("GDPR Anonymization Process")
               .withConsentDate(Instant.now())));

      ///////////////////////////////////////////
      // Step 7: log ANONYMIZED audit entry    //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
            new AuditLog()
               .withEntityType(CrmEntityType.CONTACT.getId())
               .withEntityId(contactId)
               .withAction(CrmAuditAction.ANONYMIZED.getId())
               .withUserId(sessionUserId)
               .withMessage("Contact anonymized per GDPR right-to-erasure request")));

      output.addValue("contactId", contactId);
      output.addValue("anonymized", true);
   }



   /***************************************************************************
    ** Scrub audit log entries for this contact. Sets a GDPR bypass flag on
    ** the session so the immutability customizers allow the update.
    ***************************************************************************/
   private void scrubAuditLogEntries(Integer contactId) throws QException
   {
      ///////////////////////////////////////////
      // set GDPR bypass flag on session       //
      ///////////////////////////////////////////
      QContext.getQSession().setValue("gdprBypass", "true");
      try
      {
         /////////////////////////////////////////////
         // query audit entries for this contact    //
         /////////////////////////////////////////////
         QueryOutput auditEntries = new QueryAction().execute(
            new QueryInput(AuditLog.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, CrmEntityType.CONTACT.getId()))
                  .withCriteria(new QFilterCriteria("entityId", QCriteriaOperator.EQUALS, contactId))));

         /////////////////////////////////////////////
         // anonymize each entry                    //
         /////////////////////////////////////////////
         for(QRecord record : auditEntries.getRecords())
         {
            QRecord update = new QRecord();
            update.setValue("id", record.getValueInteger("id"));
            update.setValue("oldValue", ANONYMIZED);
            update.setValue("newValue", ANONYMIZED);
            update.setValue("message", ANONYMIZED);
            new UpdateAction().execute(new UpdateInput(AuditLog.TABLE_NAME).withRecords(List.of(update)));
         }
      }
      finally
      {
         QContext.getQSession().setValue("gdprBypass", "false");
      }
   }



   /***************************************************************************
    ** Delete attachments linked to this contact.
    ***************************************************************************/
   private void deleteAttachments(Integer contactId) throws QException
   {
      QueryOutput attachments = new QueryAction().execute(
         new QueryInput(Attachment.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, CrmEntityType.CONTACT.getId()))
               .withCriteria(new QFilterCriteria("entityId", QCriteriaOperator.EQUALS, contactId))));

      for(QRecord record : attachments.getRecords())
      {
         new DeleteAction().execute(
            new DeleteInput(Attachment.TABLE_NAME).withPrimaryKeys(List.of(record.getValueInteger("id"))));
      }
   }



   /***************************************************************************
    ** Unenroll from any active sequence enrollments for this contact.
    ***************************************************************************/
   private void unenrollFromSequences(Integer contactId) throws QException
   {
      try
      {
         QueryOutput enrollments = new QueryAction().execute(
            new QueryInput("crmSequenceEnrollment")
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))
                  .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getId()))));

         for(QRecord record : enrollments.getRecords())
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue("status", CrmEnrollmentStatus.UNENROLLED.getId());

            new UpdateAction().execute(
               new UpdateInput("crmSequenceEnrollment").withRecord(updateRecord));
         }
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////
         // sequence enrollment table may not exist yet -- that is ok   //
         //////////////////////////////////////////////////////////////////
      }
   }

}
