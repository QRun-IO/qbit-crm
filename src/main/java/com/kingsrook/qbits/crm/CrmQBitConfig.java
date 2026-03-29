/*******************************************************************************
 ** Configuration for the CRM QBit.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** QBitConfig for the CRM QBit. Holds backend name, security configuration,
 ** table metadata customizer, and companion QBit namespace references.
 *******************************************************************************/
public class CrmQBitConfig implements QBitConfig
{
   public static final CrmQBitConfig DEFAULT_CONFIG = new CrmQBitConfig();

   private String                                      defaultBackendNameForTables;
   private String                                      tableNamePrefix;
   private MetaDataCustomizerInterface<QTableMetaData>  tableMetaDataCustomizer;
   private List<QFieldMetaData>                         securityFields;
   private List<RecordSecurityLock>                     recordSecurityLocks;

   private String quickSearchQBitNamespace;
   private String webhooksQBitNamespace;
   private String workflowsQBitNamespace;



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
    ** Getter for tableNamePrefix
    *******************************************************************************/
   public String getTableNamePrefix()
   {
      return (this.tableNamePrefix);
   }



   /*******************************************************************************
    ** Fluent setter for tableNamePrefix
    *******************************************************************************/
   public CrmQBitConfig withTableNamePrefix(String tableNamePrefix)
   {
      this.tableNamePrefix = tableNamePrefix;
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
    ** Getter for quickSearchQBitNamespace
    *******************************************************************************/
   public String getQuickSearchQBitNamespace()
   {
      return (this.quickSearchQBitNamespace);
   }



   /*******************************************************************************
    ** Fluent setter for quickSearchQBitNamespace
    *******************************************************************************/
   public CrmQBitConfig withQuickSearchQBitNamespace(String quickSearchQBitNamespace)
   {
      this.quickSearchQBitNamespace = quickSearchQBitNamespace;
      return (this);
   }



   /*******************************************************************************
    ** Getter for webhooksQBitNamespace
    *******************************************************************************/
   public String getWebhooksQBitNamespace()
   {
      return (this.webhooksQBitNamespace);
   }



   /*******************************************************************************
    ** Fluent setter for webhooksQBitNamespace
    *******************************************************************************/
   public CrmQBitConfig withWebhooksQBitNamespace(String webhooksQBitNamespace)
   {
      this.webhooksQBitNamespace = webhooksQBitNamespace;
      return (this);
   }



   /*******************************************************************************
    ** Getter for workflowsQBitNamespace
    *******************************************************************************/
   public String getWorkflowsQBitNamespace()
   {
      return (this.workflowsQBitNamespace);
   }



   /*******************************************************************************
    ** Fluent setter for workflowsQBitNamespace
    *******************************************************************************/
   public CrmQBitConfig withWorkflowsQBitNamespace(String workflowsQBitNamespace)
   {
      this.workflowsQBitNamespace = workflowsQBitNamespace;
      return (this);
   }

}
