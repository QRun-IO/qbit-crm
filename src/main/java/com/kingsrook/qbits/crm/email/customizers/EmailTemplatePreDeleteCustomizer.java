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
 ** PRE_DELETE customizer for EmailTemplate -- prevents deletion of templates
 ** referenced by active email sequences.
 *******************************************************************************/

package com.kingsrook.qbits.crm.email.customizers;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.model.SequenceEnrollment;
import com.kingsrook.qbits.crm.email.model.SequenceStep;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;


/*******************************************************************************
 ** Prevents deletion of email templates that are referenced by sequence steps
 ** belonging to sequences with active enrollments.
 *******************************************************************************/
public class EmailTemplatePreDeleteCustomizer extends AbstractPreDeleteCustomizer
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records) throws QException
   {
      for(QRecord record : records)
      {
         Integer templateId = record.getValueInteger("id");

         ///////////////////////////////////////////////////////////////////////
         // find sequence steps that reference this template                  //
         ///////////////////////////////////////////////////////////////////////
         QueryOutput stepOutput = new QueryAction().execute(
            new QueryInput(SequenceStep.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("emailTemplateId", QCriteriaOperator.EQUALS, templateId))));

         if(stepOutput.getRecords().isEmpty())
         {
            continue;
         }

         ///////////////////////////////////////////////////////////////////////
         // collect sequence IDs from matching steps                          //
         ///////////////////////////////////////////////////////////////////////
         Set<Integer> sequenceIds = new HashSet<>();
         for(QRecord step : stepOutput.getRecords())
         {
            sequenceIds.add(step.getValueInteger("sequenceId"));
         }

         ///////////////////////////////////////////////////////////////////////
         // check if any of those sequences have active enrollments           //
         ///////////////////////////////////////////////////////////////////////
         QueryOutput enrollmentOutput = new QueryAction().execute(
            new QueryInput(SequenceEnrollment.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("sequenceId", QCriteriaOperator.IN, sequenceIds))
                  .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getPossibleValueId()))));

         if(!enrollmentOutput.getRecords().isEmpty())
         {
            record.addError(new BadInputStatusMessage("Cannot delete template referenced by active email sequences"));
         }
      }

      return (records);
   }

}
