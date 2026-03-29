/*******************************************************************************
 ** Unit tests for the ActivityType entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities;


import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for ActivityType CRUD operations and metadata registration.
 *******************************************************************************/
class ActivityTypeTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the activity type table is registered in the QInstance.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      assertNotNull(QContext.getQInstance().getTable(ActivityType.TABLE_NAME));
   }



   /*******************************************************************************
    ** Insert an activity type and query it back.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      ActivityType activityType = new ActivityType()
         .withName("Call")
         .withIconName("phone")
         .withIsSystem(true)
         .withSortOrder(1)
         .withIsActive(true);

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME).withRecordEntity(activityType));

      QRecord insertedRecord = insertOutput.getRecords().get(0);
      assertNotNull(insertedRecord.getValueInteger("id"));

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityType.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      ActivityType fetched = new ActivityType(queryOutput.getRecords().get(0));
      assertEquals("Call", fetched.getName());
      assertEquals("phone", fetched.getIconName());
      assertTrue(fetched.getIsSystem());
      assertEquals(1, fetched.getSortOrder());
      assertTrue(fetched.getIsActive());
   }



   /*******************************************************************************
    ** Insert multiple activity types and verify sort order and count.
    *******************************************************************************/
   @Test
   void testInsertMultiple() throws QException
   {
      InsertInput insertInput = new InsertInput(ActivityType.TABLE_NAME)
         .withRecordEntity(new ActivityType().withName("Call").withIconName("phone").withIsSystem(true).withSortOrder(1).withIsActive(true))
         .withRecordEntity(new ActivityType().withName("Email").withIconName("email").withIsSystem(true).withSortOrder(2).withIsActive(true))
         .withRecordEntity(new ActivityType().withName("Meeting").withIconName("event").withIsSystem(true).withSortOrder(3).withIsActive(true))
         .withRecordEntity(new ActivityType().withName("Note").withIconName("note").withIsSystem(false).withSortOrder(4).withIsActive(true))
         .withRecordEntity(new ActivityType().withName("Task").withIconName("task_alt").withIsSystem(false).withSortOrder(5).withIsActive(false));

      new InsertAction().execute(insertInput);

      QueryOutput queryOutput = new QueryAction().execute(new QueryInput(ActivityType.TABLE_NAME));
      assertEquals(5, queryOutput.getRecords().size());

      ////////////////////////////////////////////////////
      // verify inactive type was stored correctly       //
      ////////////////////////////////////////////////////
      ActivityType task = new ActivityType(queryOutput.getRecords().stream()
         .filter(r -> "Task".equals(r.getValueString("name")))
         .findFirst()
         .orElseThrow());
      assertFalse(task.getIsActive());
   }



   /*******************************************************************************
    ** Verify that isSystem flag data is preserved (the actual PRE_DELETE
    ** customizer will be wired later; this test confirms the data contract).
    *******************************************************************************/
   @Test
   void testIsSystemFlagPreserved() throws QException
   {
      /////////////////////////////////////
      // insert a system activity type   //
      /////////////////////////////////////
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName("Call")
               .withIconName("phone")
               .withIsSystem(true)
               .withSortOrder(1)
               .withIsActive(true)));

      Integer systemId = insertOutput.getRecords().get(0).getValueInteger("id");

      ////////////////////////////////////////
      // insert a non-system activity type  //
      ////////////////////////////////////////
      InsertOutput insertOutput2 = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName("Custom")
               .withIconName("star")
               .withIsSystem(false)
               .withSortOrder(10)
               .withIsActive(true)));

      Integer nonSystemId = insertOutput2.getRecords().get(0).getValueInteger("id");

      ///////////////////////////////////////////////////////////
      // query and verify system vs non-system flags           //
      ///////////////////////////////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(new QueryInput(ActivityType.TABLE_NAME));
      assertEquals(2, queryOutput.getRecords().size());

      ActivityType systemType = new ActivityType(queryOutput.getRecords().stream()
         .filter(r -> systemId.equals(r.getValueInteger("id")))
         .findFirst()
         .orElseThrow());
      assertTrue(systemType.getIsSystem());

      ActivityType nonSystemType = new ActivityType(queryOutput.getRecords().stream()
         .filter(r -> nonSystemId.equals(r.getValueInteger("id")))
         .findFirst()
         .orElseThrow());
      assertFalse(nonSystemType.getIsSystem());
   }

}
