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

package com.kingsrook.qbits.crm.api;


import java.util.List;
import java.util.Locale;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Idempotent activity-code initialization through normal table actions, never producer side effects.
 *******************************************************************************/
public final class CrmStarterData
{
   /*******************************************************************************
    ** No instances.
    *******************************************************************************/
   private CrmStarterData()
   {
   }

   /*******************************************************************************
    ** Find or insert each supported activity code in the current instance.
    *******************************************************************************/
   public static void ensure() throws QException
   {
      for(ActivityKind kind : ActivityKind.values())
      {
         if(!CrmStore.by("crm_activity_type", "code", kind.name()).isEmpty())
         {
            continue;
         }
         String label = kind.name().substring(0, 1) + kind.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
         List<QRecord> named = CrmStore.by("crm_activity_type", "name", label);
         if(!named.isEmpty() && named.get(0).getValue("code") == null)
         {
            CrmStore.update("crm_activity_type", new QRecord().withValue("id", named.get(0).getValue("id")).withValue("code", kind.name()));
         }
         else
         {
            try
            {
               CrmStore.insert("crm_activity_type", new QRecord().withValue("name", label).withValue("code", kind.name())
                  .withValue("sortOrder", kind.ordinal()).withValue("isSystem", true));
            }
            catch(QException e)
            {
               if(CrmStore.by("crm_activity_type", "code", kind.name()).isEmpty())
               {
                  throw e;
               }
            }
         }
      }
   }
}
