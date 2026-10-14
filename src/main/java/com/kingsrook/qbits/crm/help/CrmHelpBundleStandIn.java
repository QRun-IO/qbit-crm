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

package com.kingsrook.qbits.crm.help;


import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qqq#1198, removeBy=4.1.0-RC.9)
 ** Load the future bundle format with core's Jackson YAML parser. RC.6 lacks
 ** process/PVS help slots: action content is applied to frontend steps instead;
 ** enum entries remain in the bundle for the central loader.
 *******************************************************************************/
public final class CrmHelpBundleStandIn
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmHelpBundleStandIn()
   {
   }


   /*******************************************************************************
    ** Validate the resource eagerly so malformed shipped help fails production.
    *******************************************************************************/
   public static Map<String, JsonNode> entries() throws QException
   {
      try(InputStream stream = CrmHelpBundleStandIn.class.getResourceAsStream("/help/qbit-crm.yaml"))
      {
         JsonNode bundle = new ObjectMapper(new YAMLFactory()).readTree(stream);
         if(!"qbit-crm".equals(bundle.path("bundle").asText()) || bundle.path("version").asInt() != 1
            || !"QBIT".equals(bundle.path("layer").asText()))
         {
            throw (new IOException("Invalid CRM bundle header"));
         }
         Map<String, JsonNode> entries = new LinkedHashMap<>();
         for(JsonNode entry : bundle.path("entries"))
         {
            String key = entry.path("key").asText();
            if(key.isBlank() || entry.path("summary").asText().isBlank() || entry.path("summary").asText().length() > 120
               || entry.path("content").asText().isBlank() || entries.put(key, entry) != null)
            {
               throw (new IOException("Invalid or duplicate help entry: " + key));
            }
         }
         return (entries);
      }
      catch(IOException | IllegalArgumentException e)
      {
         throw (new QException("Could not load help/qbit-crm.yaml", e));
      }
   }


   /*******************************************************************************
    ** Apply help to the objects produced by this QBit only.
    *******************************************************************************/
   public static void apply(MetaDataProducerMultiOutput output) throws QException
   {
      Map<String, JsonNode> entries = entries();
      for(QTableMetaData table : output.getEach(QTableMetaData.class))
      {
         table.withHelpContent("help", content(entries, "table:" + table.getName() + ";slot:help"));
         for(var field : table.getFields().values())
         {
            String key = "table:" + table.getName() + ";field:" + field.getName();
            if(entries.containsKey(key))
            {
               field.withHelpContent(content(entries, key));
            }
         }
      }
      for(QProcessMetaData process : output.getEach(QProcessMetaData.class))
      {
         QHelpContent help = content(entries, "process:" + process.getName() + ";slot:help");
         for(var step : process.getStepList())
         {
            if(step instanceof QFrontendStepMetaData frontend)
            {
               frontend.withHelpContent(help.clone());
            }
         }
      }
      for(QAppMetaData app : output.getEach(QAppMetaData.class))
      {
         app.withHelpContent("help", content(entries, "app:" + app.getName() + ";slot:help"));
      }
   }


   /*******************************************************************************
    ** RC.6 has no summary property, so retain the summary in the bundle only.
    *******************************************************************************/
   private static QHelpContent content(Map<String, JsonNode> entries, String key) throws QException
   {
      if(!entries.containsKey(key))
      {
         throw (new QException("Missing CRM help entry: " + key));
      }
      return (new QHelpContent().withContentAsMarkdown(entries.get(key).get("content").asText()));
   }
}
