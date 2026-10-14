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

package com.kingsrook.qbits.crm;


import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.security.CrmDataPolicy;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Optional host API publication and the core record-search contract.
 *******************************************************************************/
public final class CrmIntegrations
{
   private static final Map<String, List<String>> SEARCH_FIELDS = Map.of(
      "crm_contact", List.of("firstName", "lastName", "email"),
      "crm_company", List.of("name", "domain"), "crm_deal", List.of("name"),
      "crm_activity", List.of("subject"), "crm_form_submission", List.of("email"), "crm_email_list", List.of("name"));


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmIntegrations()
   {
   }


   /*******************************************************************************
    ** Enable publication only when the host supplies both API settings.
    *******************************************************************************/
   public static void apply(MetaDataProducerMultiOutput output, CrmQBitConfig config) throws QException
   {
      if(config.getEsbDestinationName() != null)
      {
         throw (new QException("CRM ESB publication requires classified-value exclusion, which QQQ 4.1.0-RC.6 does not provide."));
      }
      boolean api = config.getApiName() != null && config.getApiVersion() != null;
      if(api)
      {
         try
         {
            Class.forName("com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer");
         }
         catch(ClassNotFoundException | LinkageError e)
         {
            throw (new QException("CRM API publication requires qqq-middleware-api on the host classpath.", e));
         }
      }
      for(QTableMetaData table : output.getEach(QTableMetaData.class))
      {
         if(SEARCH_FIELDS.containsKey(table.getName()))
         {
            table.withSearchFields(SEARCH_FIELDS.get(table.getName()));
         }
         if(api && CrmDataPolicy.isPublishedTable(table.getName()))
         {
            ApiTableMetaDataContainer.ofOrWithNew(table).withApiTableMetaData(config.getApiName(),
               new ApiTableMetaData().withInitialVersion(config.getApiVersion()).withIsExcluded(false));
         }
      }
   }
}
