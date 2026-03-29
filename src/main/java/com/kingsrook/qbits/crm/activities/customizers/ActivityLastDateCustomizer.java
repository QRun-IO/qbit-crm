/*******************************************************************************
 ** POST_INSERT table customizer that propagates lastActivityDate and
 ** lastContactedDate to linked Contact, Company, and Deal records whenever
 ** a new Activity is created.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.customizers;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** On activity insert, update the lastActivityDate (and, for outbound
 ** activities, lastContactedDate) on any linked Contact, Company, or Deal.
 *******************************************************************************/
public class ActivityLastDateCustomizer extends AbstractPostInsertCustomizer
{
   private static final QLogger LOG = QLogger.getLogger(ActivityLastDateCustomizer.class);



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

         Integer direction = ValueUtils.getValueAsInteger(record.getValue("direction"));
         boolean isOutbound = direction != null && direction.equals(CrmDirection.OUTBOUND.getPossibleValueId());

         ///////////////////////////////////////////
         // update linked contact if present      //
         ///////////////////////////////////////////
         Integer contactId = ValueUtils.getValueAsInteger(record.getValue("contactId"));
         if(contactId != null)
         {
            updateContactDates(contactId, now, isOutbound);
         }

         ///////////////////////////////////////////
         // update linked company if present      //
         ///////////////////////////////////////////
         Integer companyId = ValueUtils.getValueAsInteger(record.getValue("companyId"));
         if(companyId != null)
         {
            updateCompanyDates(companyId, now, isOutbound);
         }

         ///////////////////////////////////////////
         // update linked deal if present         //
         ///////////////////////////////////////////
         Integer dealId = ValueUtils.getValueAsInteger(record.getValue("dealId"));
         if(dealId != null)
         {
            updateDealLastActivityDate(dealId, now);
         }
      }

      return (records);
   }



   /*******************************************************************************
    ** Update a contact's lastActivityDate, and lastContactedDate if outbound.
    *******************************************************************************/
   private void updateContactDates(Integer contactId, Instant now, boolean isOutbound) throws QException
   {
      try
      {
         GetOutput getOutput = new GetAction().execute(new GetInput(Contact.TABLE_NAME).withPrimaryKey(contactId));
         if(getOutput.getRecord() == null)
         {
            return;
         }

         QRecord updateRecord = new QRecord();
         updateRecord.setValue("id", contactId);
         updateRecord.setValue("lastActivityDate", now);

         if(isOutbound)
         {
            updateRecord.setValue("lastContactedDate", now);
         }

         new UpdateAction().execute(new UpdateInput(Contact.TABLE_NAME).withRecord(updateRecord));
      }
      catch(Exception e)
      {
         LOG.warn("Error updating contact dates for contactId=" + contactId, e);
      }
   }



   /*******************************************************************************
    ** Update a company's lastActivityDate, and lastContactedDate if outbound.
    *******************************************************************************/
   private void updateCompanyDates(Integer companyId, Instant now, boolean isOutbound) throws QException
   {
      try
      {
         GetOutput getOutput = new GetAction().execute(new GetInput(Company.TABLE_NAME).withPrimaryKey(companyId));
         if(getOutput.getRecord() == null)
         {
            return;
         }

         QRecord updateRecord = new QRecord();
         updateRecord.setValue("id", companyId);
         updateRecord.setValue("lastActivityDate", now);

         if(isOutbound)
         {
            updateRecord.setValue("lastContactedDate", now);
         }

         new UpdateAction().execute(new UpdateInput(Company.TABLE_NAME).withRecord(updateRecord));
      }
      catch(Exception e)
      {
         LOG.warn("Error updating company dates for companyId=" + companyId, e);
      }
   }



   /*******************************************************************************
    ** Update a deal's lastActivityDate.
    *******************************************************************************/
   private void updateDealLastActivityDate(Integer dealId, Instant now) throws QException
   {
      try
      {
         GetOutput getOutput = new GetAction().execute(new GetInput(Deal.TABLE_NAME).withPrimaryKey(dealId));
         if(getOutput.getRecord() == null)
         {
            return;
         }

         QRecord updateRecord = new QRecord();
         updateRecord.setValue("id", dealId);
         updateRecord.setValue("lastActivityDate", now);

         new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(updateRecord));
      }
      catch(Exception e)
      {
         LOG.warn("Error updating deal lastActivityDate for dealId=" + dealId, e);
      }
   }

}
