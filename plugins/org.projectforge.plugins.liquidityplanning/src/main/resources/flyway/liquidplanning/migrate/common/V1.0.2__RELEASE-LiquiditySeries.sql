-- Recurring liquidity entries (series): a rule + template values stored once as t_plugin_liqui_series.
-- Occurrences are projected virtually on read and materialized into t_plugin_liqui_entry only when the
-- user touches one; a materialized entry then carries series_id + series_date (its stable anchor).
-- CREATE TABLE and ADD COLUMN in this shape work on both PostgreSQL (prod) and HSQLDB (test), so this
-- lives in migrate/common. The pk/sequence idiom follows the existing t_plugin_liqui_entry (shared
-- hibernate_sequence, pk INTEGER, PK constraint added separately).

CREATE TABLE t_plugin_liqui_series (
  pk              INTEGER NOT NULL,
  created         TIMESTAMP WITHOUT TIME ZONE,
  deleted         BOOLEAN NOT NULL,
  last_update     TIMESTAMP WITHOUT TIME ZONE,
  start_date      DATE,
  frequency       CHARACTER VARYING(20),
  interval_months INTEGER,
  installments    INTEGER,
  amount          NUMERIC(12, 2),
  subject         CHARACTER VARYING(1000),
  comment         CHARACTER VARYING(4000),
  auto_set_paid   BOOLEAN DEFAULT FALSE
);

ALTER TABLE t_plugin_liqui_series
  ADD CONSTRAINT t_plugin_liqui_series_pkey PRIMARY KEY (pk);

-- The occurrence's link back to its series and its stable anchor day (identity of the n-th occurrence,
-- independent of the freely editable date_of_payment). Both null for plain (non-series) entries.
ALTER TABLE t_plugin_liqui_entry ADD COLUMN series_id INTEGER;
ALTER TABLE t_plugin_liqui_entry ADD COLUMN series_date DATE;

CREATE INDEX idx_fk_t_plugin_liqui_entry_series_id
  ON t_plugin_liqui_entry (series_id);

-- Rollback:
-- DROP INDEX IF EXISTS idx_fk_t_plugin_liqui_entry_series_id;
-- ALTER TABLE t_plugin_liqui_entry DROP COLUMN IF EXISTS series_date;
-- ALTER TABLE t_plugin_liqui_entry DROP COLUMN IF EXISTS series_id;
-- DROP TABLE IF EXISTS t_plugin_liqui_series;
-- DELETE FROM t_flyway_liquidplanning_schema_version WHERE version = '1.0.2';
