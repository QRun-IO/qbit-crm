/*******************************************************************************
 ** Scheduled process that fires reminders for activities with isReminderSet
 ** = true and reminderDate <= now. Clears the reminder flag after processing.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for firing reminders. Intended to run on a
 ** 5-minute schedule. Finds activities with active reminders whose
 ** reminderDate has passed, clears the flag, and would fire notifications
 ** in a production environment.
 *******************************************************************************/
public class FireRemindersProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "fireReminders";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Fire Reminders")
         .withIcon(new QIcon().withName("alarm"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(FireRemindersProcess.class))
         ));
   }



   /*******************************************************************************
    ** Execute: find activities with due reminders and clear the reminder flag.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      ///////////////////////////////////////////////
      // query activities with pending reminders   //
      ///////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("isReminderSet", QCriteriaOperator.EQUALS, true))
               .withCriteria(new QFilterCriteria("reminderDate", QCriteriaOperator.LESS_THAN_OR_EQUALS, Instant.now()))
               .withCriteria(new QFilterCriteria("isCompleted", QCriteriaOperator.EQUALS, false))));

      int firedCount = 0;
      for(QRecord record : queryOutput.getRecords())
      {
         /////////////////////////////////////////////////////////////
         // In production this would fire a notification. For now,  //
         // just clear the reminder flag.                           //
         /////////////////////////////////////////////////////////////
         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue("isReminderSet", false);

         new UpdateAction().execute(
            new UpdateInput(Activity.TABLE_NAME).withRecord(updateRecord));

         firedCount++;
      }

      output.addValue("firedCount", firedCount);
   }

}
