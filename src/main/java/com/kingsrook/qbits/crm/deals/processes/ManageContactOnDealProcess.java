/*******************************************************************************
 ** Process to add or remove a contact from a deal via the DealContact
 ** junction table. Supports isPrimary enforcement (clearing other primaries
 ** when a new primary is set) and optional contactRoleId assignment.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.util.List;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
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
 ** MetaDataProducer + BackendStep for adding or removing a contact on a deal.
 ** ADD: inserts a DealContact row, with isPrimary enforcement.
 ** REMOVE: deletes the DealContact row matching dealId + contactId.
 *******************************************************************************/
public class ManageContactOnDealProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "manageContactOnDeal";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Manage Contact on Deal")
         .withTableName(Deal.TABLE_NAME)
         .withIcon(new QIcon().withName("person_add"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ManageContactOnDealProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("dealId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("contactId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("action", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("contactRoleId", QFieldType.INTEGER),
                  new QFieldMetaData("isPrimary", QFieldType.BOOLEAN)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the manage contact process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer dealId        = input.getValueInteger("dealId");
      Integer contactId     = input.getValueInteger("contactId");
      String  action        = input.getValueString("action");
      Integer contactRoleId = input.getValueInteger("contactRoleId");
      Boolean isPrimary     = input.getValueBoolean("isPrimary");

      if("ADD".equalsIgnoreCase(action))
      {
         ///////////////////////////////////////////////////////////////////
         // if isPrimary=true, unset isPrimary on all other DealContacts  //
         ///////////////////////////////////////////////////////////////////
         if(Boolean.TRUE.equals(isPrimary))
         {
            clearPrimaryContacts(dealId);
         }

         ///////////////////////////////////////////////////////////////////
         // insert the new DealContact row                                //
         ///////////////////////////////////////////////////////////////////
         DealContact dealContact = new DealContact()
            .withDealId(dealId)
            .withContactId(contactId)
            .withContactRoleId(contactRoleId)
            .withIsPrimary(Boolean.TRUE.equals(isPrimary));

         new InsertAction().execute(
            new InsertInput(DealContact.TABLE_NAME).withRecordEntity(dealContact));
      }
      else if("REMOVE".equalsIgnoreCase(action))
      {
         ///////////////////////////////////////////////////////////////////
         // find and delete the DealContact row matching dealId+contactId //
         ///////////////////////////////////////////////////////////////////
         QueryOutput queryOutput = new QueryAction().execute(
            new QueryInput(DealContact.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))
                  .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))));

         if(queryOutput.getRecords().isEmpty())
         {
            throw new QUserFacingException("Contact " + contactId + " is not linked to deal " + dealId);
         }

         List<Integer> idsToDelete = queryOutput.getRecords().stream()
            .map(r -> r.getValueInteger("id"))
            .toList();

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(DealContact.TABLE_NAME);
         deleteInput.setPrimaryKeys(idsToDelete.stream().map(id -> (java.io.Serializable) id).toList());
         new DeleteAction().execute(deleteInput);
      }
      else
      {
         throw new QUserFacingException("Invalid action: " + action + ". Must be ADD or REMOVE.");
      }
   }



   /***************************************************************************
    ** Clear isPrimary on all existing DealContact rows for the given deal.
    ***************************************************************************/
   private void clearPrimaryContacts(Integer dealId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(DealContact.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))
               .withCriteria(new QFilterCriteria("isPrimary", QCriteriaOperator.EQUALS, true))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue("isPrimary", false);

         new UpdateAction().execute(
            new UpdateInput(DealContact.TABLE_NAME).withRecord(updateRecord));
      }
   }

}
