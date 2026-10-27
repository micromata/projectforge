-- Chat (PF-CHAT, MSG-08): a message may quote another message of the same conversation. No foreign key: deleting a
-- conversation deletes its replies before the other messages, and a message may quote a reply.
ALTER TABLE T_CHAT_MESSAGE
    ADD COLUMN quoted_message_fk BIGINT;
