-- Passwords are now in separate table, so password isn't NOT NULL anymore.
-- Attribute isn't used anymore but still exists for any roll-back-scenario.

ALTER TABLE T_PF_USER ALTER COLUMN password DROP NOT NULL;
