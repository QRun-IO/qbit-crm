/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*******************************************************************************
 ** Scheduled process to calculate lead scores for all contacts.
 *******************************************************************************/

package com.kingsrook.qbits.crm.scoring.processes;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.crm.core.CrmRecords;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmScoreOperator;
import com.kingsrook.qbits.crm.deals.CrmDealNotifications;
import com.kingsrook.qbits.crm.scoring.JobState;
import com.kingsrook.qbits.crm.scoring.model.LeadScoreRule;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
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
   private static final QLogger LOG = QLogger.getLogger(CalculateLeadScoresProcess.class);

   public static final String NAME = "crm_calculate_lead_scores";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withIsHidden(true)
         .withLabel("Calculate Lead Scores")
         .withIcon(new QIcon().withName("score"))
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
      Instant startedAt = Instant.now();
      List<QRecord> states = new QueryAction().execute(new QueryInput(JobState.TABLE_NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, NAME)))).getRecords();
      QRecord state = states.isEmpty() ? new QRecord().withValue("name", NAME) : states.getFirst();
      Instant lastSuccess = state.getValueInstant("lastSuccess");
      Instant lastFullPass = state.getValueInstant("lastFullPass");
      boolean full = Boolean.TRUE.equals(input.getValueBoolean("fullRecalc")) || lastSuccess == null || lastFullPass == null
         || !lastFullPass.isAfter(startedAt.minus(24, ChronoUnit.HOURS));
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

      ///////////////////////////////////////////////////////////////
      // paginate through all contacts to avoid OOM on large CRMs //
      ///////////////////////////////////////////////////////////////
      int pageSize     = 200;
      int updatedCount = 0;
      int totalContacts = 0;
      Integer lastId   = null;

      while(true)
      {
         QQueryFilter filter = new QQueryFilter()
            .withOrderBy(new QFilterOrderBy("id"))
            .withLimit(pageSize);

         if(lastId != null)
         {
            filter.withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId));
         }

         if(!full)
         {
            filter.withCriteria(new QFilterCriteria("modifyDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, lastSuccess));
         }
         QueryOutput contactsQuery = new QueryAction().execute(
            new QueryInput(Contact.TABLE_NAME).withFilter(filter));

         if(contactsQuery.getRecords().isEmpty())
         {
            break;
         }

         Map<Integer, QRecord> companies = loadCompanies(contactsQuery.getRecords());
         List<QRecord> changes = new ArrayList<>();
         for(QRecord contactRecord : contactsQuery.getRecords())
         {
            totalContacts++;
            QRecord change = calculateChange(contactRecord, rules, companies);
            if(change != null)
            {
               changes.add(change);
            }
            lastId = contactRecord.getValueInteger("id");
         }
         writeScores(changes);
         updatedCount += changes.size();

         /////////////////////////////////////////////
         // if page was not full, we are done       //
         /////////////////////////////////////////////
         if(contactsQuery.getRecords().size() < pageSize)
         {
            break;
         }
      }

      CrmDealNotifications.checkRotting();
      state.withValue("lastSuccess", startedAt);
      if(full)
      {
         state.withValue("lastFullPass", startedAt);
      }
      if(states.isEmpty())
      {
         CrmRecords.insert(JobState.TABLE_NAME, state);
      }
      else
      {
         CrmRecords.update(JobState.TABLE_NAME, state);
      }
      output.addValue("fullRecalc", full);
      output.addValue("updatedCount", updatedCount);
      output.addValue("totalContacts", totalContacts);
      output.addValue("totalRules", rules.size());
   }



   /*******************************************************************************
    ** Recalculate only the selected contact using the same rules as the job.
    *******************************************************************************/
   public void recalculate(QRecord contact) throws QException
   {
      List<LeadScoreRule> rules = new QueryAction().execute(new QueryInput(LeadScoreRule.TABLE_NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true))
            .withOrderBy(new QFilterOrderBy("sortOrder")))).getRecords().stream().map(LeadScoreRule::new).toList();
      QRecord change = calculateChange(contact, rules, loadCompanies(List.of(contact)));
      writeScores(change == null ? List.of() : List.of(change));
   }

   /*******************************************************************************
    ** Compute a score update only when the result changed.
    *******************************************************************************/
   private QRecord calculateChange(QRecord contact, List<LeadScoreRule> rules, Map<Integer, QRecord> companies) throws QException
   {
      int score = 0;
      for(LeadScoreRule rule : rules)
      {
         if(evaluateRule(rule, contact, companies))
         {
            score += rule.getScoreAdjustment();
         }
      }
      Integer current = contact.getValueInteger("leadScore");
      if(Objects.equals(current == null ? 0 : current, score))
      {
         return (null);
      }
      return (new QRecord().withValue("id", contact.getValue("id")).withValue("leadScore", score));
   }

   /*******************************************************************************
    ** Batch score writes so core audit label resolution also operates per page.
    *******************************************************************************/
   private void writeScores(List<QRecord> changes) throws QException
   {
      if(changes.isEmpty())
      {
         return;
      }
      UpdateInput update = new UpdateInput(Contact.TABLE_NAME).withOmitModifyDateUpdate(true);
      update.setRecords(changes);
      var updated = new UpdateAction().execute(update).getRecords();
      if(updated.isEmpty() || updated.stream().anyMatch(row -> row.getErrors() != null && !row.getErrors().isEmpty()))
      {
         throw (new QException("Lead score update was rejected"));
      }
   }


   /***************************************************************************
    ** Evaluate a single rule against a contact record. Supports dot-notation
    ** field paths for related entities (e.g., "company.industry").
    ***************************************************************************/
   private boolean evaluateRule(LeadScoreRule rule, QRecord contactRecord, Map<Integer, QRecord> companies)
   {
      CrmScoreOperator operator = CrmScoreOperator.getById(rule.getOperator());
      if(operator == null)
      {
         return (false);
      }

      String fieldPath = rule.getFieldPath();
      String actualValue = resolveFieldValue(fieldPath, contactRecord, companies);
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

         case DAYS_AGO_GT:
         case DAYS_AGO_LT:
            return (compareAge(actualValue, expectedValue, operator));

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



   /***************************************************************************
    ** Resolve a field value from a contact record, supporting dot-notation
    ** for related entity fields (e.g., "company.industry" loads the contact's
    ** company record and returns its "industry" field).
    ***************************************************************************/
   private String resolveFieldValue(String fieldPath, QRecord contactRecord, Map<Integer, QRecord> companies)
   {
      if(fieldPath == null || !fieldPath.contains("."))
      {
         return (contactRecord.getValueString(fieldPath));
      }
      String[] parts = fieldPath.split("\\.", 2);
      if("company".equalsIgnoreCase(parts[0]))
      {
         QRecord company = companies.get(contactRecord.getValueInteger("companyId"));
         return (company == null ? null : company.getValueString(parts[1]));
      }
      LOG.warn("Unsupported dot-notation prefix in lead score rule fieldPath: " + parts[0]);
      return (null);
   }

   /*******************************************************************************
    ** One company query per page, shared by every rule and contact in that page.
    *******************************************************************************/
   private Map<Integer, QRecord> loadCompanies(List<QRecord> contacts) throws QException
   {
      List<Integer> ids = contacts.stream().map(row -> row.getValueInteger("companyId")).filter(Objects::nonNull).distinct().toList();
      Map<Integer, QRecord> companies = new HashMap<>();
      if(!ids.isEmpty())
      {
         for(QRecord company : new QueryAction().execute(new QueryInput(Company.TABLE_NAME)
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, ids)))).getRecords())
         {
            companies.put(company.getValueInteger("id"), company);
         }
      }
      return (companies);
   }

   /*******************************************************************************
    ** Compare instants or UTC calendar dates with the strict N-days-ago boundary.
    *******************************************************************************/
   private boolean compareAge(String actual, String expected, CrmScoreOperator operator)
   {
      try
      {
         Instant date = actual.length() == 10 ? LocalDate.parse(actual).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.parse(actual);
         Instant threshold = Instant.now().minus(Long.parseLong(expected), ChronoUnit.DAYS);
         return (operator == CrmScoreOperator.DAYS_AGO_GT ? date.isBefore(threshold) : date.isAfter(threshold));
      }
      catch(RuntimeException e)
      {
         return (false);
      }
   }
}
