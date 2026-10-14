/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*******************************************************************************
 ** Scheduled process to execute pending email sequence steps.
 *******************************************************************************/

package com.kingsrook.qbits.crm.email.processes;


import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmMarketingApi;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmSequenceStepType;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.email.CrmEmails;
import com.kingsrook.qbits.crm.email.model.EmailSequence;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qbits.crm.email.model.SequenceStep;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
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
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to process pending sequence steps. Intended
 ** to run on a schedule (every 5 minutes). Queries active enrollments whose
 ** nextStepDate has passed and executes the appropriate step action.
 *******************************************************************************/
public class ProcessSequenceStepsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_process_sequence_steps";

   private static final int MAX_FAILURES = 3;

   /*******************************************************************************
    ** Resolve the host transport; tests may supply a fake.
    *******************************************************************************/
   protected CrmEmailTransport emailTransport() throws QException
   {
      return (new CrmEmailTransport(CrmApi.config()));
   }




   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withIsHidden(true)
         .withLabel("Process Sequence Steps")
         .withIcon(new QIcon().withName("play_circle"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ProcessSequenceStepsProcess.class))
         ));
   }



   /*******************************************************************************
    ** Execute the step processing logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      ///////////////////////////////////////////////////////////
      // query enrollments where status=ACTIVE and due now     //
      ///////////////////////////////////////////////////////////
      CrmEmailTransport transport = emailTransport();
      boolean ready = transport.readiness() != CrmReadiness.NOT_CONFIGURED;
      boolean activeSequences = !new QueryAction().execute(new QueryInput(EmailSequence.TABLE_NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)).withLimit(1))).getRecords().isEmpty();
      if(!ready && activeSequences)
      {
         QAlerts.raise(new AlertSignal("crm.email.unavailable").withDedupKey("transport").withTitle("CRM email is unavailable"));
      }
      else if(ready)
      {
         QAlerts.resolve("crm.email.unavailable", "transport", "Email transport is ready");
      }
      int processedCount = 0;
      Integer lastId = null;
      Instant dueThrough = Instant.now();
      while(true)
      {
         QQueryFilter filter = new QQueryFilter()
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getPossibleValueId()))
            .withCriteria(new QFilterCriteria("nextStepDate", QCriteriaOperator.LESS_THAN_OR_EQUALS, dueThrough))
            .withOrderBy(new QFilterOrderBy("id")).withLimit(200);
         if(lastId != null)
         {
            filter.withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId));
         }
         List<QRecord> page = new QueryAction().execute(new QueryInput(SequenceEnrollment.TABLE_NAME).withFilter(filter)).getRecords();
         for(QRecord enrollmentRecord : page)
         {
            SequenceEnrollment enrollment = new SequenceEnrollment(enrollmentRecord);
            try
            {
               processEnrollment(enrollment, transport);
               processedCount++;
            }
            catch(Exception e)
            {
               handleFailure(enrollment, e.getMessage());
            }
            lastId = enrollment.getId();
         }
         if(page.size() < 200)
         {
            break;
         }
      }

      output.addValue("processedCount", processedCount);
   }



   /***************************************************************************
    ** Process a single enrollment: load the next step and execute it.
    ** Respects businessDaysOnly and sendWindow settings on the sequence.
    ***************************************************************************/
   private void processEnrollment(SequenceEnrollment enrollment, CrmEmailTransport transport) throws QException
   {
      Integer nextStepNumber = enrollment.getCurrentStepNumber() + 1;

      /////////////////////////////////////////////
      // load the sequence for totalSteps check  //
      /////////////////////////////////////////////
      GetOutput sequenceGet = new GetAction().execute(
         new GetInput(EmailSequence.TABLE_NAME).withPrimaryKey(enrollment.getSequenceId()));

      EmailSequence sequence = sequenceGet.getRecord() != null
         ? new EmailSequence(sequenceGet.getRecord()) : null;

      if(sequence == null || !Boolean.TRUE.equals(sequence.getIsActive()))
      {
         return;
      }

      /////////////////////////////////////////////
      // load the contact for timezone           //
      /////////////////////////////////////////////
      GetOutput contactGet = new GetAction().execute(
         new GetInput(Contact.TABLE_NAME).withPrimaryKey(enrollment.getContactId()));

      String contactTimezone = null;
      if(contactGet.getRecord() != null)
      {
         contactTimezone = contactGet.getRecord().getValueString("timezone");
      }

      ZoneId zoneId = resolveZoneId(contactTimezone);
      ZonedDateTime nowInZone = Instant.now().atZone(zoneId);

      ///////////////////////////////////////////////////////////////
      // check businessDaysOnly -- skip weekends                   //
      ///////////////////////////////////////////////////////////////
      if(sequence != null && Boolean.TRUE.equals(sequence.getBusinessDaysOnly()))
      {
         DayOfWeek dayOfWeek = nowInZone.getDayOfWeek();
         if(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
         {
            return;
         }
      }

      ///////////////////////////////////////////////////////////////
      // check send window -- skip if outside window               //
      ///////////////////////////////////////////////////////////////
      if(sequence != null
         && sequence.getSendWindowStartHour() != null
         && sequence.getSendWindowEndHour() != null)
      {
         int currentHour = nowInZone.getHour();
         if(currentHour < sequence.getSendWindowStartHour()
            || currentHour >= sequence.getSendWindowEndHour())
         {
            return;
         }
      }

      /////////////////////////////////////////////
      // load the next step                      //
      /////////////////////////////////////////////
      QueryOutput stepQuery = new QueryAction().execute(
         new QueryInput(SequenceStep.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("sequenceId", QCriteriaOperator.EQUALS, enrollment.getSequenceId()))
               .withCriteria(new QFilterCriteria("stepNumber", QCriteriaOperator.EQUALS, nextStepNumber))));

      if(stepQuery.getRecords().isEmpty())
      {
         ///////////////////////////////////////////////////
         // no more steps -- mark as completed            //
         ///////////////////////////////////////////////////
         completeEnrollment(enrollment);
         return;
      }

      SequenceStep step = new SequenceStep(stepQuery.getRecords().get(0));
      CrmSequenceStepType stepType = CrmSequenceStepType.getById(step.getStepType());

      /////////////////////////////////////////////
      // execute based on step type              //
      /////////////////////////////////////////////
      if(stepType != null)
      {
         switch(stepType)
         {
            case EMAIL ->
            {
               var eligibility = new CrmMarketingApi().checkEmailEligibility(enrollment.getContactId());
               if(!eligibility.eligible())
               {
                  CrmRecords.update(SequenceEnrollment.TABLE_NAME, new QRecord().withValue("id", enrollment.getId())
                     .withValue("status", CrmEnrollmentStatus.PAUSED.getId()).withValue("lastFailureMessage", eligibility.reason())
                     .withValue("nextStepDate", null));
                  return;
               }
               if(transport.readiness() == CrmReadiness.NOT_CONFIGURED)
               {
                  return;
               }
               if(step.getEmailTemplateId() == null)
               {
                  throw (new QException("Sequence email template is required"));
               }
               CrmEmails.send(transport, contactGet.getRecord(), step.getEmailTemplateId(), null, null,
                  "seq", enrollment.getId() + ":" + step.getId(), enrollment.getId());
            }
            case CALL -> executeCallStep(enrollment, step);
            case TASK -> executeTaskStep(enrollment, step);
            case WAIT ->
            {
               // just advance
            }
            default -> throw (new QException("Unsupported sequence step type: " + stepType));
         }
      }

      /////////////////////////////////////////////
      // advance to next step                    //
      /////////////////////////////////////////////
      boolean businessDaysOnly = sequence != null && Boolean.TRUE.equals(sequence.getBusinessDaysOnly());
      advanceEnrollment(enrollment, nextStepNumber, sequence, businessDaysOnly, contactTimezone);
   }



   /***************************************************************************
    ** Execute a CALL step: create a task activity assigned to the contact owner.
    ***************************************************************************/
   private void executeCallStep(SequenceEnrollment enrollment, SequenceStep step) throws QException
   {
      String ownerUserId = "system";
      GetOutput contactGet = new GetAction().execute(
         new GetInput(Contact.TABLE_NAME).withPrimaryKey(enrollment.getContactId()));

      if(contactGet.getRecord() != null)
      {
         String contactOwner = contactGet.getRecord().getValueString("ownerUserId");
         if(StringUtils.hasContent(contactOwner))
         {
            ownerUserId = contactOwner;
         }
      }

      Integer activityTypeId = lookupActivityTypeByName("Call");

      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(activityTypeId)
               .withContactId(enrollment.getContactId())
               .withDealId(enrollment.getDealId())
               .withSubject("Call task: sequence step " + step.getStepNumber())
               .withOwnerUserId(ownerUserId)
               .withIsCompleted(false)));
   }



   /***************************************************************************
    ** Execute a TASK step: create a task activity with subject/description.
    ***************************************************************************/
   private void executeTaskStep(SequenceEnrollment enrollment, SequenceStep step) throws QException
   {
      String subject = StringUtils.hasContent(step.getTaskSubject())
         ? step.getTaskSubject()
         : "Task: sequence step " + step.getStepNumber();

      Integer activityTypeId = lookupActivityTypeByName("Task");

      new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(
            new Activity()
               .withActivityTypeId(activityTypeId)
               .withContactId(enrollment.getContactId())
               .withDealId(enrollment.getDealId())
               .withSubject(subject)
               .withDescription(step.getTaskDescription())
               .withOwnerUserId("system")
               .withIsCompleted(false)));
   }



   /***************************************************************************
    ** Look up an ActivityType by name. Returns null if not found.
    ***************************************************************************/
   private Integer lookupActivityTypeByName(String name) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityType.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, name))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }



   /***************************************************************************
    ** Advance the enrollment to the next step and calculate the next date.
    ** Respects businessDaysOnly when computing the delay.
    ***************************************************************************/
   private void advanceEnrollment(SequenceEnrollment enrollment, Integer newStepNumber,
                                  EmailSequence sequence, boolean businessDaysOnly,
                                  String contactTimezone) throws QException
   {
      ///////////////////////////////////////////////
      // check if we've completed all steps        //
      ///////////////////////////////////////////////
      Integer totalSteps = (sequence != null && sequence.getTotalSteps() != null)
         ? sequence.getTotalSteps() : Integer.MAX_VALUE;

      if(newStepNumber >= totalSteps)
      {
         completeEnrollment(enrollment);
         return;
      }

      ///////////////////////////////////////////////
      // look up the NEXT step for delay calc      //
      ///////////////////////////////////////////////
      Integer followingStepNumber = newStepNumber + 1;
      QueryOutput nextStepQuery = new QueryAction().execute(
         new QueryInput(SequenceStep.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("sequenceId", QCriteriaOperator.EQUALS, enrollment.getSequenceId()))
               .withCriteria(new QFilterCriteria("stepNumber", QCriteriaOperator.EQUALS, followingStepNumber))));

      Instant nextDate;
      if(nextStepQuery.getRecords().isEmpty())
      {
         ////////////////////////////////////////////////////////
         // no following step -- this was the last step        //
         ////////////////////////////////////////////////////////
         completeEnrollment(enrollment);
         return;
      }
      else
      {
         SequenceStep nextStep = new SequenceStep(nextStepQuery.getRecords().get(0));
         Integer delayDays = nextStep.getDelayDays() != null ? nextStep.getDelayDays() : 0;
         Integer delayHours = nextStep.getDelayHours() != null ? nextStep.getDelayHours() : 0;
         nextDate = calculateNextStepDate(Instant.now(), delayDays, delayHours, businessDaysOnly, contactTimezone);
      }

      QRecord updateRecord = new QRecord()
         .withValue("id", enrollment.getId())
         .withValue("currentStepNumber", newStepNumber)
         .withValue("nextStepDate", nextDate);

      new UpdateAction().execute(
         new UpdateInput(SequenceEnrollment.TABLE_NAME).withRecord(updateRecord));
   }



   /***************************************************************************
    ** Mark an enrollment as completed.
    ***************************************************************************/
   private void completeEnrollment(SequenceEnrollment enrollment) throws QException
   {
      QRecord updateRecord = new QRecord()
         .withValue("id", enrollment.getId())
         .withValue("status", CrmEnrollmentStatus.COMPLETED.getPossibleValueId())
         .withValue("completedDate", Instant.now())
         .withValue("nextStepDate", null);

      new UpdateAction().execute(
         new UpdateInput(SequenceEnrollment.TABLE_NAME).withRecord(updateRecord));
   }



   /***************************************************************************
    ** Calculate the next step date, adding delay days/hours to now.
    ** If businessDaysOnly is true, weekends are skipped when counting days.
    ***************************************************************************/
   static Instant calculateNextStepDate(Instant now, int delayDays, int delayHours,
                                        boolean businessDaysOnly, String contactTimezone)
   {
      ZoneId zoneId = resolveZoneId(contactTimezone);
      ZonedDateTime result = now.atZone(zoneId).plus(delayHours, ChronoUnit.HOURS);

      if(businessDaysOnly && delayDays > 0)
      {
         int addedDays = 0;
         while(addedDays < delayDays)
         {
            result = result.plusDays(1);
            DayOfWeek dow = result.getDayOfWeek();
            if(dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY)
            {
               addedDays++;
            }
         }
      }
      else
      {
         result = result.plusDays(delayDays);
      }

      return (result.toInstant());
   }



   /***************************************************************************
    ** Resolve a timezone string to a ZoneId. Falls back to UTC.
    ***************************************************************************/
   private static ZoneId resolveZoneId(String timezone)
   {
      if(StringUtils.hasContent(timezone))
      {
         try
         {
            return (ZoneId.of(timezone));
         }
         catch(Exception e)
         {
            // fall through to UTC
         }
      }
      return (ZoneId.of("UTC"));
   }



   /***************************************************************************
    ** Handle a failure: increment failure count, mark as FAILED after max.
    ***************************************************************************/
   private void handleFailure(SequenceEnrollment enrollment, String message) throws QException
   {
      Integer newFailureCount = (enrollment.getFailureCount() != null ? enrollment.getFailureCount() : 0) + 1;

      QRecord updateRecord = new QRecord()
         .withValue("id", enrollment.getId())
         .withValue("failureCount", newFailureCount)
         .withValue("lastFailureMessage", message);

      if(newFailureCount >= MAX_FAILURES)
      {
         updateRecord.withValue("status", CrmEnrollmentStatus.FAILED.getPossibleValueId());
         updateRecord.withValue("nextStepDate", null);
         QAlerts.raise(new AlertSignal("crm.sequence.failing").withDedupKey("enrollment:" + enrollment.getId())
            .withTitle("Sequence enrollment failed three times"));
      }

      new UpdateAction().execute(
         new UpdateInput(SequenceEnrollment.TABLE_NAME).withRecord(updateRecord));
   }

}
