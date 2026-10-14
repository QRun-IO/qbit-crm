# QBit CRM

**0.2.0-SNAPSHOT — unreleased, P1 Platform.** CRM tables, processes and widgets for QQQ applications. This phase upgrades the build, names, permissions, audit integration and database schema. It does not complete the later 0.2.0 product phases.

## Build and verification

Use **Java 21** and the included Maven wrapper (**Maven 3.9.11**). The build uses `com.kingsrook:qbit-build-parent:2.1.2` and a default-active `qqq-snapshot` profile importing `qqq-bom-pom:4.1.0-RC.6`.

```sh
./mvnw -q verify
# Windows PowerShell:
./mvnw.cmd -q verify
# If dependencies are already cached and network resolution is unavailable:
./mvnw.cmd -o -q verify
```

Verification runs Checkstyle on production and test sources, architecture guardrails (including strict UI API routes), JUnit tests and JaCoCo 0.8.15. Coverage gates are 70% instructions and 90% classes. The current suite runs **372 tests**, with no failures or skips, including four H2 integration tests. Tests use H2 2.3.232 and AssertJ 3.27.7. CircleCI uses `kingsrook/qqq-orb@0.6.9`.

The H2 tests execute the shipped PostgreSQL DDL in PostgreSQL mode. They verify all 33 tables and 395 columns, the 21 declared unique keys, relationship indexes, seven persisted entity types, duplicate list-member rejection and DDL/changelog parity. They do not replace deployment verification against the host's PostgreSQL installation.

## Install in a host application

1. Build/install this unreleased snapshot with `./mvnw install`, or resolve the same artifact from your configured snapshot repository. The dependency version is explicit; do not assume the QQQ BOM manages this QBit.

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-crm</artifactId>
   <version>0.2.0-SNAPSHOT</version>
</dependency>
```

2. Align the host with QQQ **4.1.0-RC.6**. For a new database, apply [the reference PostgreSQL DDL](docs/ddl/postgres.sql), or include the packaged Liquibase formatted SQL at `db/liquibase/qbit-crm/crm.changelog.sql`. Choose one installation path. The Java producer does not create the schema.

3. Register the host backend and authentication, then produce and add the CRM metadata:

```java
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.CrmQBitProducer;

// The host has already registered the backend named "rdbms".
CrmQBitConfig crmConfig = new CrmQBitConfig()
   .withDefaultBackendNameForTables("rdbms");

new CrmQBitProducer()
   .withQBitConfig(crmConfig)
   .produce(qInstance)
   .addSelfToInstance(qInstance);

