# Upgrading

## Shared email templates (Unreleased / 0.2.0)

This version uses public qbit-email `0.1.0-RC.10` and QQQ `4.1.0-RC.20`. Shared templates remain disabled by default. Existing optional-email and tenant-enabled hosts retain their legacy template behavior.

For a non-tenant host adopting shared templates:

1. Install the email qbit and configure its template permission base as `crm.manage`. Existing per-template edit permissions and record locks remain enforced; this grant does not expose other email settings.
2. Before enabling `CrmQBitConfig.withSharedEmailTemplates(true)`, migrate legacy template content into email-owned `CRM_LEGACY` templates and active versions, remap sequence/history and generic record references, and retain source provenance and rollback data. This library does not run a host database migration. Populated deployments require a documented CRM maintenance window with writes and workers quiesced, fresh preflight counts and verified backups.
3. Enable the opt-in only after migration. It omits `crm_email_template` metadata and its possible-value source entirely and uses `email_template` for actions and sequences. Tenant mode and missing email installations reject the opt-in.
4. Native email templates retain their category and content policies. Migrated compatibility templates preserve historical substitutions and overrides. Checked replay can reject a changed template version or envelope; reconcile the existing effect rather than silently retrying with a new idempotency key.

The producer also registers `crm.email`, `crm.sequence-lag` and `crm.lead-score` with core health metadata. Existing legacy adapters remain available. Checks use execution-time context, preserve tenant visibility and do not assign tenant keys to contextless workers.

## Starter defaults (Unreleased / 0.2.0)

The CRM now ships `db/liquibase/qbit-crm/crm-defaults.changelog.sql`, a separate insert-only changelog. Without it,
converting a lead fails with "no default pipeline found", new web leads and converted contacts silently get no
lifecycle stage, and amounts have no currency prefix.

What a host must do:

1. Add one line to its master changelog, after `crm.changelog.sql` and, on tenant-enabled installs, after `crm-tenancy.changelog.sql` (the file checks for `tenant_key` columns, so the order matters):

   ```yaml
   - include: { file: db/liquibase/qbit-crm/crm-defaults.changelog.sql }
   ```

2. Review what it seeds (see README, "Starter defaults"). Existing configuration is never touched: lists are seeded
   only while their table is empty, the pipeline only when no pipeline exists, its stages only when it is the only pipeline and no stage or deal exists. Lifecycle stages and the base currency are seeded only on a fresh install (no contact, respectively no deal, yet), because they change what intake and Convert lead assign. Rollbacks are empty.
3. Tenant-enabled hosts (the `crm-tenancy` changelog added `tenant_key` columns): the changelog skips those tables
   (marks them ran), because rows without a tenant key are invisible to tenants. Insert the same rows once per tenant
   with `tenant_key` set. The Setup card on the CRM home shows what is still missing.
4. `crm_currency.rate_last_updated_date` is now written by the CRM when a rate is set or changed and is read-only in
   the UI. Existing values are kept.
5. No schema change. `crm_external_mapping` is documented as not written by the CRM yet.
