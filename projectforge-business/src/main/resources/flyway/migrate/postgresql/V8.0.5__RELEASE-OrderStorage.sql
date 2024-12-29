CREATE TABLE t_fibu_orderbook_storage
(
    date                 DATE NOT NULL,
    incremental_based_on DATE,
    serialized_orderbook BYTEA
);

ALTER TABLE t_fibu_orderbook_storage
    ADD CONSTRAINT t_fibu_orderbook_storage_pkey PRIMARY KEY (date);
