/*******************************************************************************
 ** Scheduled process to calculate lead scores for all contacts.
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.processes;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
import com.kingsrook.qbits.crm.scoring.model.LeadScoreRule;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to recalculate lead scores for all contacts.
 ** Evaluates all active LeadScoreRule records against each contact and updates
 ** the contact's leadScore field where it has changed.
 *******************************************************************************/
public class CalculateLeadScoresProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "calculateLeadScores";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Calculate Lead Scores")
         .withIcon(new QIcon().withName("score"))
         // Scheduling is configured by the host application via ScheduledJob records
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(CalculateLeadScoresProcess.class))
         ));
   }



   /*******************************************************************************
    ** Execute the scoring logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      ///////////////////////////////////////////////
      // load all active rules ordered by sort     //
      ///////////////////////////////////////////////
      QueryOutput rulesQuery = new QueryAction().execute(
         new QueryInput(LeadScoreRule.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true))
               .withOrderBy(new QFilterOrderBy("sortOrder"))));

      List<LeadScoreRule> rules = rulesQuery.getRecords().stream()
         .map(LeadScoreRule::new)
         .toList();

      ///////////////////////////////////////////////
      // load all contacts                         //
      ///////////////////////////////////////////////
      QueryOutput contactsQuery = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));

      int updatedCount = 0;

      for(QRecord contactRecord : contactsQuery.getRecords())
      {
         Contact contact = new Contact(contactRecord);
         int newScore = 0;

         ///////////////////////////////////////////////
         // evaluate each rule against the contact    //
         ///////////////////////////////////////////////
         for(LeadScoreRule rule : rules)
         {
            if(evaluateRule(rule, contactRecord))
            {
               newScore += rule.getScoreAdjustment();
            }
         }

         ///////////////////////////////////////////////
         // update only if score changed              //
         ///////////////////////////////////////////////
         Integer currentScore = contact.getLeadScore() != null ? contact.getLeadScore() : 0;
         if(!Objects.equals(currentScore, newScore))
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", contact.getId())
               .withValue("leadScore", newScore);

            new UpdateAction().execute(
               new UpdateInput(Contact.TABLE_NAME).withRecord(updateRecord));

            updatedCount++;
         }
      }

      output.addValue("updatedCount", updatedCount);
      output.addValue("totalContacts", contactsQuery.getRecords().size());
      output.addValue("totalRules", rules.size());
   }



   /***************************************************************************
    ** Evaluate a single rule against a contact record.
    ***************************************************************************/
   private boolean evaluateRule(LeadScoreRule rule, QRecord contactRecord)
   {
      CrmScoreOperator operator = CrmScoreOperator.getById(rule.getOperator());
      if(operator == null)
      {
         return (false);
      }

      String fieldPath = rule.getFieldPath();
      String actualValue = contactRecord.getValueString(fieldPath);
      String expectedValue = rule.getFieldValue();

      switch(operator)
      {
         case EQUALS:
            return (Objects.equals(actualValue, expectedValue));

         case NOT_EQUALS:
            return (!Objects.equals(actualValue, expectedValue));

         case CONTAINS:
            return (actualValue != null && expectedValue != null && actualValue.contains(expectedValue));

         case GT:
            return (compareNumeric(actualValue, expectedValue) > 0);

         case LT:
            return (compareNumeric(actualValue, expectedValue) < 0);

         case IS_SET:
            return (StringUtils.hasContent(actualValue));

         case IS_NOT_SET:
            return (!StringUtils.hasContent(actualValue));

         default:
            return (false);
      }
   }



   /***************************************************************************
    ** Compare two values as numbers. Returns 0 if either is not numeric.
    ***************************************************************************/
   private int compareNumeric(String actual, String expected)
   {
      try
      {
         if(actual == null || expected == null)
         {
            return (0);
         }
         return (Double.compare(Double.parseDouble(actual), Double.parseDouble(expected)));
      }
      catch(NumberFormatException e)
      {
         return (0);
      }
   }

}
