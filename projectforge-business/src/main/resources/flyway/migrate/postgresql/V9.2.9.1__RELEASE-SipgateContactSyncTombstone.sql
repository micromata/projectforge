-- Sipgate contact sync: a force deleted address keeps its sync entry without address (remote status DELETED_BY_LOCAL),
-- so the next sync deletes the remote contact instead of importing it again as a new address.
ALTER TABLE t_sipgate_contact_sync ALTER COLUMN address_id DROP NOT NULL;
