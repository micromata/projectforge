CREATE TABLE t_fibu_orderbook_storage
(
    pk                   LONG NOT NULL,
    date                 DATE NOT NULL,
    serialized_orderbook BYTEA,
);

ALTER TABLE t_fibu_orderbook_storage
    ADD CONSTRAINT t_fibu_orderbook_storage_pkey PRIMARY KEY (pk);
ALTER TABLE t_fibu_orderbook_storage
    ADD CONSTRAINT unique_t_fibu_orderbook_storage UNIQUE (date);
