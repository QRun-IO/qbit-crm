/*******************************************************************************
 ** Process to subscribe an email address to an email list. Handles
 ** resubscription after unsubscribe and links to an existing contact.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to subscribe an email address to a list.
 ** Handles: new subscription, resubscription after unsub, and no-op for
 ** existing active subscribers.
 *******************************************************************************/
public class SubscribeToListProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "subscribeToList";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Subscribe to List")
         .withIcon(new QIcon().withName("person_add"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(SubscribeToListProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("email", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("emailListId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("firstName", QFieldType.STRING),
                  new QFieldMetaData("subscriptionSource", QFieldType.STRING)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the subscription logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String  email              = input.getValueString("email");
      Integer emailListId        = input.getValueInteger("emailListId");
      String  firstName          = input.getValueString("firstName");
      String  subscriptionSource = input.getValueString("subscriptionSource");

      ////////////////////////////////////////////////////
      // check if already subscribed to this list       //
      ////////////////////////////////////////////////////
      QueryOutput existingMembers = new QueryAction().execute(
         new QueryInput(EmailListMember.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("emailListId", QCriteriaOperator.EQUALS, emailListId))
               .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email))));

      if(!existingMembers.getRecords().isEmpty())
      {
         QRecord existingRecord = existingMembers.getRecords().get(0);
         Integer currentStatus = existingRecord.getValueInteger("status");

         if(CrmListMemberStatus.UNSUBSCRIBED.getId().equals(currentStatus))
         {
            ///////////////////////////////////////////////
            // resubscribe: update status and date       //
            ///////////////////////////////////////////////
            QRecord updateRecord = new QRecord()
               .withValue("id", existingRecord.getValueInteger("id"))
               .withValue("status", CrmListMemberStatus.SUBSCRIBED.getId())
               .withValue("subscribedDate", Instant.now())
               .withValue("unsubscribedDate", null);

            new UpdateAction().execute(
               new UpdateInput(EmailListMember.TABLE_NAME).withRecord(updateRecord));

            output.addValue("action", "RESUBSCRIBED");
            output.addValue("emailListMemberId", existingRecord.getValueInteger("id"));
         }
         else
         {
            /////////////////////////
            // already subscribed  //
            /////////////////////////
            output.addValue("action", "NOOP");
            output.addValue("emailListMemberId", existingRecord.getValueInteger("id"));
         }

         return;
      }

      ///////////////////////////////////////////////
      // look up contact by email for linking      //
      ///////////////////////////////////////////////
      Integer contactId = lookupContactByEmail(email);

      ///////////////////////////////////////////////
      // insert new EmailListMember               //
      ///////////////////////////////////////////////
      EmailListMember member = new EmailListMember()
         .withEmailListId(emailListId)
         .withEmail(email)
         .withFirstName(firstName)
         .withContactId(contactId)
         .withStatus(CrmListMemberStatus.SUBSCRIBED.getId())
         .withSubscribedDate(Instant.now())
         .withSubscriptionSource(subscriptionSource);

      new InsertAction().execute(
         new InsertInput(EmailListMember.TABLE_NAME).withRecordEntity(member));

      output.addValue("action", "SUBSCRIBED");
   }



   /***************************************************************************
    ** Look up a contact id by email address. Returns null if not found.
    ***************************************************************************/
   private Integer lookupContactByEmail(String email) throws QException
   {
      if(!StringUtils.hasContent(email))
      {
         return (null);
      }

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }

}
