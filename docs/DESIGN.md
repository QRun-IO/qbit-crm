# QBit CRM: design (0.2.0)

This is the design for qbit-crm 0.2.0, a full-featured CRM for QQQ 4.1 applications. It also
works with **qbit-marketing** and with a host CMS such as the kof22 website (`cms_` tables,
the `/v2/contact` form and newsletter capture). The document records three things:
- the gap audit of 0.1.0
- the target design
- the integration contracts and the delivery phases

0.2.0 is a breaking, fresh-install release: tables are renamed, and several bespoke features are
replaced by platform features. 0.1.0 never went live anywhere (Voyage was a proof of concept), so
there is no upgrade path or compatibility layer.

## Goals

- **The whole sales and relationship workflow in one app:**
  - contacts and companies
  - deals through pipelines
  - logged activities
  - lists and consent
  - web leads with attribution
  - sequences
  - scoring and assignment
  - goals
  - reporting
- **Standard QQQ throughout.** Every record is in a visible `crm_` table with a full record
  screen. Work runs as table and record actions on the standard streamed ETL. The app has one
  real home.
- **Reuse the platform instead of rebuilding it:**
  - email through qbit-email or core messaging
  - tags, comments, attachments, follow and tasks through collaboration
  - notifications through alerts
  - long work as durable jobs
  - imports through bulk load with saved profiles
  - reporting through data analysis
  - PII through secured data
- **Honest consent.** Nobody gets marketing email without a recorded, unexpired Email consent.
  Every suppression is respected and every change is auditable.
- **Integrations through published contracts only.** The `com.kingsrook.qbits.crm.api` package
  is the only thing other qbits and hosts compile against (guardrail G1).

## Gap audit of 0.1.0 (develop @ 547c923)

### What 0.1.0 claims versus what works

