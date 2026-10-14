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
 ** Configuration for the CRM QBit.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** QBitConfig for the CRM QBit. Holds backend name, security configuration,
 ** table metadata customizer and backend column naming.
 *******************************************************************************/
public class CrmQBitConfig implements QBitConfig
{
   public static final CrmQBitConfig DEFAULT_CONFIG = new CrmQBitConfig();

   private String                                      defaultBackendNameForTables;
   private Boolean                                     inferBackendNames = true;
   private MetaDataCustomizerInterface<QTableMetaData>  tableMetaDataCustomizer;
   private List<QFieldMetaData>                         securityFields;
   private List<RecordSecurityLock>                     recordSecurityLocks;



   /*******************************************************************************
    ** Validate configuration.
    *******************************************************************************/
   @Override
   public void validate(com.kingsrook.qqq.backend.core.model.metadata.QInstance qInstance, List<String> errors)
   {
      assertCondition(StringUtils.hasContent(defaultBackendNameForTables),
         "CrmQBitConfig.defaultBackendNameForTables is required", errors);
   }



   /*******************************************************************************
    ** Getter for defaultBackendNameForTables
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (this.defaultBackendNameForTables);
   }



   /*******************************************************************************
    ** Setter for defaultBackendNameForTables
    *******************************************************************************/
   public void setDefaultBackendNameForTables(String defaultBackendNameForTables)
   {
      this.defaultBackendNameForTables = defaultBackendNameForTables;
   }



   /*******************************************************************************
    ** Fluent setter for defaultBackendNameForTables
    *******************************************************************************/
   public CrmQBitConfig withDefaultBackendNameForTables(String defaultBackendNameForTables)
   {
      this.defaultBackendNameForTables = defaultBackendNameForTables;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableMetaDataCustomizer
    *******************************************************************************/
   @Override
   public MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (this.tableMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Setter for tableMetaDataCustomizer
    *******************************************************************************/
   public void setTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for tableMetaDataCustomizer
    *******************************************************************************/
   public CrmQBitConfig withTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for securityFields
    *******************************************************************************/
   public List<QFieldMetaData> getSecurityFields()
   {
      return (this.securityFields);
   }



   /*******************************************************************************
    ** Fluent setter for securityFields
    *******************************************************************************/
   public CrmQBitConfig withSecurityFields(List<QFieldMetaData> securityFields)
   {
      this.securityFields = securityFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordSecurityLocks
    *******************************************************************************/
   public List<RecordSecurityLock> getRecordSecurityLocks()
   {
      return (this.recordSecurityLocks);
   }



   /*******************************************************************************
    ** Fluent setter for recordSecurityLocks
    *******************************************************************************/
   public CrmQBitConfig withRecordSecurityLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      this.recordSecurityLocks = recordSecurityLocks;
      return (this);
   }



   /*******************************************************************************
    ** Whether to infer snake_case database column names.
    *******************************************************************************/
   public Boolean getInferBackendNames()
   {
      return (this.inferBackendNames);
   }



   /*******************************************************************************
    ** Configure automatic database column naming.
    *******************************************************************************/
   public CrmQBitConfig withInferBackendNames(Boolean inferBackendNames)
   {
      this.inferBackendNames = inferBackendNames;
      return (this);
   }

}
