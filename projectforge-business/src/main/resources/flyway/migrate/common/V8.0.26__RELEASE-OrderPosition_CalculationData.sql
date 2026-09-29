-- Persists the order-position net-sum calculator breakdown (role / daily rate / person-days lines) as
-- JSON, so the calculation behind a position's net sum survives save and can be reopened and edited.

ALTER TABLE t_fibu_auftrag_position ADD COLUMN calculation_data VARCHAR(10000);
