/*******************************************************************************
 ** Tests for AuditLog entity -- validates insert succeeds and that update/delete
 ** are blocked by the immutability customizers.
 *******************************************************************************/
package com.kingsrook.qbits.crm.audit;


import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Tests for the AuditLog entity and its immutability customizers.
 *******************************************************************************/
class AuditLogTest extends BaseTest
{

   /*******************************************************************************
    ** Verify that audit log records can be inserted successfully.
    *******************************************************************************/
   @Test
   void testInsertAuditLog() throws Exception
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(AuditLog.TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("entityType", CrmEntityType.CONTACT.getId())
         .withValue("entityId", 42)
         .withValue("action", CrmAuditAction.CREATED.getId())
         .withValue("userId", "testUser")
         .withValue("message", "Contact created")));

      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      assertThat(insertOutput.getRecords()).hasSize(1);
      QRecord inserted = insertOutput.getRecords().get(0);
      assertThat(inserted.getValue("id")).isNotNull();
      assertThat(inserted.getValueInteger("entityId")).isEqualTo(42);
      assertThat(inserted.getValueInteger("action")).isEqualTo(CrmAuditAction.CREATED.getId());
      assertThat(inserted.getValueString("userId")).isEqualTo("testUser");
      assertThat(inserted.getValueString("message")).isEqualTo("Contact created");
   }



   /*******************************************************************************
    ** Verify that updating an audit log record throws QUserFacingException.
    *******************************************************************************/
   @Test
   void testUpdateAuditLogThrows() throws Exception
   {
      /////////////////////////////
      // insert a record first   //
      /////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(AuditLog.TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("entityType", CrmEntityType.CONTACT.getId())
         .withValue("entityId", 1)
         .withValue("action", CrmAuditAction.CREATED.getId())
         .withValue("userId", "testUser")));

      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer      id           = insertOutput.getRecords().get(0).getValueInteger("id");

      /////////////////////////////
      // attempt to update it    //
      /////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(AuditLog.TABLE_NAME);
      updateInput.setRecords(List.of(new QRecord()
         .withValue("id", id)
         .withValue("message", "This should fail")));

      assertThatThrownBy(() -> new UpdateAction().execute(updateInput))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Audit log records cannot be modified or deleted");
   }



   /*******************************************************************************
    ** Verify that deleting an audit log record throws QUserFacingException.
    *******************************************************************************/
   @Test
   void testDeleteAuditLogThrows() throws Exception
   {
      /////////////////////////////
      // insert a record first   //
      /////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(AuditLog.TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord()
         .withValue("entityType", CrmEntityType.COMPANY.getId())
         .withValue("entityId", 99)
         .withValue("action", CrmAuditAction.DELETED.getId())
         .withValue("userId", "testUser")));

      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer      id           = insertOutput.getRecords().get(0).getValueInteger("id");

      /////////////////////////////
      // attempt to delete it    //
      /////////////////////////////
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(AuditLog.TABLE_NAME);
      deleteInput.setPrimaryKeys(List.of(id));

      assertThatThrownBy(() -> new DeleteAction().execute(deleteInput))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Audit log records cannot be modified or deleted");
   }

}
