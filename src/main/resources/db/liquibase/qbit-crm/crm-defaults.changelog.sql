-- liquibase formatted sql
-- Copyright 2026 Kingsrook, LLC. Licensed under the Apache License, Version 2.0.
-- https://www.apache.org/licenses/LICENSE-2.0

-- qbit-crm starter defaults: a CRM that works out of the box.
-- Include it from the host's master changelog AFTER the changesets that create
-- the crm_* tables (crm.changelog.sql, or docs/ddl/postgres.sql):
--
--   - include: { file: db/liquibase/qbit-crm/crm-defaults.changelog.sql }
--
-- Without these rows, converting a lead fails ("no default pipeline"), deals
-- and web leads silently lose their lifecycle stage, and amounts have no
-- currency prefix.
--
-- Rules for this file:
--   * insert-only and never disruptive: a list is seeded only while its table is
--     empty (a host that configured its own keeps exactly that), the pipeline
--     only when no pipeline exists, the base currency only when none exists.
--     Nothing is ever updated, so an administrator's edits survive;
--   * portable SQL (PostgreSQL and H2): INSERT ... SELECT ... WHERE NOT EXISTS;
--   * not for tenant-enabled installs: once the crm-tenancy changelog has added
--     tenant_key to a table, that table's changeset is skipped (MARK_RAN).
--     Every row of a tenant-locked table needs a tenant key and only the host
--     knows its tenants, so such a host seeds these same rows once per tenant,
--     with tenant_key set;
--   * every changeset checks that its tables exist (SELECT ... WHERE 1 = 0 errors
--     when one is missing): a host that has not created them yet simply skips it
--     (MARK_RAN).
-- CrmDefaultsChangelogTest runs this file twice against a fresh schema.

-- changeset qbit-crm:defaults-1
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_activity_type WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_activity_type' AND LOWER(column_name) = 'tenant_key'
-- comment: The six activity kinds the CRM logs. Same codes and names CrmStarterData creates on first use, so that stays a no-op.
INSERT INTO crm_activity_type (code, name, is_system, sort_order, is_active, create_date, modify_date)
SELECT v.code, v.n, TRUE, v.so, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 'NOTE' AS code, 'Note' AS n, 0 AS so
   UNION ALL SELECT 'EMAIL', 'Email', 1
   UNION ALL SELECT 'CALL', 'Call', 2
   UNION ALL SELECT 'MEETING', 'Meeting', 3
   UNION ALL SELECT 'FORM_SUBMISSION', 'Form submission', 4
   UNION ALL SELECT 'SEQUENCE_STEP', 'Sequence step', 5) v
WHERE NOT EXISTS (SELECT 1 FROM crm_activity_type x WHERE x.code = v.code OR x.name = v.n);
-- rollback DELETE FROM crm_activity_type WHERE code IN ('NOTE', 'EMAIL', 'CALL', 'MEETING', 'FORM_SUBMISSION', 'SEQUENCE_STEP');

-- changeset qbit-crm:defaults-2
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_activity_outcome WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_activity_type WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_activity_outcome' AND LOWER(column_name) = 'tenant_key'
-- comment: Call and meeting outcomes, offered when an activity of that type is logged. Needs the activity types above.
INSERT INTO crm_activity_outcome (activity_type_id, name, sort_order, is_active, create_date, modify_date)
SELECT t.id, v.n, v.so, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM crm_activity_type t
JOIN (SELECT 'CALL' AS code, 'Connected' AS n, 1 AS so
   UNION ALL SELECT 'CALL', 'Left voicemail', 2
   UNION ALL SELECT 'CALL', 'No answer', 3
   UNION ALL SELECT 'MEETING', 'Meeting held', 1
   UNION ALL SELECT 'MEETING', 'Meeting no-show', 2) v ON v.code = t.code
WHERE NOT EXISTS (SELECT 1 FROM crm_activity_outcome);
-- rollback DELETE FROM crm_activity_outcome WHERE name IN ('Connected', 'Left voicemail', 'No answer', 'Meeting held', 'Meeting no-show');

