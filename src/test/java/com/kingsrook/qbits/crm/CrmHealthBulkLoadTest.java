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

package com.kingsrook.qbits.crm;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.bulkload.CrmBulkLoadProfiles;
import com.kingsrook.qbits.crm.core.model.enums.CrmEnrollmentStatus;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qbits.crm.health.CrmHealthChecks;
import com.kingsrook.qbits.crm.scoring.processes.CalculateLeadScoresProcess;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfileMetaDataProvider;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Maintenance health thresholds and host-owned bulk-load profile seeding.
 *******************************************************************************/
class CrmHealthBulkLoadTest extends BaseTest
{
   /*******************************************************************************
    ** No transport or score run needs attention.
    *******************************************************************************/
   @Test
   void noTransportOrScoreRunNeedsAttention() throws Exception
   {
      var checks = new CrmHealthChecks(new CrmQBitConfig());
      var results = checks.run();
      assertThat(results.get("crm.email").getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(results.get("crm.leadScore").getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(results.get("crm.sequenceLag").getStatus()).isEqualTo(HealthStatus.UP);
      checks.indicators().forEach(indicator ->
      {
         try
         {
            assertThat(indicator.check(QContext.getQInstance()).getStatus()).isEqualTo(results.get(indicator.getName()).getStatus());
         }
         catch(Exception e)
         {
            throw (new AssertionError(e));
         }
      });
   }


   /*******************************************************************************
    ** Healthy then late jobs.
    *******************************************************************************/
   @Test
   void healthyThenLateJobs() throws Exception
   {
      CrmEmailTransport transport = new CrmEmailTransport(new CrmQBitConfig())
      {
         @Override
         public CrmReadiness readiness()
         {
            return (CrmReadiness.CORE_MESSAGING);
         }
      };
      CrmHealthChecks checks = new CrmHealthChecks(transport);
      P4TestSupport.seed("crm_job_state", new QRecord().withValue("id", 1).withValue("name", CalculateLeadScoresProcess.NAME)
         .withValue("lastSuccess", Instant.now().minus(2, ChronoUnit.HOURS)));
      P4TestSupport.seed("crm_sequence_enrollment", new QRecord().withValue("id", 1).withValue("status", CrmEnrollmentStatus.ACTIVE.getId())
         .withValue("nextStepDate", Instant.now().minus(29, ChronoUnit.MINUTES)),
         new QRecord().withValue("id", 2).withValue("status", CrmEnrollmentStatus.UNENROLLED.getId())
            .withValue("nextStepDate", Instant.now().minus(2, ChronoUnit.DAYS)));
      assertThat(checks.run().values()).allMatch(result -> result.getStatus() == HealthStatus.UP);
      com.kingsrook.qbits.crm.core.CrmRecords.update("crm_job_state", new QRecord().withValue("id", 1)
         .withValue("lastSuccess", Instant.now().minus(181, ChronoUnit.MINUTES)));
      com.kingsrook.qbits.crm.core.CrmRecords.update("crm_sequence_enrollment", new QRecord().withValue("id", 1)
         .withValue("nextStepDate", Instant.now().minus(31, ChronoUnit.MINUTES)));
      assertThat(checks.run().get("crm.sequenceLag").getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(checks.run().get("crm.leadScore").getStatus()).isEqualTo(HealthStatus.DEGRADED);
   }


   /*******************************************************************************
    ** Health failure reports unknown.
    *******************************************************************************/
   @Test
   void healthFailureReportsUnknown() throws Exception
   {
      CrmHealthChecks checks = new CrmHealthChecks(new CrmEmailTransport(new CrmQBitConfig())
      {
         @Override
         public CrmReadiness readiness()
         {
            throw (new IllegalStateException("unavailable"));
         }
      });
      assertThat(checks.indicators().getFirst().check(QContext.getQInstance()).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
   }


   /*******************************************************************************
    ** Profiles are optional idempotent and valid core mappings.
    *******************************************************************************/
   @Test
   void profilesAreOptionalIdempotentAndValidCoreMappings() throws Exception
   {
      CrmBulkLoadProfiles.ensure();
      assertThat(QContext.getQInstance().getTable("savedBulkLoadProfile")).isNull();
      var table = new SavedBulkLoadProfileMetaDataProvider().defineSavedBulkLoadProfileTable(BACKEND_NAME, ignored ->
      {
         // Host has no additional backend details.
      });
      QContext.getQInstance().addTable(table);
      CrmBulkLoadProfiles.ensure();
      CrmBulkLoadProfiles.ensure();
      var rows = new QueryAction().execute(new QueryInput("savedBulkLoadProfile")).getRecords();
      assertThat(rows).hasSize(3);
      for(QRecord row : rows)
      {
         BulkLoadProfile profile = new ObjectMapper().readValue(row.getValueString("mappingJson"), BulkLoadProfile.class);
         assertThat(profile.getHasHeaderRow()).isTrue();
         assertThat(profile.getLayout()).isEqualTo("FLAT");
         assertThat(profile.getFieldList()).isNotEmpty();
         profile.getFieldList().forEach(field -> assertThat(QContext.getQInstance().getTable(row.getValueString("tableName")).getField(field.getFieldName())).isNotNull());
         if(row.getValueString("tableName").equals("crm_contact"))
         {
            assertThat(profile.getFieldList().stream().map(field -> field.getHeaderName())).containsExactlyElementsOf(
               List.of("First Name", "Last Name", "Email", "Phone", "Company", "Job Title"));
         }
      }
      QRecord first = rows.getFirst();
      com.kingsrook.qbits.crm.core.CrmRecords.update("savedBulkLoadProfile", new QRecord().withValue("id", first.getValue("id"))
         .withValue("mappingJson", "{\"hostEdited\":true}"));
      CrmBulkLoadProfiles.ensure();
      assertThat(com.kingsrook.qqq.backend.core.actions.tables.GetAction.execute("savedBulkLoadProfile", first.getValue("id"))
         .getValueString("mappingJson")).contains("hostEdited");
   }

   /*******************************************************************************
    ** Health middleware invokes checks in worker threads without a caller QContext.
    *******************************************************************************/
   @Test
   void indicatorsCarryMonitoringSessionAndRestoreWorkerContext() throws Exception
   {
      var instance = QContext.getQInstance();
      var indicator = new CrmHealthChecks(new CrmQBitConfig()).indicators(QContext.getQSession()).get(1);
      var status = java.util.concurrent.CompletableFuture.supplyAsync(() ->
      {
         try
         {
            assertThat(QContext.getQInstance()).isNull();
            var result = indicator.check(instance);
            assertThat(QContext.getQInstance()).isNull();
            assertThat(QContext.getQSession()).isNull();
            return (result.getStatus());
         }
         catch(Exception e)
         {
            throw (new AssertionError(e));
         }
      }).get();
      assertThat(status).isEqualTo(HealthStatus.UP);
      assertThat(QContext.getQInstance()).isSameAs(instance);
   }



   /*******************************************************************************
    ** Regression: indicators registered with no caller session (at host startup)
    ** must still run on a host whose authentication rejects tokenless sessions
    ** (OAuth2, Auth0). They used a plain new QSession(), so every check failed
    ** session validation and reported UNKNOWN.
    *******************************************************************************/
   @Test
   void contextlessIndicatorsRunOnTokenRequiredHost() throws Exception
   {
      var instance = QContext.getQInstance();
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData()
         .withName("token-required")
         .withType(QAuthenticationType.MOCK)
         .withModuleCodeReference(new QCodeReference(TokenRequiredAuthenticationModule.class)));
      var status = java.util.concurrent.CompletableFuture.supplyAsync(() ->
      {
         try
         {
            var indicator = new CrmHealthChecks(new CrmQBitConfig()).indicators().get(1);
            return (indicator.check(instance).getStatus());
         }
         catch(Exception e)
         {
            throw (new AssertionError(e));
         }
      }).get();
      assertThat(status).isEqualTo(HealthStatus.UP);
   }

}
