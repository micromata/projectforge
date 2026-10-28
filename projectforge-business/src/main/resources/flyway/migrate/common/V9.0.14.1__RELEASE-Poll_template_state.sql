-- Polls: a template is neither running nor finished, it has a state of its own (PollDO.State.TEMPLATE).
UPDATE t_poll
SET state = 'TEMPLATE'
WHERE template = TRUE;
