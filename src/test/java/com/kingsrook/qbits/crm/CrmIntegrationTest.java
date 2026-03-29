/*******************************************************************************
 ** End-to-end integration test for the CRM QBit. Exercises the full lifecycle:
 ** contacts, companies, form submissions, activities, pipelines, deals,
 ** deal products, stage changes, audit logs, and stage history.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityType;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmDirection;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.deals.model.DealStageHistory;
import com.kingsrook.qbits.crm.deals.model.Pipeline;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qbits.crm.deals.model.Product;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Full CRM lifecycle integration test. Validates that all major CRM operations
 ** work together end-to-end: contacts, companies, forms, activities, pipelines,
 ** deals, products, stage transitions, auditing, and stage history.
 *******************************************************************************/
class CrmIntegrationTest extends BaseTest
{

   /*******************************************************************************
    ** Walk through the complete CRM lifecycle in a single test to verify all
    ** customizers and cross-entity behaviors fire correctly together.
    *******************************************************************************/
   @Test
   void testFullCrmLifecycle() throws QException
   {
      ////////////////////////////////////////////////////////////////////////
      // Step 1: Insert a Contact                                           //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput contactInsert = new InsertAction().execute(
         new InsertInput(Contact.TABLE_NAME)
            .withRecordEntity(new Contact()
               .withFirstName("John")
               .withLastName("Doe")
               .withEmail("john@example.com")
               .withOwnerUserId("rep1")));
      Integer contactId = contactInsert.getRecords().get(0).getValueInteger("id");
      assertThat(contactId).isNotNull();

