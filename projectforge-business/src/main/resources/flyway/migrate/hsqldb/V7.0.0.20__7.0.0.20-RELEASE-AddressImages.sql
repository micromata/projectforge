ALTER TABLE T_ADDRESS ADD COLUMN image BOOLEAN;
ALTER TABLE T_ADDRESS ADD COLUMN image_last_update TIMESTAMP WITHOUT TIME ZONE;

CREATE TABLE T_ADDRESS_IMAGE (
  pk                                     INTEGER                      NOT NULL,
  address_fk                             INTEGER                      NOT NULL,
  last_update                            TIMESTAMP WITHOUT TIME ZONE,
  image                                  BLOB,
  image_preview                          BLOB
);

ALTER TABLE T_ADDRESS_IMAGE
  ADD CONSTRAINT t_address_image_pkey PRIMARY KEY (pk);
ALTER TABLE T_ADDRESS_IMAGE
  ADD CONSTRAINT fk_address_image_address FOREIGN KEY (address_fk) REFERENCES t_address (pk);

CREATE INDEX idx_fk_t_address_image_address_fk
  ON T_ADDRESS_IMAGE (address_fk);
