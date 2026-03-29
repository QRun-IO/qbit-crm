/*******************************************************************************
 ** PRE_UPDATE table customizer that handles deal pipeline stage transitions.
 **
 ** When a deal's pipelineStageId changes:
 **   - validates required fields for the target stage
 **   - recalculates weightedAmount and amountInBaseCurrency
 **   - sets stageEnteredDate
 **   - sets actualCloseDate on terminal stages (closedWon / closedLost)
 **   - clears actualCloseDate and winLossReasonId on reopen
 **   - inserts a DealStageHistory row
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.sync.CrmCurrencyUtils;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.json.JSONArray;


/*******************************************************************************
 ** Pre-update customizer for the Deal table that handles pipeline stage
 ** transitions and all associated business logic.
 *******************************************************************************/
public class DealStageChangeCustomizer extends AbstractPreUpdateCustomizer
{
   private static final QLogger LOG = QLogger.getLogger(DealStageChangeCustomizer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records) throws QException
   {
      if(isPreview)
      {
         return (records);
      }

      Map<Serializable, QRecord> oldRecordMap = getOldRecordMap();

      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Serializable primaryKey = record.getValue("id");
         QRecord      oldRecord = oldRecordMap.get(primaryKey);
         if(oldRecord == null)
         {
            continue;
         }

         Integer oldStageId = ValueUtils.getValueAsInteger(oldRecord.getValue("pipelineStageId"));
         Integer newStageId = ValueUtils.getValueAsInteger(record.getValue("pipelineStageId"));

         ///////////////////////////////////////////////////////////
         // only fire stage-change logic if the stage has changed //
         ///////////////////////////////////////////////////////////
         if(newStageId == null || Objects.equals(oldStageId, newStageId))
         {
            continue;
         }

         handleStageChange(record, oldRecord, oldStageId, newStageId);
      }

