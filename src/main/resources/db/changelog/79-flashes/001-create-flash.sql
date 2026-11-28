--liquibase formatted sql

--changeset flolec:001-create-flash
CREATE TABLE flash
(
    id         UUID          NOT NULL,
    artist_id  UUID          NOT NULL,
    image_key  VARCHAR(1024) NOT NULL,
    caption    VARCHAR(255),
    position   INTEGER       NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_flash PRIMARY KEY (id),
    CONSTRAINT uq_flash_image_key UNIQUE (image_key),
    CONSTRAINT fk_flash_artist FOREIGN KEY (artist_id) REFERENCES tattoo_artist (id) ON DELETE CASCADE
);
CREATE INDEX idx_flash_artist_id_position ON flash (artist_id, position);
--rollback DROP TABLE flash;
