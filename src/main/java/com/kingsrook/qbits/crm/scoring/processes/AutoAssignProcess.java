/*******************************************************************************
 ** Process to auto-assign a contact or deal based on assignment rules.
 *******************************************************************************/
package com.kingsrook.qbits.crm.scoring.processes;


import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignStrategy;
import com.kingsrook.qbits.crm.core.model.enums.CrmAssignableEntity;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRule;
import com.kingsrook.qbits.crm.scoring.model.AssignmentRuleMember;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
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
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep to auto-assign a contact or deal to a user
 ** based on configured assignment rules. Evaluates rules in sortOrder; first
 ** match wins. Supports SPECIFIC_USER, ROUND_ROBIN, and LEAST_ACTIVE strategies.
 *******************************************************************************/
public class AutoAssignProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "autoAssign";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Auto Assign")
         .withIcon(new QIcon().withName("assignment_ind"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(AutoAssignProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("entityType", QFieldType.STRING).withIsRequired(true),
                  new QFieldMetaData("recordId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the assignment logic.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String  entityTypeStr = input.getValueString("entityType");
      Integer recordId      = input.getValueInteger("recordId");

      ///////////////////////////////////////////////
      // resolve entity type                       //
      ///////////////////////////////////////////////
      CrmAssignableEntity entityType = null;
      for(CrmAssignableEntity e : CrmAssignableEntity.values())
      {
         if(e.getLabel().equalsIgnoreCase(entityTypeStr) || e.name().equalsIgnoreCase(entityTypeStr))
         {
            entityType = e;
            break;
         }
      }

      if(entityType == null)
      {
         output.addValue("action", "NO_MATCH");
         return;
      }

      ///////////////////////////////////////////////
      // resolve table name                        //
      ///////////////////////////////////////////////
      String tableName = entityType == CrmAssignableEntity.CONTACT
         ? Contact.TABLE_NAME : Deal.TABLE_NAME;

      ///////////////////////////////////////////////
      // load the record                           //
      ///////////////////////////////////////////////
      GetOutput recordGet = new GetAction().execute(
         new GetInput(tableName).withPrimaryKey(recordId));

      if(recordGet.getRecord() == null)
      {
         output.addValue("action", "RECORD_NOT_FOUND");
         return;
      }

      QRecord record = recordGet.getRecord();

      ///////////////////////////////////////////////
      // query active rules for this entity type   //
      ///////////////////////////////////////////////
      QueryOutput rulesQuery = new QueryAction().execute(
         new QueryInput(AssignmentRule.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("entityType", QCriteriaOperator.EQUALS, entityType.getId()))
               .withCriteria(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true))
               .withOrderBy(new QFilterOrderBy("sortOrder"))));

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

               new UpdateAction().execute(
                  new UpdateInput(tableName).withRecord(updateRecord));

               output.addValue("action", "ASSIGNED");
               output.addValue("assignedUserId", assignedUserId);
               output.addValue("ruleId", rule.getId());
               return;
            }
         }
      }

      ///////////////////////////////////////////////
      // no rules matched -- keep existing owner   //
      ///////////////////////////////////////////////
      output.addValue("action", "NO_MATCH");
   }



   /***************************************************************************
    ** Evaluate a rule's criteria against a record. If criteriaJson is null
    ** or empty, the rule matches all records.
    ***************************************************************************/
   private boolean evaluateCriteria(AssignmentRule rule, QRecord record)
   {
      /////////////////////////////////////////////
      // null or empty criteria matches all      //
      /////////////////////////////////////////////
      if(!StringUtils.hasContent(rule.getCriteriaJson()))
      {
         return (true);
      }

      /////////////////////////////////////////////
      // simple JSON criteria evaluation         //
      // format: {"fieldName":"expectedValue"}   //
      /////////////////////////////////////////////
      try
      {
         String json = rule.getCriteriaJson().trim();
         if(json.startsWith("{") && json.endsWith("}"))
         {
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",");
            for(String pair : pairs)
            {
               String[] kv = pair.split(":", 2);
               if(kv.length == 2)
               {
                  String fieldName = kv[0].trim().replace("\"", "");
                  String expectedValue = kv[1].trim().replace("\"", "");
                  String actualValue = record.getValueString(fieldName);

                  if(!Objects.equals(actualValue, expectedValue))
                  {
                     return (false);
                  }
               }
            }
         }
         return (true);
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /***************************************************************************
    ** Resolve the user to assign to based on the rule's strategy.
    ***************************************************************************/
   private String resolveAssignment(AssignmentRule rule) throws QException
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
   private String resolveRoundRobin(AssignmentRule rule) throws QException
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
   private String resolveLeastActive(AssignmentRule rule) throws QException
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
   private List<AssignmentRuleMember> getActiveMembers(Integer ruleId) throws QException
   {
      QueryOutput membersQuery = new QueryAction().execute(
         new QueryInput(AssignmentRuleMember.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("assignmentRuleId", QCriteriaOperator.EQUALS, ruleId))
               .withCriteria(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true))
               .withOrderBy(new QFilterOrderBy("sortOrder"))));

      return (membersQuery.getRecords().stream()
         .map(AssignmentRuleMember::new)
         .toList());
   }



   /***************************************************************************
    ** Update the lastAssignedDate on a member.
    ***************************************************************************/
   private void updateLastAssignedDate(AssignmentRuleMember member) throws QException
   {
      QRecord updateRecord = new QRecord()
         .withValue("id", member.getId())
         .withValue("lastAssignedDate", Instant.now());

      new UpdateAction().execute(
         new UpdateInput(AssignmentRuleMember.TABLE_NAME).withRecord(updateRecord));
   }

}
