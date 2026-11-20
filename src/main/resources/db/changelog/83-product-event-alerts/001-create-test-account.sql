--liquibase formatted sql

--changeset flolec:001-create-test-account
CREATE TABLE test_account
(
    email VARCHAR(255) NOT NULL,
    CONSTRAINT pk_test_account PRIMARY KEY (email)
);
--rollback DROP TABLE test_account;
