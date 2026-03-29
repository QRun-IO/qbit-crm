/*******************************************************************************
 ** Process to move a deal from its current pipeline stage to a target stage.
 ** Validates stage membership in the pipeline, checks required fields, updates
 ** weighted amount, sets close date on terminal stages, and logs stage history
 ** and audit entries.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
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
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for moving a deal to a new pipeline stage.
 ** Enforces stage-belongs-to-pipeline validation, required field gating,
 ** weighted amount recalculation, close-date stamping, and stage history
 ** tracking.
 *******************************************************************************/
public class MoveDealStageProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "moveDealStage";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Move Deal Stage")
         .withTableName(Deal.TABLE_NAME)
         .withIcon(new QIcon().withName("swap_horiz"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(MoveDealStageProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("dealId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("targetStageId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the stage move process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer dealId        = input.getValueInteger("dealId");
      Integer targetStageId = input.getValueInteger("targetStageId");
      String  sessionUserId = QContext.getQSession().getIdReference();

      ///////////////////////////////
      // Step 1: load the deal     //
      ///////////////////////////////
      QRecord dealRecord = GetAction.execute(Deal.TABLE_NAME, dealId);
      if(dealRecord == null)
      {
         throw new QUserFacingException("Deal not found: " + dealId);
      }

      Deal deal = new Deal(dealRecord);

      /////////////////////////////////////////
      // Step 2: load the target stage       //
      /////////////////////////////////////////
      QRecord targetStageRecord = GetAction.execute(PipelineStage.TABLE_NAME, targetStageId);
      if(targetStageRecord == null)
      {
         throw new QUserFacingException("Target stage not found: " + targetStageId);
      }

      PipelineStage targetStage = new PipelineStage(targetStageRecord);

      ///////////////////////////////////////////////////////////////////
      // Step 3: validate target stage belongs to deal's pipeline      //
      ///////////////////////////////////////////////////////////////////
      if(!deal.getPipelineId().equals(targetStage.getPipelineId()))
      {
         throw new QUserFacingException("Target stage does not belong to this deal's pipeline");
      }

      /////////////////////////////////////////////////////////////////////
      // Step 4: validate required fields if stage has requiredFieldsJson //
      /////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(targetStage.getRequiredFieldsJson()))
      {
         List<String> requiredFields;
         try
         {
            requiredFields = JsonUtils.toObject(targetStage.getRequiredFieldsJson(), List.class);
         }
         catch(Exception e)
         {
            throw new QException("Error parsing requiredFieldsJson for stage: " + targetStage.getName(), e);
         }

         for(String fieldName : requiredFields)
         {
            Object value = dealRecord.getValue(fieldName);
            if(value == null)
            {
               throw new QUserFacingException("Required field '" + fieldName + "' must be set before moving to stage: " + targetStage.getName());
            }
         }
      }

      //////////////////////////////////////////////
      // Step 5: capture the old stage id         //
      //////////////////////////////////////////////
      Integer oldStageId = deal.getPipelineStageId();
      Instant now        = Instant.now();

      /////////////////////////////////////////////////////////////////////
      // Step 6: recalculate weighted amount                              //
      /////////////////////////////////////////////////////////////////////
      BigDecimal weightedAmount = calculateWeightedAmount(deal, targetStage);

      //////////////////////////////////////////////
      // Step 7: build the deal update record     //
      //////////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", dealId)
         .withValue("pipelineStageId", targetStageId)
         .withValue("stageEnteredDate", now)
         .withValue("weightedAmount", weightedAmount);

      /////////////////////////////////////////////////////////////////
      // Step 8: if closing, set actualCloseDate                     //
      /////////////////////////////////////////////////////////////////
      if(Boolean.TRUE.equals(targetStage.getIsClosedWon()) || Boolean.TRUE.equals(targetStage.getIsClosedLost()))
      {
         updateRecord.setValue("actualCloseDate", LocalDate.now());
      }

      new UpdateAction().execute(
         new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      //////////////////////////////////////////////////////////////
      // Step 9: insert DealStageHistory row                      //
      //////////////////////////////////////////////////////////////
      Integer durationDays = null;
      if(deal.getStageEnteredDate() != null)
      {
         durationDays = (int) ChronoUnit.DAYS.between(deal.getStageEnteredDate(), now);
      }

      DealStageHistory history = new DealStageHistory()
         .withDealId(dealId)
         .withFromStageId(oldStageId)
         .withToStageId(targetStageId)
         .withUserId(sessionUserId)
         .withTransitionDate(now)
         .withDurationInFromStageDays(durationDays);

      new InsertAction().execute(
         new InsertInput(DealStageHistory.TABLE_NAME).withRecordEntity(history));

      //////////////////////////////////////////////////////////////
      // Step 10: log audit entry                                 //
      //////////////////////////////////////////////////////////////
      AuditLog auditEntry = new AuditLog()
         .withEntityType(CrmEntityType.DEAL.getId())
         .withEntityId(dealId)
         .withAction(CrmAuditAction.STAGE_CHANGED.getId())
         .withUserId(sessionUserId)
         .withFieldName("pipelineStageId")
         .withOldValue(oldStageId != null ? String.valueOf(oldStageId) : null)
         .withNewValue(String.valueOf(targetStageId))
         .withMessage("Deal moved to stage: " + targetStage.getName());

      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(auditEntry));

      output.addValue("dealId", dealId);
      output.addValue("newStageId", targetStageId);
   }



   /*******************************************************************************
    ** Calculate weighted amount based on deal amount and stage probability.
    ** Uses probabilityOverridePct if set, otherwise stage's probabilityPct.
    *******************************************************************************/
   static BigDecimal calculateWeightedAmount(Deal deal, PipelineStage stage)
   {
      if(deal.getAmount() == null)
      {
         return (null);
      }

      Integer probability = deal.getProbabilityOverridePct();
      if(probability == null)
      {
         probability = stage.getProbabilityPct();
      }

      if(probability == null)
      {
         return (null);
      }

      return (deal.getAmount()
         .multiply(new BigDecimal(probability))
         .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
   }

}
