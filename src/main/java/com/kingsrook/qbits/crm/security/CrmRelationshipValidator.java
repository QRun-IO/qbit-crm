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

package com.kingsrook.qbits.crm.security;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;


/*******************************************************************************
 ** CRM relationship invariants, enforced for inserts and partial updates.
 *******************************************************************************/
public class CrmRelationshipValidator implements TableCustomizerInterface
{
   @Override
   public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview,
      Optional<List<QRecord>> oldRecords) throws QException
   {
      Map<Object, QRecord> oldById = new HashMap<>();
      oldRecords.orElse(List.of()).forEach(row -> oldById.put(row.getValue("id"), row));
      for(QRecord record : records)
      {
         QRecord effective = new QRecord();
         QRecord old = oldById.get(record.getValue("id"));
         if(old != null)
         {
            effective.getValues().putAll(old.getValues());
         }
         effective.getValues().putAll(record.getValues());
         if(effective.getValues().containsKey("pipelineStageId"))
         {
            validate(record, effective, "pipelineStageId", "pipelineId", "crm_pipeline_stage");
         }
         if(effective.getValues().containsKey("outcomeId"))
         {
            validate(record, effective, "outcomeId", "activityTypeId", "crm_activity_outcome");
         }
      }
      return (records);
   }


   /*******************************************************************************
    ** Missing optional choices are allowed; populated choices must belong to the parent.
    *******************************************************************************/
   private void validate(QRecord record, QRecord effective, String childField, String parentField, String source) throws QException
   {
      if(effective.getValue(childField) == null)
      {
         return;
      }
      QRecord choice = GetAction.execute(source, effective.getValue(childField));
      if(choice == null || effective.getValue(parentField) == null
         || !Objects.equals(choice.getValueInteger(parentField), effective.getValueInteger(parentField)))
      {
         record.addError(new BadInputStatusMessage("Choose a value belonging to the selected " + parentField + "."));
      }
   }
}
