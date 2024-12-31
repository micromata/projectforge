CREATE TABLE t_fibu_orderbook_snapshots
(
    date                 DATE NOT NULL,
    created              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    incremental_based_on DATE,
    serialized_orderbook BYTEA,
    size                 INTEGER
);

ALTER TABLE t_fibu_orderbook_snapshots
    ADD CONSTRAINT t_fibu_orderbook_snapshots_pkey PRIMARY KEY (date);
