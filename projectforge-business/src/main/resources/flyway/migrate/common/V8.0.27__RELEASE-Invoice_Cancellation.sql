-- RechnungDO: a cancellation invoice (typ CANCELLATION) references the invoice it cancels. The cancellation
-- has no number of its own, its document number is derived from the original one ("<nummer>-S").
ALTER TABLE t_fibu_rechnung ADD COLUMN original_rechnung_fk BIGINT;

ALTER TABLE t_fibu_rechnung
    ADD CONSTRAINT fk_t_fibu_rechnung_original_rechnung FOREIGN KEY (original_rechnung_fk) REFERENCES t_fibu_rechnung (pk);

CREATE INDEX idx_fk_t_fibu_rechnung_original_rechnung
    ON t_fibu_rechnung (original_rechnung_fk);

-- Rollback:
-- DROP INDEX IF EXISTS idx_fk_t_fibu_rechnung_original_rechnung;
-- ALTER TABLE t_fibu_rechnung DROP CONSTRAINT IF EXISTS fk_t_fibu_rechnung_original_rechnung;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS original_rechnung_fk;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.27';