qInstance.setEnforcePermissionsInCoreActions(true);
```

4. Map the host's authenticated roles to the permissions below. Configure the host's core audit storage. Add tables, processes and widgets to the host UI as needed: `produceAppSection()` currently returns an empty CRM section; P1 does not provide an assembled app or home dashboard.

## Configuration and access

- `defaultBackendNameForTables`: required backend name.
- `inferBackendNames`: defaults to true; sets snake_case SQL column names while Java/QQQ field names remain camelCase. SQL table names equal the QQQ table names. Set false only for an explicit host column mapping.
- `tableMetaDataCustomizer`: optional host table customizer.
- `securityFields` and `recordSecurityLocks`: retained in P1, applied to contact, company and deal. Host-managed schema columns must match these fields. This is not yet tenant isolation across every child table or workflow.

| Scope | Permissions |
|---|---|
| Main and sub-tables | `crm.read`, `crm.write` |
| Settings tables except currency | `crm.manage.read`, `crm.manage.write` |
| Currency | `crm.admin.read`, `crm.admin.write` |
| Widgets and everyday processes | `crm.hasAccess` |
| Merge, bulk assignment, list and sequence management processes | `crm.manage.hasAccess` |
| GDPR anonymization | `crm.admin.hasAccess` |

Permissions are independent grants. Every CRM-produced table, process and widget has explicit `QPermissionRules` with `DenyBehavior.HIDDEN` and QBit provenance. `security.CrmPermissions.ALL` lists the grants for host administration and tests. The widget provenance adapter supplies core's `SourceQBitAware` interface because RC.6's widget class lacks it.

## Tables

All 33 table names and table-backed possible-value sources use `crm_` snake_case. The 23 enum possible-value sources retain their enum names. Relationship columns have indexes; the reference DDL does not impose foreign-key constraints or host-specific tenant columns. Unique constraints match the current QQQ metadata.

### Main tables (6)

`crm_contact`, `crm_company`, `crm_deal`, `crm_activity`, `crm_form_submission`, `crm_email_list`.

### Settings tables (15)

`crm_pipeline`, `crm_product`, `crm_email_template`, `crm_email_sequence`, `crm_lead_score_rule`, `crm_assignment_rule`, `crm_sales_goal`, `crm_currency`, `crm_lifecycle_stage`, `crm_lead_source`, `crm_industry`, `crm_activity_type`, `crm_activity_outcome`, `crm_contact_role`, `crm_win_loss_reason`.

### Sub-tables (12)

`crm_pipeline_stage`, `crm_consent_record`, `crm_email_list_member`, `crm_email_message`, `crm_sequence_step`, `crm_sequence_enrollment`, `crm_deal_contact`, `crm_deal_product`, `crm_deal_stage_history`, `crm_activity_participant`, `crm_assignment_rule_member`, `crm_external_mapping`.

Contact, company, deal and activity declare core FIELD-level audit rules. The bespoke tag tables, attachment table, audit-log table and bulk tag processes have been removed. 0.2.0 is a fresh-install release with no upgrade path from 0.1.0. Collaboration replacements arrive in a later phase.

## Processes

The QBit produces 25 processes with these names and labels. QQQ may additionally generate standard table actions such as `<table>.bulkInsert`; those retain core's naming conventions. CRM processes are available to host wiring and are not registered as scheduled jobs.

| Process name | Label |
|---|---|
| `crm_auto_assign` | Auto Assign |
| `crm_bulk_assign_owner` | Bulk Assign Owner |
| `crm_calculate_lead_scores` | Calculate Lead Scores |
| `crm_clone_deal` | Clone Deal |
| `crm_convert_form_to_contact` | Convert Form to Contact |
| `crm_convert_lead` | Convert Lead |
| `crm_detect_duplicates` | Detect Duplicates |
| `crm_enroll_in_sequence` | Enroll in Sequence |
| `crm_fire_reminders` | Fire Reminders |
| `crm_gdpr_anonymize_contact` | GDPR Anonymize Contact |
| `crm_import_contacts` | Import Contacts |
| `crm_log_activity` | Log Activity |
| `crm_manage_contact_on_deal` | Manage Contact on Deal |
| `crm_merge_companies` | Merge Companies |
| `crm_merge_contacts` | Merge Contacts |
| `crm_move_deal_stage` | Move Deal Stage |
| `crm_process_overdue_tasks` | Process Overdue Tasks |
| `crm_process_sequence_steps` | Process Sequence Steps |
| `crm_reopen_deal` | Reopen Deal |
| `crm_send_email` | Send Email |
| `crm_subscribe_to_list` | Subscribe to List |
| `crm_track_email_event` | Track Email Event |
| `crm_transfer_ownership` | Transfer Ownership |
| `crm_unenroll_from_sequence` | Unenroll from Sequence |
| `crm_unsubscribe_from_list` | Unsubscribe from List |

## Current behavior and limits

The existing pipeline/stage logic, stage history, product totals, currency conversion, contact/company merge, activity logging, form auto-linking, list subscribe/unsubscribe, scoring and assignment processes remain. Fourteen custom dashboard widget producers remain available for host placement, together with generated child-record widgets.

Some process names describe unfinished behavior inherited from 0.1.0:

- Send Email and sequence email steps record CRM activity/message state; they do not deliver email. Template merging and live delivery tracking are not implemented here.
- Sequence execution, lead scoring and overdue-task work require host invocation. No schedules are shipped. Assignment is a process, not an automatic insert hook; reminders do not deliver notifications.
- Scoring's date-decay operators remain unimplemented. Import Contacts expects pre-parsed input and does not provide the planned streamed bulk-load workflow.
- GDPR anonymization scrubs the contact, unenrolls sequences and records withdrawn consent. It does not yet erase all linked PII or core audit details. The P1 schema retains existing PII fields; classification, complete erasure and redesigned tenancy are later work.

The later intake APIs, public subscription routes, collaboration integration, real email transport, durable jobs and full app navigation are not part of this phase.

## Source layout

`CrmQBitProducer` discovers entity, process and widget producers. `core/` holds contacts, companies, forms, lists and consent; `deals/`, `activities/`, `email/` and `scoring/` hold their respective metadata and behavior. `security/` defines permissions, `sync/` holds external mappings and currency utilities, and `widgets/` contains widget producers/renderers. SQL is shipped under `src/main/resources/db/liquibase/qbit-crm/`.

Licensed under [Apache License 2.0](LICENSE).
