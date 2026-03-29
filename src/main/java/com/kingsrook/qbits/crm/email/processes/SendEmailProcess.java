/*******************************************************************************
 ** Process to send an email to a contact. Creates an Activity and an
 ** EmailMessage record, respecting doNotEmail flags and optional template
 ** lookups.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
import com.kingsrook.qbits.crm.email.model.EmailMessage;
import com.kingsrook.qbits.crm.email.model.EmailTemplate;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
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
 ** MetaDataProducer + BackendStep for sending an email to a CRM contact.
 ** Validates doNotEmail, optionally loads a template, creates an Activity
 ** record, and creates a tracked EmailMessage record.
 *******************************************************************************/
public class SendEmailProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "sendEmail";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Send Email")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("email"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(SendEmailProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("contactId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("subject", QFieldType.STRING),
                  new QFieldMetaData("bodyHtml", QFieldType.STRING),
                  new QFieldMetaData("bodyText", QFieldType.STRING),
                  new QFieldMetaData("emailTemplateId", QFieldType.INTEGER),
                  new QFieldMetaData("dealId", QFieldType.INTEGER),
                  new QFieldMetaData("ccAddresses", QFieldType.STRING),
                  new QFieldMetaData("bccAddresses", QFieldType.STRING)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the send-email process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer contactId       = input.getValueInteger("contactId");
      String  subject         = input.getValueString("subject");
      String  bodyHtml        = input.getValueString("bodyHtml");
      String  bodyText        = input.getValueString("bodyText");
      Integer emailTemplateId = input.getValueInteger("emailTemplateId");
      Integer dealId          = input.getValueInteger("dealId");
      String  ccAddresses     = input.getValueString("ccAddresses");
      String  bccAddresses    = input.getValueString("bccAddresses");
      String  sessionUserId   = QContext.getQSession().getIdReference();

      ///////////////////////////////////////////
      // Step 1: load and validate the contact //
      ///////////////////////////////////////////
      QRecord contactRecord = GetAction.execute(Contact.TABLE_NAME, contactId);
      if(contactRecord == null)
      {
         throw new QException("Contact not found: " + contactId);
      }

      Contact contact = new Contact(contactRecord);
      if(Boolean.TRUE.equals(contact.getDoNotEmail()))
      {
         throw new QUserFacingException("Cannot send email: contact has doNotEmail set to true.");
      }

      //////////////////////////////////////////////////////
      // Step 2: if template provided, load and apply it  //
      //////////////////////////////////////////////////////
      if(emailTemplateId != null)
      {
         QRecord templateRecord = GetAction.execute(EmailTemplate.TABLE_NAME, emailTemplateId);
         if(templateRecord != null)
         {
            EmailTemplate template = new EmailTemplate(templateRecord);
            if(subject == null)
            {
               subject = template.getSubject();
            }
            if(bodyHtml == null)
            {
               bodyHtml = template.getBodyHtml();
            }
            if(bodyText == null)
            {
               bodyText = template.getBodyText();
            }
         }
      }

      /////////////////////////////////////////
      // Step 3: look up Email activity type //
      /////////////////////////////////////////
      Integer emailTypeId = lookupEmailActivityTypeId();

      //////////////////////////////////////////
      // Step 4: create the Activity record   //
      //////////////////////////////////////////
      Activity activity = new Activity()
         .withActivityTypeId(emailTypeId)
         .withSubject(subject)
         .withContactId(contactId)
         .withDealId(dealId)
         .withDirection(CrmDirection.OUTBOUND.getId())
         .withOwnerUserId(sessionUserId)
         .withIsCompleted(true);

      InsertOutput activityInsert = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(activity));
      Integer activityId = activityInsert.getRecords().get(0).getValueInteger("id");

      //////////////////////////////////////////////
      // Step 5: create the EmailMessage record   //
      //////////////////////////////////////////////
      String toAddress = contact.getEmail() != null ? contact.getEmail() : "";

      EmailMessage emailMessage = new EmailMessage()
         .withActivityId(activityId)
         .withFromAddress(sessionUserId)
         .withFromName(sessionUserId)
         .withToAddresses(toAddress)
         .withCcAddresses(ccAddresses)
         .withBccAddresses(bccAddresses)
         .withSubject(subject != null ? subject : "")
         .withBodyHtml(bodyHtml)
         .withBodyText(bodyText)
         .withDirection(CrmDirection.OUTBOUND.getId())
         .withStatus(CrmEmailStatus.SENT.getId())
         .withSentDate(Instant.now())
         .withIsTracked(true)
         .withOpenCount(0)
         .withClickCount(0)
         .withEmailTemplateId(emailTemplateId);

      InsertOutput emailInsert = new InsertAction().execute(
         new InsertInput(EmailMessage.TABLE_NAME).withRecordEntity(emailMessage));
      Integer emailMessageId = emailInsert.getRecords().get(0).getValueInteger("id");

      output.addValue("emailMessageId", emailMessageId);
   }



   /***************************************************************************
    ** Look up the "Email" activity type id. Returns null if not found.
    ***************************************************************************/
   private Integer lookupEmailActivityTypeId() throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityType.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Email"))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }

}
