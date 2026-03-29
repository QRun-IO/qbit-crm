/*******************************************************************************
 ** Unit tests for the ActivityParticipant entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityParticipant;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.enums.CrmParticipantRole;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for ActivityParticipant CRUD operations and data contract for the
 ** contactId-or-userId validation (actual validation customizer is wired later).
 *******************************************************************************/
class ActivityParticipantTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the activity participant table is registered in the QInstance.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      assertNotNull(QContext.getQInstance().getTable(ActivityParticipant.TABLE_NAME));
   }



   /*******************************************************************************
    ** Insert a participant with a contactId and verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertWithContact() throws QException
   {
      Integer activityId = insertTestActivity();

      ActivityParticipant participant = new ActivityParticipant()
         .withActivityId(activityId)
         .withContactId(42)
         .withRole(CrmParticipantRole.ATTENDEE.getId());

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(ActivityParticipant.TABLE_NAME).withRecordEntity(participant));

      QRecord insertedRecord = insertOutput.getRecords().get(0);
      assertNotNull(insertedRecord.getValueInteger("id"));

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityParticipant.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      ActivityParticipant fetched = new ActivityParticipant(queryOutput.getRecords().get(0));
      assertEquals(activityId, fetched.getActivityId());
      assertEquals(42, fetched.getContactId());
      assertNull(fetched.getUserId());
      assertEquals(CrmParticipantRole.ATTENDEE.getId(), fetched.getRole());
   }



   /*******************************************************************************
    ** Insert a participant with a userId and verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertWithUser() throws QException
   {
      Integer activityId = insertTestActivity();

      ActivityParticipant participant = new ActivityParticipant()
         .withActivityId(activityId)
         .withUserId("user-abc")
         .withRole(CrmParticipantRole.ORGANIZER.getId());

      new InsertAction().execute(
         new InsertInput(ActivityParticipant.TABLE_NAME).withRecordEntity(participant));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityParticipant.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      ActivityParticipant fetched = new ActivityParticipant(queryOutput.getRecords().get(0));
      assertEquals(activityId, fetched.getActivityId());
      assertNull(fetched.getContactId());
      assertEquals("user-abc", fetched.getUserId());
      assertEquals(CrmParticipantRole.ORGANIZER.getId(), fetched.getRole());
   }



   /*******************************************************************************
    ** Verify that both contactId and userId can be set (edge case).
    *******************************************************************************/
   @Test
   void testInsertWithBothContactAndUser() throws QException
   {
      Integer activityId = insertTestActivity();

      ActivityParticipant participant = new ActivityParticipant()
         .withActivityId(activityId)
         .withContactId(99)
         .withUserId("user-xyz")
         .withRole(CrmParticipantRole.OPTIONAL.getId());

      new InsertAction().execute(
         new InsertInput(ActivityParticipant.TABLE_NAME).withRecordEntity(participant));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityParticipant.TABLE_NAME));

      ActivityParticipant fetched = new ActivityParticipant(queryOutput.getRecords().get(0));
      assertEquals(99, fetched.getContactId());
      assertEquals("user-xyz", fetched.getUserId());
   }



   /*******************************************************************************
    ** Verify that a participant with neither contactId nor userId can be stored
    ** (the validation customizer that rejects this will be added later).
    *******************************************************************************/
   @Test
   void testInsertWithNeitherContactNorUser() throws QException
   {
      Integer activityId = insertTestActivity();

      ActivityParticipant participant = new ActivityParticipant()
         .withActivityId(activityId)
         .withRole(CrmParticipantRole.ATTENDEE.getId());

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(ActivityParticipant.TABLE_NAME).withRecordEntity(participant));

      ///////////////////////////////////////////////////////////////////////
      // should insert successfully for now (no validation customizer yet) //
      ///////////////////////////////////////////////////////////////////////
      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityParticipant.TABLE_NAME));

      ActivityParticipant fetched = new ActivityParticipant(queryOutput.getRecords().get(0));
      assertNull(fetched.getContactId());
      assertNull(fetched.getUserId());
   }



   /*******************************************************************************
    ** Insert multiple participants for the same activity.
    *******************************************************************************/
   @Test
   void testMultipleParticipants() throws QException
   {
      Integer activityId = insertTestActivity();

      new InsertAction().execute(new InsertInput(ActivityParticipant.TABLE_NAME)
         .withRecordEntity(new ActivityParticipant()
            .withActivityId(activityId)
            .withUserId("user-organizer")
            .withRole(CrmParticipantRole.ORGANIZER.getId()))
         .withRecordEntity(new ActivityParticipant()
            .withActivityId(activityId)
            .withContactId(10)
            .withRole(CrmParticipantRole.ATTENDEE.getId()))
         .withRecordEntity(new ActivityParticipant()
            .withActivityId(activityId)
            .withContactId(20)
            .withRole(CrmParticipantRole.OPTIONAL.getId())));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ActivityParticipant.TABLE_NAME));

      assertEquals(3, queryOutput.getRecords().size());
   }



   /***************************************************************************
    ** Helper: insert prerequisite ActivityType and Activity, return activity id.
    ***************************************************************************/
   private Integer insertTestActivity() throws QException
   {
      InsertOutput typeOutput = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName("Meeting")
               .withIconName("event")
               .withIsSystem(true)
               .withSortOrder(1)
               .withIsActive(true)));

      Integer activityTypeId = typeOutput.getRecords().get(0).getValueInteger("id");

      InsertOutput activityOutput = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME)
            .withRecordEntity(new Activity()
               .withActivityTypeId(activityTypeId)
               .withSubject("Test meeting")
               .withOwnerUserId("user-test")
               .withIsCompleted(false)));

      return (activityOutput.getRecords().get(0).getValueInteger("id"));
   }

}
