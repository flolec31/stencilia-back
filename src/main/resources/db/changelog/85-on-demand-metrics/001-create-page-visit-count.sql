--liquibase formatted sql

--changeset flolec:001-create-page-visit-count
CREATE TABLE page_visit_count
(
    artist_id UUID   NOT NULL,
    count     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_page_visit_count PRIMARY KEY (artist_id),
    CONSTRAINT fk_page_visit_count_artist FOREIGN KEY (artist_id) REFERENCES tattoo_artist (id) ON DELETE CASCADE
);
--rollback DROP TABLE page_visit_count;