| Area | README claim | Actual state | 0.2.0 action |
|---|---|---|---|
| App and nav | CRM section links every table, 14 dashboard widgets | `produceAppSection()` returns an empty section; no app, no dashboard; the widgets are on nothing | One `crm` app, real home, main tables only (see [App](#app-home-and-nav)) |
| Email sending | Send, track opens/clicks/bounces | Nothing is sent. `sendEmail` writes an activity and a `SENT` message row; `fromAddress` is a user id; templates have no merge. Sequence email steps only log an activity | Send through qbit-email `EmailApi` or core `SendMessageAction`; delivery state from qbit-email |
| Scheduled maintenance | Sequences every 5 min, scoring hourly, overdue tasks daily | No schedules exist; nothing runs unless the host adds jobs | Shipped schedules that run as durable jobs |
| Auto-assign | POST_INSERT hook | A standalone process only; never triggered | Insert customizer on contact and deal plus the record action |
| Lead score decay | `DAYS_AGO_GT/LT` operators | Enum values exist, evaluation returns false | Implemented |
| Reminders | Reminder notifications | `fireReminders` clears the flag ("for now") | Collaboration tasks and alerts notifications |
| Import | CSV import with dedup | Needs pre-parsed records; loads every contact into memory | Core bulk load with saved profiles; dedup by unique key and the Find Duplicates action |
| Multi-tenant | Locks on all tables, propagated to children | Locks on contact, company and deal only; no child propagation; widgets and processes query unscoped | Lock on every table (direct or by join chain), configurable key type |
| Permissions | Not stated | No permission rules at all | `crm.*` permission rules on every table and action |
| Webhooks, quick search, workflows | Config namespaces | Dead config, never read | Removed. Search via table search fields; events via ESB |
| GDPR erase | Anonymizes a contact | Leaves PII in email messages, form submissions, list members, activities, consent IP, external mappings | Erase covers every table that holds the contact's PII |
| Audit | Field-level audit | Bespoke `crmAuditLog` on 4 tables, stores PII copies | Core table audit; erase redacts audit details |
| Tags, attachments | Bulk tag management, attachments | Junction tables; attachments are metadata only, with no storage | Collaboration TAGS and ATTACHMENTS (object storage) |
| `bulkConvertForms` | Listed | Does not exist | Convert to Contact is a multi-record action |
| Tests | 73 classes / 271 tests | 81 classes / 296 tests, memory backend only | Add RDBMS (H2) tests for DDL, unique keys and snake_case columns |

These parts are real and are kept, adapted to 0.2.0:
- pipelines and stages with required-field gating and stage history
- deal products with total recalculation
- multi-currency conversion
- merge contacts and merge companies
- sequence timing (business days, send windows, contact time zone)
- the assignment strategies (specific user, round robin, least active)
- scoring operators 1-7
- form-submission auto-linking
- list subscribe and unsubscribe
- the 24 enum possible-value sources
- the three contingent dropdowns from #6

### Against the current platform

| Standard | 0.1.0 | 0.2.0 |
|---|---|---|
| Build | parent 1.4.0, QQQ 0.27.9, Java 17, orb 0.3.8 | parent 2.1.2 (guardrails profile), QQQ 4.1.0-RC.6 (RC.7 when cut), Java 21, `./mvnw`, orb 0.6.9 |
| Table names | `crmContact` (camelCase) | `crm_contact`, backend name = table name, snake_case columns |
| Permissions | none | `crm`, `crm.manage`, `crm.admin` bases (see [Security](#security-tenancy-and-pii)) |
| UI | n/a | `/qqq/v1` only; the one public machine route is under `/qqq/hooks/crm/` (G2, G3) |
| Messaging | own (fake) sending | qbit-email public API (allow-listed by G1) or core `SendMessageAction` |
| Collaboration | bespoke tags, attachments, reminders | core `RecordFeaturesMetaData` (TAGS, COMMENTS, ATTACHMENTS, FOLLOW, TASKS, ASSIGNMENT, TIMELINE) |
| Alerts | none | `QAlerts` producer SPI, `crm.*` alert types |
| Heavy work | synchronous steps over whole tables | durable jobs (`DurableProcessMetaData`), streamed ETL, keyset pages |
| Imports | `importContacts` | core bulk load + saved bulk-load profiles |
| PII | none | `DataClassification.PII` on every personal field; secured-data tiers apply |
| Reporting | 14 bespoke widgets | home widgets for the daily view; trend reports via data analysis (`crm_*.analyze`) |
| Health | none | `crm` health checks (stand-in until core #1199) |
| Help | none | `help/qbit-crm.yaml` bundle (stand-in loader until core #1198) |
| Tenancy | clientId lock on 3 tables | configurable tenant key type on every table; the host maps Authentik claims to keys |

## Data model (33 tables: 6 main, 15 settings, 12 sub-tables)

All names are `crm_` snake_case. Fields stay camelCase in Java and snake_case in the database
(`inferBackendNames`). Each table has a reference DDL in `docs/ddl/postgres.sql` and a
Liquibase changelog in `src/main/resources/db/liquibase/qbit-crm/`.

**Main tables** (in the nav; ESB events, quick search, API):

| Table | Holds |
|---|---|
| `crm_contact` | Person: names, emails, phones, address, title, company, owner, lifecycle stage, lead source, lead score, time zone, `doNotEmail` / `doNotCall`, unsubscribe date, first- and last-touch attribution (UTM source, medium, campaign, landing page, referrer, touch dates), last activity date. |
| `crm_company` | Organization: name, domain, industry, type, size, revenue, parent company, owner, address, last activity date. |
| `crm_deal` | Opportunity: pipeline, stage, amount, currency, base-currency amount, weighted amount, probability, expected close, owner, company, primary contact, priority, win/loss reason, stage entered date, attribution copied from the contact at creation. |
| `crm_activity` | Logged interaction (call, meeting, email, note, sequence step) with type, outcome, direction, times, related contact, company and deal. Future to-dos are collaboration tasks, not activities. |
| `crm_form_submission` | One web form submission: form key and type, name, email, phone, company, message, consent given and its text, UTM fields, landing page, referrer, visitor key (hashed), contact link, conversion status. Raw IP is never stored; only a salted client hash. |
| `crm_email_list` | Mailing list: key, name, description, double opt-in flag, consent wording, active. |

**Settings tables** (Settings section of the nav):

`crm_pipeline`, `crm_product`, `crm_email_template`, `crm_email_sequence`,
`crm_lead_score_rule`, `crm_assignment_rule`, `crm_sales_goal`, `crm_currency`,
`crm_lifecycle_stage`, `crm_lead_source`, `crm_industry`, `crm_activity_type`,
`crm_activity_outcome`, `crm_contact_role`, `crm_win_loss_reason`.

**Sub-tables** (child lists on their parents, not in the nav):

| Table | Parent | Notes |
|---|---|---|
| `crm_pipeline_stage` | pipeline | Probability, rot days, required fields, won/lost flags. |
| `crm_consent_record` | contact | Append-only: type, status, legal basis, source, wording shown, evidence (form submission), recorded and expiry dates. |
| `crm_email_list_member` | list, contact | Status (pending confirmation, subscribed, unsubscribed, bounced), confirmation token hash, dates, source submission. |
| `crm_email_message` | contact, deal | CRM view of a sent email: template, subject, status, the qbit-email message id and idempotency key, sent/opened/clicked/bounced dates. Bodies stay in qbit-email. |
| `crm_sequence_step` | sequence | |
| `crm_sequence_enrollment` | sequence, contact | Status, current step, next step date, failures. |
| `crm_deal_contact` | deal | Role on the deal. |
| `crm_deal_product` | deal | Line items. |
| `crm_deal_stage_history` | deal | Read-only. |
| `crm_activity_participant` | activity | |
| `crm_assignment_rule_member` | assignment rule | |
| `crm_external_mapping` | any | External system ids for sync connectors. |

**Removed:**

| Removed | Replaced by |
|---|---|
| `crmTag`, `crmContactTag`, `crmCompanyTag`, `crmDealTag` | Collaboration TAGS |
| `crmAttachment` | Collaboration ATTACHMENTS (object storage) |
| `crmAuditLog` | Core table audit (`withAuditRules`, FIELD level) |

## App, home and nav

There is one app, `crm` ("CRM"). Its home is the only CRM dashboard. All widgets are standard,
full-width and real-data:

1. **Today** (statistics row):
   - open pipeline (base currency)
   - won this month
   - new contacts (30 days)
   - web leads (7 days)
   - my overdue tasks
2. **Pipeline by stage**: stage, deals, amount and weighted amount. The first pipeline is the
   default; a pipeline picker is added when there is more than one.
3. **Forecast**: expected close by month for the next 6 months, weighted.
4. **Needs attention**: rotting deals (days in stage past the stage's `rotDays`).
5. **Recent web leads**: the last 10 form submissions, with their contact and conversion status.
6. **Goals**: attainment for the current period. It is shown only when goals exist.
7. **Leaderboard**: won amount by owner this quarter.

Every table widget has at most 3 columns, and each column links to its records. On a contact:
- a **Timeline** child widget (activities, messages, submissions, consent, in date order)
- the child lists for deals, lists, consent and enrollments

On a deal:
- stage history
- line items
- contacts

The nav has two parts:
- **Main tables:** contacts, companies, deals, activities, web leads (`crm_form_submission`) and
  lists.
- **Settings:** the settings tables.

Processes are never in the nav. The trend reports from 0.1.0 (win rate, stage duration, sales
cycle, conversion, average deal size) become data-analysis analyses. Deals carry the computed
fields `daysInStage` and `cycleDays`, and the analyses are seeded as saved analyses when
qbit-data-analysis is installed. Without it, they are table views with group-by. They are not
custom widgets.

## Actions

Every action is a table or record action with a plain label. Each runs on
`StreamedETLWithFrontendProcess` with the standard Review and Result summary lines, and has no
custom dry-run forms. Large selections run as durable jobs.

| Table | Actions |
|---|---|
| Contact | Assign Owner, Auto-Assign, Add to List, Remove from List, Enroll in Sequence, Unenroll, Send Email, Log Activity, Recalculate Score, Merge (2+ selected), Find Duplicates, Erase Personal Data (GDPR, `crm.admin`), Record Consent |
| Company | Assign Owner, Merge, Find Duplicates, Log Activity |
| Deal | Move Stage, Mark Won, Mark Lost, Reopen, Clone, Assign Owner, Add Contact, Log Activity |
| Web lead | Convert to Contact (multi-record), Mark Spam |
| List | Send Confirmation Again (pending members) |
| Sequence | Activate, Pause |
| Email message | Refresh Delivery Status |

**Scheduled work**, shipped as `QScheduleMetaData` (the host can turn each one off in config).
Each runs as a durable job:

| Job | Schedule | Work |
|---|---|---|
| Run Sequence Steps | every 5 min | Due enrollments, in pages of 200 |
| Recalculate Lead Scores | hourly | Contacts whose inputs changed since the last run, plus a full pass nightly |
| Sync Email Delivery | every 15 min | qbit-email status for messages sent by the CRM in the last 30 days. Hard bounces and complaints set `doNotEmail` and add a consent record |
| Expire Consents | daily | Expired consents and stale pending confirmations (14 days) |

## Platform reuse

### Email

Email sending goes in this order:
1. **qbit-email, when installed.** `CrmEmailTransport` calls `EmailApi.sendOnce` with:
   - category MARKETING for list and sequence mail, or TRANSACTIONAL for one-to-one mail
   - idempotency key `crm:<purpose>:<id>`
   - a linked record (the contact)

   The transport is the only class that references `com.kingsrook.qbits.email.api`. That
   package is allow-listed by G1, and the dependency is optional. qbit-email owns branding,
   tracking, the transport-level suppression and RFC 8058 one-click unsubscribe.
2. **Core `SendMessageAction`, otherwise.** It uses the host's provider name from config.
3. **Neither.** Send actions are hidden, and the readiness check reports *Not configured*.

Templates are `crm_email_template` rows, rendered by qbit-email's templating when available.
They have merge fields for:
- the contact (`firstName`, `company.name`, ...)
- the owner
- the unsubscribe link

Before every marketing send, the CRM checks eligibility: consent, `doNotEmail`, list status and
unsubscribe date. qbit-email then applies its own suppression.

### Collaboration

The CRM declares `RecordFeaturesMetaData` on contact, company, deal and activity. Features:
- TAGS, COMMENTS, ATTACHMENTS, FOLLOW, TIMELINE, RECENTS and FAVORITES on all four
- TASKS and ASSIGNMENT on contact, company and deal

Ownership is `RecordOwnership.withOwnerFieldName("ownerUserId")`. The record screens and
"My Work" come from qbit-collaboration when it is installed. Without it, the core declarations
are inert.

### Alerts and notifications

| Kind | Name | Fires when |
|---|---|---|
| Alert type | `crm.sequence.failing` | An enrollment fails 3 times |
| Alert type | `crm.email.unavailable` | The transport is not ready while sequences are active |
| Notification | `crm.lead.assigned` | A new web lead is assigned |
| Notification | `crm.deal.rotting` | A deal passes its stage's rot days |
| Notification | `crm.deal.won` | A deal is won |
| Notification | `crm.task.due` | A collaboration task comes due |

Notifications go through `QAlerts.notify`. qbit-alerts then delivers each one to the user's
channels (in-app, email, Slack) per their notification preferences. Without qbit-alerts, the
signals are logged.

### Durable jobs and bulk load

- The four scheduled processes, Recalculate Score, Merge and Erase are `DurableProcessMetaData`
  (eligible). Recalculate and the sequence runner are `alwaysDurable`.
- Imports use core bulk load on contact, company, deal and activity. The host registers
  `SavedBulkLoadProfileMetaDataProvider` once. The CRM ships starter profiles (contacts from a
  generic CSV, companies, deals). Duplicate emails are refused by the `crm_contact` unique key
  (email, tenant) and reported in the bulk-load result.

### Health

`CrmHealthChecks` reports three checks:
- email transport readiness
- sequence lag (the oldest due enrollment more than 30 min late)
- the scoring job's last success time

It is a `STAND-IN(core=QRun-IO/qqq#1199, removeBy=4.1.0-RC.9)` adapter onto the
`qqq-middleware-health` `HealthIndicator` until the central `HealthCheck` SPI ships.

### Help

`src/main/resources/help/qbit-crm.yaml` is in the qqq#1198 bundle format. It has entries for:
- the app
- every table, field, action and possible-value source

`HelpBundleAssertions` checks that it is complete. Until `HelpBundles.register` is in a core RC,
`CrmHelpBundleStandIn` (`STAND-IN(core=QRun-IO/qqq#1198, removeBy=4.1.0-RC.9)`) loads the same
file into `QHelpContent`.

### Contingent dropdowns

These dropdowns are contingent on another field:
- pipeline stage on pipeline (from #6)
- activity outcome on activity type (from #6)
- list-member submission on contact (from #6)
- deal primary contact on company (`SHOW_ALL_UNTIL_SET`)
- deal contact on deal company
- sequence step template on nothing (all templates)
- sales goal pipeline stage on pipeline

Server-side validation (`ValidateContingentPossibleValuesHelper`) is on for stage and outcome.

### API, ESB and search

- **API:** the host's API name and version come from config. When set, main and settings tables
  get `ApiTableMetaDataContainer` entries, and sub-tables are reachable as children. The
  published API matches the admin, and a parity test compares the API field list with the table
  fields.
- **ESB:** main tables carry `EsbTableMetaData` publications to the configured destination, with
  INSERT, UPDATE and DELETE, and classified values excluded.
- **Quick search:** main tables declare `searchFields`, so qbit-quick-search indexes them
  without a compile dependency.

## Security, tenancy and PII

**Permissions** use the full qbit name:

| Permission | Allows |
|---|---|
| `crm.hasAccess`, `crm.read`, `crm.write` | The app; main and sub-tables; everyday actions |
| `crm.manage.*` | Settings tables (including currencies); merge, bulk assign, list and sequence management |
| `crm.admin.*` | Erase personal data and consent overrides |

Each table's standard QQQ suffixes map onto these, through `QPermissionRules` with
`DenyBehavior.HIDDEN`.

**Tenancy (multi-company).** `CrmQBitConfig.withTenantSecurityKeyType("crmTenant")` (off by
default) does three things:
- adds a nullable `tenantKey` column and a `RecordSecurityLock` to every main and settings table
- locks sub-tables by join chain
- tells collaboration to mirror the key type

The host maps Authentik groups or claims to keys in its `AuthentikPolicyClaimMapper` override.
An all-access key is configurable. Widgets, processes and the API query with the session, so
every read is scoped. kof22 is single-tenant and leaves this off. The 0.1.0 `securityFields` and
`recordSecurityLocks` config is replaced.

**PII classification** (`DataClassification.PII`):
- contact: names, emails, phones, address, LinkedIn, time zone
- company: phone and address
- form submission: name, email, phone, message, client hash
- list member: email and name
- email message: addresses and subject
- consent: evidence text
- activity: description

With qbit-secured-data, these fields follow its tiers (masked, revealable or hidden), and
classified values are left out of ESB and AI tool output.

**Erase personal data** (GDPR Art. 17) removes or anonymizes everything that names the contact:
- the contact's PII fields
- form submissions, list memberships and email-message addresses
- activity descriptions
- external mappings
- audit details on the contact's records

It keeps one consent record, "erased on <date>", with no PII. It runs as a durable job and
leaves an alert-free audit line.

## Integration contracts (`com.kingsrook.qbits.crm.api`)

The package is published inside the qbit-crm jar. Hosts and qbits call it in-process, and it is
the only supported surface.

**`CrmApi`**
- `isInstalled(QInstance)`
- `readiness()`

**`CrmIntakeApi`** (for host websites such as kof22)

`submitForm(CrmFormRequest)` returns `CrmFormResult(submissionId, contactId, contactCreated)`.
- **Request fields:**
  - form key and type (CONTACT, NEWSLETTER, DEMO, OTHER)
  - name, email, phone, company, message
  - `marketingConsent` (boolean) and the exact consent wording shown
  - UTM fields, landing page, referrer
  - an optional hashed visitor key and client hash
- **The CRM:**
  - stores the submission
  - links the contact by email, or creates it (lifecycle "Lead", source from UTM or "Website")
  - sets first-touch attribution when it is empty and always sets last-touch attribution
  - logs a "Form submission" activity
  - auto-assigns the contact
  - raises `crm.lead.assigned`
- **Consent:** a contact form with no consent box is recorded as legitimate interest to reply
  only. It never grants marketing email. Marketing consent is recorded only from an explicit
  opt-in, with the wording as evidence.

`subscribe(CrmSubscribeRequest)` takes a list key, email, name, consent wording, source, UTM and
an optional visitor key. It returns PENDING_CONFIRMATION, SUBSCRIBED or ALREADY_SUBSCRIBED.
- With double opt-in (the default for lists), it creates a pending member and sends a
  confirmation email.
- The member's consent is granted only by `confirm(token)`.

`unsubscribe(token)` and `unsubscribeByContact(contactId, listKey-or-null, source)` both record
the unsubscribe and a withdrawn consent.

**`CrmMarketingApi`** (for qbit-marketing; agreed with the marketing agent 2026-10-14)

| Method | Behavior |
|---|---|
| `resolveRecipients(scope)` | Scopes `crm-list:<id>` and `crm-contact:<id>`. Returns (contactId, address only when eligible, eligibility). |
| `checkEmailEligibility(contactId)` | The latest Email consent decides (granted and not expired; none means no consent). Suppressed by `doNotEmail`, an unsubscribe date, or a non-subscribed membership. Returns reason codes, no PII. |
| `listFormSubmissions(utmCampaign, since)` | Ids and times only. |
| `recordActivity(contactId, ActivityKind.NOTE, subject, description)` | The CRM picks the activity type by code, so marketing no longer looks up "Note" by name. |
| `recordUnsubscribe(contactId, listId-or-null, source)` | Records the unsubscribe. |
| `buildUnsubscribeUrl(contactId, listId-or-null)` | A signed, expiring token link to `/qqq/hooks/crm/unsubscribe/{token}` that marketing puts in its footer. The CRM owns list-level unsubscribe; qbit-email's one-click covers its category. |
| `recipientScopeOptions()` | Lists and their labels, for marketing's recipient-scope dropdown. |

**Public route.** `/qqq/hooks/crm/unsubscribe/{token}` (GET shows a confirm page, POST does it)
and `/qqq/hooks/crm/confirm/{token}` (double opt-in). These are the only HTTP routes outside
`/qqq/v1`, and they live in a route provider. Tokens are HMAC-signed with a host secret from
config, expire after 90 days, and are stored only as hashes.

**Attribution from analytics.** Hosts pass the UTM fields they already capture: for kof22, the
`cms_analytics` session's first and last UTM for a consenting visitor, or else the page URL
parameters. Without visitor-identity consent, the CRM stores no visitor key.

## kof22 adoption (Website-Backend)

The adoption changes:
- **Dependency and wiring:** add `qbit-crm` and register it through `WebsiteCrmMetaData.configure`
  from `WebsiteMetaDataProvider`. It uses the CMS backend, with tenancy off.
- **Permissions:** grant them in `AuthentikAuthenticationCustomizer` (admins get `crm.*`).
- **Schema:** a dated Liquibase file, `changesets/features/2026-10-14-qbit-crm.sql`. kof22's
  numbered files stop at 0041, and newer changes use dated files.
- **`/v2/contact`:** keeps its honeypot, timing, validation and rate limits, then calls
  `CrmIntakeApi.submitForm` (form key `kof22-contact`).
  - The notification to the site owner keeps today's delivery path (qbit-email's outbox when it
    is installed, otherwise the existing mailer). It links to the CRM web lead.
  - `cms_contact_message` keeps receiving rows until one release after adoption. Existing rows
    are copied into `crm_form_submission`, and a later cleanup PR drops the table.
- **Newsletter:** new `POST /v2/newsletter/subscribe` (same protections) calls
  `CrmIntakeApi.subscribe` for list `kof22-newsletter`, with double opt-in. The confirm and
  unsubscribe links use the CRM routes. The frontend form is a separate Website-Frontend PR.
- **Marketing:** qbit-marketing on kof22 uses `QbitCrmGateway`, which delegates to
  `CrmMarketingApi`.

## No upgrade path from 0.1.0

0.1.0 never went live. 0.2.0 is a fresh install: there are no rename scripts and no compatibility shims. These 0.1.0 config options are removed:
- `tableNamePrefix`
- `quickSearchQBitNamespace`
- `webhooksQBitNamespace`
- `workflowsQBitNamespace`

`securityFields` and `recordSecurityLocks` are replaced by `withTenantSecurityKeyType` in P4.

## Delivery phases

| Phase | Delivers | Demonstrable result |
|---|---|---|
| P1 Platform | Parent 2.1.2, QQQ 4.1.0-RC.6, Java 21, orb 0.6.9, guardrails; `crm_` rename with snake_case columns; permission rules; reference DDL and Liquibase; H2 tests | `./mvnw verify` green on the new stack |
| P2 Standards | App, home and nav; record actions on streamed ETL; PII classification; contingent dropdowns; API, ESB and search metadata; help bundle; core audit | The CRM app runs in a sample server with the home and record screens |
| P3 Intake and contracts | `crm.api` (`CrmIntakeApi`, `CrmMarketingApi`), consent, double opt-in, the unsubscribe and confirm routes, attribution | A form post creates a contact and lead, and marketing resolves eligibility |
| P4 Platform reuse | Email transport, collaboration features, alerts, durable jobs and schedules, bulk-load profiles, tenancy, health, GDPR erase coverage, scoring decay, auto-assign hook | Sequences send through qbit-email; imports use saved profiles |
| P5 kof22 | Website-Backend draft PR (install, Liquibase, `/v2/contact`, newsletter) | Contact form leads appear in the CRM on a local stack |
| P6 Release | Screenshots (home and records, light and dark, 390 and 1440 px), CHANGELOG, "ready: qbit-crm release" | 0.2.0 RC |

## Owner decisions (2026-10-14): all recommendations accepted (standing approval)

The owner's standing approval (2026-10-12, and repeated for this program on 2026-10-14)
accepts every recommendation above. Specifically:

| # | Decision |
|---|---|
| 1 | 0.2.0 is a breaking, fresh-install release with `crm_` names and no compatibility work for 0.1.0. Voyage was a proof of concept (owner, 2026-10-14). |
| 2 | Tags, attachments, reminders and audit move to collaboration, alerts and core audit. The bespoke tables are removed. |
| 3 | The CRM never sends email itself: it uses qbit-email first, then core messaging. |
| 4 | Lists use double opt-in by default. Contact forms never grant marketing consent without an explicit opt-in. |
| 5 | The CRM owns list-level unsubscribe and the confirm and unsubscribe routes. qbit-email owns transport suppression. |
| 6 | Trend reports move to data analysis. The home keeps the daily-work widgets. |
| 7 | Tenancy is a configurable key type, off by default. kof22 runs single-tenant. |
| 8 | kof22 keeps `cms_contact_message` for one release after adoption, then drops it. |
| 9 | Raw IP addresses are never stored. Only salted client hashes and hashed visitor keys are kept. |
| 10 | Stand-ins for central health (#1199) and help bundles (#1198) are allowed, with `removeBy=4.1.0-RC.9`. |
