/*******************************************************************************
 ** Process to reopen a closed deal by moving it to an active (non-closed)
 ** pipeline stage. Clears actualCloseDate and winLossReasonId, recalculates
 ** weighted amount, and logs stage history and audit entries.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.processes;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
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


/*******************************************************************************
 ** MetaDataProducer + BackendStep for reopening a closed deal by moving it
 ** to an active pipeline stage. Validates the deal is currently closed, the
 ** target stage is not closed, clears close data, and logs the transition.
 *******************************************************************************/
public class ReopenDealProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "reopenDeal";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Reopen Deal")
         .withTableName(Deal.TABLE_NAME)
         .withIcon(new QIcon().withName("replay"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ReopenDealProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("dealId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("targetStageId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the reopen process.
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

      ///////////////////////////////////////////////////////////////////
      // Step 2: load the current stage and verify deal is closed      //
      ///////////////////////////////////////////////////////////////////
      QRecord currentStageRecord = GetAction.execute(PipelineStage.TABLE_NAME, deal.getPipelineStageId());
      if(currentStageRecord == null)
      {
         throw new QUserFacingException("Current stage not found: " + deal.getPipelineStageId());
      }

      PipelineStage currentStage = new PipelineStage(currentStageRecord);
      if(!Boolean.TRUE.equals(currentStage.getIsClosedWon()) && !Boolean.TRUE.equals(currentStage.getIsClosedLost()))
      {
         throw new QUserFacingException("Deal is not in a closed stage and cannot be reopened");
      }

      /////////////////////////////////////////
      // Step 3: load the target stage       //
      /////////////////////////////////////////
      QRecord targetStageRecord = GetAction.execute(PipelineStage.TABLE_NAME, targetStageId);
      if(targetStageRecord == null)
      {
         throw new QUserFacingException("Target stage not found: " + targetStageId);
      }

      PipelineStage targetStage = new PipelineStage(targetStageRecord);

      ///////////////////////////////////////////////////////////////////
      // Step 4: validate target stage is NOT closed                   //
      ///////////////////////////////////////////////////////////////////
      if(Boolean.TRUE.equals(targetStage.getIsClosedWon()) || Boolean.TRUE.equals(targetStage.getIsClosedLost()))
      {
         throw new QUserFacingException("Target stage cannot be a closed stage when reopening a deal");
      }

      //////////////////////////////////////////////
      // Step 5: capture the old stage id         //
      //////////////////////////////////////////////
      Integer oldStageId = deal.getPipelineStageId();
      Instant now        = Instant.now();

      /////////////////////////////////////////////////////////////////////
      // Step 6: recalculate weighted amount                              //
      /////////////////////////////////////////////////////////////////////
      BigDecimal weightedAmount = MoveDealStageProcess.calculateWeightedAmount(deal, targetStage);

      //////////////////////////////////////////////////////////////
      // Step 7: update the deal: clear close data, set new stage //
      //////////////////////////////////////////////////////////////
      QRecord updateRecord = new QRecord()
         .withValue("id", dealId)
         .withValue("pipelineStageId", targetStageId)
         .withValue("stageEnteredDate", now)
         .withValue("weightedAmount", weightedAmount)
         .withValue("actualCloseDate", null)
         .withValue("winLossReasonId", null);

      new UpdateAction().execute(
         new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));

      //////////////////////////////////////////////////////////////
      // Step 8: insert DealStageHistory row                      //
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
      // Step 9: log audit entry                                  //
      //////////////////////////////////////////////////////////////
      AuditLog auditEntry = new AuditLog()
         .withEntityType(CrmEntityType.DEAL.getId())
         .withEntityId(dealId)
         .withAction(CrmAuditAction.STAGE_CHANGED.getId())
         .withUserId(sessionUserId)
         .withFieldName("pipelineStageId")
         .withOldValue(oldStageId != null ? String.valueOf(oldStageId) : null)
         .withNewValue(String.valueOf(targetStageId))
         .withMessage("Deal reopened to stage: " + targetStage.getName());

      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(auditEntry));

      output.addValue("dealId", dealId);
      output.addValue("newStageId", targetStageId);
   }

}
