-- Currency conversion tables for managing exchange rates between currency pairs.
-- Master-Detail pattern: Currency pairs with time-dependent conversion rates.

CREATE TABLE T_FIBU_CURRENCY_PAIR
(
    pk                        INTEGER                      NOT NULL,
    created                   TIMESTAMP WITHOUT TIME ZONE,
    deleted                   BOOLEAN                      NOT NULL,
    last_update               TIMESTAMP WITHOUT TIME ZONE,
    source_currency           CHARACTER VARYING(3)         NOT NULL,
    target_currency           CHARACTER VARYING(3)         NOT NULL,
    comment                   CHARACTER VARYING(4000)
);

ALTER TABLE T_FIBU_CURRENCY_PAIR
    ADD CONSTRAINT t_fibu_currency_pair_pkey PRIMARY KEY (pk);

CREATE INDEX idx_t_fibu_currency_pair_source
    ON T_FIBU_CURRENCY_PAIR (source_currency);

CREATE INDEX idx_t_fibu_currency_pair_target
    ON T_FIBU_CURRENCY_PAIR (target_currency);

ALTER TABLE T_FIBU_CURRENCY_PAIR
    ADD CONSTRAINT unique_t_fibu_currency_pair UNIQUE (source_currency, target_currency);


CREATE TABLE T_FIBU_CURRENCY_CONVERSION_RATE
(
    pk                        INTEGER                      NOT NULL,
    created                   TIMESTAMP WITHOUT TIME ZONE,
    deleted                   BOOLEAN                      NOT NULL,
    last_update               TIMESTAMP WITHOUT TIME ZONE,
    currency_pair_fk          INTEGER                      NOT NULL,
    valid_from                DATE                         NOT NULL,
    conversion_rate           NUMERIC(18,8)                NOT NULL,
    comment                   CHARACTER VARYING(4000)
);

ALTER TABLE T_FIBU_CURRENCY_CONVERSION_RATE
    ADD CONSTRAINT t_fibu_currency_conversion_rate_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_fibu_curr_conv_rate_pair
    ON T_FIBU_CURRENCY_CONVERSION_RATE (currency_pair_fk);

CREATE INDEX idx_t_fibu_curr_conv_rate_valid
    ON T_FIBU_CURRENCY_CONVERSION_RATE (valid_from);

ALTER TABLE T_FIBU_CURRENCY_CONVERSION_RATE
    ADD CONSTRAINT t_fibu_curr_conv_rate_fk_pair FOREIGN KEY (currency_pair_fk) REFERENCES T_FIBU_CURRENCY_PAIR (pk);

ALTER TABLE T_FIBU_CURRENCY_CONVERSION_RATE
    ADD CONSTRAINT unique_t_fibu_curr_conv_rate UNIQUE (currency_pair_fk, valid_from);
