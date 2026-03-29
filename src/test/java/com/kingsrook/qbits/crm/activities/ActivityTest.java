/*******************************************************************************
 ** Unit tests for the Activity entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityOutcome;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.enums.CrmActivityPriority;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for Activity CRUD operations, field persistence, and linked entity
 ** references.
 *******************************************************************************/
class ActivityTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the activity table is registered in the QInstance.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      assertNotNull(QContext.getQInstance().getTable(Activity.TABLE_NAME));
   }



   /*******************************************************************************
    ** Insert an activity with all fields populated and verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertWithAllFields() throws QException
   {
      ////////////////////////////////////
      // set up prerequisite reference   //
      // data: activity type & outcome   //
      ////////////////////////////////////
      Integer activityTypeId = insertActivityType("Call", "phone", true, 1);
      Integer outcomeId = insertActivityOutcome(activityTypeId, "Connected", 1);

      Instant now = Instant.now();

      Activity activity = new Activity()
         .withActivityTypeId(activityTypeId)
         .withSubject("Follow-up call with prospect")
         .withDescription("Discussed pricing and next steps for Q2 contract renewal.")
         .withContactId(100)
         .withCompanyId(200)
         .withDealId(300)
         .withOwnerUserId("user-001")
         .withAssignedToUserId("user-002")
         .withStartDate(now)
         .withEndDate(now.plusSeconds(1800))
         .withDueDate(now.plusSeconds(86400))
         .withCompletedDate(null)
         .withIsCompleted(false)
         .withPriority(CrmActivityPriority.HIGH.getId())
         .withDurationMinutes(30)
         .withOutcomeId(outcomeId)
         .withLocation("123 Main St, Suite 200")
         .withConferenceLink("https://meet.example.com/abc-123")
         .withDirection(CrmDirection.OUTBOUND.getId())
         .withIsReminderSet(true)
         .withReminderDate(now.plusSeconds(3600))
         .withExternalCalendarId("gcal-event-xyz-789");

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(activity));

      QRecord insertedRecord = insertOutput.getRecords().get(0);
      assertNotNull(insertedRecord.getValueInteger("id"));

      ///////////////////////////////////
      // query back and verify fields  //
      ///////////////////////////////////
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      Activity fetched = new Activity(queryOutput.getRecords().get(0));
      assertEquals(activityTypeId, fetched.getActivityTypeId());
      assertEquals("Follow-up call with prospect", fetched.getSubject());
      assertEquals("Discussed pricing and next steps for Q2 contract renewal.", fetched.getDescription());
      assertEquals(100, fetched.getContactId());
      assertEquals(200, fetched.getCompanyId());
      assertEquals(300, fetched.getDealId());
      assertEquals("user-001", fetched.getOwnerUserId());
      assertEquals("user-002", fetched.getAssignedToUserId());
      assertNotNull(fetched.getStartDate());
      assertNotNull(fetched.getEndDate());
      assertNotNull(fetched.getDueDate());
      assertFalse(fetched.getIsCompleted());
      assertEquals(CrmActivityPriority.HIGH.getId(), fetched.getPriority());
      assertEquals(30, fetched.getDurationMinutes());
      assertEquals(outcomeId, fetched.getOutcomeId());
      assertEquals("123 Main St, Suite 200", fetched.getLocation());
      assertEquals("https://meet.example.com/abc-123", fetched.getConferenceLink());
      assertEquals(CrmDirection.OUTBOUND.getId(), fetched.getDirection());
      assertTrue(fetched.getIsReminderSet());
      assertNotNull(fetched.getReminderDate());
      assertEquals("gcal-event-xyz-789", fetched.getExternalCalendarId());
   }



   /*******************************************************************************
    ** Insert a minimal activity (only required fields) and verify defaults.
    *******************************************************************************/
   @Test
   void testInsertMinimalActivity() throws QException
   {
      Integer activityTypeId = insertActivityType("Note", "note", false, 1);

      Activity activity = new Activity()
         .withActivityTypeId(activityTypeId)
         .withSubject("Quick internal note")
         .withOwnerUserId("user-001")
         .withIsCompleted(false);

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(activity));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      Activity fetched = new Activity(queryOutput.getRecords().get(0));
      assertEquals("Quick internal note", fetched.getSubject());
      assertEquals("user-001", fetched.getOwnerUserId());
      assertFalse(fetched.getIsCompleted());
   }



   /*******************************************************************************
    ** Verify that linked entity foreign keys (activityTypeId, outcomeId) are
    ** stored and retrievable.
    *******************************************************************************/
   @Test
   void testLinkedEntities() throws QException
   {
      Integer callTypeId = insertActivityType("Call", "phone", true, 1);
      Integer meetingTypeId = insertActivityType("Meeting", "event", true, 2);
      Integer outcomeId = insertActivityOutcome(callTypeId, "Left Voicemail", 1);

      //////////////////////////////////////////
      // insert activity linked to call type  //
      //////////////////////////////////////////
      new InsertAction().execute(new InsertInput(Activity.TABLE_NAME)
         .withRecordEntity(new Activity()
            .withActivityTypeId(callTypeId)
            .withSubject("Outbound call")
            .withOwnerUserId("user-001")
            .withIsCompleted(false)
            .withOutcomeId(outcomeId)
            .withDirection(CrmDirection.OUTBOUND.getId())));

      /////////////////////////////////////////////
      // insert activity linked to meeting type  //
      /////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(Activity.TABLE_NAME)
         .withRecordEntity(new Activity()
            .withActivityTypeId(meetingTypeId)
            .withSubject("Team sync")
            .withOwnerUserId("user-002")
            .withIsCompleted(true)));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Activity.TABLE_NAME));

      assertEquals(2, queryOutput.getRecords().size());

      ///////////////////////////////////////////////////
      // verify the call activity has the right links   //
      ///////////////////////////////////////////////////
      Activity callActivity = new Activity(queryOutput.getRecords().stream()
         .filter(r -> "Outbound call".equals(r.getValueString("subject")))
         .findFirst()
         .orElseThrow());

      assertEquals(callTypeId, callActivity.getActivityTypeId());
      assertEquals(outcomeId, callActivity.getOutcomeId());
      assertEquals(CrmDirection.OUTBOUND.getId(), callActivity.getDirection());

      //////////////////////////////////////////////////////
      // verify the meeting activity has the right links   //
      //////////////////////////////////////////////////////
      Activity meetingActivity = new Activity(queryOutput.getRecords().stream()
         .filter(r -> "Team sync".equals(r.getValueString("subject")))
         .findFirst()
         .orElseThrow());

      assertEquals(meetingTypeId, meetingActivity.getActivityTypeId());
      assertTrue(meetingActivity.getIsCompleted());
   }



   /***************************************************************************
    ** Helper: insert an ActivityType and return its id.
    ***************************************************************************/
   private Integer insertActivityType(String name, String iconName, Boolean isSystem, Integer sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withIconName(iconName)
               .withIsSystem(isSystem)
               .withSortOrder(sortOrder)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an ActivityOutcome and return its id.
    ***************************************************************************/
   private Integer insertActivityOutcome(Integer activityTypeId, String name, Integer sortOrder) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityOutcome.TABLE_NAME)
            .withRecordEntity(new ActivityOutcome()
               .withActivityTypeId(activityTypeId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
