/*******************************************************************************
 ** POST_INSERT, POST_UPDATE, and POST_DELETE table customizer for
 ** DealProduct that recalculates line-item totalAmount and rolls up
 ** the sum to the parent Deal's amount field.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Recalculates totalAmount on each DealProduct line item, then sums all
 ** line items for the parent deal and updates deal.amount.
 *******************************************************************************/
public class DealProductRecalculationCustomizer implements TableCustomizerInterface
{
   private static final QLogger LOG = QLogger.getLogger(DealProductRecalculationCustomizer.class);



   /*******************************************************************************
    ** Post-insert: recalculate totalAmount for inserted records and update parent.
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      return (recalculate(records));
   }



   /*******************************************************************************
    ** Post-update: recalculate totalAmount for updated records and update parent.
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, java.util.Optional<List<QRecord>> oldRecordList) throws QException
   {
      return (recalculate(records));
   }



   /*******************************************************************************
    ** Post-delete: recalculate parent deal's amount from remaining line items.
    *******************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      Set<Integer> dealIds = new HashSet<>();
      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Integer dealId = ValueUtils.getValueAsInteger(record.getValue("dealId"));
         if(dealId != null)
         {
            dealIds.add(dealId);
         }
      }

      for(Integer dealId : dealIds)
      {
         recalculateParentDeal(dealId);
      }

      return (records);
   }



   /*******************************************************************************
    ** Recalculate totalAmount on each record and then update parent deals.
    ** Tracks computed sums in-memory to avoid stale re-reads from the database.
    *******************************************************************************/
   private List<QRecord> recalculate(List<QRecord> records) throws QException
   {
      Map<Integer, BigDecimal> dealIdToComputedSum = new HashMap<>();
      List<QRecord> updateRecords = new ArrayList<>();

      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Integer recordId = ValueUtils.getValueAsInteger(record.getValue("id"));
         if(recordId == null)
         {
            continue;
         }

         try
         {
            BigDecimal totalAmount = calculateTotalAmount(record);
            if(totalAmount != null)
            {
               QRecord updateRecord = new QRecord();
               updateRecord.setValue("id", recordId);
               updateRecord.setValue("totalAmount", totalAmount);
               updateRecords.add(updateRecord);

               /////////////////////////////////////////////////////////////////
               // track in-memory sum per deal to avoid stale re-reads (C-3) //
               /////////////////////////////////////////////////////////////////
               Integer dealId = ValueUtils.getValueAsInteger(record.getValue("dealId"));
               if(dealId != null)
               {
                  dealIdToComputedSum.merge(dealId, totalAmount, BigDecimal::add);
               }
            }
            else
            {
               Integer dealId = ValueUtils.getValueAsInteger(record.getValue("dealId"));
               if(dealId != null)
               {
                  dealIdToComputedSum.putIfAbsent(dealId, BigDecimal.ZERO);
               }
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error calculating totalAmount for DealProduct id=" + recordId, e);
         }
      }

      ///////////////////////////////////////////
      // batch update all DealProduct rows     //
      ///////////////////////////////////////////
      if(!updateRecords.isEmpty())
      {
         new UpdateAction().execute(new UpdateInput(DealProduct.TABLE_NAME).withRecords(updateRecords));
      }

      /////////////////////////////////////////////////////////////////////////
      // update each parent deal using in-memory sums plus any pre-existing //
      // DealProduct rows not in this batch                                 //
      /////////////////////////////////////////////////////////////////////////
      for(Integer dealId : dealIdToComputedSum.keySet())
      {
         recalculateParentDealWithInMemorySums(dealId, dealIdToComputedSum.get(dealId), records);
      }

      return (records);
   }