      ////////////////////////////////////////////////////////////////////////
      // Step 2: Insert a Company, then update contact.companyId            //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput companyInsert = new InsertAction().execute(
         new InsertInput(Company.TABLE_NAME)
            .withRecordEntity(new Company()
               .withName("Acme Inc")
               .withOwnerUserId("rep1")));
      Integer companyId = companyInsert.getRecords().get(0).getValueInteger("id");
      assertThat(companyId).isNotNull();

      QRecord contactUpdate = new QRecord();
      contactUpdate.setValue("id", contactId);
      contactUpdate.setValue("companyId", companyId);
      new UpdateAction().execute(new UpdateInput(Contact.TABLE_NAME).withRecord(contactUpdate));

      Contact updatedContact = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertThat(updatedContact.getCompanyId()).isEqualTo(companyId);

      ////////////////////////////////////////////////////////////////////////
      // Step 3: Insert FormSubmission -> verify auto-link to contact       //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput formInsert = new InsertAction().execute(
         new InsertInput(FormSubmission.TABLE_NAME)
            .withRecordEntity(new FormSubmission()
               .withEmail("john@example.com")
               .withFormType(CrmFormType.CONTACT.getPossibleValueId())
               .withFirstName("John")));
      Integer formId = formInsert.getRecords().get(0).getValueInteger("id");

      FormSubmission form = new FormSubmission(GetAction.execute(FormSubmission.TABLE_NAME, formId));
      assertThat(form.getContactId()).isEqualTo(contactId);

      ////////////////////////////////////////////////////////////////////////
      // Step 4: Insert ActivityType (Call, isSystem=true)                   //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput activityTypeInsert = new InsertAction().execute(
         new InsertInput(ActivityType.TABLE_NAME)
            .withRecordEntity(new ActivityType()
               .withName("Call")
               .withSortOrder(1)
               .withIsActive(true)
               .withIsSystem(true)));
      Integer activityTypeId = activityTypeInsert.getRecords().get(0).getValueInteger("id");

      ////////////////////////////////////////////////////////////////////////
      // Step 5: Insert Activity -> verify contact lastActivityDate and     //
      //         lastContactedDate are set                                  //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput activityInsert = new InsertAction().execute(
         new InsertInput(Activity.TABLE_NAME)
            .withRecordEntity(new Activity()
               .withActivityTypeId(activityTypeId)
               .withSubject("Follow-up call")
               .withDirection(CrmDirection.OUTBOUND.getPossibleValueId())
               .withContactId(contactId)
               .withCompanyId(companyId)
               .withOwnerUserId("rep1")));
      Integer activityId = activityInsert.getRecords().get(0).getValueInteger("id");
      assertThat(activityId).isNotNull();

      Contact contactAfterActivity = new Contact(GetAction.execute(Contact.TABLE_NAME, contactId));
      assertThat(contactAfterActivity.getLastActivityDate()).isNotNull();
      assertThat(contactAfterActivity.getLastContactedDate()).isNotNull();

      ////////////////////////////////////////////////////////////////////////
      // Step 6: Insert Pipeline + 3 stages                                 //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput pipelineInsert = new InsertAction().execute(
         new InsertInput(Pipeline.TABLE_NAME)
            .withRecordEntity(new Pipeline()
               .withName("Sales Pipeline")
               .withIsDefault(true)
               .withIsActive(true)));
      Integer pipelineId = pipelineInsert.getRecords().get(0).getValueInteger("id");

      InsertOutput prospectingInsert = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Prospecting")
               .withSortOrder(1)
               .withProbabilityPct(10)
               .withIsClosedWon(false)
               .withIsClosedLost(false)));
      Integer prospectingStageId = prospectingInsert.getRecords().get(0).getValueInteger("id");

      InsertOutput closedWonInsert = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Closed Won")
               .withSortOrder(2)
               .withProbabilityPct(100)
               .withIsClosedWon(true)
               .withIsClosedLost(false)));
      Integer closedWonStageId = closedWonInsert.getRecords().get(0).getValueInteger("id");

      InsertOutput closedLostInsert = new InsertAction().execute(
         new InsertInput(PipelineStage.TABLE_NAME)
            .withRecordEntity(new PipelineStage()
               .withPipelineId(pipelineId)
               .withName("Closed Lost")
               .withSortOrder(3)
               .withProbabilityPct(0)
               .withIsClosedWon(false)
               .withIsClosedLost(true)));
      Integer closedLostStageId = closedLostInsert.getRecords().get(0).getValueInteger("id");
      assertThat(closedLostStageId).isNotNull();

      ////////////////////////////////////////////////////////////////////////
      // Step 7: Insert Deal -> verify stageEnteredDate and weightedAmount  //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput dealInsert = new InsertAction().execute(
         new InsertInput(Deal.TABLE_NAME)
            .withRecordEntity(new Deal()
               .withName("Big Deal")
               .withAmount(new BigDecimal("10000"))
               .withPipelineId(pipelineId)
               .withPipelineStageId(prospectingStageId)
               .withOwnerUserId("rep1")));
      Integer dealId = dealInsert.getRecords().get(0).getValueInteger("id");

      Deal deal = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertThat(deal.getStageEnteredDate()).isNotNull();
      assertThat(deal.getWeightedAmount()).isEqualByComparingTo(new BigDecimal("1000"));

      ////////////////////////////////////////////////////////////////////////
      // Step 8: Insert DealProduct -> verify deal.amount recalculated      //
      ////////////////////////////////////////////////////////////////////////
      InsertOutput productInsert = new InsertAction().execute(
         new InsertInput(Product.TABLE_NAME)
            .withRecordEntity(new Product()
               .withName("Widget")
               .withSku("WDG-001")
               .withUnitPrice(new BigDecimal("2000"))
               .withIsActive(true)));
      Integer productId = productInsert.getRecords().get(0).getValueInteger("id");

      InsertOutput dealProductInsert = new InsertAction().execute(
         new InsertInput(DealProduct.TABLE_NAME)
            .withRecordEntity(new DealProduct()
               .withDealId(dealId)
               .withProductId(productId)
               .withQuantity(new BigDecimal("5"))
               .withUnitPrice(new BigDecimal("2000"))
               .withDiscountPct(new BigDecimal("0"))));
      Integer dealProductId = dealProductInsert.getRecords().get(0).getValueInteger("id");
      assertThat(dealProductId).isNotNull();

      Deal dealAfterProduct = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertThat(dealAfterProduct.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));

      ////////////////////////////////////////////////////////////////////////
      // Step 9: Update deal to Closed Won -> verify actualCloseDate and    //
      //         weightedAmount                                             //
      ////////////////////////////////////////////////////////////////////////
      QRecord dealStageUpdate = new QRecord();
      dealStageUpdate.setValue("id", dealId);
      dealStageUpdate.setValue("pipelineStageId", closedWonStageId);
      new UpdateAction().execute(new UpdateInput(Deal.TABLE_NAME).withRecord(dealStageUpdate));

      Deal dealClosedWon = new Deal(GetAction.execute(Deal.TABLE_NAME, dealId));
      assertThat(dealClosedWon.getActualCloseDate()).isNotNull();
      assertThat(dealClosedWon.getWeightedAmount()).isEqualByComparingTo(new BigDecimal("10000"));

      ////////////////////////////////////////////////////////////////////////
      // Step 10: Query AuditLog for CONTACT -> verify CREATED entry        //
      ////////////////////////////////////////////////////////////////////////
      QueryOutput contactAuditQuery = new QueryAction().execute(
         new QueryInput(AuditLog.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, CrmEntityType.CONTACT.getPossibleValueId()))));
      List<QRecord> contactAudits = contactAuditQuery.getRecords();
      assertThat(contactAudits).isNotEmpty();
      assertThat(contactAudits).anyMatch(r -> CrmAuditAction.CREATED.getPossibleValueId().equals(r.getValueInteger("action")));

      ////////////////////////////////////////////////////////////////////////
      // Step 11: Query AuditLog for DEAL -> verify CREATED and additional  //
      ////////////////////////////////////////////////////////////////////////
      QueryOutput dealAuditQuery = new QueryAction().execute(
         new QueryInput(AuditLog.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, CrmEntityType.DEAL.getPossibleValueId()))));
      List<QRecord> dealAudits = dealAuditQuery.getRecords();
      assertThat(dealAudits).anyMatch(r -> CrmAuditAction.CREATED.getPossibleValueId().equals(r.getValueInteger("action")));
      assertThat(dealAudits.size()).isGreaterThanOrEqualTo(2);

      ////////////////////////////////////////////////////////////////////////
      // Step 12: Query DealStageHistory -> verify at least 2 rows          //
      ////////////////////////////////////////////////////////////////////////
      QueryOutput stageHistoryQuery = new QueryAction().execute(
         new QueryInput(DealStageHistory.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("dealId", QCriteriaOperator.EQUALS, dealId))));
      assertThat(stageHistoryQuery.getRecords().size()).isGreaterThanOrEqualTo(2);
   }

}
