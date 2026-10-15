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
 ** QBitMetaDataProducer for the CRM QBit.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import com.kingsrook.qbits.crm.activities.customizers.ActivityLastDateCustomizer;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.core.customizers.CurrencyRateDateCustomizer;
import com.kingsrook.qbits.crm.core.customizers.FormSubmissionAutoLinkCustomizer;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.Currency;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.deals.customizers.DealInitializationCustomizer;
import com.kingsrook.qbits.crm.deals.customizers.DealProductRecalculationCustomizer;
import com.kingsrook.qbits.crm.deals.customizers.DealStageChangeCustomizer;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.DealProduct;
import com.kingsrook.qbits.crm.email.CrmEmailProcessIdentity;
import com.kingsrook.qbits.crm.scoring.AutoAssignCustomizer;
import com.kingsrook.qbits.crm.security.CrmDataPolicy;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qbits.crm.security.CrmRelationshipValidator;
import com.kingsrook.qbits.crm.widgets.CrmWidgetMetaData;
import com.kingsrook.qqq.backend.core.actions.customizers.MultiCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordOwnership;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** Producer for the CRM QBit. Auto-discovers all entity producers in this
 ** package via the Gen 2 QBitMetaDataProducer pattern.
 *******************************************************************************/
public class CrmQBitProducer implements QBitMetaDataProducer<CrmQBitConfig>
{
   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "crm";
   public static final String VERSION     = loadVersion();

   private CrmQBitConfig qBitConfig;



   /*******************************************************************************
    ** Read the artifact version from the Maven-filtered properties resource.
    *******************************************************************************/
   private static String loadVersion()
   {
      try(InputStream inputStream = CrmQBitProducer.class.getClassLoader().getResourceAsStream("qbit-crm.properties"))
      {
         if(inputStream == null)
         {
            throw new IllegalStateException("Missing qbit-crm.properties; build with Maven to provide the artifact version");
         }
         Properties properties = new Properties();
         properties.load(inputStream);
         String version = properties.getProperty("version");
         if(version == null || version.isBlank() || version.contains("${"))
         {
            throw new IllegalStateException("Missing or unfiltered version in qbit-crm.properties; build with Maven to provide the artifact version");
         }
         return (version);
      }
      catch(IOException e)
      {
         throw new IllegalStateException("Could not read artifact version from qbit-crm.properties", e);
      }
   }



