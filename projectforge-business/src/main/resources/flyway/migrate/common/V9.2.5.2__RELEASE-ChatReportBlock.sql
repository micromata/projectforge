-- Chat (PF-CHAT): users block each other (no direct messages, no notifications, mentions, unread counts or search hits
-- of the blocked one for the blocker) and report messages to the administrators (Apple App Review guideline 1.2).
CREATE TABLE T_CHAT_USER_BLOCK
(
    pk         BIGINT                      NOT NULL,
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    blocker_fk BIGINT                      NOT NULL,
    blocked_fk BIGINT                      NOT NULL,
    CONSTRAINT t_chat_user_block_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_chat_user_block UNIQUE (blocker_fk, blocked_fk)
);

ALTER TABLE T_CHAT_USER_BLOCK
    ADD CONSTRAINT fk_t_chat_user_block_blocker FOREIGN KEY (blocker_fk) REFERENCES T_PF_USER (pk);

ALTER TABLE T_CHAT_USER_BLOCK
    ADD CONSTRAINT fk_t_chat_user_block_blocked FOREIGN KEY (blocked_fk) REFERENCES T_PF_USER (pk);

-- Looked up from the other side: who has blocked this user?
CREATE INDEX idx_t_chat_user_block_blocked
    ON T_CHAT_USER_BLOCK (blocked_fk);

-- A reported message. The administrators have no access to the conversations, so the report keeps an excerpt of the
-- message (the author may edit or delete it afterwards).
CREATE TABLE T_CHAT_REPORT
(
    pk               BIGINT                      NOT NULL,
    created          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    message_fk       BIGINT                      NOT NULL,
    conversation_fk  BIGINT                      NOT NULL,
    reporter_fk      BIGINT                      NOT NULL,
    author_fk        BIGINT                      NOT NULL,
    reason           CHARACTER VARYING(1000),
    content_snapshot CHARACTER VARYING(500),
    status           CHARACTER VARYING(20)       NOT NULL,
    CONSTRAINT t_chat_report_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_chat_report UNIQUE (reporter_fk, message_fk)
);

ALTER TABLE T_CHAT_REPORT
    ADD CONSTRAINT fk_t_chat_report_message FOREIGN KEY (message_fk) REFERENCES T_CHAT_MESSAGE (pk);

ALTER TABLE T_CHAT_REPORT
    ADD CONSTRAINT fk_t_chat_report_reporter FOREIGN KEY (reporter_fk) REFERENCES T_PF_USER (pk);

ALTER TABLE T_CHAT_REPORT
    ADD CONSTRAINT fk_t_chat_report_author FOREIGN KEY (author_fk) REFERENCES T_PF_USER (pk);

-- Rollback:
-- DROP TABLE IF EXISTS T_CHAT_REPORT;
-- DROP TABLE IF EXISTS T_CHAT_USER_BLOCK;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.5.2';
