--liquibase formatted sql
-- Run after the existing CRM base schema, before deploying shared catalog metadata.
-- Existing product identities, prices, deal-line amounts and tenant keys are preserved.
--changeset qbit-crm:crm_product_shared_catalog
ALTER TABLE crm_product ADD COLUMN slug VARCHAR(64);
ALTER TABLE crm_product ADD CONSTRAINT uq_crm_product_slug UNIQUE (slug);
ALTER TABLE crm_product ALTER COLUMN unit_price DROP NOT NULL;
