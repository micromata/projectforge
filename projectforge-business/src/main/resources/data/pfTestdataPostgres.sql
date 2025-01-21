-- We should increment the start values of the sequences, otherwise unique constraint violations will occur
-- after starting new databases pre-filled with test data.

ALTER SEQUENCE hibernate_sequence RESTART WITH 1000;
