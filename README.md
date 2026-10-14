# QBit CRM

**0.2.0-SNAPSHOT — unreleased, P1 Platform + P2 App, home and nav.** CRM tables, processes and widgets for QQQ applications. This work upgrades the build, names, permissions, audit integration and database schema, and supplies the CRM app and record navigation. It does not complete the later 0.2.0 product phases.

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

4. Map the host's authenticated roles to the permissions below. Configure the host's core audit storage. The `crm` app is enabled by default, with main and Settings navigation and a real-data home. Set `withAppEnabled(false)` to embed the tables in the host's own app. `produceAppSection()` returns the same populated main section; processes are record/table actions, never navigation entries.

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

Permissions are independent grants. Every CRM-produced table, process and widget has explicit `QPermissionRules` with `DenyBehavior.HIDDEN` and QBit provenance. `security.CrmPermissions.ALL` lists the grants for host administration and tests. The app and widget provenance adapters supply core's `SourceQBitAware` interface because RC.6's app and widget classes lack it.

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

The QBit produces 28 table-bound record actions. Every action uses the standard
streamed ETL Review and Result screens with linked summary lines. Required inputs
are collected before Review. Processes never appear in app navigation. Find
Duplicates scans the selected records (select all matching records to scan the
table); each duplicate group links to its members. Merges require exactly two
selected records and a choice of which record to keep.

| Table | Process name | Label |
|---|---|---|
| `crm_contact` | `crm_bulk_assign_owner` | Assign Owner |
| `crm_contact` | `crm_auto_assign` | Auto-Assign |
| `crm_contact` | `crm_subscribe_to_list` | Add to List |
| `crm_contact` | `crm_unsubscribe_from_list` | Remove from List |
| `crm_contact` | `crm_enroll_in_sequence` | Enroll in Sequence |
| `crm_contact` | `crm_unenroll_from_sequence` | Unenroll from Sequence |
| `crm_contact` | `crm_log_activity` | Log Activity |
| `crm_contact` | `crm_recalculate_score` | Recalculate Score |
| `crm_contact` | `crm_merge_contacts` | Merge Contacts |
| `crm_contact` | `crm_detect_duplicates` | Find Duplicates |
| `crm_contact` | `crm_convert_lead` | Create Deal |
| `crm_contact` | `crm_gdpr_anonymize_contact` | Erase Personal Data |
| `crm_contact` | `crm_send_email` | Send Email |
| `crm_company` | `crm_assign_company_owner` | Assign Owner |
| `crm_company` | `crm_merge_companies` | Merge Companies |
| `crm_company` | `crm_find_duplicate_companies` | Find Duplicates |
| `crm_company` | `crm_log_company_activity` | Log Activity |
| `crm_deal` | `crm_move_deal_stage` | Move Stage |
| `crm_deal` | `crm_mark_deal_won` | Mark Won |
| `crm_deal` | `crm_mark_deal_lost` | Mark Lost |
| `crm_deal` | `crm_reopen_deal` | Reopen |
| `crm_deal` | `crm_clone_deal` | Clone |
| `crm_deal` | `crm_transfer_ownership` | Assign Owner |
| `crm_deal` | `crm_manage_contact_on_deal` | Add Contact |
| `crm_deal` | `crm_log_deal_activity` | Log Activity |
| `crm_form_submission` | `crm_convert_form_to_contact` | Convert to Contact |
| `crm_email_sequence` | `crm_activate_sequence` | Activate |
| `crm_email_sequence` | `crm_pause_sequence` | Pause |

Move Stage and Reopen choose a stage from the selected deals' pipeline. Select
one pipeline at a time for those actions. Mark Won and Mark Lost require a
win/loss reason and resolve the terminal stage within each deal's pipeline.

Two hidden background processes have no table binding: `crm_process_sequence_steps`
and `crm_calculate_lead_scores`. Both use id keyset pages of 200. They have no
shipped schedules yet; the host can invoke them. QQQ also generates standard
bulk-load and bulk-edit actions such as `<table>.bulkInsert`.

Fire Reminders and Process Overdue Tasks were removed in favor of collaboration
tasks and alerts. Import Contacts was removed in favor of core bulk load. Track
Email Event was removed in favor of the later delivery sync.

## Current behavior and limits

The existing pipeline/stage logic, stage history, product totals, currency conversion, contact/company merge, activity logging, form auto-linking, list subscribe/unsubscribe, scoring and assignment processes remain. The CRM home contains Today (five standard statistics), Pipeline by stage, Forecast (current month plus five), Needs attention (ten oldest rotting deals), Recent web leads (latest ten), Goals (up to fifty current goals, hidden when none are visible), and Leaderboard (quarter-to-date won revenue by owner). Reporting boundaries use UTC. Monetary summaries use base currency, with weighted amounts grouped by currency before conversion. Aggregate rows link to their underlying records; stage name and deal count share a column to keep tables to three columns. The pipeline picker appears when multiple active pipelines are visible.

The contact Timeline shows the latest fifty recorded events from activities, email messages, form submissions and consent, newest first by creation date. Native child lists connect all specified parent screens. Deal primary-contact and email-message contact references are included in the fresh-install schema. The trend widgets, redundant recent-deals/activity-count widgets and activity feed have been retired.

Later phases complete the following behavior:

