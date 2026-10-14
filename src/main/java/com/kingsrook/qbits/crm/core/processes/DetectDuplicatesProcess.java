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
 ** Process to detect duplicate contacts (by email) or companies (by domain).
 ** Returns groups where count > 1.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.processes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Backend operation used by the streamed CRM actions for detecting duplicate contacts or companies.
 *******************************************************************************/
public class DetectDuplicatesProcess implements BackendStep
{
   public static final String NAME = "crm_detect_duplicates";



   /*******************************************************************************
    ** Execute the detect duplicates process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String entityType = input.getValueString("entityType");

      if("CONTACT".equalsIgnoreCase(entityType))
      {
         detectContactDuplicates(output);
      }
      else if("COMPANY".equalsIgnoreCase(entityType))
      {
         detectCompanyDuplicates(output);
      }
      else
      {
         throw new QException("Unknown entity type: " + entityType + ". Must be CONTACT or COMPANY.");
      }
   }



   /***************************************************************************
    ** Detect contact duplicates by email.
    ***************************************************************************/
   private void detectContactDuplicates(RunBackendStepOutput output) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));

      Map<String, List<Integer>> emailToIds = new HashMap<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String email = record.getValueString("email");
         if(StringUtils.hasContent(email))
         {
            emailToIds.computeIfAbsent(email.toLowerCase(), k -> new ArrayList<>())
               .add(record.getValueInteger("id"));
         }
      }

      List<QRecord> duplicateGroups = new ArrayList<>();
      for(Map.Entry<String, List<Integer>> entry : emailToIds.entrySet())
      {
         if(entry.getValue().size() > 1)
         {
            duplicateGroups.add(new QRecord()
               .withValue("key", entry.getKey())
               .withValue("ids", entry.getValue().toString())
               .withValue("count", entry.getValue().size()));
         }
      }

      output.setRecords(duplicateGroups);
      output.addValue("duplicateGroupCount", duplicateGroups.size());
   }



   /***************************************************************************
    ** Detect company duplicates by domain.
    ***************************************************************************/
   private void detectCompanyDuplicates(RunBackendStepOutput output) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Company.TABLE_NAME));

      Map<String, List<Integer>> domainToIds = new HashMap<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String domain = record.getValueString("domain");
         if(StringUtils.hasContent(domain))
         {
            domainToIds.computeIfAbsent(domain.toLowerCase(), k -> new ArrayList<>())
               .add(record.getValueInteger("id"));
         }
      }

      List<QRecord> duplicateGroups = new ArrayList<>();
      for(Map.Entry<String, List<Integer>> entry : domainToIds.entrySet())
      {
         if(entry.getValue().size() > 1)
         {
            duplicateGroups.add(new QRecord()
               .withValue("key", entry.getKey())
               .withValue("ids", entry.getValue().toString())
               .withValue("count", entry.getValue().size()));
         }
      }

      output.setRecords(duplicateGroups);
      output.addValue("duplicateGroupCount", duplicateGroups.size());
   }

}
