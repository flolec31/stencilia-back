--liquibase formatted sql

--changeset flolec:001-widen-support-message-message
ALTER TABLE support_message ALTER COLUMN message TYPE VARCHAR(2000);
--rollback ALTER TABLE support_message ALTER COLUMN message TYPE VARCHAR(1500);
