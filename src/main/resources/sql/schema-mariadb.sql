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

-- Code table: reference_types (코드를 기본키로 사용)
CREATE TABLE reference_types
(
    code            VARCHAR(32) PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL,
    allows_multiple TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '다중 이미지 허용 여부',
    max_images      INT          NULL COMMENT '최대 이미지 개수 (NULL = 무제한)',
    description     VARCHAR(255) NULL COMMENT '참조 타입 설명'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 참조 타입 (PRODUCT, USER, POST 등)';

-- Code table: extensions (코드를 기본키로 사용)
CREATE TABLE extensions
(
    code VARCHAR(16) PRIMARY KEY,
    name VARCHAR(64) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 확장자 (JPG, PNG, WEBP 등)';

-- Root: images
CREATE TABLE images
(
    image_id          VARCHAR(255) PRIMARY KEY,
    status            VARCHAR(32)  NOT NULL COMMENT 'TEMP, CONFIRMED, DELETED 등',
    reference_type_id VARCHAR(32)  NOT NULL COMMENT '참조 타입 코드 (PRODUCT, USER 등)',
    reference_id      VARCHAR(200) NULL COMMENT '참조 대상 ID (상품 ID, 사용자 ID 등)',
    image_url         VARCHAR(500) NOT NULL,
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    uploader_id       VARCHAR(255) NOT NULL,
    created_at        DATETIME     NULL,
    updated_at        DATETIME     NULL,
    CONSTRAINT fk_images_reference_type
        FOREIGN KEY (reference_type_id) REFERENCES reference_types (code)
            ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 메인 테이블';

CREATE INDEX idx_images_reference_type ON images (reference_type_id);
CREATE INDEX idx_images_reference_id ON images (reference_id);
CREATE INDEX idx_images_status ON images (status);

-- 1:1 with shared PK: storage_objects
CREATE TABLE storage_objects
(
    image_id            VARCHAR(255) PRIMARY KEY,
    storage_location    VARCHAR(1000) NOT NULL COMMENT '스토리지 경로',
    origin_size         BIGINT        NOT NULL COMMENT '원본 파일 크기',
    converted_size      BIGINT        NULL COMMENT '변환 후 파일 크기',
    origin_format_id    VARCHAR(16)   NOT NULL COMMENT '원본 확장자 코드',
    converted_format_id VARCHAR(16)   NULL COMMENT '변환 후 확장자 코드',
    CONSTRAINT fk_storage_objects_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_storage_objects_origin_format
        FOREIGN KEY (origin_format_id) REFERENCES extensions (code)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_storage_objects_converted_format
        FOREIGN KEY (converted_format_id) REFERENCES extensions (code)
            ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 스토리지 정보';

CREATE INDEX idx_storage_objects_origin_format ON storage_objects (origin_format_id);
CREATE INDEX idx_storage_objects_converted_format ON storage_objects (converted_format_id);

-- N:1 history
CREATE TABLE status_history
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id   VARCHAR(255)  NOT NULL,
    old_status VARCHAR(32)   NOT NULL COMMENT '이전 상태',
    new_status VARCHAR(32)   NOT NULL COMMENT '변경된 상태',
    updated_at DATETIME      NOT NULL,
    updated_by VARCHAR(255)  NULL COMMENT '변경자 ID',
    reason     VARCHAR(1000) NULL COMMENT '변경 사유',
    CONSTRAINT fk_status_history_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 상태 변경 이력';

CREATE INDEX idx_status_history_image ON status_history (image_id);
CREATE INDEX idx_status_history_updated_at ON status_history (updated_at);

-- N:1 variants
CREATE TABLE image_variants
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id     VARCHAR(255)  NOT NULL,
    variant_code VARCHAR(32)   NOT NULL COMMENT '변형 코드 (THUMBNAIL, SMALL, MEDIUM 등)',
    is_thumbnail TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '썸네일 여부',
    uploader_id  VARCHAR(255)  NULL,
    uploaded_at  DATETIME      NULL,
    width        INT           NULL COMMENT '이미지 너비',
    height       INT           NULL COMMENT '이미지 높이',
    url          VARCHAR(1000) NULL COMMENT '변형 이미지 URL',
    CONSTRAINT fk_image_variants_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 변형 (썸네일, 리사이즈 등)';

CREATE INDEX idx_image_variants_image ON image_variants (image_id);
CREATE INDEX idx_image_variants_code ON image_variants (variant_code);

-- Image sequence: 이미지 순서 관리
CREATE TABLE image_sequence
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기본키 (Auto Increment)',
    reference_id VARCHAR(255) NOT NULL COMMENT '참조 ID (상품 ID, 게시글 ID 등)',
    image_id     VARCHAR(255) NOT NULL COMMENT '이미지 ID',
    seq_number   INT          NOT NULL COMMENT '순서 번호 (0부터 시작)',
    created_at   DATETIME     NOT NULL COMMENT '생성 시간',
    updated_at   DATETIME     NULL COMMENT '수정 시간',
    CONSTRAINT fk_image_sequence_image
        FOREIGN KEY (image_id) REFERENCES images (image_id)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '이미지 순서 관리 (다중 이미지용)';

-- 인덱스
CREATE INDEX idx_reference_seq ON image_sequence (reference_id, seq_number);
CREATE INDEX idx_image_id ON image_sequence (image_id);

-- 유니크 제약
CREATE UNIQUE INDEX uk_reference_image ON image_sequence (reference_id, image_id);
CREATE UNIQUE INDEX uk_reference_seq ON image_sequence (reference_id, seq_number);


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