-- changeset qbit-crm:defaults-3
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_pipeline WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_pipeline' AND LOWER(column_name) = 'tenant_key'
-- comment: The default pipeline, only when the host has no pipeline yet. Convert lead refuses to run without a default one.
INSERT INTO crm_pipeline (name, description, is_default, is_active, create_date, modify_date)
SELECT 'Sales Pipeline', 'Default sales process for new deals and converted leads.', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM crm_pipeline);
-- rollback DELETE FROM crm_pipeline WHERE name = 'Sales Pipeline';

-- changeset qbit-crm:defaults-4
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_pipeline WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_pipeline_stage WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_pipeline' AND LOWER(column_name) = 'tenant_key'
-- comment: Stages of Sales Pipeline with their win probability, only while that pipeline has no stages. Closed Won and Closed Lost end a deal.
INSERT INTO crm_pipeline_stage (pipeline_id, name, sort_order, probability_pct, is_closed_won, is_closed_lost, create_date, modify_date)
SELECT p.id, v.n, v.so, v.pct, v.won, v.lost, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM crm_pipeline p
JOIN (SELECT 'Qualification' AS n, 1 AS so, 10 AS pct, FALSE AS won, FALSE AS lost
   UNION ALL SELECT 'Discovery', 2, 25, FALSE, FALSE
   UNION ALL SELECT 'Proposal', 3, 50, FALSE, FALSE
   UNION ALL SELECT 'Negotiation', 4, 75, FALSE, FALSE
   UNION ALL SELECT 'Closed Won', 5, 100, TRUE, FALSE
   UNION ALL SELECT 'Closed Lost', 6, 0, FALSE, TRUE) v ON 1 = 1
WHERE p.name = 'Sales Pipeline'
   AND NOT EXISTS (SELECT 1 FROM crm_pipeline_stage);
-- rollback DELETE FROM crm_pipeline_stage WHERE pipeline_id IN (SELECT id FROM crm_pipeline WHERE name = 'Sales Pipeline');

-- changeset qbit-crm:defaults-5
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_lifecycle_stage WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_lifecycle_stage' AND LOWER(column_name) = 'tenant_key'
-- comment: Lifecycle stages. The CRM looks up Lead (new web leads) and Opportunity (Convert lead) by these exact names.
INSERT INTO crm_lifecycle_stage (name, sort_order, is_active, description, create_date, modify_date)
SELECT v.n, v.so, TRUE, v.d, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 'Subscriber' AS n, 1 AS so, 'Signed up for updates; not yet a lead.' AS d
   UNION ALL SELECT 'Lead', 2, 'Showed interest. Web leads start here.'
   UNION ALL SELECT 'Marketing Qualified Lead', 3, 'Engaged enough for marketing to hand over.'
   UNION ALL SELECT 'Sales Qualified Lead', 4, 'Accepted by sales as worth pursuing.'
   UNION ALL SELECT 'Opportunity', 5, 'Has an open deal. Convert lead moves the contact here.'
   UNION ALL SELECT 'Customer', 6, 'Has bought.'
   UNION ALL SELECT 'Evangelist', 7, 'A happy customer who refers others.'
   UNION ALL SELECT 'Other', 8, 'Does not fit the stages above.') v
WHERE NOT EXISTS (SELECT 1 FROM crm_lifecycle_stage);
-- rollback DELETE FROM crm_lifecycle_stage WHERE name IN ('Subscriber', 'Lead', 'Marketing Qualified Lead', 'Sales Qualified Lead', 'Opportunity', 'Customer', 'Evangelist', 'Other');

-- changeset qbit-crm:defaults-6
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_lead_source WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_lead_source' AND LOWER(column_name) = 'tenant_key'
-- comment: Where contacts and deals come from. Website is the source the CRM's web forms use.
INSERT INTO crm_lead_source (name, sort_order, is_active, create_date, modify_date)
SELECT v.n, v.so, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 'Website' AS n, 1 AS so
   UNION ALL SELECT 'Referral', 2
   UNION ALL SELECT 'Event', 3
   UNION ALL SELECT 'Email Campaign', 4
   UNION ALL SELECT 'Social', 5
   UNION ALL SELECT 'Partner', 6
   UNION ALL SELECT 'Cold Outreach', 7
   UNION ALL SELECT 'Other', 8) v
