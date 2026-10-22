# Upgrading

## Shared product catalog (unreleased)

`crm_product` now supports an optional unique `slug` (64 characters) and an absent catalog `unit_price` for informational products. No price is invented. Deal-line `unit_price` remains required; existing prices and amounts are unchanged.

Before running the updated metadata against an existing schema, include `db/liquibase/qbit-crm/crm-product-catalog.changelog.sql` after the base CRM schema. This additive migration also works for hosts that copied the old CRM DDL; do not replay the base changelog against an already-created schema. Fresh installs using `crm.changelog.sql` must include the catalog changelog too. `docs/ddl/postgres.sql` describes the final schema, so hosts creating tables directly from that file must not then replay the additive migration.

The migration adds nullable `slug`, its unique constraint, and drops only the catalog price NOT NULL constraint. It preserves product IDs, timestamps, prices and tenant keys. It does not migrate Marketing data or alter any deal-line price. Roll back application code only after checking schema compatibility; do not drop populated slugs or force null prices to zero.

Catalog permissions stay on `crm.manage.read/write` unless the host opts into `withProductPermissionBase("crm.product")`. Opt-in requires an explicit role migration in the same adoption rollout:

- Existing `crm.manage.read` holders retain that grant and gain `crm.product.read`.
- Existing `crm.manage.write` holders retain that grant and gain `crm.product.write` (preserve their existing read grant separately).
- Marketing roles needing catalog selection receive `crm.product.read` only. Their marketing permissions remain unchanged.
- Do not grant `crm.manage` to Marketing users or infer permissions from navigation placement.

The native QQQ permission checks enforce both API writes and UI operation checks. Tenant locks continue to apply. Marketing integration and host data migration must be adopted separately before retiring `marketing_product`.

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
