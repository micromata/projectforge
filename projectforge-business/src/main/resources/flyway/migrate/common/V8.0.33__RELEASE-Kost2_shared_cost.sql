-- Marks a cost unit (Kost2) as a shared cost element (true), explicitly not (false) or "as the structure element"
-- (null, the default): time sheets booked on a shared cost element may overlap in time with time sheets of other
-- projects. A set value takes precedence over the task's allow_time_overlap. Null => existing behaviour unchanged.

ALTER TABLE t_fibu_kost2 ADD COLUMN shared_cost BOOLEAN;
