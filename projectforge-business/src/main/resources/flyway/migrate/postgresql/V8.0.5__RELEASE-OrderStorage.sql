CREATE TABLE t_fibu_orderbook_storage
(
    date                 DATE NOT NULL,
    serialized_orderbook BYTEA
);

ALTER TABLE t_fibu_orderbook_storage
    ADD CONSTRAINT t_fibu_orderbook_storage_pkey PRIMARY KEY (date);
