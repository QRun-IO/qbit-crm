/*******************************************************************************
 ** Process to track email engagement events (open, click, bounce, spam, reply).
 ** Updates the EmailMessage record counters and flags, and may set doNotEmail
 ** on the linked contact for hard bounces and spam complaints.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmBounceType;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.model.EmailMessage;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
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
 ** MetaDataProducer + BackendStep for tracking email engagement events.
 ** Handles OPEN, CLICK, BOUNCE, SPAM, and REPLY event types.
 *******************************************************************************/
public class TrackEmailEventProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "trackEmailEvent";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Track Email Event")
         .withTableName(EmailMessage.TABLE_NAME)
         .withIcon(new QIcon().withName("track_changes"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(TrackEmailEventProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("emailMessageId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("eventType", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("bounceTypeId", QFieldType.INTEGER)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the tracking event process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer emailMessageId = input.getValueInteger("emailMessageId");
      String  eventType      = input.getValueString("eventType");
      Integer bounceTypeId   = input.getValueInteger("bounceTypeId");

      //////////////////////////////////////////
      // Step 1: load the email message       //
      //////////////////////////////////////////
      QRecord emailRecord = GetAction.execute(EmailMessage.TABLE_NAME, emailMessageId);
      if(emailRecord == null)
      {
         throw new QException("Email message not found: " + emailMessageId);
      }

      EmailMessage email = new EmailMessage(emailRecord);

      //////////////////////////////////////////
      // Step 2: handle event type            //
      //////////////////////////////////////////
      QRecord updateRecord = new QRecord().withValue("id", emailMessageId);

      switch(eventType.toUpperCase())
      {
         case "OPEN":
         {
            int currentOpenCount = email.getOpenCount() != null ? email.getOpenCount() : 0;
            updateRecord.setValue("openCount", currentOpenCount + 1);
            if(email.getFirstOpenedDate() == null)
            {
               updateRecord.setValue("firstOpenedDate", Instant.now());
            }
            break;
         }

         case "CLICK":
         {
            int currentClickCount = email.getClickCount() != null ? email.getClickCount() : 0;
            updateRecord.setValue("clickCount", currentClickCount + 1);
            if(email.getFirstClickedDate() == null)
            {
               updateRecord.setValue("firstClickedDate", Instant.now());
            }
            break;
         }

         case "BOUNCE":
         {
            updateRecord.setValue("bounceType", bounceTypeId);

            /////////////////////////////////////////////////////
            // if hard bounce, set contact doNotEmail to true   //
            /////////////////////////////////////////////////////
            if(bounceTypeId != null && bounceTypeId.equals(CrmBounceType.HARD.getId()))
            {
               setContactDoNotEmail(email);
            }
            break;
         }

         case "SPAM":
         {
            updateRecord.setValue("isSpamComplaint", true);
            setContactDoNotEmail(email);
            break;
         }

         case "REPLY":
         {
            //////////////////////////////////////////////////////////////
            // if email is linked to a sequence enrollment, mark REPLIED //
            //////////////////////////////////////////////////////////////
            if(email.getSequenceEnrollmentId() != null)
            {
               QRecord enrollmentUpdate = new QRecord()
                  .withValue("id", email.getSequenceEnrollmentId())
                  .withValue("status", CrmEnrollmentStatus.REPLIED.getId());

               new UpdateAction().execute(
                  new UpdateInput("crmSequenceEnrollment").withRecord(enrollmentUpdate));
            }
            break;
         }

         default:
            throw new QException("Unknown event type: " + eventType);
      }

      new UpdateAction().execute(
         new UpdateInput(EmailMessage.TABLE_NAME).withRecord(updateRecord));
   }



   /***************************************************************************
    ** Finds the contact linked to an email message (via activity) and sets
    ** doNotEmail = true. Looks up by the toAddresses field.
    ***************************************************************************/
   private void setContactDoNotEmail(EmailMessage email) throws QException
   {
      if(email.getToAddresses() == null)
      {
         return;
      }

      QueryOutput contactQuery = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email.getToAddresses()))));

      if(!contactQuery.getRecords().isEmpty())
      {
         QRecord contactUpdate = new QRecord()
            .withValue("id", contactQuery.getRecords().get(0).getValueInteger("id"))
            .withValue("doNotEmail", true);

         new UpdateAction().execute(
            new UpdateInput(Contact.TABLE_NAME).withRecord(contactUpdate));
      }
   }

}
