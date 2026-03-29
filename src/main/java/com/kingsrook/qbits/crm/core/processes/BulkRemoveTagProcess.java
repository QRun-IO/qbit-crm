/*******************************************************************************
 ** Bulk action process to remove a tag from selected contact records. Deletes
 ** the junction row in crmContactTag.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.ContactTag;
import com.kingsrook.qbits.crm.core.model.Tag;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for bulk-removing a tag from contacts.
 ** For each selected record, deletes the crmContactTag junction row if it
 ** exists for that contactId + tagId combination.
 *******************************************************************************/
public class BulkRemoveTagProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "bulkRemoveTag";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Bulk Remove Tag")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("label_off"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(BulkRemoveTagProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("tagId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(Tag.TABLE_NAME),
                  new QFieldMetaData("recordIds", QFieldType.STRING).withIsRequired(true).withLabel("Record IDs (comma-separated)")
               )))
         ));
   }



   /*******************************************************************************
    ** Execute: for each record id, delete the junction row if it exists.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer tagId     = input.getValueInteger("tagId");
      String  recordIds = input.getValueString("recordIds");

      Integer removedCount = 0;
      Integer skippedCount = 0;

      for(String idStr : recordIds.split(","))
      {
         Integer contactId = Integer.parseInt(idStr.trim());

         /////////////////////////////////////////////////////////////
         // find the junction row                                   //
         /////////////////////////////////////////////////////////////
         QueryOutput existingTags = new QueryAction().execute(
            new QueryInput(ContactTag.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))
                  .withCriteria(new QFilterCriteria("tagId", QCriteriaOperator.EQUALS, tagId))));

         if(!existingTags.getRecords().isEmpty())
         {
            QRecord tagRecord = existingTags.getRecords().get(0);
            new DeleteAction().execute(
               new DeleteInput(ContactTag.TABLE_NAME)
                  .withPrimaryKeys(List.of(tagRecord.getValueInteger("id"))));
            removedCount++;
         }
         else
         {
            skippedCount++;
         }
      }

      output.addValue("removedCount", removedCount);
      output.addValue("skippedCount", skippedCount);
   }

}
