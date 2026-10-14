# Upgrading

## Starter defaults (Unreleased / 0.2.0)

The CRM now ships `db/liquibase/qbit-crm/crm-defaults.changelog.sql`, a separate insert-only changelog. Without it,
converting a lead fails with "no default pipeline found", new web leads and converted contacts silently get no
lifecycle stage, and amounts have no currency prefix.

What a host must do:

1. Add one line to its master changelog, after the changesets that create the `crm_*` tables:

   ```yaml
   - include: { file: db/liquibase/qbit-crm/crm-defaults.changelog.sql }
   ```

2. Review what it seeds (see README, "Starter defaults"). Existing configuration is never touched: lists are seeded
   only while their table is empty, the pipeline only when no pipeline exists, the base currency only when none exists.
3. Tenant-enabled hosts (the `crm-tenancy` changelog added `tenant_key` columns): the changelog skips those tables
   (marks them ran), because rows without a tenant key are invisible to tenants. Insert the same rows once per tenant
   with `tenant_key` set. The Setup card on the CRM home shows what is still missing.
4. `crm_currency.rate_last_updated_date` is now written by the CRM when a rate is set or changed and is read-only in
   the UI. Existing values are kept.
5. No schema change. `crm_external_mapping` is documented as not written by the CRM yet.
