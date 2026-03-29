/*******************************************************************************
 ** Tests for CrmAuditLogCustomizer -- validates audit log entry creation
 ** for insert, update, and delete operations on tracked entities.
 **
 ** Since Contact/Company entities may not yet be registered with full
 ** customizers, these tests exercise the customizer directly by calling its
 ** methods with constructed QRecords and verifying audit log entries are
 ** created in the crm_audit_log table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.audit;


import java.util.List;
import java.util.Optional;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.audit.customizers.CrmAuditLogCustomizer;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for CrmAuditLogCustomizer. Exercises the customizer directly by
 ** invoking postInsert, postUpdate, and postDelete with mock data, then
 ** verifying that audit log records were created in the memory backend.
 *******************************************************************************/
class CrmAuditLogCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Test that postInsert creates a CREATED audit log entry.
    *******************************************************************************/
   @Test
   void testPostInsertCreatesAuditEntry() throws Exception
   {
      CrmAuditLogCustomizer customizer = new CrmAuditLogCustomizer();

      //////////////////////////////////////////
      // simulate a record that was inserted  //
      //////////////////////////////////////////
      QRecord insertedRecord = new QRecord()
         .withValue("id", 1)
         .withValue("name", "Test Contact");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("crmContact");

      customizer.postInsert(insertInput, List.of(insertedRecord));

      //////////////////////////////////////////////
      // verify the audit log entry was created   //
      //////////////////////////////////////////////
      List<QRecord> auditRecords = queryAllAuditLogs();
      assertThat(auditRecords).hasSize(1);

      QRecord auditEntry = auditRecords.get(0);
      assertThat(auditEntry.getValueInteger("entityType")).isEqualTo(CrmEntityType.CONTACT.getId());
      assertThat(auditEntry.getValueInteger("entityId")).isEqualTo(1);
      assertThat(auditEntry.getValueInteger("action")).isEqualTo(CrmAuditAction.CREATED.getId());
      assertThat(auditEntry.getValueString("message")).isEqualTo("Record created");
   }



   /*******************************************************************************
    ** Test that postUpdate creates UPDATED entries for changed fields.
    ** Uses AuditLog.TABLE_NAME as the table so that getTable() resolves.
    *******************************************************************************/
   @Test
   void testPostUpdateCreatesAuditEntries() throws Exception
   {
      CrmAuditLogCustomizer customizer = new CrmAuditLogCustomizer();

      //////////////////////////////////////////
      // build old and new records            //
      //////////////////////////////////////////
      QRecord oldRecord = new QRecord()
         .withValue("id", 10)
         .withValue("message", "Old message")
         .withValue("userId", "sameUser");

      QRecord newRecord = new QRecord()
         .withValue("id", 10)
         .withValue("message", "New message")
         .withValue("userId", "sameUser");

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(AuditLog.TABLE_NAME);

      customizer.postUpdate(updateInput, List.of(newRecord), Optional.of(List.of(oldRecord)));

      //////////////////////////////////////////////
      // verify audit log entries were created    //
      //////////////////////////////////////////////
      List<QRecord> auditRecords = queryAllAuditLogs();

      /////////////////////////////////////////////////////////////////////
      // should have one entry for the "message" field that changed      //
      // "userId" did not change so it should not have an audit entry    //
      /////////////////////////////////////////////////////////////////////
      assertThat(auditRecords).hasSize(1);

      QRecord auditEntry = auditRecords.get(0);
      assertThat(auditEntry.getValueString("fieldName")).isEqualTo("message");
      assertThat(auditEntry.getValueString("oldValue")).isEqualTo("Old message");
      assertThat(auditEntry.getValueString("newValue")).isEqualTo("New message");
      assertThat(auditEntry.getValueInteger("action")).isEqualTo(CrmAuditAction.UPDATED.getId());
      assertThat(auditEntry.getValueInteger("entityId")).isEqualTo(10);
   }



   /*******************************************************************************
    ** Test that postUpdate detects OWNER_CHANGED for ownerUserId field changes.
    ** Uses AuditLog.TABLE_NAME as the table so that getTable() resolves.
    *******************************************************************************/
   @Test
   void testPostUpdateDetectsOwnerChange() throws Exception
   {
      CrmAuditLogCustomizer customizer = new CrmAuditLogCustomizer();

      QRecord oldRecord = new QRecord()
         .withValue("id", 20)
         .withValue("ownerUserId", "userA");

      QRecord newRecord = new QRecord()
         .withValue("id", 20)
         .withValue("ownerUserId", "userB");

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(AuditLog.TABLE_NAME);

      customizer.postUpdate(updateInput, List.of(newRecord), Optional.of(List.of(oldRecord)));

      List<QRecord> auditRecords = queryAllAuditLogs();
      assertThat(auditRecords).hasSize(1);
      assertThat(auditRecords.get(0).getValueInteger("action")).isEqualTo(CrmAuditAction.OWNER_CHANGED.getId());
      assertThat(auditRecords.get(0).getValueString("fieldName")).isEqualTo("ownerUserId");
   }



   /*******************************************************************************
    ** Test that postUpdate detects STAGE_CHANGED for pipelineStageId field changes.
    ** Uses AuditLog.TABLE_NAME as the table so that getTable() resolves.
    *******************************************************************************/
   @Test
   void testPostUpdateDetectsStageChange() throws Exception
   {
      CrmAuditLogCustomizer customizer = new CrmAuditLogCustomizer();

      QRecord oldRecord = new QRecord()
         .withValue("id", 30)
         .withValue("pipelineStageId", 1);

      QRecord newRecord = new QRecord()
         .withValue("id", 30)
         .withValue("pipelineStageId", 2);

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(AuditLog.TABLE_NAME);

      customizer.postUpdate(updateInput, List.of(newRecord), Optional.of(List.of(oldRecord)));

      List<QRecord> auditRecords = queryAllAuditLogs();
      assertThat(auditRecords).hasSize(1);
      assertThat(auditRecords.get(0).getValueInteger("action")).isEqualTo(CrmAuditAction.STAGE_CHANGED.getId());
      assertThat(auditRecords.get(0).getValueString("fieldName")).isEqualTo("pipelineStageId");
   }



   /*******************************************************************************
    ** Test that postDelete creates a DELETED audit log entry.
    *******************************************************************************/
   @Test
   void testPostDeleteCreatesAuditEntry() throws Exception
   {
      CrmAuditLogCustomizer customizer = new CrmAuditLogCustomizer();

      QRecord deletedRecord = new QRecord()
         .withValue("id", 50);

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName("crmCompany");

      customizer.postDelete(deleteInput, List.of(deletedRecord));

      List<QRecord> auditRecords = queryAllAuditLogs();
      assertThat(auditRecords).hasSize(1);

      QRecord auditEntry = auditRecords.get(0);
      assertThat(auditEntry.getValueInteger("entityType")).isEqualTo(CrmEntityType.COMPANY.getId());
      assertThat(auditEntry.getValueInteger("entityId")).isEqualTo(50);
      assertThat(auditEntry.getValueInteger("action")).isEqualTo(CrmAuditAction.DELETED.getId());
      assertThat(auditEntry.getValueString("message")).isEqualTo("Record deleted");
   }



   /*******************************************************************************
    ** Test that records with errors are skipped during audit logging.
    *******************************************************************************/
   @Test
   void testRecordsWithErrorsAreSkipped() throws Exception
   {
      CrmAuditLogCustomizer customizer = new CrmAuditLogCustomizer();

      QRecord goodRecord = new QRecord()
         .withValue("id", 1);

      QRecord badRecord = new QRecord()
         .withValue("id", 2);
      badRecord.addError(new com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage("test error"));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("crmDeal");

      customizer.postInsert(insertInput, List.of(goodRecord, badRecord));

      List<QRecord> auditRecords = queryAllAuditLogs();
      assertThat(auditRecords).hasSize(1);
      assertThat(auditRecords.get(0).getValueInteger("entityId")).isEqualTo(1);
   }



   /*******************************************************************************
    ** Helper to query all records from the audit log table.
    *******************************************************************************/
   private List<QRecord> queryAllAuditLogs() throws Exception
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(AuditLog.TABLE_NAME);
      queryInput.setFilter(new QQueryFilter());

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords());
   }

}
