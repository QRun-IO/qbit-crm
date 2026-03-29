/*******************************************************************************
 ** Process to clone a deal including its contacts, products, and tags.
 ** The cloned deal gets a "(Copy)" suffix, resets to the first pipeline stage,
 ** and clears close-related fields.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.deals.model.DealTag;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
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
 ** MetaDataProducer + BackendStep for cloning a deal and its child records.
 *******************************************************************************/
public class CloneDealProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "cloneDeal";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Clone Deal")
         .withTableName(Deal.TABLE_NAME)
         .withIcon(new QIcon().withName("content_copy"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(CloneDealProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("dealId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the clone deal process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer dealId = input.getValueInteger("dealId");

      ///////////////////////////////////
      // Step 1: load the source deal  //
      ///////////////////////////////////
      QRecord dealRecord = GetAction.execute(Deal.TABLE_NAME, dealId);
      if(dealRecord == null)
      {
         throw new QException("Deal not found: " + dealId);
      }

      Deal sourceDeal = new Deal(dealRecord);

      /////////////////////////////////////////////////////////
      // Step 2: find the first stage of the deal's pipeline //
      /////////////////////////////////////////////////////////
      Integer firstStageId = lookupFirstStageId(sourceDeal.getPipelineId());

      /////////////////////////////////////////
      // Step 3: create the cloned deal      //
      /////////////////////////////////////////
      QRecord newDealRecord = new QRecord()
         .withValue("name", sourceDeal.getName() + " (Copy)")
         .withValue("pipelineId", sourceDeal.getPipelineId())
         .withValue("pipelineStageId", firstStageId != null ? firstStageId : sourceDeal.getPipelineStageId())
         .withValue("amount", sourceDeal.getAmount())
         .withValue("currencyCode", sourceDeal.getCurrencyCode())
         .withValue("companyId", sourceDeal.getCompanyId())
         .withValue("ownerUserId", sourceDeal.getOwnerUserId())
         .withValue("leadSourceId", sourceDeal.getLeadSourceId())
         .withValue("expectedCloseDate", sourceDeal.getExpectedCloseDate())
         .withValue("description", sourceDeal.getDescription())
         .withValue("priority", sourceDeal.getPriority())
         .withValue("stageEnteredDate", Instant.now());

      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecord(newDealRecord));
      Integer newDealId = dealInsert.getRecords().get(0).getValueInteger("id");

      /////////////////////////////////////////////
      // Step 4: clone DealContact rows          //
      /////////////////////////////////////////////
      cloneChildRecords(DealContact.TABLE_NAME, "dealId", dealId, newDealId);

      /////////////////////////////////////////////
      // Step 5: clone DealProduct rows          //
      /////////////////////////////////////////////
      cloneChildRecords(DealProduct.TABLE_NAME, "dealId", dealId, newDealId);

      /////////////////////////////////////////////
      // Step 6: clone DealTag rows              //
      /////////////////////////////////////////////
      cloneChildRecords(DealTag.TABLE_NAME, "dealId", dealId, newDealId);

      output.addValue("dealId", newDealId);
   }



   /***************************************************************************
    ** Look up the first pipeline stage (lowest sortOrder) for a pipeline.
    ***************************************************************************/
   private Integer lookupFirstStageId(Integer pipelineId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(PipelineStage.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("pipelineId", QCriteriaOperator.EQUALS, pipelineId))
               .withOrderBy(new QFilterOrderBy("sortOrder", true))
               .withLimit(1)));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }



   /***************************************************************************
    ** Clone child records from one deal to another.
    ***************************************************************************/
   private void cloneChildRecords(String tableName, String fkField, Integer oldDealId, Integer newDealId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fkField, QCriteriaOperator.EQUALS, oldDealId))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord clone = new QRecord(record);
         clone.removeValue("id");
         clone.setValue(fkField, newDealId);
         clone.removeValue("createDate");
         clone.removeValue("modifyDate");

         new InsertAction().execute(
            new InsertInput(tableName).withRecord(clone));
      }
   }

}
