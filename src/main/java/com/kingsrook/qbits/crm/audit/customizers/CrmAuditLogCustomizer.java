/*******************************************************************************
 ** POST_INSERT, POST_UPDATE, and POST_DELETE table customizer that creates
 ** immutable audit log entries for tracked CRM entities.
 **
 ** Registered globally on tracked tables (contact, company, deal, activity)
 ** via MultiCustomizer in CrmQBitProducer.postProduceActions.
 *******************************************************************************/
package com.kingsrook.qbits.crm.audit.customizers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Audit log customizer that creates crm_audit_log entries for every insert,
 ** update, and delete on tracked entities. For updates, field-level change
 ** tracking captures old and new values, with special detection for owner
 ** and pipeline stage changes.
 *******************************************************************************/
public class CrmAuditLogCustomizer implements TableCustomizerInterface
{
   private static final QLogger LOG = QLogger.getLogger(CrmAuditLogCustomizer.class);

   ///////////////////////////////////////////////////////////////////////////
   // map of table names to CrmEntityType IDs for tracked entities         //
   ///////////////////////////////////////////////////////////////////////////
   private static final Map<String, Integer> TABLE_TO_ENTITY_TYPE = new HashMap<>();

   static
   {
      TABLE_TO_ENTITY_TYPE.put("crmContact", CrmEntityType.CONTACT.getPossibleValueId());
      TABLE_TO_ENTITY_TYPE.put("crmCompany", CrmEntityType.COMPANY.getPossibleValueId());
      TABLE_TO_ENTITY_TYPE.put("crmDeal", CrmEntityType.DEAL.getPossibleValueId());
      TABLE_TO_ENTITY_TYPE.put("crmActivity", CrmEntityType.ACTIVITY.getPossibleValueId());
   }



   /*******************************************************************************
    ** Post-insert: log a CREATED action for each inserted record.
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      String tableName = insertInput.getTableName();
      String userId    = getCurrentUserId();

      List<QRecord> auditRecords = new ArrayList<>();
      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Integer entityId = ValueUtils.getValueAsInteger(record.getValue("id"));
         if(entityId == null)
         {
            continue;
         }

         auditRecords.add(buildAuditRecord(tableName, entityId, CrmAuditAction.CREATED, userId,
            null, null, null, "Record created"));
      }

      insertAuditRecords(auditRecords);
      return (records);
   }



   /*******************************************************************************
    ** Post-update: log an UPDATED action per changed field, with special
    ** detection for OWNER_CHANGED and STAGE_CHANGED actions.
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      String tableName = updateInput.getTableName();
      String userId    = getCurrentUserId();

      Optional<Map<Serializable, QRecord>> oldRecordMap = oldRecordListToMap(
         updateInput.getTable().getPrimaryKeyField(), oldRecordList);

      if(oldRecordMap.isEmpty())
      {
         ///////////////////////////////////////////////////////////////////////////
         // without old records, we cannot determine what changed -- skip logging //
         ///////////////////////////////////////////////////////////////////////////
         return (records);
      }

      List<QRecord> auditRecords = new ArrayList<>();
      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Serializable primaryKey = record.getValue(updateInput.getTable().getPrimaryKeyField());
         Integer      entityId  = ValueUtils.getValueAsInteger(primaryKey);
         QRecord      oldRecord = oldRecordMap.get().get(primaryKey);

         if(entityId == null || oldRecord == null)
         {
            continue;
         }

         for(String fieldName : record.getValues().keySet())
         {
            Serializable oldVal = oldRecord.getValue(fieldName);
            Serializable newVal = record.getValue(fieldName);

            if(!Objects.equals(oldVal, newVal))
            {
               String oldStr = oldVal == null ? null : String.valueOf(oldVal);
               String newStr = newVal == null ? null : String.valueOf(newVal);

               /////////////////////////////////////////////////////////////////
               // determine the specific action for special tracked fields    //
               /////////////////////////////////////////////////////////////////
               CrmAuditAction auditAction = CrmAuditAction.UPDATED;
               if("ownerUserId".equals(fieldName))
               {
                  auditAction = CrmAuditAction.OWNER_CHANGED;
               }
               else if("pipelineStageId".equals(fieldName))
               {
                  auditAction = CrmAuditAction.STAGE_CHANGED;
               }

               auditRecords.add(buildAuditRecord(tableName, entityId, auditAction, userId,
                  fieldName, oldStr, newStr, null));
            }
         }
      }

      insertAuditRecords(auditRecords);
      return (records);
   }



   /*******************************************************************************
    ** Post-delete: log a DELETED action for each deleted record.
    *******************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      String tableName = deleteInput.getTableName();
      String userId    = getCurrentUserId();

      List<QRecord> auditRecords = new ArrayList<>();
      for(QRecord record : records)
      {
         if(record.getErrors() != null && !record.getErrors().isEmpty())
         {
            continue;
         }

         Integer entityId = ValueUtils.getValueAsInteger(record.getValue("id"));
         if(entityId == null)
         {
            continue;
         }

         auditRecords.add(buildAuditRecord(tableName, entityId, CrmAuditAction.DELETED, userId,
            null, null, null, "Record deleted"));
      }

      insertAuditRecords(auditRecords);
      return (records);
   }



   /*******************************************************************************
    ** Build a single audit log QRecord.
    *******************************************************************************/
   private QRecord buildAuditRecord(String tableName, Integer entityId, CrmAuditAction action,
                                    String userId, String fieldName, String oldValue,
                                    String newValue, String message)
   {
      QRecord auditRecord = new QRecord();
      auditRecord.setValue("entityType", resolveEntityType(tableName));
      auditRecord.setValue("entityId", entityId);
      auditRecord.setValue("action", action.getPossibleValueId());
      auditRecord.setValue("userId", userId);

      if(StringUtils.hasContent(fieldName))
      {
         auditRecord.setValue("fieldName", fieldName);
      }

      if(oldValue != null)
      {
         auditRecord.setValue("oldValue", oldValue);
      }

      if(newValue != null)
      {
         auditRecord.setValue("newValue", newValue);
      }

      if(StringUtils.hasContent(message))
      {
         auditRecord.setValue("message", message);
      }

      return (auditRecord);
   }



   /*******************************************************************************
    ** Resolve a table name to its CrmEntityType integer ID.
    ** Returns the mapped ID if known, or falls back to the table name string
    ** for unrecognized tables (allowing forward-compatibility).
    *******************************************************************************/
   private Object resolveEntityType(String tableName)
   {
      Integer entityTypeId = TABLE_TO_ENTITY_TYPE.get(tableName);
      if(entityTypeId != null)
      {
         return (entityTypeId);
      }

      ///////////////////////////////////////////////////////////////////////////
      // fallback: use the table name directly for unrecognized entity types   //
      ///////////////////////////////////////////////////////////////////////////
      return (tableName);
   }



   /*******************************************************************************
    ** Insert a batch of audit log records using InsertAction.
    *******************************************************************************/
   private void insertAuditRecords(List<QRecord> auditRecords) throws QException
   {
      if(auditRecords.isEmpty())
      {
         return;
      }

      try
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(AuditLog.TABLE_NAME);
         insertInput.setRecords(auditRecords);
         new InsertAction().execute(insertInput);
      }
      catch(Exception e)
      {
         LOG.warn("Error inserting audit log records", e);
      }
   }



   /*******************************************************************************
    ** Get the current user ID from the QContext session.
    *******************************************************************************/
   private String getCurrentUserId()
   {
      return (CrmSessionUtils.getCurrentUserId());
   }

}
