/*******************************************************************************
 ** Simplified import process for contacts. Takes a list of QRecords,
 ** validates required fields, checks for duplicates by email, inserts new
 ** contacts, and logs audit entries.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for importing contacts from QRecords.
 *******************************************************************************/
public class ImportContactsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "importContacts";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Import Contacts")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("upload"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ImportContactsProcess.class))
         ));
   }



   /*******************************************************************************
    ** Execute the import contacts process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      List<QRecord> inputRecords = input.getRecords();
      String sessionUserId = QContext.getQSession().getIdReference();

      ////////////////////////////////////////////////////////////
      // build set of existing emails for duplicate detection   //
      ////////////////////////////////////////////////////////////
      Set<String> existingEmails = new HashSet<>();
      QueryOutput existingContacts = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));
      for(QRecord record : existingContacts.getRecords())
      {
         String email = record.getValueString("email");
         if(StringUtils.hasContent(email))
         {
            existingEmails.add(email.toLowerCase());
         }
      }

      int importedCount = 0;
      int skippedCount  = 0;
      int errorCount    = 0;

      for(QRecord inputRecord : inputRecords)
      {
         String firstName = inputRecord.getValueString("firstName");
         String lastName  = inputRecord.getValueString("lastName");
         String email     = inputRecord.getValueString("email");

         ///////////////////////////////////////////
         // validate required fields              //
         ///////////////////////////////////////////
         if(!StringUtils.hasContent(firstName) || !StringUtils.hasContent(lastName) || !StringUtils.hasContent(email))
         {
            errorCount++;
            continue;
         }

         ///////////////////////////////////////////
         // check for duplicate by email          //
         ///////////////////////////////////////////
         if(existingEmails.contains(email.toLowerCase()))
         {
            skippedCount++;
            continue;
         }

         ///////////////////////////////////////////
         // insert the new contact                //
         ///////////////////////////////////////////
         QRecord newContact = new QRecord()
            .withValue("firstName", firstName)
            .withValue("lastName", lastName)
            .withValue("email", email)
            .withValue("phone", inputRecord.getValueString("phone"))
            .withValue("ownerUserId", sessionUserId);

         InsertOutput insertOutput = new InsertAction().execute(
            new InsertInput(Contact.TABLE_NAME).withRecord(newContact));
         Integer newContactId = insertOutput.getRecords().get(0).getValueInteger("id");

         existingEmails.add(email.toLowerCase());

         ///////////////////////////////////////////
         // log IMPORTED audit entry              //
         ///////////////////////////////////////////
         new InsertAction().execute(
            new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
               new AuditLog()
                  .withEntityType(CrmEntityType.CONTACT.getPossibleValueId())
                  .withEntityId(newContactId)
                  .withAction(CrmAuditAction.IMPORTED.getPossibleValueId())
                  .withUserId(sessionUserId)
                  .withMessage("Contact imported")));

         importedCount++;
      }

      output.addValue("importedCount", importedCount);
      output.addValue("skippedCount", skippedCount);
      output.addValue("errorCount", errorCount);
   }

}
