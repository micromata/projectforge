-- If set, a manually entered (positive) max hours value takes precedence over the person days calculated from
-- assigned order positions (of this task or any sub task). Default false => behaviour for existing tasks is unchanged.

ALTER TABLE t_task ADD COLUMN max_hours_has_priority BOOLEAN DEFAULT FALSE;
