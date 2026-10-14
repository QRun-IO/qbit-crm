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

package com.kingsrook.qbits.crm.scoring;


import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostInsertCustomizer;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Apply assignment rules to new contacts and deals without replacing explicit owners.
 *******************************************************************************/
public class AutoAssignCustomizer extends AbstractPostInsertCustomizer
{
   @Override
   public List<QRecord> apply(List<QRecord> records) throws QException
   {
      for(QRecord row : records)
      {
         if(row.getErrors() != null && !row.getErrors().isEmpty())
         {
            continue;
         }
         String owner = row.getValueString("ownerUserId");
         if(owner == null || "system".equals(owner))
         {
            CrmAutoAssign.Result result = CrmAutoAssign.assign("crm_contact".equals(insertInput.getTableName())
               ? CrmAssignableEntity.CONTACT : CrmAssignableEntity.DEAL, row.getValueInteger("id"));
            if("ASSIGNED".equals(result.action()))
            {
               row.setValue("ownerUserId", result.assignedUserId());
               QAlerts.notify(new NotificationSignal("CRM lead assigned").withSourceName("crm.lead.assigned")
                  .withSourceKey(insertInput.getTableName() + ":" + row.getValue("id"))
                  .withSourceTableName(insertInput.getTableName()).withSourceRecordId(row.getValueString("id"))
                  .withAudience(NotificationAudience.user(result.assignedUserId())), insertInput.getTransaction());
            }
         }
      }
      return (records);
   }
}
