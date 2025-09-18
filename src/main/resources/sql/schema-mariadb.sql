SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop in dependency order (children first)
DROP TABLE IF EXISTS image_sequence;
DROP TABLE IF EXISTS image_variants;
DROP TABLE IF EXISTS status_history;
DROP TABLE IF EXISTS storage_objects;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS reference_types;
DROP TABLE IF EXISTS extensions;

SET FOREIGN_KEY_CHECKS = 1;

-- Code table: reference_types
CREATE TABLE reference_types
(
    id   INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    CONSTRAINT uk_reference_type_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Code table: extensions
CREATE TABLE extensions
(
    extension_id INT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(16) NOT NULL,
    name         VARCHAR(64) NOT NULL,
    CONSTRAINT uk_extension_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Root: images
CREATE TABLE images
(
    image_id          VARCHAR(255) PRIMARY KEY,
    status            VARCHAR(32)  NOT NULL,
    reference_type_id INT          NOT NULL,
    image_url         VARCHAR(500) NOT NULL,
    is_deleted        TINYINT(1)   NOT NULL,
    created_at        DATETIME     NULL,
    updated_at        DATETIME     NULL,
    CONSTRAINT fk_images_reference_type
        FOREIGN KEY (reference_type_id) REFERENCES reference_types (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE INDEX idx_images_reference_type ON images (reference_type_id);

-- 1:1 with shared PK: storage_objects
CREATE TABLE storage_objects
(
    image_id            VARCHAR(255) PRIMARY KEY,
    storage_location    VARCHAR(1000) NOT NULL,
    origin_size         BIGINT        NOT NULL,
    converted_size      BIGINT        NULL,
    origin_format_id    INT           NOT NULL,
    converted_format_id INT           NULL,
    CONSTRAINT fk_storage_objects_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_storage_objects_origin_format
        FOREIGN KEY (origin_format_id) REFERENCES extensions (extension_id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_storage_objects_converted_format
        FOREIGN KEY (converted_format_id) REFERENCES extensions (extension_id)
            ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE INDEX idx_storage_objects_origin_format ON storage_objects (origin_format_id);
CREATE INDEX idx_storage_objects_converted_format ON storage_objects (converted_format_id);

-- N:1 history
CREATE TABLE status_history
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id   VARCHAR(255)  NOT NULL,
    old_status VARCHAR(32)   NOT NULL,
    new_status VARCHAR(32)   NOT NULL,
    updated_at DATETIME      NOT NULL,
    updated_by VARCHAR(255)  NULL,
    reason     VARCHAR(1000) NULL,
    CONSTRAINT fk_status_history_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE INDEX idx_status_history_image ON status_history (image_id);

-- N:1 variants
CREATE TABLE image_variants
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id     VARCHAR(255)  NOT NULL,
    variant_code VARCHAR(32)   NOT NULL,
    is_thumbnail TINYINT(1)    NOT NULL,
    uploader_id  VARCHAR(255)  NULL,
    uploaded_at  DATETIME      NULL,
    width        INT           NULL,
    height       INT           NULL,
    url          VARCHAR(1000) NULL,
    CONSTRAINT fk_image_variants_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE INDEX idx_image_variants_image ON image_variants (image_id);
CREATE INDEX idx_image_variants_code ON image_variants (variant_code);

-- Composite key: image_sequence (id, image_id)
CREATE TABLE image_sequence
(
    id         BIGINT       NOT NULL,
    image_id   VARCHAR(255) NOT NULL,
    seq_number INT          NOT NULL,
    PRIMARY KEY (id, image_id),
    CONSTRAINT fk_image_sequence_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE INDEX idx_image_sequence_image ON image_sequence (image_id);
CREATE UNIQUE INDEX uk_image_sequence_img_seq ON image_sequence (image_id, seq_number);


CREATE TABLE shedlock
(
    name       varchar(64)  NOT NULL,
    lock_until timestamp(3) NOT NULL,
    locked_at  timestamp(3) NOT NULL,
    locked_by  varchar(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
