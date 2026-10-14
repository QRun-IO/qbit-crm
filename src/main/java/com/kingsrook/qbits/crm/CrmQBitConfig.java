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
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** QBitConfig for the CRM QBit. Holds backend name, security configuration,
 ** table metadata customizer and backend column naming.
 *******************************************************************************/
public class CrmQBitConfig implements QBitConfig
{
   public static final CrmQBitConfig DEFAULT_CONFIG = new CrmQBitConfig();

   private String tenantSecurityKeyType;
   private String tenantAllAccessKeyName;
   private String apiName;
   private String apiVersion;
   private String esbDestinationName;

   private String schedulerName;
   private Boolean schedulesEnabled = true;

   private String messagingProviderName;

   private String publicBaseUrl;

   private String tokenSecret;

   private String                                      defaultBackendNameForTables;
   private Boolean                                     inferBackendNames = true;
   private Boolean                                     appEnabled = true;
   private MetaDataCustomizerInterface<QTableMetaData>  tableMetaDataCustomizer;



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


   /*******************************************************************************
    ** Whether to publish the standalone CRM app.
    *******************************************************************************/
   public Boolean getAppEnabled()
   {
      return (appEnabled);
   }


   /*******************************************************************************
    ** Allow a host to put CRM tables in its own app.
    *******************************************************************************/
   public CrmQBitConfig withAppEnabled(Boolean appEnabled)
   {
      this.appEnabled = appEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tokenSecret
    *******************************************************************************/
   public String getTokenSecret()
   {
      return (tokenSecret);
   }


   /*******************************************************************************
    ** Configure tokenSecret
    *******************************************************************************/
   public CrmQBitConfig withTokenSecret(String tokenSecret)
   {
      this.tokenSecret = tokenSecret;
      return (this);
   }


   /*******************************************************************************
    ** Getter for publicBaseUrl
    *******************************************************************************/
   public String getPublicBaseUrl()
   {
      return (publicBaseUrl);
   }


   /*******************************************************************************
    ** Configure publicBaseUrl
    *******************************************************************************/
   public CrmQBitConfig withPublicBaseUrl(String publicBaseUrl)
   {
      this.publicBaseUrl = publicBaseUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for messagingProviderName
    *******************************************************************************/
   public String getMessagingProviderName()
   {
      return (messagingProviderName);
   }


   /*******************************************************************************
    ** Configure messagingProviderName
    *******************************************************************************/
   public CrmQBitConfig withMessagingProviderName(String messagingProviderName)
   {
      this.messagingProviderName = messagingProviderName;
      return (this);
   }

   /*******************************************************************************
    ** Scheduler configuration: schedulerName.
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (schedulerName);
   }

   /*******************************************************************************
    ** Configure schedulerName.
    *******************************************************************************/
   public CrmQBitConfig withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }

   /*******************************************************************************
    ** Scheduler configuration: schedulesEnabled.
    *******************************************************************************/
   public Boolean getSchedulesEnabled()
   {
      return (schedulesEnabled);
   }

   /*******************************************************************************
    ** Configure schedulesEnabled.
    *******************************************************************************/
   public CrmQBitConfig withSchedulesEnabled(Boolean schedulesEnabled)
   {
      this.schedulesEnabled = schedulesEnabled;
      return (this);
   }

   /*******************************************************************************
    ** Getter for tenantSecurityKeyType.
    *******************************************************************************/
   public String getTenantSecurityKeyType()
   {
      return (tenantSecurityKeyType);
   }


   /*******************************************************************************
    ** Configure tenantSecurityKeyType; null disables the optional integration.
    *******************************************************************************/
   public CrmQBitConfig withTenantSecurityKeyType(String tenantSecurityKeyType)
   {
      this.tenantSecurityKeyType = tenantSecurityKeyType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tenantAllAccessKeyName.
    *******************************************************************************/
   public String getTenantAllAccessKeyName()
   {
      return (tenantAllAccessKeyName);
   }


   /*******************************************************************************
    ** Configure tenantAllAccessKeyName; null disables the optional integration.
    *******************************************************************************/
   public CrmQBitConfig withTenantAllAccessKeyName(String tenantAllAccessKeyName)
   {
      this.tenantAllAccessKeyName = tenantAllAccessKeyName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for apiName.
    *******************************************************************************/
   public String getApiName()
   {
      return (apiName);
   }


   /*******************************************************************************
    ** Configure apiName; null disables the optional integration.
    *******************************************************************************/
   public CrmQBitConfig withApiName(String apiName)
   {
      this.apiName = apiName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for apiVersion.
    *******************************************************************************/
   public String getApiVersion()
   {
      return (apiVersion);
   }


   /*******************************************************************************
    ** Configure apiVersion; null disables the optional integration.
    *******************************************************************************/
   public CrmQBitConfig withApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
      return (this);
   }


   /*******************************************************************************
    ** Getter for esbDestinationName.
    *******************************************************************************/
   public String getEsbDestinationName()
   {
      return (esbDestinationName);
   }


   /*******************************************************************************
    ** Configure esbDestinationName; null disables the optional integration.
    *******************************************************************************/
   public CrmQBitConfig withEsbDestinationName(String esbDestinationName)
   {
      this.esbDestinationName = esbDestinationName;
      return (this);
   }


}