   /*******************************************************************************
    ** Produce the QBitMetaData for the CRM QBit.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return new QBitMetaData()
         .withGroupId(GROUP_ID)
         .withArtifactId(ARTIFACT_ID)
         .withVersion(VERSION)
         .withNamespace(getNamespace())
         .withConfig(getQBitConfig());
   }



   /*******************************************************************************
    ** Post-production actions: register global customizers on tracked entities.
    *******************************************************************************/
   @Override
   public void postProduceActions(MetaDataProducerMultiOutput metaDataProducerMultiOutput, QInstance qInstance) throws QException
   {
      for(QJoinMetaData join : metaDataProducerMultiOutput.getEach(QJoinMetaData.class))
      {
         join.setName(snakeCase(join.getName()));
      }
      CrmDataPolicy.apply(metaDataProducerMultiOutput, qInstance, qBitConfig);
      CrmIntegrations.apply(metaDataProducerMultiOutput, qBitConfig);
      for(QWidgetMetaData widget : metaDataProducerMultiOutput.getEach(QWidgetMetaData.class))
      {
         widget.setName(snakeCase(widget.getName()));
         QCodeReference permissionChecker = widget.getPermissionRules() == null ? null : widget.getPermissionRules().getCustomPermissionChecker();
         widget.setPermissionRules(CrmPermissions.hasAccessRules(CrmPermissions.BASE).withCustomPermissionChecker(permissionChecker));
         widget.setGridColumns(com.kingsrook.qbits.crm.widgets.CrmHomeWidgets.TODAY.contains(widget.getName()) ? 4 : 12);
         if(widget.getDefaultValues() != null && widget.getDefaultValues().get("joinName") instanceof String joinName)
         {
            String normalizedJoinName = snakeCase(joinName);
            widget.withDefaultValue("joinName", normalizedJoinName);
            QJoinMetaData join = metaDataProducerMultiOutput.getEach(QJoinMetaData.class).stream()
               .filter(candidate -> candidate.getName().equals(normalizedJoinName)).findFirst().orElseThrow();
            CrmPresentation.configureChildList(widget, join.getRightTable());
            QTableMetaData parent = metaDataProducerMultiOutput.get(QTableMetaData.class, join.getLeftTable());
            boolean alreadyAttached = parent.getSections() != null && parent.getSections().stream()
               .anyMatch(section -> section.getWidgetName() != null && snakeCase(section.getWidgetName()).equals(widget.getName()));
            if(!alreadyAttached)
            {
               parent.withSection(new QFieldSection(widget.getName(), widget.getLabel(), new QIcon("list"), Tier.T2, List.of())
                  .withWidgetName(widget.getName()));
            }
         }
      }

      metaDataProducerMultiOutput.get(QTableMetaData.class, Contact.TABLE_NAME).withSection(
         new QFieldSection("crm_contact_timeline", "Timeline", new QIcon("history"), Tier.T2, List.of())
            .withWidgetName("crm_contact_timeline"));

      for(QProcessMetaData process : metaDataProducerMultiOutput.getEach(QProcessMetaData.class))
      {
         process.setPermissionRules(CrmPermissions.processRules(process.getName()));
         process.setSourceQBitName(getQBitMetaData().getName());
         Integer seconds = Map.of("crm_process_sequence_steps", 300, "crm_calculate_lead_scores", 3600,
            "crm_sync_email_delivery", 900, "crm_expire_consents", 86400).get(process.getName());
         if(seconds != null && qBitConfig.getSchedulerName() != null && !Boolean.FALSE.equals(qBitConfig.getSchedulesEnabled()))
         {
            process.withSchedule(new QScheduleMetaData().withSchedulerName(qBitConfig.getSchedulerName()).withRepeatSeconds(seconds));
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // iterate tables produced by this QBit and register customizers       //
      /////////////////////////////////////////////////////////////////////////
      for(QTableMetaData table : metaDataProducerMultiOutput.getEach(QTableMetaData.class))
      {
         String tableName = table.getName();
         CrmPresentation.configureTable(table);
         if(tableName.equals(Contact.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.POST_QUERY_RECORD, new QCodeReference(com.kingsrook.qbits.crm.core.customizers.ContactLabelCustomizer.class));
         }
         if(tableName.equals(Deal.TABLE_NAME) || tableName.equals(Activity.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(CrmRelationshipValidator.class));
            addOrComposeCustomizer(table, TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(CrmRelationshipValidator.class));
         }
         table.setPermissionRules(CrmPermissions.tableRules(tableName));
         table.setSourceQBitName(getQBitMetaData().getName());
         if(table.getSections() != null)
         {
            table.getSections().forEach(section ->
            {
               if(section.getWidgetName() != null)
               {
                  section.setWidgetName(snakeCase(section.getWidgetName()));
               }
            });
         }
         if(Boolean.TRUE.equals(qBitConfig.getInferBackendNames()))
         {
            QInstanceEnricher.setInferredFieldBackendNames(table);
         }

         ///////////////////////////////////////////////////////////////
         // wire audit log on tracked entities                        //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Contact.TABLE_NAME) || tableName.equals(Company.TABLE_NAME)
            || tableName.equals(Deal.TABLE_NAME) || tableName.equals(Activity.TABLE_NAME))
         {
            table.withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
            RecordFeaturesMetaData features = RecordFeaturesMetaData.ofOrWithNew(table)
               .withOwnership(new RecordOwnership().withOwnerFieldName("ownerUserId"));
            for(RecordFeature feature : List.of(RecordFeature.TAGS, RecordFeature.COMMENTS, RecordFeature.ATTACHMENTS,
               RecordFeature.FOLLOW, RecordFeature.TIMELINE, RecordFeature.RECENTS, RecordFeature.FAVORITES))
            {
               features.withEnabled(feature, true);
            }
            if(!tableName.equals(Activity.TABLE_NAME))
            {
               features.withEnabled(RecordFeature.TASKS, true).withEnabled(RecordFeature.ASSIGNMENT, true);
            }
         }

         ///////////////////////////////////////////////////////////////
         // wire activity timestamp propagation                       //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Contact.TABLE_NAME) || tableName.equals(Deal.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(AutoAssignCustomizer.class));
         }
         if(tableName.equals(Activity.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ActivityLastDateCustomizer.class));
         }

         ///////////////////////////////////////////////////////////////
         // wire deal stage change and initialization                  //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Deal.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(DealStageChangeCustomizer.class));
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(DealInitializationCustomizer.class));
         }

         ///////////////////////////////////////////////////////////////
         // stamp the exchange-rate date when a rate is set or changed  //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(Currency.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(CurrencyRateDateCustomizer.OnInsert.class));
            addOrComposeCustomizer(table, TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(CurrencyRateDateCustomizer.OnUpdate.class));
         }

         ///////////////////////////////////////////////////////////////
         // wire deal product recalculation                           //
         ///////////////////////////////////////////////////////////////
         ///////////////////////////////////////////////////////////////
         // wire form submission auto-link                            //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(FormSubmission.TABLE_NAME))
         {
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, new QCodeReference(FormSubmissionAutoLinkCustomizer.class));
         }

         ///////////////////////////////////////////////////////////////
         // wire deal product recalculation                           //
         ///////////////////////////////////////////////////////////////
         if(tableName.equals(DealProduct.TABLE_NAME))
         {
            QCodeReference dpRef = new QCodeReference(DealProductRecalculationCustomizer.class);
            addOrComposeCustomizer(table, TableCustomizers.POST_INSERT_RECORD, dpRef);
            addOrComposeCustomizer(table, TableCustomizers.POST_UPDATE_RECORD, dpRef);
            addOrComposeCustomizer(table, TableCustomizers.POST_DELETE_RECORD, dpRef);
         }
      }
   }



   /*******************************************************************************
    ** Add a customizer to a table for a given role. If a customizer already exists
    ** for that role, compose both using MultiCustomizer.
    *******************************************************************************/
   private void addOrComposeCustomizer(QTableMetaData table, TableCustomizers role, QCodeReference newRef)
   {
      java.util.Optional<QCodeReference> existing = table.getCustomizer(role.getRole());

      if(existing.isEmpty())
      {
         table.withCustomizer(role, newRef);
      }
      else
      {
         QCodeReference existingRef = existing.get();

         if(existingRef instanceof QCodeReferenceWithProperties existingMulti
            && MultiCustomizer.class.getName().equals(existingMulti.getName()))
         {
            ///////////////////////////////////////////////////////////////////
            // already a MultiCustomizer -- just add the new ref to it      //
            ///////////////////////////////////////////////////////////////////
            MultiCustomizer.addTableCustomizer(existingMulti, newRef);
         }
         else
         {
            ///////////////////////////////////////////////////////////////////
            // wrap existing + new in a MultiCustomizer                      //
            ///////////////////////////////////////////////////////////////////
            QCodeReferenceWithProperties multiRef = MultiCustomizer.of(existingRef, newRef);
            table.getCustomizers().put(role.getRole(), multiRef);
         }
      }
   }



   /*******************************************************************************
    ** Produce an app section containing all CRM tables for navigation.
    *******************************************************************************/
   public static QAppSection produceAppSection()
   {
      return (CrmAppMetaData.mainSection());
   }



   /*******************************************************************************
    ** Getter for qBitConfig
    *******************************************************************************/
   @Override
   public CrmQBitConfig getQBitConfig()
   {
      return (this.qBitConfig);
   }



   /*******************************************************************************
    ** Setter for qBitConfig
    *******************************************************************************/
   public void setQBitConfig(CrmQBitConfig qBitConfig)
   {
      this.qBitConfig = qBitConfig;
   }



   /*******************************************************************************
    ** Fluent setter for qBitConfig
    *******************************************************************************/
   public CrmQBitProducer withQBitConfig(CrmQBitConfig qBitConfig)
   {
      this.qBitConfig = qBitConfig;
      return (this);
   }

   /*******************************************************************************
    ** Normalize generated metadata names as well as explicit CRM names.
    *******************************************************************************/
   private static String snakeCase(String name)
   {
      return (name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
   }

   /*******************************************************************************
    ** Add widget provenance after the standard package-discovered production.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance qInstance) throws QException
   {
      MetaDataProducerMultiOutput produced = QBitMetaDataProducer.super.produce(qInstance);
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      for(MetaDataProducerOutput item : produced.getEach(MetaDataProducerOutput.class))
      {
         output.add(item instanceof QWidgetMetaData widget
            ? new CrmWidgetMetaData(widget).withSourceQBitName(getQBitMetaData().getName()) : item);
      }
      if(!Boolean.FALSE.equals(qBitConfig.getAppEnabled()))
      {
         output.add(CrmAppMetaData.build(output, getQBitMetaData().getName()));
      }
      output.add(new QAlertTypeMetaData().withName("crm.lead.received").withLabel("New web lead")
         .withDefaultSeverity(AlertSeverity.INFO).withSourceQBitName(getQBitMetaData().getName()));
      output.add(new QAlertTypeMetaData().withName("crm.sequence.failing").withLabel("Sequence needs attention")
         .withSourceQBitName(getQBitMetaData().getName()));
      output.add(new QAlertTypeMetaData().withName("crm.email.unavailable").withLabel("Email unavailable")
         .withSourceQBitName(getQBitMetaData().getName()));
      for(String notification : List.of("crm.lead.assigned", "crm.deal.won", "crm.deal.rotting", "crm.task.due"))
      {
         output.add(new QAlertTypeMetaData().withName(notification).withLabel(notification)
            .withDefaultSeverity(AlertSeverity.INFO).withSourceQBitName(getQBitMetaData().getName()));
      }
      qInstance.withProcessLifecycleListener(new QCodeReference(CrmEmailProcessIdentity.class));
      registerHooks(qInstance);
      // Registration is metadata-only; core startup sync guards hosts without helpContent.
      HelpBundles.register(qInstance, "help/qbit-crm.yaml");
      return (output);
   }

   /*******************************************************************************
    ** Link the route provider only when both optional middleware classes exist.
    *******************************************************************************/
   private static void registerHooks(QInstance instance)
   {
      try
      {
         Class.forName("io.javalin.Javalin");
         Class.forName("com.kingsrook.qqq.middleware.javalin.QJavalinMetaData");
      }
      catch(ClassNotFoundException | LinkageError e)
      {
         return;
      }
      com.kingsrook.qbits.crm.webhook.CrmHookRouteProvider.register(instance);
   }
}
