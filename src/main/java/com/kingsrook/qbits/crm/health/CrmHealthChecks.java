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

package com.kingsrook.qbits.crm.health;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.scoring.JobState;
import com.kingsrook.qbits.crm.scoring.processes.CalculateLeadScoresProcess;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.health.HealthCategory;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;


/*******************************************************************************
 ** Core health checks with retained legacy HealthIndicator adapters. Run in the
 ** caller's tenant context when one is supplied, else as the core system user;
 ** no check returns personal values.
 *******************************************************************************/
public class CrmHealthChecks
{
   private final CrmEmailTransport transport;


   /*******************************************************************************
    ** Use the same readiness decision as actual email delivery.
    *******************************************************************************/
   public CrmHealthChecks(CrmQBitConfig config)
   {
      this(new CrmEmailTransport(config));
   }


   /*******************************************************************************
    ** Allow a host transport adapter.
    *******************************************************************************/
   public CrmHealthChecks(CrmEmailTransport transport)
   {
      this.transport = transport;
   }


   /*******************************************************************************
    ** Register fixed core definitions without database reads or captured user sessions.
    ** Legacy indicator names stay unchanged for existing middleware callers.
    *******************************************************************************/
   public static void register(QInstance instance, CrmQBitConfig config)
   {
      HealthChecksMetaData registry = HealthChecksMetaData.ofOrWithNew(instance);
      CrmHealthChecks checks = new CrmHealthChecks(config);
      Map<String, String> names = Map.of("crm.email", "crm.email", "crm.sequence-lag", "crm.sequenceLag", "crm.lead-score", "crm.leadScore");
      names.forEach((name, legacyName) ->
      {
         if(registry.getChecks().stream().noneMatch(check -> name.equals(check.getName())))
         {
            registry.withCheck(new HealthCheckMetaData().withName(name).withLabel(name).withComponent("crm")
               .withCategory(HealthCategory.INTEGRATION).withKind(HealthCheckKind.DEPENDENCY)
               .withIntervalSeconds(300).withTimeoutMs(5000).withCheckInstance(input ->
               {
                  if(input.getDeadline() != null && !Instant.now().isBefore(input.getDeadline()))
                  {
                     return (HealthCheckOutcome.unknown("CRM check deadline exceeded"));
                  }
                  HealthCheckResult result = checks.new Indicator(legacyName, QContext.getQSession()).check(input.getQInstance());
                  return (new HealthCheckOutcome().withStatus(com.kingsrook.qqq.backend.core.health.HealthStatus.valueOf(result.getStatus().name()))
                     .withMessage("CRM health check " + result.getStatus().name()));
               }));
         }
      });
   }


   /*******************************************************************************
    ** Run all checks without registering tables or starting background workers.
    *******************************************************************************/
   public Map<String, HealthCheckResult> run() throws QException
   {
      return (Map.of("crm.email", email(), "crm.sequenceLag", sequenceLag(), "crm.leadScore", leadScore()));
   }


   /*******************************************************************************
    ** Expose individual checks through the RC.6 health SPI. With no caller
    ** session (registration at startup), each check runs as the core system
    ** user: token-based authentication modules (OAuth2, Auth0) reject any other
    ** tokenless session, and the checks report only UP/DEGRADED, never values.
    *******************************************************************************/
   public List<HealthIndicator> indicators()
   {
      return (indicators(QContext.getQSession()));
   }


   /*******************************************************************************
    ** Supply the host's monitoring session explicitly for middleware worker threads.
    ** Its permissions and tenant keys are preserved; this adapter never grants keys.
    ** A null session runs each check as the core system user.
    *******************************************************************************/
   public List<HealthIndicator> indicators(QSession session)
   {
      return (List.of(new Indicator("crm.email", session), new Indicator("crm.sequenceLag", session), new Indicator("crm.leadScore", session)));
   }


   /*******************************************************************************
    ** One independently evaluated middleware indicator.
    *******************************************************************************/
   private class Indicator implements HealthIndicator
   {
      private final String name;
      private final QSession session;


      /*******************************************************************************
       ** Select a check.
       *******************************************************************************/
      private Indicator(String name, QSession session)
      {
         this.name = name;
         this.session = session;
      }


      @Override
      public String getName()
      {
         return (name);
      }


      @Override
      public HealthCheckResult check(QInstance instance)
      {
         CapturedContext original = QContext.capture();
         try
         {
            QContext.init(instance, session != null ? session : new QSystemUserSession(instance));
            return (switch(name)
            {
               case "crm.email" -> email();
               case "crm.sequenceLag" -> sequenceLag();
               default -> leadScore();
            });
         }
         catch(QException | RuntimeException e)
         {
            return (new HealthCheckResult().withStatus(HealthStatus.UNKNOWN).withDetail("reason", "CRM check could not be completed"));
         }
         finally
         {
            QContext.init(original);
         }
      }
   }


   /*******************************************************************************
    ** An unconfigured transport is degraded, not a claim that email can be sent.
    *******************************************************************************/
   private HealthCheckResult email() throws QException
   {
      return (new HealthCheckResult().withStatus(transport.readiness() == CrmReadiness.NOT_CONFIGURED ? HealthStatus.DEGRADED : HealthStatus.UP));
   }


   /*******************************************************************************
    ** Query just the oldest active due enrollment.
    *******************************************************************************/
   private HealthCheckResult sequenceLag() throws QException
   {
      List<QRecord> rows = new QueryAction().execute(new QueryInput("crm_sequence_enrollment").withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, CrmEnrollmentStatus.ACTIVE.getId()))
         .withCriteria(new QFilterCriteria("nextStepDate", QCriteriaOperator.LESS_THAN, Instant.now().minus(30, ChronoUnit.MINUTES)))
         .withOrderBy(new QFilterOrderBy("nextStepDate")).withLimit(1))).getRecords();
      return (new HealthCheckResult().withStatus(rows.isEmpty() ? HealthStatus.UP : HealthStatus.DEGRADED));
   }


   /*******************************************************************************
    ** No successful score pass, or one more than three hours old, needs attention.
    *******************************************************************************/
   private HealthCheckResult leadScore() throws QException
   {
      List<QRecord> rows = new QueryAction().execute(new QueryInput(JobState.TABLE_NAME).withFilter(new QQueryFilter(
         new QFilterCriteria("name", QCriteriaOperator.EQUALS, CalculateLeadScoresProcess.NAME)).withLimit(1))).getRecords();
      Instant last = rows.isEmpty() ? null : rows.getFirst().getValueInstant("lastSuccess");
      return (new HealthCheckResult().withStatus(last == null || last.isBefore(Instant.now().minus(3, ChronoUnit.HOURS))
         ? HealthStatus.DEGRADED : HealthStatus.UP));
   }
}
