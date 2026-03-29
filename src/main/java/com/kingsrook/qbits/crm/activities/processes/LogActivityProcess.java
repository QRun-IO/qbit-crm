/*******************************************************************************
 ** Process to log a CRM activity. Creates a single crm_activity record with
 ** the provided fields and sets ownerUserId from the current session user.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.processes;


import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityOutcome;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** MetaDataProducer + BackendStep that creates a crm_activity record from
 ** the provided input fields. Sets ownerUserId from the current session.
 *******************************************************************************/
public class LogActivityProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "logActivity";



   /*******************************************************************************
    ** Produce the process metadata with input fields for all activity fields.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Log Activity")
         .withTableName(Activity.TABLE_NAME)
         .withIcon(new QIcon().withName("event_note"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(LogActivityProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("activityTypeId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(ActivityType.TABLE_NAME),
                  new QFieldMetaData("subject", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("description", QFieldType.STRING),
                  new QFieldMetaData("contactId", QFieldType.INTEGER).withPossibleValueSourceName(Contact.TABLE_NAME),
                  new QFieldMetaData("companyId", QFieldType.INTEGER).withPossibleValueSourceName(Company.TABLE_NAME),
                  new QFieldMetaData("dealId", QFieldType.INTEGER),
                  new QFieldMetaData("direction", QFieldType.INTEGER).withPossibleValueSourceName(CrmDirection.NAME),
                  new QFieldMetaData("durationMinutes", QFieldType.INTEGER),
                  new QFieldMetaData("outcomeId", QFieldType.INTEGER).withPossibleValueSourceName(ActivityOutcome.TABLE_NAME),
                  new QFieldMetaData("location", QFieldType.STRING),
                  new QFieldMetaData("conferenceLink", QFieldType.STRING),
                  new QFieldMetaData("startDate", QFieldType.DATE_TIME),
                  new QFieldMetaData("endDate", QFieldType.DATE_TIME)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the process: build an Activity entity from inputs and insert it.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String ownerUserId = QContext.getQSession().getIdReference();

      Activity activity = new Activity()
         .withActivityTypeId(input.getValueInteger("activityTypeId"))
         .withSubject(input.getValueString("subject"))
         .withDescription(input.getValueString("description"))
         .withContactId(input.getValueInteger("contactId"))
         .withCompanyId(input.getValueInteger("companyId"))
         .withDealId(input.getValueInteger("dealId"))
         .withDirection(input.getValueInteger("direction"))
         .withDurationMinutes(input.getValueInteger("durationMinutes"))
         .withOutcomeId(input.getValueInteger("outcomeId"))
         .withLocation(input.getValueString("location"))
         .withConferenceLink(input.getValueString("conferenceLink"))
         .withStartDate(input.getValueInstant("startDate"))
         .withEndDate(input.getValueInstant("endDate"))
         .withOwnerUserId(ownerUserId)
         .withIsCompleted(false);

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(activity));

      Integer activityId = insertOutput.getRecords().get(0).getValueInteger("id");
      output.addValue("activityId", activityId);
   }

}