      return (records);
   }



   /*******************************************************************************
    ** Handle all stage-change business logic for a single deal record.
    *******************************************************************************/
   private void handleStageChange(QRecord record, QRecord oldRecord, Integer oldStageId, Integer newStageId) throws QException
   {
      ///////////////////////////////////////////
      // load the new pipeline stage record    //
      ///////////////////////////////////////////
      PipelineStage newStage = loadPipelineStage(newStageId);
      if(newStage == null)
      {
         return;
      }

      ///////////////////////////////////////////
      // validate required fields              //
      ///////////////////////////////////////////
      if(StringUtils.hasContent(newStage.getRequiredFieldsJson()))
      {
         if(!validateRequiredFields(record, oldRecord, newStage.getRequiredFieldsJson()))
         {
            return;
         }
      }

      ///////////////////////////////////////////
      // set stageEnteredDate                  //
      ///////////////////////////////////////////
      Instant now = Instant.now();
      record.setValue("stageEnteredDate", now);

      ///////////////////////////////////////////
      // calculate weightedAmount              //
      ///////////////////////////////////////////
      calculateWeightedAmount(record, oldRecord, newStage);

      ///////////////////////////////////////////
      // calculate amountInBaseCurrency        //
      ///////////////////////////////////////////
      calculateAmountInBaseCurrency(record, oldRecord);

      ///////////////////////////////////////////
      // handle closed won / closed lost       //
      ///////////////////////////////////////////
      boolean newIsClosed = Boolean.TRUE.equals(newStage.getIsClosedWon())
         || Boolean.TRUE.equals(newStage.getIsClosedLost());

      if(newIsClosed)
      {
         record.setValue("actualCloseDate", LocalDate.now());
      }

      ///////////////////////////////////////////////////////////
      // handle reopen (old was closed, new is not) -- only  //
      // load old stage when reopen check is needed (M-4)    //
      ///////////////////////////////////////////////////////////
      if(!newIsClosed)
      {
         PipelineStage oldStage = loadPipelineStage(oldStageId);
         boolean oldIsClosed = oldStage != null
            && (Boolean.TRUE.equals(oldStage.getIsClosedWon()) || Boolean.TRUE.equals(oldStage.getIsClosedLost()));

         if(oldIsClosed)
         {
            record.setValue("actualCloseDate", null);
            record.setValue("winLossReasonId", null);
         }
      }

      ///////////////////////////////////////////
      // insert DealStageHistory row           //
      ///////////////////////////////////////////
      insertStageHistoryRow(record, oldRecord, oldStageId, newStageId, now);
   }



   /*******************************************************************************
    ** Load a PipelineStage by its primary key.
    *******************************************************************************/
   private PipelineStage loadPipelineStage(Integer stageId) throws QException
   {
      if(stageId == null)
      {
         return (null);
      }

      GetOutput getOutput = new GetAction().execute(new GetInput(PipelineStage.TABLE_NAME).withPrimaryKey(stageId));
      if(getOutput.getRecord() == null)
      {
         return (null);
      }

      return (new PipelineStage(getOutput.getRecord()));
   }



   /*******************************************************************************
    ** Validate that all required fields for the target stage are non-null on
    ** the deal record. Returns false if validation fails (error added to record).
    *******************************************************************************/
   private boolean validateRequiredFields(QRecord record, QRecord oldRecord, String requiredFieldsJson)
   {
      try
      {
         JSONArray requiredFields = new JSONArray(requiredFieldsJson);
         for(int i = 0; i < requiredFields.length(); i++)
         {
            String fieldName = requiredFields.getString(i);

            /////////////////////////////////////////////////////////////////////
            // check the new record first, then fall back to the old record   //
            /////////////////////////////////////////////////////////////////////
            Serializable value = record.getValue(fieldName);
            if(value == null && oldRecord != null)
            {
               value = oldRecord.getValue(fieldName);
            }

            if(value == null)
            {
               record.addError(new BadInputStatusMessage("Required field [" + fieldName + "] must be set before moving to this stage."));
               return (false);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error parsing requiredFieldsJson: " + requiredFieldsJson, e);
      }

      return (true);
   }



   /*******************************************************************************
    ** Calculate weightedAmount based on probability and deal amount.
    ** Uses probabilityOverridePct if set, otherwise the stage's probabilityPct.
    *******************************************************************************/
   private void calculateWeightedAmount(QRecord record, QRecord oldRecord, PipelineStage newStage)
   {
      BigDecimal amount = ValueUtils.getValueAsBigDecimal(record.getValue("amount"));
      if(amount == null && oldRecord != null)
      {
         amount = ValueUtils.getValueAsBigDecimal(oldRecord.getValue("amount"));
      }

      if(amount == null)
      {
         record.setValue("weightedAmount", null);
         return;
      }

      Integer probabilityOverridePct = ValueUtils.getValueAsInteger(record.getValue("probabilityOverridePct"));
      if(probabilityOverridePct == null && oldRecord != null)
      {
         probabilityOverridePct = ValueUtils.getValueAsInteger(oldRecord.getValue("probabilityOverridePct"));
      }

      Integer probability = probabilityOverridePct != null
         ? probabilityOverridePct
         : newStage.getProbabilityPct();

      if(probability == null)
      {
         probability = 0;
      }

      BigDecimal weightedAmount = amount
         .multiply(new BigDecimal(probability))
         .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

      record.setValue("weightedAmount", weightedAmount);
   }



   /*******************************************************************************
    ** Calculate amountInBaseCurrency using the shared CrmCurrencyUtils.
    *******************************************************************************/
   private void calculateAmountInBaseCurrency(QRecord record, QRecord oldRecord) throws QException
   {
      BigDecimal amount = ValueUtils.getValueAsBigDecimal(record.getValue("amount"));
      if(amount == null && oldRecord != null)
      {
         amount = ValueUtils.getValueAsBigDecimal(oldRecord.getValue("amount"));
      }

      if(amount == null)
      {
         record.setValue("amountInBaseCurrency", null);
         return;
      }

      String currencyCode = ValueUtils.getValueAsString(record.getValue("currencyCode"));
      if(!StringUtils.hasContent(currencyCode) && oldRecord != null)
      {
         currencyCode = ValueUtils.getValueAsString(oldRecord.getValue("currencyCode"));
      }

      try
      {
         BigDecimal amountInBase = CrmCurrencyUtils.convertToBaseCurrency(amount, currencyCode);
         record.setValue("amountInBaseCurrency", amountInBase);
      }
      catch(Exception e)
      {
         LOG.warn("Error calculating amountInBaseCurrency for currencyCode=" + currencyCode, e);
         record.setValue("amountInBaseCurrency", amount);
      }
   }



   /*******************************************************************************
    ** Insert a DealStageHistory row to track the stage transition.
    *******************************************************************************/
   private void insertStageHistoryRow(QRecord record, QRecord oldRecord, Integer oldStageId, Integer newStageId, Instant now) throws QException
   {
      try
      {
         Integer dealId = ValueUtils.getValueAsInteger(record.getValue("id"));

         String userId = CrmSessionUtils.getCurrentUserId();

         /////////////////////////////////////////////////////////////////////////
         // calculate duration in from-stage: days between old stageEnteredDate //
         // and now                                                             //
         /////////////////////////////////////////////////////////////////////////
         Integer durationDays = null;
         Instant oldStageEnteredDate = ValueUtils.getValueAsInstant(oldRecord.getValue("stageEnteredDate"));
         if(oldStageEnteredDate != null)
         {
            long days = Duration.between(oldStageEnteredDate, now).toDays();
            durationDays = (int) days;
         }

         DealStageHistory history = new DealStageHistory()
            .withDealId(dealId)
            .withFromStageId(oldStageId)
            .withToStageId(newStageId)
            .withUserId(userId)
            .withTransitionDate(now)
            .withDurationInFromStageDays(durationDays);

         new InsertAction().execute(new InsertInput(DealStageHistory.TABLE_NAME).withRecordEntity(history));
      }
      catch(Exception e)
      {
         LOG.warn("Error inserting DealStageHistory row", e);
      }
   }

}
