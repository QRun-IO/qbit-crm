/*******************************************************************************
 ** POST_INSERT table customizer that initializes newly-created deals with
 ** computed fields: stageEnteredDate, weightedAmount, amountInBaseCurrency,
 ** and the initial DealStageHistory row.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.sync.CrmCurrencyUtils;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Post-insert customizer for the Deal table that sets initial computed
 ** values and creates the first DealStageHistory record.
 *******************************************************************************/
public class DealInitializationCustomizer extends AbstractPostInsertCustomizer
{
   private static final QLogger LOG = QLogger.getLogger(DealInitializationCustomizer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records) throws QException
   {
      Instant now = Instant.now();

      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Integer dealId = ValueUtils.getValueAsInteger(record.getValue("id"));
         if(dealId == null)
         {
            continue;
         }

         try
         {
            initializeDeal(record, dealId, now);
         }
         catch(Exception e)
         {
            LOG.warn("Error initializing deal id=" + dealId, e);
         }
      }

      return (records);
   }



   /*******************************************************************************
    ** Initialize a newly-inserted deal with computed values.
    *******************************************************************************/
   private void initializeDeal(QRecord record, Integer dealId, Instant now) throws QException
   {
      QRecord updateRecord = new QRecord();
      updateRecord.setValue("id", dealId);
      boolean needsUpdate = false;

      ///////////////////////////////////////////
      // set stageEnteredDate if not set       //
      ///////////////////////////////////////////
      Instant stageEnteredDate = ValueUtils.getValueAsInstant(record.getValue("stageEnteredDate"));
      if(stageEnteredDate == null)
      {
         updateRecord.setValue("stageEnteredDate", now);
         needsUpdate = true;
      }

      ///////////////////////////////////////////
      // load the pipeline stage for calcs     //
      ///////////////////////////////////////////
      Integer stageId = ValueUtils.getValueAsInteger(record.getValue("pipelineStageId"));
      PipelineStage stage = null;
      if(stageId != null)
      {
         GetOutput stageOutput = new GetAction().execute(new GetInput(PipelineStage.TABLE_NAME).withPrimaryKey(stageId));
         if(stageOutput.getRecord() != null)
         {
            stage = new PipelineStage(stageOutput.getRecord());
         }
      }

      ///////////////////////////////////////////
      // calculate weightedAmount              //
      ///////////////////////////////////////////
      BigDecimal amount = ValueUtils.getValueAsBigDecimal(record.getValue("amount"));
      if(amount != null && stage != null)
      {
         Integer probabilityOverridePct = ValueUtils.getValueAsInteger(record.getValue("probabilityOverridePct"));
         Integer probability = probabilityOverridePct != null
            ? probabilityOverridePct
            : stage.getProbabilityPct();

         if(probability == null)
         {
            probability = 0;
         }

         BigDecimal weightedAmount = amount
            .multiply(new BigDecimal(probability))
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

         updateRecord.setValue("weightedAmount", weightedAmount);
         needsUpdate = true;
      }

      ///////////////////////////////////////////
      // calculate amountInBaseCurrency        //
      ///////////////////////////////////////////
      if(amount != null)
      {
         BigDecimal amountInBase = calculateAmountInBaseCurrency(amount, record);
         updateRecord.setValue("amountInBaseCurrency", amountInBase);
         needsUpdate = true;
      }

      ///////////////////////////////////////////
      // update the deal record                //
      ///////////////////////////////////////////
      if(needsUpdate)
      {
         new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));
      }

      ///////////////////////////////////////////
      // insert initial DealStageHistory row   //
      ///////////////////////////////////////////
      if(stageId != null)
      {
         insertInitialStageHistory(dealId, stageId, now);
      }
   }



   /*******************************************************************************
    ** Calculate amountInBaseCurrency using the shared CrmCurrencyUtils.
    *******************************************************************************/
   private BigDecimal calculateAmountInBaseCurrency(BigDecimal amount, QRecord record) throws QException
   {
      String currencyCode = ValueUtils.getValueAsString(record.getValue("currencyCode"));
      try
      {
         return (CrmCurrencyUtils.convertToBaseCurrency(amount, currencyCode));
      }
      catch(Exception e)
      {
         LOG.warn("Error calculating amountInBaseCurrency for currencyCode=" + currencyCode, e);
         return (amount);
      }
   }



   /*******************************************************************************
    ** Insert the initial DealStageHistory row (fromStageId = null).
    *******************************************************************************/
   private void insertInitialStageHistory(Integer dealId, Integer toStageId, Instant now) throws QException
   {
      try
      {
         String userId = CrmSessionUtils.getCurrentUserId();

         DealStageHistory history = new DealStageHistory()
            .withDealId(dealId)
            .withFromStageId(null)
            .withToStageId(toStageId)
            .withUserId(userId)
            .withTransitionDate(now)
            .withDurationInFromStageDays(null);

         new InsertAction().execute(new InsertInput(DealStageHistory.TABLE_NAME).withRecordEntity(history));
      }
      catch(Exception e)
      {
         LOG.warn("Error inserting initial DealStageHistory for dealId=" + dealId, e);
      }
   }

}
