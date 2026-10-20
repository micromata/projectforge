-- Polls, cleaned up data model (see PollDO, PollResponseDO): attendees as users and groups (both resolved when
-- checked), questions and answers as json naming options by uid, the state by name, templates.
-- The columns are filled from the old ones by V9_0_12_3__Poll_migrate_questions_and_answers, which drops the old
-- ones afterwards.
ALTER TABLE t_poll
    ADD COLUMN attendee_user_ids VARCHAR(4000);
ALTER TABLE t_poll
    ADD COLUMN attendee_group_ids VARCHAR(4000);
ALTER TABLE t_poll
    ADD COLUMN questions VARCHAR(100000);
ALTER TABLE t_poll
    ADD COLUMN template BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE t_poll_response
    ADD COLUMN answers VARCHAR(100000);
