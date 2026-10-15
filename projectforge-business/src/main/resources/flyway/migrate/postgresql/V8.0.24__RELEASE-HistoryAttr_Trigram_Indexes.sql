-- Speeds up the history value search (DBHistoryQuery.searchHistoryEntryByCriteria):
--   lower(value) LIKE '%term%' OR lower(old_value) LIKE '%term%' on t_pf_history_attr.
-- A plain btree cannot serve a leading-wildcard LIKE, so on large history tables (millions of attr rows) this
-- search degrades to a sequential scan (measured 1.6-4.4 s). pg_trgm GIN trigram indexes make the existing LIKE
-- predicate index-backed without changing the query (substring semantics preserved). The planner combines both
-- columns via BitmapOr. Terms with fewer than 3 non-wildcard characters still fall back to a scan.
--
-- PostgreSQL-only (pg_trgm/GIN). HSQLDB keeps the LIKE fallback; no counterpart migration is needed
-- (spring.flyway.fail-on-missing-locations=false).
--
-- pg_trgm is a trusted extension since PostgreSQL 13, so a DB user with CREATE privilege on the database can
-- install it without superuser. If the production role lacks that privilege, a DBA has to run CREATE EXTENSION
-- pg_trgm once beforehand; this statement then becomes a no-op.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX ix_pf_history_attr_value_trgm
    ON t_pf_history_attr USING gin (lower(value) gin_trgm_ops);

CREATE INDEX ix_pf_history_attr_old_value_trgm
    ON t_pf_history_attr USING gin (lower(old_value) gin_trgm_ops);

-- Entity-constrained history searches (the search always fixes entity_name to the queried DO class) otherwise
-- fall back to a sequential scan of all t_pf_history rows: the existing ix_pf_history_ent leads with entity_id,
-- so it cannot serve an entity_name-only predicate. A dedicated btree turns that scan into an index/bitmap scan
-- (measured 224 ms -> 56 ms for a class with 53k history rows) and lets the planner choose freely between the
-- entity_name path (selective class) and the trigram path (selective term).
CREATE INDEX ix_pf_history_entity_name
    ON t_pf_history (entity_name);