WHERE NOT EXISTS (SELECT 1 FROM crm_lead_source);
-- rollback DELETE FROM crm_lead_source WHERE name IN ('Website', 'Referral', 'Event', 'Email Campaign', 'Social', 'Partner', 'Cold Outreach', 'Other');

-- changeset qbit-crm:defaults-7
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_industry WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_industry' AND LOWER(column_name) = 'tenant_key'
-- comment: A short generic industry list for companies.
INSERT INTO crm_industry (name, sort_order, is_active, create_date, modify_date)
SELECT v.n, v.so, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 'Technology' AS n, 1 AS so
   UNION ALL SELECT 'Healthcare', 2
   UNION ALL SELECT 'Financial Services', 3
   UNION ALL SELECT 'Manufacturing', 4
   UNION ALL SELECT 'Retail', 5
   UNION ALL SELECT 'Education', 6
   UNION ALL SELECT 'Real Estate', 7
   UNION ALL SELECT 'Professional Services', 8
   UNION ALL SELECT 'Nonprofit', 9
   UNION ALL SELECT 'Other', 10) v
WHERE NOT EXISTS (SELECT 1 FROM crm_industry);
-- rollback DELETE FROM crm_industry WHERE name IN ('Technology', 'Healthcare', 'Financial Services', 'Manufacturing', 'Retail', 'Education', 'Real Estate', 'Professional Services', 'Nonprofit', 'Other');

-- changeset qbit-crm:defaults-8
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_win_loss_reason WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_win_loss_reason' AND LOWER(column_name) = 'tenant_key'
-- comment: Why deals are won (type 1) or lost (type 2). Names are unique across both types, so each carries its outcome as a prefix.
INSERT INTO crm_win_loss_reason (name, type, sort_order, is_active, create_date, modify_date)
SELECT v.n, v.t, v.so, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 'Won: Price' AS n, 1 AS t, 1 AS so
   UNION ALL SELECT 'Won: Product fit', 1, 2
   UNION ALL SELECT 'Won: Relationship', 1, 3
   UNION ALL SELECT 'Lost: Price', 2, 4
   UNION ALL SELECT 'Lost: Competitor', 2, 5
   UNION ALL SELECT 'Lost: No budget', 2, 6
   UNION ALL SELECT 'Lost: No decision', 2, 7
   UNION ALL SELECT 'Lost: Timing', 2, 8) v
WHERE NOT EXISTS (SELECT 1 FROM crm_win_loss_reason);
-- rollback DELETE FROM crm_win_loss_reason WHERE name LIKE 'Won: %' OR name LIKE 'Lost: %';

-- changeset qbit-crm:defaults-9
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_contact_role WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_contact_role' AND LOWER(column_name) = 'tenant_key'
-- comment: Roles a contact can play on a deal.
INSERT INTO crm_contact_role (name, sort_order, is_active, create_date, modify_date)
SELECT v.n, v.so, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 'Decision Maker' AS n, 1 AS so
   UNION ALL SELECT 'Champion', 2
   UNION ALL SELECT 'Influencer', 3
   UNION ALL SELECT 'Economic Buyer', 4
   UNION ALL SELECT 'Technical Evaluator', 5
   UNION ALL SELECT 'End User', 6) v
WHERE NOT EXISTS (SELECT 1 FROM crm_contact_role);
-- rollback DELETE FROM crm_contact_role WHERE name IN ('Decision Maker', 'Champion', 'Influencer', 'Economic Buyer', 'Technical Evaluator', 'End User');

-- changeset qbit-crm:defaults-10
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM crm_currency WHERE 1 = 0
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'crm_currency' AND LOWER(column_name) = 'tenant_key'
-- comment: The base currency, only when none exists: amounts need it for their prefix and conversions. Change it before the first deal if the business does not report in USD.
INSERT INTO crm_currency (currency_code, name, symbol, exchange_rate_to_base, is_base_currency, is_active, rate_last_updated_date, create_date, modify_date)
SELECT 'USD', 'US Dollar', '$', 1, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM crm_currency WHERE currency_code = 'USD' OR is_base_currency = TRUE);
-- rollback DELETE FROM crm_currency WHERE currency_code = 'USD';
