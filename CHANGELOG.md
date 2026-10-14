# Changelog

## Unreleased / 0.2.0

- Upgrade to Java 21, QQQ 4.1.0-RC.6 and qbit-build-parent 2.1.2.
- Add Maven wrapper, CI configuration, architecture guardrails and coverage gates.
- Rename tables, joins, widgets and CRM processes to `crm_` snake_case, with inferred snake_case SQL columns.
- Remove bespoke tag, attachment and audit tables, bulk tag processes and unused namespace/prefix configuration.
- Use core FIELD-level auditing on contacts, companies, deals and activities.
- Apply CRM, management and admin permission rules and source-QBit metadata, including an RC.6 widget provenance adapter.
- Ship PostgreSQL DDL, 33 Liquibase changesets, and H2 persistence and schema tests. 0.2.0 is a fresh install; there is no upgrade path from 0.1.0.
- Correct installation instructions and document the existing P1 behavior and remaining limitations.