   /*******************************************************************************
    ** Calculate totalAmount = quantity * unitPrice * (1 - discountPct / 100).
    *******************************************************************************/
   private BigDecimal calculateTotalAmount(QRecord record)
   {
      BigDecimal quantity = ValueUtils.getValueAsBigDecimal(record.getValue("quantity"));
      BigDecimal unitPrice = ValueUtils.getValueAsBigDecimal(record.getValue("unitPrice"));

      if(quantity == null || unitPrice == null)
      {
         return (null);
      }

      BigDecimal discountPct = ValueUtils.getValueAsBigDecimal(record.getValue("discountPct"));
      if(discountPct == null)
      {
         discountPct = BigDecimal.ZERO;
      }

      ///////////////////////////////////////////////////////////
      // totalAmount = quantity * unitPrice * (1 - discount%)  //
      ///////////////////////////////////////////////////////////
      BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
         discountPct.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));

      return (quantity.multiply(unitPrice).multiply(discountMultiplier)
         .setScale(2, RoundingMode.HALF_UP));
   }



   /*******************************************************************************
    ** Query all DealProduct rows for a deal, sum totalAmount, and update
    ** the deal's amount field. Used by post-delete where no in-memory sums
    ** are available.
    *******************************************************************************/
   private void recalculateParentDeal(Integer dealId) throws QException
   {
      try
      {
         QueryOutput queryOutput = new QueryAction().execute(
            new QueryInput(DealProduct.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

         BigDecimal sum = BigDecimal.ZERO;
         for(QRecord dpRecord : queryOutput.getRecords())
         {
            BigDecimal total = ValueUtils.getValueAsBigDecimal(dpRecord.getValue("totalAmount"));
            if(total != null)
            {
               sum = sum.add(total);
            }
         }

         QRecord dealUpdate = new QRecord();
         dealUpdate.setValue("id", dealId);
         dealUpdate.setValue("amount", sum);
         new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(dealUpdate));
      }
      catch(Exception e)
      {
         LOG.warn("Error recalculating parent deal amount for dealId=" + dealId, e);
      }
   }



   /*******************************************************************************
    ** Update a parent deal's amount using in-memory computed sums for the
    ** current batch, plus any existing DealProduct rows not in this batch.
    ** Avoids re-querying rows we just updated (C-3: stale read fix).
    *******************************************************************************/
   private void recalculateParentDealWithInMemorySums(Integer dealId, BigDecimal batchSum, List<QRecord> batchRecords) throws QException
   {
      try
      {
         /////////////////////////////////////////////////////////////////
         // collect IDs of DealProduct rows in the current batch       //
         /////////////////////////////////////////////////////////////////
         Set<Integer> batchProductIds = new HashSet<>();
         for(QRecord record : batchRecords)
         {
            Integer productDealId = ValueUtils.getValueAsInteger(record.getValue("dealId"));
            if(dealId.equals(productDealId))
            {
               Integer productId = ValueUtils.getValueAsInteger(record.getValue("id"));
               if(productId != null)
               {
                  batchProductIds.add(productId);
               }
            }
         }

         /////////////////////////////////////////////////////////////////
         // query remaining DealProduct rows not in the current batch  //
         /////////////////////////////////////////////////////////////////
         QueryOutput queryOutput = new QueryAction().execute(
            new QueryInput(DealProduct.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));

         BigDecimal otherSum = BigDecimal.ZERO;
         for(QRecord dpRecord : queryOutput.getRecords())
         {
            Integer dpId = ValueUtils.getValueAsInteger(dpRecord.getValue("id"));
            if(dpId != null && !batchProductIds.contains(dpId))
            {
               BigDecimal total = ValueUtils.getValueAsBigDecimal(dpRecord.getValue("totalAmount"));
               if(total != null)
               {
                  otherSum = otherSum.add(total);
               }
            }
         }

         BigDecimal totalDealAmount = batchSum.add(otherSum);

         QRecord dealUpdate = new QRecord();
         dealUpdate.setValue("id", dealId);
         dealUpdate.setValue("amount", totalDealAmount);
         new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(dealUpdate));
      }
      catch(Exception e)
      {
         LOG.warn("Error recalculating parent deal amount for dealId=" + dealId, e);
      }
   }

}
