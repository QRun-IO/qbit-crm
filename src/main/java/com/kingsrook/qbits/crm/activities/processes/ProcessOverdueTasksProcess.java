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
 ** Scheduled process that finds overdue tasks and marks them with a system
 ** note. In production this would fire notifications; for now it logs a
 ** message on each overdue activity.
 *******************************************************************************/

package com.kingsrook.qbits.crm.activities.processes;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
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
 ** MetaDataProducer + BackendStep for processing overdue tasks. Intended to
 ** run on an hourly schedule. Finds activities of type "Task" with dueDate
 ** in the past and isCompleted=false.
 *******************************************************************************/
public class ProcessOverdueTasksProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_process_overdue_tasks";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Process Overdue Tasks")
         .withIcon(new QIcon().withName("warning"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(ProcessOverdueTasksProcess.class))
         ));
   }



   /*******************************************************************************
    ** Execute: find overdue tasks and mark them with a system note.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      ///////////////////////////////////////////////
      // look up the "Task" activity type          //
      ///////////////////////////////////////////////
      Integer taskTypeId = lookupTaskActivityTypeId();
      if(taskTypeId == null)
      {
         output.addValue("processedCount", 0);
         return;
      }

      ///////////////////////////////////////////////
      // query overdue, incomplete tasks           //
      ///////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("activityTypeId", QCriteriaOperator.EQUALS, taskTypeId))
               .withCriteria(new QFilterCriteria("dueDate", QCriteriaOperator.LESS_THAN, Instant.now()))
               .withCriteria(new QFilterCriteria("isCompleted", QCriteriaOperator.EQUALS, false))));

      int processedCount = 0;
      for(QRecord record : queryOutput.getRecords())
      {
         /////////////////////////////////////////////////////////////////////
         // In production this would create a notification. For now, add a  //
         // system note to the description indicating it is overdue.        //
         /////////////////////////////////////////////////////////////////////
         String currentDescription = record.getValueString("description");
         String note = "[OVERDUE] Task past due date.";
         String newDescription = currentDescription != null ? currentDescription + " " + note : note;

         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue("description", newDescription);

         new UpdateAction().execute(
            new UpdateInput(Activity.TABLE_NAME).withRecord(updateRecord));

         processedCount++;
      }

      output.addValue("processedCount", processedCount);
   }



   /***************************************************************************
    ** Look up the "Task" activity type id. Returns null if not found.
    ***************************************************************************/
   private Integer lookupTaskActivityTypeId() throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityType.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Task"))));

      if(!queryOutput.getRecords().isEmpty())
      {
         return (queryOutput.getRecords().get(0).getValueInteger("id"));
      }

      return (null);
   }

}