- Send Email is a non-sending action shell. It checks for `CrmEmailTransport` with `Class.forName` and reports **Email sending arrives with the email transport** when absent. It never writes a fake sent message or activity. If the class is present, the shell reports that action integration is still pending. Sequence email steps retain the existing activity-only behavior until transport integration.
- Sequence execution and lead scoring require host invocation. No schedules are shipped. Assignment remains an action; automatic insert hooks, collaboration and alert integration arrive later.
- Activate/Pause update the sequence active flag. The existing sequence runner's business logic is preserved in P2B and does not yet consult that flag; enforcing it during execution is a follow-up.
- Scoring's date-decay operators remain unimplemented. Core bulk load is the import path; saved CRM import profiles arrive later.
- GDPR anonymization scrubs the contact, unenrolls sequences and records withdrawn consent. It does not yet erase all linked PII or core audit details. The P1 schema retains existing PII fields; classification, complete erasure and redesigned tenancy are later work.

The later intake APIs, public subscription routes, collaboration integration, real email transport, durable jobs and the remaining P2 security/integration work are not part of this phase.

## Source layout

`CrmQBitProducer` discovers entity, process and widget producers. `core/` holds contacts, companies, forms, lists and consent; `deals/`, `activities/`, `email/` and `scoring/` hold their respective metadata and behavior. `actions/` defines the streamed record actions and their inputs/summaries. `security/` defines permissions, `sync/` holds external mappings and currency utilities, and `widgets/` contains widget producers/renderers. SQL is shipped under `src/main/resources/db/liquibase/qbit-crm/`.

Licensed under [Apache License 2.0](LICENSE).

## Integrating: intake and marketing APIs

Use `com.kingsrook.qbits.crm.api` from a host with an initialized `QContext`.
The API enforces the current session's CRM permissions and record locks; it does
not elevate website callers. The host supplies authentication, tenant scope and
abuse protection before invoking intake. First use initializes the six activity
codes through normal table actions and needs CRM settings write access.

Configure the signing secret from your secret store and the externally reachable
base URL. Never put the secret in source control:

```java
CrmQBitConfig config = new CrmQBitConfig()
   .withDefaultBackendNameForTables("main")
   .withTokenSecret(secretFromHost)
   .withPublicBaseUrl("https://example.com")
   .withMessagingProviderName("email"); // core fallback when qbit-email is absent
```

`CrmApi.isInstalled(instance)` checks installation. `CrmApi.readiness()` returns
`NOT_CONFIGURED`, `QBIT_EMAIL` or `CORE_MESSAGING`. qbit-email 0.1.0-RC.3 is an
optional dependency, preferred when installed; its own readiness and suppression
rules apply. Core fallback uses the named host messaging provider. The core path
has no durable send deduplication; qbit-email uses `sendOnce` with
`crm:<purpose>:<id>`. A refreshed confirmation uses a new token hash in that id.

```java
CrmIntakeApi intake = new CrmIntakeApi();
CrmFormResult lead = intake.submitForm(new CrmFormRequest(
   "contact", FormType.CONTACT, "Ada", "Lovelace", "ada@example.com",
   null, "Example", "Please contact me", false, "Reply to my inquiry",
   "search", "organic", "launch", null, null,
   "https://example.com/contact", null, null, null));

CrmSubscribeResult signup = intake.subscribe(new CrmSubscribeRequest(
   "newsletter", "ada@example.com", "Ada", "Lovelace",
   "Send me the monthly newsletter", "Website",
   "search", "organic", "launch", null, null, null, null));
```

Create an active `crm_email_list` with its required unique `listKey` before
subscribing. Double opt-in defaults to true. Pending members receive a signed
confirmation link and become eligible only after confirmation. Requests retain
the exact consent wording and submission evidence. Contact forms without an
explicit opt-in record **Reply to inquiry / Legitimate interest**, never Email
consent. Optional visitor/client identifiers must be precomputed SHA-256 hex
hashes; salt the client hash in the host. Raw IPs and user-agent strings have no
storage fields. Missing subscriber names use the label "Not provided" because
the CRM contact model requires both names.

```java
CrmMarketingApi marketing = new CrmMarketingApi();
List<RecipientScopeOption> scopes = marketing.recipientScopeOptions();
List<CrmRecipient> recipients = marketing.resolveRecipients("crm-list:1");
Eligibility eligibility = marketing.checkEmailEligibility(lead.contactId());
marketing.recordActivity(lead.contactId(), ActivityKind.NOTE, "Qualified lead", "Requested a demo");
String unsubscribeUrl = marketing.buildUnsubscribeUrl(lead.contactId(), null);
marketing.recordUnsubscribe(lead.contactId(), null, "Customer request");
```

Recipient scopes are `crm-list:<id>` and `crm-contact:<id>`. Addresses are returned
only when eligible. The latest Email consent must be granted and unexpired;
`doNotEmail`, a global unsubscribe date, or any non-subscribed membership
suppresses delivery. List-level withdrawal also appends a contact-level Email
withdrawal, so it suppresses marketing until subsequent explicit consent; it
does not set the contact's global unsubscribe date. The API never silently clears
that global suppression. `listFormSubmissions(campaign, since)` returns submission
and contact IDs plus timestamps, without form contents.

When Javalin is present, CRM registers GET and POST under
`/qqq/hooks/crm/confirm/{token}` and `/qqq/hooks/crm/unsubscribe/{token}`.
GET displays an accessible confirmation button; POST performs the operation.
Links expire after 90 days. Confirmation tokens are stored only as SHA-256 hashes;
refreshing or withdrawing a pending subscription invalidates its previous token.
The signed route uses a system session solely for the IDs authorized by that link.
Direct callers can also use `confirm(token)`, `unsubscribe(token)` and
`unsubscribeByContact(contactId, listIdOrNull, source)`. Pages contain no personal
data and responses disable caching and referrer forwarding.
