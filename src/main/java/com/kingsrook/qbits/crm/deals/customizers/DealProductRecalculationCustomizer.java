/*******************************************************************************
 ** POST_INSERT, POST_UPDATE, and POST_DELETE table customizer for
 ** DealProduct that recalculates line-item totalAmount and rolls up
 ** the sum to the parent Deal's amount field.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals.customizers;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
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
    *******************************************************************************/
   private List<QRecord> recalculate(List<QRecord> records) throws QException
   {
      Set<Integer> dealIds = new HashSet<>();

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
               new UpdateAction().execute(new UpdateInput(DealProduct.TABLE_NAME).withRecord(updateRecord));
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error calculating totalAmount for DealProduct id=" + recordId, e);
         }

         Integer dealId = ValueUtils.getValueAsInteger(record.getValue("dealId"));
         if(dealId != null)
         {
            dealIds.add(dealId);
         }
      }

      ///////////////////////////////////////////
      // recalculate each parent deal's amount //
      ///////////////////////////////////////////
      for(Integer dealId : dealIds)
      {
         recalculateParentDeal(dealId);
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
    ** the deal's amount field.
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

}
