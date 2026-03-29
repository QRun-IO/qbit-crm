/*******************************************************************************
 ** Process to transfer ownership of all contacts, companies, and deals from
 ** one user to another, logging audit entries for each change.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.util.List;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
 ** MetaDataProducer + BackendStep for transferring ownership of contacts,
 ** companies, and deals from one user to another.
 *******************************************************************************/
public class TransferOwnershipProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "transferOwnership";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Transfer Ownership")
         .withIcon(new QIcon().withName("swap_horiz"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(TransferOwnershipProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("fromUserId", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("toUserId", QFieldType.STRING).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the transfer ownership process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String fromUserId    = input.getValueString("fromUserId");
      String toUserId      = input.getValueString("toUserId");
      String sessionUserId = QContext.getQSession().getIdReference();

      ///////////////////////////////////
      // Transfer contacts             //
      ///////////////////////////////////
      transferRecords(Contact.TABLE_NAME, CrmEntityType.CONTACT, fromUserId, toUserId, sessionUserId);

      ///////////////////////////////////
      // Transfer companies            //
      ///////////////////////////////////
      transferRecords(Company.TABLE_NAME, CrmEntityType.COMPANY, fromUserId, toUserId, sessionUserId);

      ///////////////////////////////////
      // Transfer deals                //
      ///////////////////////////////////
      transferRecords(Deal.TABLE_NAME, CrmEntityType.DEAL, fromUserId, toUserId, sessionUserId);
   }



   /***************************************************************************
    ** Query records owned by fromUserId, update to toUserId, and log audit.
    ***************************************************************************/
   private void transferRecords(String tableName, CrmEntityType entityType, String fromUserId, String toUserId, String sessionUserId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("ownerUserId", QCriteriaOperator.EQUALS, fromUserId))));

      for(QRecord record : queryOutput.getRecords())
      {
         Integer recordId = record.getValueInteger("id");

         //////////////////////////////////
         // update the owner             //
         //////////////////////////////////
         QRecord updateRecord = new QRecord()
            .withValue("id", recordId)
            .withValue("ownerUserId", toUserId);

         new UpdateAction().execute(
            new UpdateInput(tableName).withRecord(updateRecord));

         //////////////////////////////////
         // log audit entry              //
         //////////////////////////////////
         new InsertAction().execute(
            new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
               new AuditLog()
                  .withEntityType(entityType.getPossibleValueId())
                  .withEntityId(recordId)
                  .withAction(CrmAuditAction.OWNER_CHANGED.getPossibleValueId())
                  .withUserId(sessionUserId)
                  .withFieldName("ownerUserId")
                  .withOldValue(fromUserId)
                  .withNewValue(toUserId)
                  .withMessage("Ownership transferred from " + fromUserId + " to " + toUserId)));
      }
   }

}
