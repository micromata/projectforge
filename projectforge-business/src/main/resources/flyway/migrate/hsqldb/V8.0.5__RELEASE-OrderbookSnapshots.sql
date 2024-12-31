CREATE TABLE t_fibu_orderbook_snapshots
(
    date                 DATE NOT NULL,
    incremental_based_on DATE,
    serialized_orderbook BLOB
);

ALTER TABLE t_fibu_orderbook_snapshots
    ADD CONSTRAINT t_fibu_orderbook_snapshots_pkey PRIMARY KEY (date);
