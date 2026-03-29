/*******************************************************************************
 ** Unit tests for ActivityLastDateCustomizer -- verifies that inserting an
 ** Activity updates lastActivityDate and lastContactedDate on linked
 ** Contact, Company, and Deal records.
 *******************************************************************************/
package com.kingsrook.qbits.crm.activities.customizers;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests for ActivityLastDateCustomizer: verifies that creating activities
 ** propagates lastActivityDate and lastContactedDate to linked records.
 *******************************************************************************/
class ActivityLastDateCustomizerTest extends BaseTest
{

   /*******************************************************************************
    ** Insert a contact, then insert an activity linked to it. Verify
    ** contact.lastActivityDate is set.
    *******************************************************************************/
   @Test
   void testActivityInsertUpdatesContactLastActivityDate() throws QException
   {
      Integer activityTypeId = insertActivityType("Call");
      Integer contactId = insertContact("John", "Doe", "john@example.com");

      insertActivity(activityTypeId, "Test call", contactId, null, null, null);

      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertNotNull(contact.getLastActivityDate());
   }



   /*******************************************************************************
    ** Insert contact, insert activity with direction=OUTBOUND. Verify
    ** contact.lastContactedDate is set.
    *******************************************************************************/
   @Test
   void testOutboundActivityUpdatesContactLastContactedDate() throws QException
   {
      Integer activityTypeId = insertActivityType("Call");
      Integer contactId = insertContact("Jane", "Doe", "jane@example.com");

      insertActivity(activityTypeId, "Outbound call", contactId, null, null, CrmDirection.OUTBOUND.getPossibleValueId());

      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertNotNull(contact.getLastActivityDate());
      assertNotNull(contact.getLastContactedDate());
   }



   /*******************************************************************************
    ** Insert contact, insert INBOUND activity. Verify lastContactedDate is
    ** still null.
    *******************************************************************************/
   @Test
   void testInboundActivityDoesNotUpdateLastContactedDate() throws QException
   {
      Integer activityTypeId = insertActivityType("Call");
      Integer contactId = insertContact("Bob", "Smith", "bob@example.com");

      insertActivity(activityTypeId, "Inbound call", contactId, null, null, CrmDirection.INBOUND.getPossibleValueId());

      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertNotNull(contact.getLastActivityDate());
      assertNull(contact.getLastContactedDate());
   }



   /*******************************************************************************
    ** Insert company, insert activity linked to it. Verify
    ** company.lastActivityDate is set.
    *******************************************************************************/
   @Test
   void testActivityInsertUpdatesCompanyLastActivityDate() throws QException
   {
      Integer activityTypeId = insertActivityType("Meeting");
      Integer companyId = insertCompany("Acme Inc");

      insertActivity(activityTypeId, "Company meeting", null, companyId, null, null);

      Company company = new Company(GetAction.execute(Company.TABLE_NAME, companyId));
      assertNotNull(company.getLastActivityDate());
   }



   /*******************************************************************************
    ** Insert pipeline+stage+deal, insert activity linked to deal. Verify
    ** deal.lastActivityDate is set.
    *******************************************************************************/
   @Test
   void testActivityInsertUpdatesDealLastActivityDate() throws QException
   {
      Integer activityTypeId = insertActivityType("Note");
      Integer pipelineId = insertPipeline("Sales");
      Integer stageId = insertStage(pipelineId, "Open", 1, 10);
      Integer dealId = insertDeal("Test Deal", pipelineId, stageId, new BigDecimal("5000"));

      insertActivity(activityTypeId, "Deal note", null, null, dealId, null);

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getLastActivityDate());
   }



   /*******************************************************************************
    ** Insert contact + company + deal, insert activity linked to all three.
    ** Verify all three have lastActivityDate set.
    *******************************************************************************/
   @Test
   void testActivityWithMultipleLinksUpdatesAll() throws QException
   {
      Integer activityTypeId = insertActivityType("Email");
      Integer contactId = insertContact("Multi", "Link", "multi@example.com");
      Integer companyId = insertCompany("Multi Corp");
      Integer pipelineId = insertPipeline("Multi Pipeline");
      Integer stageId = insertStage(pipelineId, "Open", 1, 20);
      Integer dealId = insertDeal("Multi Deal", pipelineId, stageId, new BigDecimal("3000"));

      insertActivity(activityTypeId, "Multi-link activity", contactId, companyId, dealId, CrmDirection.OUTBOUND.getPossibleValueId());

      Contact contact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertNotNull(contact.getLastActivityDate());
      assertNotNull(contact.getLastContactedDate());

      Company company = new Company(GetAction.execute(Company.TABLE_NAME, companyId));
      assertNotNull(company.getLastActivityDate());
      assertNotNull(company.getLastContactedDate());

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertNotNull(deal.getLastActivityDate());
   }



   /////////////////////////////////////////////////////////////////////////////
   // Helper methods                                                          //
   /////////////////////////////////////////////////////////////////////////////

   /***************************************************************************
    ** Helper: insert an ActivityType and return its id.
    ***************************************************************************/
   private Integer insertActivityType(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName(name)
               .withSortOrder(1)
               .withIsActive(true)
               .withIsSystem(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Contact and return its id.
    ***************************************************************************/
   private Integer insertContact(String firstName, String lastName, String email) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName(firstName)
               .withLastName(lastName)
               .withEmail(email)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Company and return its id.
    ***************************************************************************/
   private Integer insertCompany(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Company.TABLE_NAME)
            .withRecordEntity(new Company()
               .withName(name)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Pipeline and return its id.
    ***************************************************************************/
   private Integer insertPipeline(String name) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName(name)
               .withIsDefault(true)
               .withIsActive(true)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a PipelineStage and return its id.
    ***************************************************************************/
   private Integer insertStage(Integer pipelineId, String name, Integer sortOrder, Integer probabilityPct) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName(name)
               .withSortOrder(sortOrder)
               .withProbabilityPct(probabilityPct)
               .withIsClosedWon(false)
               .withIsClosedLost(false)));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert a Deal and return its id.
    ***************************************************************************/
   private Integer insertDeal(String name, Integer pipelineId, Integer stageId, BigDecimal amount) throws QException
   {
      InsertOutput output = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName(name)
               .withPipelineId(pipelineId)
               .withPipelineStageId(stageId)
               .withAmount(amount)
               .withOwnerUserId("user-001")));
      return (output.getRecords().get(0).getValueInteger("id"));
   }



   /***************************************************************************
    ** Helper: insert an Activity and return its id.
    ***************************************************************************/
   private Integer insertActivity(Integer activityTypeId, String subject, Integer contactId,
                                  Integer companyId, Integer dealId, Integer direction) throws QException
   {
      Activity activity = new Activity()
         .withActivityTypeId(activityTypeId)
         .withSubject(subject)
         .withOwnerUserId("user-001");

      if(contactId != null)
      {
         activity.withContactId(contactId);
      }
      if(companyId != null)
      {
         activity.withCompanyId(companyId);
      }
      if(dealId != null)
      {
         activity.withDealId(dealId);
      }
      if(direction != null)
      {
         activity.withDirection(direction);
      }

      InsertOutput output = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME).withRecordEntity(activity));
      return (output.getRecords().get(0).getValueInteger("id"));
   }

}
