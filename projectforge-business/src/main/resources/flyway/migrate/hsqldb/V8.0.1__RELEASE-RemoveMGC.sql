ALTER TABLE t_plugin_calendar_event ADD COLUMN original_ics_entry VARCHAR(10000);

ALTER TABLE t_pf_history ALTER COLUMN createdat SET NULL; -- modifiedat is used instead.
ALTER TABLE t_pf_history ALTER COLUMN createdby SET NULL; -- modifiedby is used instead.
ALTER TABLE t_pf_history ALTER COLUMN updatecounter SET NULL;

ALTER TABLE t_pf_history_attr ALTER COLUMN value varchar(50000);
ALTER TABLE t_pf_history_attr ALTER COLUMN createdat SET NULL; -- parent.modifiedat is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN createdby SET NULL; -- parent.modifieby is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN modifiedat SET NULL; -- parent.modifiedat is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN modifiedby SET NULL; -- parent.modifieby is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN updatecounter SET NULL;
ALTER TABLE t_pf_history_attr ALTER COLUMN type SET NULL;

ALTER TABLE t_configuration ALTER COLUMN intvalue bigint;
ALTER TABLE t_configuration ALTER COLUMN intvalue RENAME TO longvalue;

-- Plugins without own flyway scripts:
-- Can't alter tables because of foreign key constraints.
-- ALTER TABLE t_plugin_calendar_event ALTER COLUMN pk bigint;
-- ...
