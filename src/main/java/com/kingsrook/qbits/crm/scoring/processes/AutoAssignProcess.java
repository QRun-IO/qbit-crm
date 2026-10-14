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
 ** Process to auto-assign a contact or deal based on assignment rules.
 *******************************************************************************/

package com.kingsrook.qbits.crm.scoring.processes;


import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.scoring.CrmAutoAssign;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to auto-assign a contact or deal to a user
 ** based on configured assignment rules. Evaluates rules in sortOrder; first
 ** match wins. Supports SPECIFIC_USER, ROUND_ROBIN, and LEAST_ACTIVE strategies.
 *******************************************************************************/
public class AutoAssignProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "crm_auto_assign";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Auto Assign")
         .withIcon(new QIcon().withName("assignment_ind"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(AutoAssignProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("entityType", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("recordId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the assignment logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String  entityTypeStr = input.getValueString("entityType");
      Integer recordId      = input.getValueInteger("recordId");

      ///////////////////////////////////////////////
      // resolve entity type                       //
      ///////////////////////////////////////////////
      CrmAssignableEntity entityType = null;
      for(CrmAssignableEntity e : CrmAssignableEntity.values())
      {
         if(e.getLabel().equalsIgnoreCase(entityTypeStr) || e.name().equalsIgnoreCase(entityTypeStr))
         {
            entityType = e;
            break;
         }
      }

      if(entityType == null)
      {
         output.addValue("action", "NO_MATCH");
         return;
      }

      CrmAutoAssign.Result result = CrmAutoAssign.assign(entityType, recordId);
      output.addValue("action", result.action());
      if(result.assignedUserId() != null)
      {
         output.addValue("assignedUserId", result.assignedUserId());
         output.addValue("ruleId", result.ruleId());
      }
   }
}
