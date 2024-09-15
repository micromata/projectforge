ALTER TABLE t_plugin_datatransfer_area ALTER COLUMN pk bigint;
ALTER TABLE t_plugin_datatransfer_audit ALTER COLUMN pk bigint;

ALTER TABLE t_plugin_datatransfer_audit ALTER COLUMN area_fk bigint;
ALTER TABLE t_plugin_datatransfer_audit ALTER COLUMN by_user_fk bigint;
ALTER TABLE t_plugin_datatransfer_audit ALTER COLUMN upload_by_user_fk bigint;
