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


package com.kingsrook.qbits.crm.scoring;


import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRule;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRuleMember;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Internal assignment work shared by intake and the permission-gated UI process.
 ** Evaluates active rules in sort order and preserves the first matching strategy.
 *******************************************************************************/
public final class CrmAutoAssign
{
   private static final QLogger LOG = QLogger.getLogger(CrmAutoAssign.class);

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmAutoAssign()
   {
   }

   /*******************************************************************************
    ** Assignment outcome retained for the existing process output contract.
    *******************************************************************************/
   public record Result(String action, String assignedUserId, Integer ruleId)
   {
   }

   /*******************************************************************************
    ** Assign as trusted internal work after the caller validates its entry point.
    *******************************************************************************/
   public static Result assign(CrmAssignableEntity entityType, Integer recordId) throws QException
   {
      ///////////////////////////////////////////////
      // resolve table name                        //
      ///////////////////////////////////////////////
      String tableName = entityType == CrmAssignableEntity.CONTACT
         ? Contact.TABLE_NAME : Deal.TABLE_NAME;

      ///////////////////////////////////////////////
      // load the record                           //
      ///////////////////////////////////////////////
      GetInput getInput = new GetInput(tableName).withPrimaryKey(recordId);
      getInput.withPermissionBypass("CRM assignment rules run as internal work after intake or process authorization");
      GetOutput recordGet = new GetAction().execute(getInput);

      if(recordGet.getRecord() == null)
      {
         return (new Result("RECORD_NOT_FOUND", null, null));
      }

      QRecord record = recordGet.getRecord();

      ///////////////////////////////////////////////
      // query active rules for this entity type   //
      ///////////////////////////////////////////////
      QueryInput rulesInput = new QueryInput(AssignmentRule.TABLE_NAME)
         .withFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, entityType.getId()))
            .withCriteria(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true))
            .withOrderBy(new QFilterOrderBy("sortOrder")));
      rulesInput.withPermissionBypass("CRM assignment rules run as internal work after intake or process authorization");
      QueryOutput rulesQuery = new QueryAction().execute(rulesInput);

      ///////////////////////////////////////////////
      // evaluate rules, first match wins          //
      ///////////////////////////////////////////////
      for(QRecord ruleRecord : rulesQuery.getRecords())
      {
         AssignmentRule rule = new AssignmentRule(ruleRecord);

         if(evaluateCriteria(rule, record))
         {
            String assignedUserId = resolveAssignment(rule);

            if(StringUtils.hasContent(assignedUserId))
            {
               QRecord updateRecord = new QRecord()
                  .withValue("id", recordId)
                  .withValue("ownerUserId", assignedUserId);

               UpdateInput updateInput = new UpdateInput(tableName).withRecord(updateRecord);
               updateInput.withPermissionBypass("CRM assignment rules run as internal work after intake or process authorization");
               new UpdateAction().execute(updateInput);

               return (new Result("ASSIGNED", assignedUserId, rule.getId()));
            }
         }
      }

      ///////////////////////////////////////////////
      // no rules matched -- keep existing owner   //
      ///////////////////////////////////////////////
      return (new Result("NO_MATCH", null, null));
   }



   /***************************************************************************
    ** Evaluate a rule's criteria against a record. If criteriaJson is null
    ** or empty, the rule matches all records.
    ***************************************************************************/
   private static boolean evaluateCriteria(AssignmentRule rule, QRecord record)
   {
      /////////////////////////////////////////////
      // null or empty criteria matches all      //
      /////////////////////////////////////////////
      if(!StringUtils.hasContent(rule.getCriteriaJson()))
      {
         return (true);
      }

      /////////////////////////////////////////////
      // parse criteria JSON using JsonUtils     //
      // format: {"fieldName":"expectedValue"}   //
      /////////////////////////////////////////////
      try
      {
         @SuppressWarnings("unchecked")
         Map<String, Object> criteriaMap = JsonUtils.toObject(rule.getCriteriaJson(), Map.class);
         for(Map.Entry<String, Object> entry : criteriaMap.entrySet())
         {
            String fieldName     = entry.getKey();
            String expectedValue = entry.getValue() == null ? null : String.valueOf(entry.getValue());
            String actualValue   = record.getValueString(fieldName);

            if(!Objects.equals(actualValue, expectedValue))
            {
               return (false);
            }
         }

         return (true);
      }
      catch(Exception e)
      {
         LOG.warn("Error parsing criteriaJson for assignment rule", e);
         return (false);
      }
   }



   /***************************************************************************
    ** Resolve the user to assign to based on the rule's strategy.
    ***************************************************************************/
   private static String resolveAssignment(AssignmentRule rule) throws QException
   {
      CrmAssignStrategy strategy = CrmAssignStrategy.getById(rule.getAssignStrategy());

      if(strategy == null)
      {
         return (null);
      }

      switch(strategy)
      {
         case SPECIFIC_USER:
            return (rule.getAssignToUserId());

         case ROUND_ROBIN:
            return (resolveRoundRobin(rule));

         case LEAST_ACTIVE:
            return (resolveLeastActive(rule));

         default:
            return (null);
      }
   }



   /***************************************************************************
    ** Round robin: find active members, pick the one after the most recently
    ** assigned (by sortOrder), wrapping around.
    ***************************************************************************/
   private static String resolveRoundRobin(AssignmentRule rule) throws QException
   {
      List<AssignmentRuleMember> members = getActiveMembers(rule.getId());

      if(members.isEmpty())
      {
         return (null);
      }

      /////////////////////////////////////////////
      // find the last assigned member           //
      /////////////////////////////////////////////
      AssignmentRuleMember lastAssigned = null;
      for(AssignmentRuleMember member : members)
      {
         if(member.getLastAssignedDate() != null)
         {
            if(lastAssigned == null || member.getLastAssignedDate().isAfter(lastAssigned.getLastAssignedDate()))
            {
               lastAssigned = member;
            }
         }
      }

      /////////////////////////////////////////////
      // pick next member after last assigned    //
      /////////////////////////////////////////////
      AssignmentRuleMember selected;
      if(lastAssigned == null)
      {
         selected = members.get(0);
      }
      else
      {
         int lastIndex = members.indexOf(lastAssigned);
         int nextIndex = (lastIndex + 1) % members.size();
         selected = members.get(nextIndex);
      }

      /////////////////////////////////////////////
      // update lastAssignedDate                 //
      /////////////////////////////////////////////
      updateLastAssignedDate(selected);

      return (selected.getUserId());
   }



   /***************************************************************************
    ** Least active: find active members, pick the one with fewest owned
    ** records (approximated by oldest lastAssignedDate or null).
    ***************************************************************************/
   private static String resolveLeastActive(AssignmentRule rule) throws QException
   {
      List<AssignmentRuleMember> members = getActiveMembers(rule.getId());

      if(members.isEmpty())
      {
         return (null);
      }

      /////////////////////////////////////////////////////////////
      // pick member with null lastAssignedDate, or oldest date  //
      /////////////////////////////////////////////////////////////
      AssignmentRuleMember selected = members.stream()
         .min(Comparator.comparing(
            m -> m.getLastAssignedDate() != null ? m.getLastAssignedDate() : Instant.EPOCH))
         .orElse(members.get(0));

      updateLastAssignedDate(selected);

      return (selected.getUserId());
   }



   /***************************************************************************
    ** Query active members for a rule, ordered by sortOrder.
    ***************************************************************************/
   private static List<AssignmentRuleMember> getActiveMembers(Integer ruleId) throws QException
   {
      QueryInput membersInput = new QueryInput(AssignmentRuleMember.TABLE_NAME)
         .withFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("assignmentRuleId", QCriteriaOperator.EQUALS, ruleId))
            .withCriteria(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true))
            .withOrderBy(new QFilterOrderBy("sortOrder")));
      membersInput.withPermissionBypass("CRM assignment rules run as internal work after intake or process authorization");
      QueryOutput membersQuery = new QueryAction().execute(membersInput);

      return (membersQuery.getRecords().stream()
         .map(AssignmentRuleMember::new)
         .toList());
   }



   /***************************************************************************
    ** Update the lastAssignedDate on a member.
    ***************************************************************************/
   private static void updateLastAssignedDate(AssignmentRuleMember member) throws QException
   {
      QRecord updateRecord = new QRecord()
         .withValue("id", member.getId())
         .withValue("lastAssignedDate", Instant.now());

      UpdateInput updateInput = new UpdateInput(AssignmentRuleMember.TABLE_NAME).withRecord(updateRecord);
      updateInput.withPermissionBypass("CRM assignment rules run as internal work after intake or process authorization");
      new UpdateAction().execute(updateInput);
   }

}
