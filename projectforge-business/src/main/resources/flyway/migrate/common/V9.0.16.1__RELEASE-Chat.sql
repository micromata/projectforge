-- Chat (PF-CHAT): conversations about host objects, the built-in hosts are chat rooms with participants and direct
-- messages between two users. A conversation counts its changes (change_seq), every message stores the sequence of its
-- last change, so clients fetch the changes since the last one they know (live updates, reconnects and polling alike).
-- The files of messages are stored in the data transfer: in a synthetic area per conversation (file_area_id) or, for
-- direct messages, in the personal box of the recipient.
CREATE TABLE T_CHAT_ROOM
(
    pk                    BIGINT                      NOT NULL,
    created               TIMESTAMP WITHOUT TIME ZONE,
    deleted               BOOLEAN                     NOT NULL,
    last_update           TIMESTAMP WITHOUT TIME ZONE,
    name                  CHARACTER VARYING(255)      NOT NULL,
    description           CHARACTER VARYING(4000),
    owner_fk              BIGINT                      NOT NULL,
    moderator_user_ids    CHARACTER VARYING(4000),
    participant_user_ids  CHARACTER VARYING(10000),
    participant_group_ids CHARACTER VARYING(4000),
    CONSTRAINT t_chat_room_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_CHAT_ROOM
    ADD CONSTRAINT fk_t_chat_room_owner FOREIGN KEY (owner_fk) REFERENCES T_PF_USER (pk);

-- Direct messages: one pair of users (user1_fk < user2_fk) per conversation.
CREATE TABLE T_CHAT_DIRECT
(
    pk       BIGINT                      NOT NULL,
    created  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user1_fk BIGINT                      NOT NULL,
    user2_fk BIGINT                      NOT NULL,
    CONSTRAINT t_chat_direct_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_chat_direct_users UNIQUE (user1_fk, user2_fk)
);

ALTER TABLE T_CHAT_DIRECT
    ADD CONSTRAINT fk_t_chat_direct_user1 FOREIGN KEY (user1_fk) REFERENCES T_PF_USER (pk);

ALTER TABLE T_CHAT_DIRECT
    ADD CONSTRAINT fk_t_chat_direct_user2 FOREIGN KEY (user2_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_t_chat_direct_user2
    ON T_CHAT_DIRECT (user2_fk);

CREATE TABLE T_CHAT_CONVERSATION
(
    pk              BIGINT                      NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    host_type       CHARACTER VARYING(50)       NOT NULL,
    host_id         BIGINT                      NOT NULL,
    channel         CHARACTER VARYING(50)       NOT NULL,
    change_seq      BIGINT                      NOT NULL,
    last_message_at TIMESTAMP WITHOUT TIME ZONE,
    file_area_id    BIGINT,
    CONSTRAINT t_chat_conversation_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_chat_conversation_host UNIQUE (host_type, host_id, channel)
);

-- Messages and the replies of their threads (thread_root_fk), with history (edits, deletion, hiding), their files as
-- JSON (attachments: area and file id in the data transfer, name, size, image or not). Reactions
-- (JSON: emoji -> user ids) and the followers of a thread are stored with the message, without history.
CREATE TABLE T_CHAT_MESSAGE
(
    pk                BIGINT                      NOT NULL,
    created           TIMESTAMP WITHOUT TIME ZONE,
    deleted           BOOLEAN                     NOT NULL,
    last_update       TIMESTAMP WITHOUT TIME ZONE,
    conversation_fk   BIGINT                      NOT NULL,
    author_fk         BIGINT                      NOT NULL,
    thread_root_fk    BIGINT,
    content           CHARACTER VARYING(100000),
    content_text      CHARACTER VARYING(10000),
    attachments       CHARACTER VARYING(10000),
    mentioned_ids     CHARACTER VARYING(4000),
    edited_at         TIMESTAMP WITHOUT TIME ZONE,
    hidden_by_fk      BIGINT,
    hidden_at         TIMESTAMP WITHOUT TIME ZONE,
    hidden_reason     CHARACTER VARYING(1000),
    reply_count       INTEGER                     NOT NULL,
    last_reply_at     TIMESTAMP WITHOUT TIME ZONE,
    change_seq        BIGINT                      NOT NULL,
    reactions         CHARACTER VARYING(10000),
    follower_ids      CHARACTER VARYING(10000),
    unfollower_ids    CHARACTER VARYING(10000),
    CONSTRAINT t_chat_message_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_t_chat_message_conversation_seq
    ON T_CHAT_MESSAGE (conversation_fk, change_seq);

CREATE INDEX idx_t_chat_message_conversation_root
    ON T_CHAT_MESSAGE (conversation_fk, thread_root_fk, pk);

ALTER TABLE T_CHAT_MESSAGE
    ADD CONSTRAINT fk_t_chat_message_conversation FOREIGN KEY (conversation_fk) REFERENCES T_CHAT_CONVERSATION (pk);

ALTER TABLE T_CHAT_MESSAGE
    ADD CONSTRAINT fk_t_chat_message_author FOREIGN KEY (author_fk) REFERENCES T_PF_USER (pk);

ALTER TABLE T_CHAT_MESSAGE
    ADD CONSTRAINT fk_t_chat_message_thread_root FOREIGN KEY (thread_root_fk) REFERENCES T_CHAT_MESSAGE (pk);

ALTER TABLE T_CHAT_MESSAGE
    ADD CONSTRAINT fk_t_chat_message_hidden_by FOREIGN KEY (hidden_by_fk) REFERENCES T_PF_USER (pk);

-- Per user and conversation: the last message read and whether the conversation is muted.
CREATE TABLE T_CHAT_MEMBER_STATE
(
    pk                   BIGINT  NOT NULL,
    conversation_fk      BIGINT  NOT NULL,
    user_fk              BIGINT  NOT NULL,
    last_read_message_id BIGINT,
    muted                BOOLEAN NOT NULL,
    CONSTRAINT t_chat_member_state_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_chat_member_state UNIQUE (conversation_fk, user_fk)
);

ALTER TABLE T_CHAT_MEMBER_STATE
    ADD CONSTRAINT fk_t_chat_member_state_conversation FOREIGN KEY (conversation_fk) REFERENCES T_CHAT_CONVERSATION (pk);

ALTER TABLE T_CHAT_MEMBER_STATE
    ADD CONSTRAINT fk_t_chat_member_state_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);

-- Rollback:
-- DROP TABLE IF EXISTS T_CHAT_MEMBER_STATE;
-- DROP TABLE IF EXISTS T_CHAT_MESSAGE;
-- DROP TABLE IF EXISTS T_CHAT_CONVERSATION;
-- DROP TABLE IF EXISTS T_CHAT_DIRECT;
-- DROP TABLE IF EXISTS T_CHAT_ROOM;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.16.1';
