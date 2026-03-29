/*******************************************************************************
 ** Process to merge two contacts. Re-links activities, deal contacts, tags,
 ** and email list memberships from the secondary to the primary, then deletes
 ** the secondary contact and logs audit entries.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.ContactTag;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.deals.model.DealContact;
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
 ** MetaDataProducer + BackendStep for merging two contacts into one.
 *******************************************************************************/
public class MergeContactsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "mergeContacts";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Merge Contacts")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("merge"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(MergeContactsProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("primaryContactId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("secondaryContactId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the merge contacts process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer primaryContactId   = input.getValueInteger("primaryContactId");
      Integer secondaryContactId = input.getValueInteger("secondaryContactId");
      String  sessionUserId      = QContext.getQSession().getIdReference();

      /////////////////////////////////////////
      // validate both contacts exist        //
      /////////////////////////////////////////
      QRecord primaryRecord = GetAction.execute(Contact.TABLE_NAME, primaryContactId);
      QRecord secondaryRecord = GetAction.execute(Contact.TABLE_NAME, secondaryContactId);

      if(primaryRecord == null)
      {
         throw new QException("Primary contact not found: " + primaryContactId);
      }
      if(secondaryRecord == null)
      {
         throw new QException("Secondary contact not found: " + secondaryContactId);
      }

      ///////////////////////////////////////////////
      // Re-link activities from secondary         //
      ///////////////////////////////////////////////
      relinkRecords(Activity.TABLE_NAME, "contactId", secondaryContactId, primaryContactId);

      ///////////////////////////////////////////////
      // Re-link DealContact rows                  //
      ///////////////////////////////////////////////
      relinkDealContacts(primaryContactId, secondaryContactId);

      ///////////////////////////////////////////////
      // Re-link ContactTag rows                   //
      ///////////////////////////////////////////////
      relinkJunctionRecords(ContactTag.TABLE_NAME, "contactId", "tagId", primaryContactId, secondaryContactId);

      ///////////////////////////////////////////////
      // Re-link EmailListMember rows              //
      ///////////////////////////////////////////////
      relinkJunctionRecords(EmailListMember.TABLE_NAME, "contactId", "emailListId", primaryContactId, secondaryContactId);

      ///////////////////////////////////////////////
      // Delete the secondary contact              //
      ///////////////////////////////////////////////
      new DeleteAction().execute(
         new DeleteInput(Contact.TABLE_NAME).withPrimaryKeys(List.of(secondaryContactId)));

      ///////////////////////////////////////////////
      // Log audit entries                         //
      ///////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
            new AuditLog()
               .withEntityType(CrmEntityType.CONTACT.getId())
               .withEntityId(primaryContactId)
               .withAction(CrmAuditAction.MERGED.getId())
               .withUserId(sessionUserId)
               .withMessage("Merged contact " + secondaryContactId + " into this contact")));

      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
            new AuditLog()
               .withEntityType(CrmEntityType.CONTACT.getId())
               .withEntityId(secondaryContactId)
               .withAction(CrmAuditAction.DELETED.getId())
               .withUserId(sessionUserId)
               .withMessage("Contact deleted during merge into contact " + primaryContactId)));

      output.addValue("primaryContactId", primaryContactId);
   }



   /***************************************************************************
    ** Re-link simple FK records from secondary to primary.
    ***************************************************************************/
   private void relinkRecords(String tableName, String fkField, Integer fromId, Integer toId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fkField, QCriteriaOperator.EQUALS, fromId))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue(fkField, toId);

         new UpdateAction().execute(
            new UpdateInput(tableName).withRecord(updateRecord));
      }
   }



   /***************************************************************************
    ** Re-link DealContact rows: if same deal exists for both, delete secondary;
    ** otherwise update contactId.
    ***************************************************************************/
   private void relinkDealContacts(Integer primaryContactId, Integer secondaryContactId) throws QException
   {
      //////////////////////////////////////////////////////////
      // find deals already linked to primary                 //
      //////////////////////////////////////////////////////////
      QueryOutput primaryDeals = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, primaryContactId))));

      Set<Integer> primaryDealIds = new HashSet<>();
      for(QRecord record : primaryDeals.getRecords())
      {
         primaryDealIds.add(record.getValueInteger("dealId"));
      }

      //////////////////////////////////////////////////////////
      // process secondary deal contacts                      //
      //////////////////////////////////////////////////////////
      QueryOutput secondaryDeals = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, secondaryContactId))));

      for(QRecord record : secondaryDeals.getRecords())
      {
         Integer dealId = record.getValueInteger("dealId");
         if(primaryDealIds.contains(dealId))
         {
            new DeleteAction().execute(
               new DeleteInput(DealContact.TABLE_NAME).withPrimaryKeys(List.of(record.getValueInteger("id"))));
         }
         else
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue("contactId", primaryContactId);
            new UpdateAction().execute(
               new UpdateInput(DealContact.TABLE_NAME).withRecord(updateRecord));
         }
      }
   }



   /***************************************************************************
    ** Re-link junction table records: if same FK2 exists for both, delete
    ** secondary; otherwise update FK1 (contactId).
    ***************************************************************************/
   private void relinkJunctionRecords(String tableName, String fk1Field, String fk2Field, Integer primaryId, Integer secondaryId) throws QException
   {
      //////////////////////////////////////////////////////////
      // find FK2 values already linked to primary            //
      //////////////////////////////////////////////////////////
      QueryOutput primaryRecords = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fk1Field, QCriteriaOperator.EQUALS, primaryId))));

      Set<Integer> primaryFk2Ids = new HashSet<>();
      for(QRecord record : primaryRecords.getRecords())
      {
         primaryFk2Ids.add(record.getValueInteger(fk2Field));
      }

      //////////////////////////////////////////////////////////
      // process secondary records                            //
      //////////////////////////////////////////////////////////
      QueryOutput secondaryRecords = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fk1Field, QCriteriaOperator.EQUALS, secondaryId))));

      for(QRecord record : secondaryRecords.getRecords())
      {
         Integer fk2Value = record.getValueInteger(fk2Field);
         if(primaryFk2Ids.contains(fk2Value))
         {
            new DeleteAction().execute(
               new DeleteInput(tableName).withPrimaryKeys(List.of(record.getValueInteger("id"))));
         }
         else
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue(fk1Field, primaryId);
            new UpdateAction().execute(
               new UpdateInput(tableName).withRecord(updateRecord));
         }
      }
   }

}
