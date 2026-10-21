-- The history keeps the old and the new value of a property in full: as long as the longest column historized,
-- T_CONFIGURATION.stringvalue (100000, see V8.0.28 and HistoryEntryAttrDO). A longer JSON parameter made the
-- history insert, and with it the save, fail.
ALTER TABLE t_pf_history_attr ALTER COLUMN value VARCHAR(100000);
ALTER TABLE t_pf_history_attr ALTER COLUMN old_value VARCHAR(100000);
