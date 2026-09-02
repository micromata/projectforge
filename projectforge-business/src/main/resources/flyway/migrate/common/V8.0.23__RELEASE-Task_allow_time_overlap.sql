-- Marks a task (structure element) as a shared cost element: time sheets may overlap in time with time sheets of
-- other (non same-project) tasks. Inherited by sub tasks. Default false => behaviour for existing tasks is unchanged.

ALTER TABLE t_task ADD COLUMN allow_time_overlap BOOLEAN DEFAULT FALSE;
