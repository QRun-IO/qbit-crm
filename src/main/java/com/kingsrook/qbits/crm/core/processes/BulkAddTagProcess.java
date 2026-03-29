/*******************************************************************************
 ** Bulk action process to add a tag to selected contact records. Inserts a
 ** junction row in crmContactTag if one does not already exist.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.ContactTag;
import com.kingsrook.qbits.crm.core.model.Tag;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
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
 ** MetaDataProducer + BackendStep for bulk-adding a tag to contacts.
 ** For each selected record, inserts a crmContactTag junction row if one
 ** does not already exist for that contactId + tagId combination.
 *******************************************************************************/
public class BulkAddTagProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "bulkAddTag";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Bulk Add Tag")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("label"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(BulkAddTagProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("tagId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(Tag.TABLE_NAME),
                  new QFieldMetaData("recordIds", QFieldType.STRING).withIsRequired(true).withLabel("Record IDs (comma-separated)")
               )))
         ));
   }



   /*******************************************************************************
    ** Execute: for each record id, insert a junction row if not exists.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer tagId     = input.getValueInteger("tagId");
      String  recordIds = input.getValueString("recordIds");

      Integer addedCount   = 0;
      Integer skippedCount = 0;

      for(String idStr : recordIds.split(","))
      {
         Integer contactId = Integer.parseInt(idStr.trim());

         /////////////////////////////////////////////////////////////
         // check if the junction row already exists                //
         /////////////////////////////////////////////////////////////
         QueryOutput existingTags = new QueryAction().execute(
            new QueryInput(ContactTag.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))
                  .withCriteria(new QFilterCriteria("tagId", QCriteriaOperator.EQUALS, tagId))));

         if(existingTags.getRecords().isEmpty())
         {
            new InsertAction().execute(
               new InsertInput(ContactTag.TABLE_NAME).withRecordEntity(
                  new ContactTag()
                     .withContactId(contactId)
                     .withTagId(tagId)));
            addedCount++;
         }
         else
         {
            skippedCount++;
         }
      }

      output.addValue("addedCount", addedCount);
      output.addValue("skippedCount", skippedCount);
   }

}
