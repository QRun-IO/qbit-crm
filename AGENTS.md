# QBit CRM

CRM metadata, processes and widgets for QQQ. P1 upgrades the platform; later phases are defined in the 0.2.0 design.

## Build

Use Java 21 and the Maven wrapper: `./mvnw -q verify` runs checkstyle, tests and JaCoCo (70% instructions, 90% classes). Parent: `qbit-build-parent:2.1.2`; default-active `qqq-snapshot` BOM: `4.1.0-RC.6`. Architecture guardrails and strict UI API routes stay enabled.

## Conventions

- Entry point: `CrmQBitProducer`, with package-discovered metadata producers.
- Table, join, widget and process names use `crm_` snake_case. Database columns use snake_case; Java fields stay camelCase.
- Allman braces, 3-space indentation, `return (x);`, fluent setters and Javadoc flower-box banners.
- New source files carry the Apache 2.0 license header.
- Keep changes scoped and run verify before committing. Do not push without authorization.
