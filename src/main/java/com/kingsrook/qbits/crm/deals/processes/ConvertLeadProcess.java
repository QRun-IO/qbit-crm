/*******************************************************************************
 ** Process to convert a contact (lead) into a deal opportunity. Updates the
 ** contact's lifecycle stage to "Opportunity", creates a new Deal, links
 ** them via DealContact junction, and logs audit entries.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.LifecycleStage;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealContact;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
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
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for converting a lead (contact) into a
 ** deal opportunity. Transitions the contact's lifecycle stage, creates
 ** a deal with the first pipeline stage, and links them via DealContact.
 *******************************************************************************/
public class ConvertLeadProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "convertLead";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Convert Lead")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("trending_up"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ConvertLeadProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("contactId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("pipelineId", QFieldType.INTEGER),
                  new QFieldMetaData("dealName", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("dealAmount", QFieldType.DECIMAL)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the lead conversion process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer    contactId  = input.getValueInteger("contactId");
      Integer    pipelineId = input.getValueInteger("pipelineId");
      String     dealName   = input.getValueString("dealName");
      BigDecimal dealAmount = ValueUtils.getValueAsBigDecimal(input.getValue("dealAmount"));
      String     userId     = QContext.getQSession().getIdReference();

      ///////////////////////////////////////
      // Step 1: load the contact          //
      ///////////////////////////////////////
      QRecord contactRecord = GetAction.execute(Contact.TABLE_NAME, contactId);
      if(contactRecord == null)
      {
         throw new QUserFacingException("Contact not found: " + contactId);
      }

      Contact contact = new Contact(contactRecord);

      /////////////////////////////////////////////////////////////////////
      // Step 2: update contact lifecycle stage to "Opportunity"         //
      /////////////////////////////////////////////////////////////////////
      Integer opportunityStageId = lookupLifecycleStageId("Opportunity");
      if(opportunityStageId != null)
      {
         QRecord contactUpdate = new QRecord()
            .withValue("id", contactId)
            .withValue("lifecycleStageId", opportunityStageId);

         new UpdateAction().execute(
            new UpdateInput(Contact.TABLE_NAME).withRecord(contactUpdate));
      }

      /////////////////////////////////////////////////////////////////////
      // Step 3: resolve pipeline (use provided or default)              //
      /////////////////////////////////////////////////////////////////////
      if(pipelineId == null)
      {
         pipelineId = lookupDefaultPipelineId();
      }

      if(pipelineId == null)
      {
         throw new QUserFacingException("No pipeline specified and no default pipeline found");
      }

      /////////////////////////////////////////////////////////////////////
      // Step 4: find the first stage of the pipeline (lowest sortOrder) //
      /////////////////////////////////////////////////////////////////////
      Integer firstStageId = lookupFirstStageId(pipelineId);
      if(firstStageId == null)
      {
         throw new QUserFacingException("No stages found for pipeline: " + pipelineId);
      }

      /////////////////////////////////////////////////////////////////////
      // Step 5: create the deal                                         //
      /////////////////////////////////////////////////////////////////////
      Deal deal = new Deal()
         .withName(dealName)
         .withPipelineId(pipelineId)
         .withPipelineStageId(firstStageId)
         .withAmount(dealAmount)
         .withCompanyId(contact.getCompanyId())
         .withOwnerUserId(userId)
         .withStageEnteredDate(Instant.now());

      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME).withRecordEntity(deal));

      Integer dealId = dealInsert.getRecords().get(0).getValueInteger("id");

      /////////////////////////////////////////////////////////////////////
      // Step 6: create DealContact junction                              //
      /////////////////////////////////////////////////////////////////////
      DealContact dealContact = new DealContact()
         .withDealId(dealId)
         .withContactId(contactId)
         .withIsPrimary(true);

      new InsertAction().execute(
         new InsertInput(DealContact.TABLE_NAME).withRecordEntity(dealContact));

      /////////////////////////////////////////////////////////////////////
      // Step 7: log audit entries                                       //
      /////////////////////////////////////////////////////////////////////
      AuditLog contactAudit = new AuditLog()
         .withEntityType(CrmEntityType.CONTACT.getId())
         .withEntityId(contactId)
         .withAction(CrmAuditAction.STAGE_CHANGED.getId())
         .withUserId(userId)
         .withFieldName("lifecycleStageId")
         .withNewValue(opportunityStageId != null ? String.valueOf(opportunityStageId) : null)
         .withMessage("Lead converted to deal: " + dealName);

      AuditLog dealAudit = new AuditLog()
         .withEntityType(CrmEntityType.DEAL.getId())
         .withEntityId(dealId)
         .withAction(CrmAuditAction.CREATED.getId())
         .withUserId(userId)
         .withMessage("Deal created from lead conversion (contact " + contactId + ")");

      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecords(List.of(
            contactAudit.toQRecord(), dealAudit.toQRecord())));

      output.addValue("dealId", dealId);
   }



   /***************************************************************************
    ** Look up the "Opportunity" lifecycle stage id. Returns null if not found.
    ***************************************************************************/
   private Integer lookupLifecycleStageId(String name) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(LifecycleStage.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, name))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }



   /***************************************************************************
    ** Look up the default pipeline id. Returns null if not found.
    ***************************************************************************/
   private Integer lookupDefaultPipelineId() throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Pipeline.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("isDefault", QCriteriaOperator.EQUALS, true))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }



   /***************************************************************************
    ** Look up the first stage (lowest sortOrder) for a pipeline.
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

}
