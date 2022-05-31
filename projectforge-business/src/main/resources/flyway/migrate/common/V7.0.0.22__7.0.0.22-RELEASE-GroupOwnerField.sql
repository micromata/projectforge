ALTER TABLE T_GROUP ADD COLUMN group_owner_fk INTEGER;

ALTER TABLE T_GROUP ADD CONSTRAINT t_group_owner_fk_const FOREIGN KEY (group_owner_fk) REFERENCES t_pf_user (pk);

