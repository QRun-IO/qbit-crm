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

package com.kingsrook.qbits.crm.bulkload;


import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;


/*******************************************************************************
 ** Starter mappings for the host's core bulk loader. The host owns its tables.
 *******************************************************************************/
public final class CrmBulkLoadProfiles
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmBulkLoadProfiles()
   {
   }


   /*******************************************************************************
    ** Call once during host startup, after core profile tables are installed.
    ** Existing profiles, including user edits, are preserved by label.
    *******************************************************************************/
   public static void ensure() throws QException
   {
      if(QContext.getQInstance().getTable(SavedBulkLoadProfile.TABLE_NAME) == null)
      {
         return;
      }
      try(InputStream stream = CrmBulkLoadProfiles.class.getResourceAsStream("/bulkload/crm-profiles.json"))
      {
         ObjectMapper mapper = new ObjectMapper();
         for(JsonNode profile : mapper.readTree(stream))
         {
            String label = profile.get("label").asText();
            if(!new QueryAction().execute(new QueryInput(SavedBulkLoadProfile.TABLE_NAME).withFilter(new QQueryFilter(
               new QFilterCriteria("label", QCriteriaOperator.EQUALS, label)).withLimit(1))).getRecords().isEmpty())
            {
               continue;
            }
            BulkLoadProfile mapping = mapper.treeToValue(profile.get("mapping"), BulkLoadProfile.class);
            CrmRecords.insert(SavedBulkLoadProfile.TABLE_NAME, new QRecord().withValue("label", label)
               .withValue("tableName", profile.get("tableName").asText()).withValue("isBulkEdit", false)
               .withValue("mappingJson", mapper.writeValueAsString(mapping)));
         }
      }
      catch(IOException | IllegalArgumentException e)
      {
         throw (new QException("Could not load CRM bulk-load profiles", e));
      }
   }
}
