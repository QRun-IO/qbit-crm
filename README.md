# QBit: CRM

[![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)](https://github.com/QRun-IO/qbit-crm)
[![License](https://img.shields.io/badge/license-GNU%20Affero%20GPL%20v3-green.svg)](https://www.gnu.org/licenses/agpl-3.0.en.html)
[![Java](https://img.shields.io/badge/java-17+-blue.svg)](https://adoptium.net/)

> **Customer Relationship Management for QQQ Applications - Activity-Centric, Pipeline-Driven**

This QBit provides a complete CRM system for QQQ applications, covering contact and company management, deal pipeline tracking, activity logging, email tracking, product catalog, lead scoring, assignment automation, website form capture, mailing list subscriptions, and reporting.

## Core Capabilities

- **Contact & Company Management**: Full lifecycle tracking with tagging, segmentation, and duplicate detection
- **Deal Pipeline**: Configurable multi-pipeline stages with probability weighting, rot/aging detection, and stage-gating rules
- **Activity Tracking**: Unified activity table for calls, emails, meetings, notes, and tasks with per-record timelines
- **Email Integration**: Tracked email sending, open/click/bounce detection, templates with merge fields
- **Email Sequences**: Multi-step automated outreach with configurable send windows and failure handling
- **Lead Scoring**: Rule-based scoring with dot-notation field paths and time-decay operators
- **Assignment Automation**: Round-robin, least-active, and rule-based auto-assignment
- **Website Forms**: Capture web form submissions with UTM tracking and lead conversion
- **Mailing Lists**: Newsletter and subscription management independent of sales sequences
- **GDPR Compliance**: Consent tracking, right-to-erasure via anonymization (not deletion)
- **Multi-Currency**: Exchange rate table with converted amounts for accurate pipeline reporting
- **Immutable Audit Log**: Field-level change tracking with anonymization support for GDPR
- **14 Dashboard Widgets**: Pipeline summary, forecast, leaderboard, conversion rates, win rate, activity feed, and more

## Open Source & Full Control

QBit CRM is 100% open source under AGPL v3. All data stays in your systems. No external CRM services required.

## Architecture

### Design Principles

1. **Activity-centric**: A unified `crm_activity` table is the heart of the system. Every interaction (calls, emails, meetings, notes, tasks) is an activity linked to contacts, companies, and deals. One table to query, one timeline per record.

2. **Pipeline-driven**: Deals move through configurable pipeline stages. Each stage has probability weighting, rot thresholds, and optional required-field gating. Stage transitions are first-class events captured in `crm_deal_stage_history` for reliable analytics.

3. **Perpetual activity log**: The `crm_audit_log` table is immutable and append-only. Every meaningful change is recorded with field-level old/new values. For GDPR, PII is anonymized in-place rather than deleted.

### Technology Stack

- **Java 17+** with QQQ backend modules
- **QQQ Framework**: Entities, processes, widgets, permissions, API layer
- **Database**: RDBMS through QQQ's backend abstraction
- **MemoryRecordStore**: Full test suite runs in-memory (no database required)

### Module Organization

```
qbit-crm/
  src/main/java/com/kingsrook/qbits/crm/
    CrmQBitConfig.java            -- Configuration (backend, security, companion QBits)
    CrmQBitProducer.java          -- Gen 2 QBitMetaDataProducer (auto-discovers all entities)
    core/                         -- Contacts, Companies, Tags, Attachments, Forms, Email Lists
    deals/                        -- Deals, Pipelines, Stages, Products, Line Items, Stage History
    activities/                   -- Activities, Activity Types, Outcomes, Participants
    email/                        -- Email Messages, Templates, Sequences, Enrollments
    scoring/                      -- Lead Score Rules, Assignment Rules, Sales Goals
    audit/                        -- Immutable audit log + CrmAuditLogCustomizer
    sync/                         -- External system mapping, Currency management
    widgets/                      -- 14 dashboard widgets (producers + renderers)
```

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **QQQ Application** (this is a QBit, not a standalone application)

### Usage

#### Maven dependency

```xml
<dependency>
    <groupId>com.kingsrook.qbits</groupId>
    <artifactId>qbit-crm</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### Minimal setup

```java
CrmQBitConfig config = new CrmQBitConfig()
   .withDefaultBackendNameForTables("yourBackendName");

CrmQBitProducer producer = new CrmQBitProducer()
   .withQBitConfig(config);

MetaDataProducerMultiOutput output = producer.produce(qInstance);
output.addSelfToInstance(qInstance);
```

#### With multi-tenant security

```java
CrmQBitConfig config = new CrmQBitConfig()
   .withDefaultBackendNameForTables("yourBackendName")
   .withRecordSecurityLocks(List.of(
      new RecordSecurityLock()
         .withFieldName("clientId")
         .withSecurityKeyType("crmClientAccess")
         .withNullValueBehavior(NullValueBehavior.DENY)
   ));
```

#### With companion QBits

```java
CrmQBitConfig config = new CrmQBitConfig()
   .withDefaultBackendNameForTables("yourBackendName")
   .withQuickSearchQBitNamespace("quickSearch")
   .withWebhooksQBitNamespace("webhooks")
   .withWorkflowsQBitNamespace("workflows");
```

#### Add CRM navigation section to your app

```java
QAppMetaData app = new QAppMetaData()
   .withName("myApp")
   .withSections(List.of(
      CrmQBitProducer.produceAppSection()
   ));
```

## Data Model

### Tables (39 total)

#### Core (Phase 1a)

| Table | Description |
|-------|-------------|
| `crmContact` | People with lifecycle stages, lead scoring, consent tracking |
| `crmCompany` | Organizations with industry, type, parent-child hierarchy |
| `crmLifecycleStage` | Configurable contact lifecycle (Lead, MQL, SQL, Customer, etc.) |
| `crmLeadSource` | How contacts were acquired (Website, Referral, etc.) |
| `crmIndustry` | Industry classification for companies |
| `crmTag` | Free-form tags for categorizing records |
| `crmContactTag` / `crmCompanyTag` | Tag junction tables |
| `crmAttachment` | File attachments on any entity (polymorphic) |
| `crmConsentRecord` | GDPR consent tracking (append-only) |
| `crmFormSubmission` | Website form capture with UTM parameters |
| `crmEmailList` | Marketing mailing lists |
| `crmEmailListMember` | List subscribers (standalone or linked to contacts) |
| `crmCurrency` | Exchange rates for multi-currency support |
| `crmAuditLog` | Immutable field-level change tracking |

#### Activities

| Table | Description |
|-------|-------------|
| `crmActivity` | Unified table for calls, emails, meetings, notes, tasks |
| `crmActivityType` | Configurable activity types with icons |
| `crmActivityOutcome` | Call/meeting outcomes (Connected, No Answer, etc.) |
| `crmActivityParticipant` | Additional participants on activities |

#### Deals & Pipeline (Phase 1b)

| Table | Description |
|-------|-------------|
| `crmPipeline` | Pipeline definitions (multiple pipelines supported) |
| `crmPipelineStage` | Stages with probability, rot days, required-field gating |
| `crmDeal` | Deals with multi-currency, weighted amounts, probability override |
| `crmDealContact` | Contact-to-deal junction with roles and primary flag |
| `crmContactRole` | Roles contacts play on deals (Decision Maker, etc.) |
| `crmWinLossReason` | Win/loss reasons for closed deals |
| `crmDealTag` | Deal tag junction |
| `crmDealStageHistory` | Stage transition log for pipeline analytics |

#### Email (Phase 2)

| Table | Description |
|-------|-------------|
| `crmEmailTemplate` | Reusable templates with merge field support |
| `crmEmailMessage` | Tracked emails with open/click/bounce/spam detection |

#### Products (Phase 2)

| Table | Description |
|-------|-------------|
| `crmProduct` | Product catalog with SKU and pricing |
| `crmDealProduct` | Line items on deals with quantity, discount, calculated totals |

#### Sequences & Scoring (Phase 3)

| Table | Description |
|-------|-------------|
| `crmEmailSequence` | Multi-step outreach sequences with send windows |
| `crmSequenceStep` | Individual steps (email, call, task, wait) |
| `crmSequenceEnrollment` | Contact enrollment tracking with failure handling |
| `crmLeadScoreRule` | Scoring rules with dot-notation field paths |
| `crmAssignmentRule` | Auto-assignment rules (round-robin, least-active, specific user) |
| `crmAssignmentRuleMember` | Users in assignment pools |
| `crmSalesGoal` | Revenue quotas per user per period |

#### Sync (Phase 2)

| Table | Description |
|-------|-------------|
| `crmExternalMapping` | Bidirectional sync mapping for external systems |

### Enum PossibleValueSources (24)

CrmEntityType, CrmCompanyType, CrmDealPriority, CrmActivityPriority, CrmDirection, CrmBounceType, CrmWinLossType, CrmParticipantRole, CrmSequenceStepType, CrmEnrollmentStatus, CrmEmailStatus, CrmScorableEntity, CrmScoreOperator, CrmAssignableEntity, CrmAssignStrategy, CrmConsentType, CrmConsentStatus, CrmLegalBasis, CrmGoalPeriod, CrmSyncStatus, CrmAuditAction, CrmFormType, CrmFormConversionStatus, CrmListMemberStatus

## Processes (27)

### Core Operations

| Process | Description |
|---------|-------------|
| `logActivity` | Quick-log a call, meeting, note, or task from any record |
| `convertFormToContact` | Convert website form submission to CRM contact |
| `bulkConvertForms` | Batch convert multiple form submissions |
| `subscribeToList` / `unsubscribeFromList` | Mailing list management |
| `bulkAssignOwner` | Reassign record ownership in bulk |
| `bulkAddTag` / `bulkRemoveTag` | Tag management in bulk |
| `transferOwnership` | Transfer all records from one user to another |

### Deal Operations

| Process | Description |
|---------|-------------|
| `moveDealStage` | Move deal to new stage with required-field validation |
| `reopenDeal` | Reopen a closed deal to an active stage |
| `convertLead` | Convert contact to deal (atomic: lifecycle + deal + junction) |
| `manageContactOnDeal` | Add/remove contacts with role and primary flag enforcement |
| `cloneDeal` | Duplicate deal with contacts, products, and tags |

### Email & Sequences

| Process | Description |
|---------|-------------|
| `sendEmail` | Compose and send tracked email with template resolution |
| `trackEmailEvent` | Webhook callback for open/click/bounce/spam events |
| `enrollInSequence` | Enroll contacts in multi-step sequences |
| `processSequenceSteps` | Scheduled: execute pending sequence steps (5 min interval) |
| `unenrollFromSequence` | Remove contact from active sequence |

### Scoring & Assignment

| Process | Description |
|---------|-------------|
| `calculateLeadScores` | Scheduled daily: recalculate scores from rules |
| `autoAssign` | POST_INSERT hook: first-match rule-based assignment |

### Data Management

| Process | Description |
|---------|-------------|
| `mergeContacts` / `mergeCompanies` | Merge duplicates with unique-key conflict resolution |
| `importContacts` | CSV import with validation and duplicate detection |
| `detectDuplicates` | Find duplicate contacts by email or companies by domain |
| `gdprAnonymizeContact` | Right-to-erasure: anonymize PII, scrub audit log, delete attachments |

### Scheduled Maintenance

| Process | Description |
|---------|-------------|
| `processOverdueTasks` | Hourly: detect overdue task activities |
| `fireReminders` | Every 5 min: fire and clear activity reminders |

## Widgets (14)

| Widget | Type | Description |
|--------|------|-------------|
| `crmPipelineSummary` | Table | Deal count and amount per stage |
| `crmActivityFeed` | Table | Most recent activities across all entities |
| `crmDealForecast` | Table | Weighted pipeline by expected close month |
| `crmRottingDeals` | Table | Deals exceeding stage rot threshold |
| `crmContactTimeline` | Stepper | Per-contact activity timeline |
| `crmActivityCount` | Statistics | Activity counts by type (today/week/month) |
| `crmRecentDeals` | Table | Recently won/lost deals |
| `crmSalesLeaderboard` | Table | Users ranked by closed-won revenue |
| `crmQuotaAttainment` | Statistics | Won revenue vs. sales goal target |
| `crmPipelineConversion` | Table | Stage-to-stage conversion rates |
| `crmStageDuration` | Table | Average days per pipeline stage |
| `crmWinRate` | Table | Win rate trend over time |
| `crmAvgDealSize` | Table | Average won deal size |
| `crmSalesCycleLength` | Table | Average days from creation to close |

## Companion QBit Integration

### qbit-quick-search (ElasticSearch)

Index `crmContact`, `crmCompany`, and `crmDeal` for full-text global search.

### qbit-webhooks

Supported event types:

| Event | Fires When |
|-------|------------|
| `crm.contact.created` / `crm.contact.updated` | Contact inserted or updated |
| `crm.company.created` / `crm.company.updated` | Company inserted or updated |
| `crm.deal.created` / `crm.deal.stageChanged` | Deal created or stage changed |
| `crm.deal.won` / `crm.deal.lost` | Deal moved to terminal stage |
| `crm.activity.created` | Activity logged |
| `crm.consent.changed` | Consent granted or withdrawn |
| `crm.form.submitted` / `crm.form.converted` | Form submitted or converted to contact |

### qbit-workflows

User-defined automation beyond built-in scoring/assignment. The CRM does not re-implement a workflow engine.

## Phased Delivery

| Phase | Tables | Processes | Widgets | Focus |
|-------|--------|-----------|---------|-------|
| 1a | 18 | 7 | 6 | Core + Website Integration |
| 1b | 8 | 4 | 3 | Deals + Pipeline |
| 2 | 5 | 7 | 1 | Email, Products, Scheduling |
| 3 | 7 | 5 | 2 | Sequences, Scoring, Automation |
| 4 | 1 | 7 | 5 | Data Management, Analytics, GDPR |
| **Total** | **39** | **30** | **14** | |

## Testing

```bash
mvn test                    # Run all 271 tests
mvn test -Dtest=BaseTest    # Verify QBit produces metadata
```

### Coverage

- **91.5% instruction coverage** (Jacoco)
- **73 test classes** covering all entities, processes, widgets, and customizers
- All tests run in-memory via MemoryRecordStore (no database required)

```bash
mvn org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test \
    org.jacoco:jacoco-maven-plugin:0.8.11:report
# Report at: target/site/jacoco/index.html
```

## Documentation

- **[QQQ Wiki](https://github.com/Kingsrook/qqq/wiki)** - Framework documentation
- **[QBit Development Guide](https://github.com/Kingsrook/qqq/wiki/QBit-Development)** - How QBits work
- **Design Spec** - Full data model and architecture in `QRun-IO/specs/2026-03-28-qbit-crm-design-v2.md`

## Contributing

QBit CRM is open source and welcomes contributions.

- **[Report Issues](https://github.com/QRun-IO/qqq/issues)** - Bug reports and feature requests
- **[QQQ Contribution Guide](https://github.com/Kingsrook/qqq/wiki/Contribution-Guidelines)** - How to contribute

## About Kingsrook

QBit CRM is built by **[Kingsrook](https://qrun.io)** - making engineers more productive through intelligent automation and developer tools.

- **Website**: [https://qrun.io](https://qrun.io)
- **Contact**: [contact@kingsrook.com](mailto:contact@kingsrook.com)
- **GitHub**: [https://github.com/QRun-IO](https://github.com/QRun-IO)

## License

This project is licensed under the **GNU Affero General Public License v3.0** - see the [LICENSE.txt](LICENSE.txt) file for details.
