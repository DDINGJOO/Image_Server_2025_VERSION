-- MariaDB DDL & DML for image service

-- 1) Enum 타입 정의: MariaDB는 ENUM을 지원하므로 image_status.status 에 ENUM 사용
--    (대안: 별도 status 테이블을 두는 방법도 가능)
SET sql_mode = CONCAT(@@sql_mode, ',NO_ENGINE_SUBSTITUTION');

-- 2) reference_types 테이블: 레퍼런스 종류(예: POST, PRODUCT)
CREATE TABLE IF NOT EXISTS reference_types (
                                               id INT AUTO_INCREMENT PRIMARY KEY,
                                               code VARCHAR(64) NOT NULL UNIQUE,         -- 예: 'POST', 'PRODUCT'
                                               name VARCHAR(255) DEFAULT NULL,           -- 사람 읽기용 이름
                                               active TINYINT(1) NOT NULL DEFAULT 1,
                                               meta JSON DEFAULT NULL,
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) images 테이블: 이미지 메타
CREATE TABLE IF NOT EXISTS images (
                                      image_id VARCHAR(128) PRIMARY KEY,        -- 애플리케이션에서 생성하는 식별자(UUID 등)
                                      reference_type_id INT NULL,               -- FK -> reference_types.id
                                      reference_id VARCHAR(128) DEFAULT NULL,   -- 레퍼런스 엔티티 id (문자열로 둠)
                                      uploader_id VARCHAR(128) DEFAULT NULL,
                                      uploaded_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                                      is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                                      CONSTRAINT fk_images_reference_types FOREIGN KEY (reference_type_id)
                                          REFERENCES reference_types(id)
                                          ON DELETE SET NULL
                                          ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) image_variants: 한 이미지가 가질 수 있는 여러 변환/해상도/포맷
CREATE TABLE IF NOT EXISTS image_variants (
                                              variant_id INT AUTO_INCREMENT PRIMARY KEY,
                                              image_id VARCHAR(128) NOT NULL,
                                              seq_number INT DEFAULT 0,
                                              is_thumbnail TINYINT(1) NOT NULL DEFAULT 0,
                                              origin_format VARCHAR(64) DEFAULT NULL,      -- ex: 'jpg', 'png', 'webp' (참고로 extensions.name와 매핑)
                                              converted_format VARCHAR(64) DEFAULT NULL,
                                              origin_width INT DEFAULT NULL,
                                              origin_height INT DEFAULT NULL,
                                              converted_width INT DEFAULT NULL,
                                              converted_height INT DEFAULT NULL,
                                              origin_size BIGINT DEFAULT NULL,
                                              converted_size BIGINT DEFAULT NULL,
                                              quality_hint INT DEFAULT NULL,
                                              optimized TINYINT(1) NOT NULL DEFAULT 0,
                                              last_processed_at TIMESTAMP NULL DEFAULT NULL,
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                              CONSTRAINT fk_image_variants_images FOREIGN KEY (image_id)
                                                  REFERENCES images(image_id)
                                                  ON DELETE CASCADE
                                                  ON UPDATE CASCADE,
                                              INDEX idx_image_variants_image_id (image_id),
                                              INDEX idx_image_variants_seq_number (seq_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5) storage_objects: 실제 저장소 레코드 (S3 등)
CREATE TABLE IF NOT EXISTS storage_objects (
                                               storage_id INT AUTO_INCREMENT PRIMARY KEY,
                                               variant_id INT NOT NULL,
                                               storage_location VARCHAR(1024) NOT NULL,  -- ex: "s3://bucket/key" 또는 https URI
                                               storage_size BIGINT NOT NULL,             -- 실제 저장소에서 차지하는 바이트
                                               content_hash VARCHAR(128) DEFAULT NULL,   -- 예: SHA256 등
                                               mime_type VARCHAR(128) DEFAULT NULL,
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               CONSTRAINT fk_storage_objects_variant FOREIGN KEY (variant_id)
                                                   REFERENCES image_variants(variant_id)
                                                   ON DELETE CASCADE
                                                   ON UPDATE CASCADE,
                                               INDEX idx_storage_objects_variant (variant_id),
                                               INDEX idx_storage_objects_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6) image_status: 현재 이미지 상태 (ENUM 사용)
CREATE TABLE IF NOT EXISTS image_status (
                                            image_id VARCHAR(128) PRIMARY KEY,
                                            status ENUM ('TEMP','CONFIRMED','READY','DELETED','FAILED') NOT NULL DEFAULT 'TEMP',
                                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            updated_by VARCHAR(128) DEFAULT NULL,
                                            CONSTRAINT fk_image_status_images FOREIGN KEY (image_id)
                                                REFERENCES images(image_id)
                                                ON DELETE CASCADE
                                                ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7) status_history: 상태 변경 로그
CREATE TABLE IF NOT EXISTS status_history (
                                              id INT AUTO_INCREMENT PRIMARY KEY,
                                              image_id VARCHAR(128) NOT NULL,
                                              old_status ENUM ('TEMP','CONFIRMED','READY','DELETED','FAILED') DEFAULT NULL,
                                              new_status ENUM ('TEMP','CONFIRMED','READY','DELETED','FAILED') NOT NULL,
                                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_by VARCHAR(128) DEFAULT NULL,
                                              reason VARCHAR(1024) DEFAULT NULL,
                                              CONSTRAINT fk_status_history_images FOREIGN KEY (image_id)
                                                  REFERENCES images(image_id)
                                                  ON DELETE CASCADE
                                                  ON UPDATE CASCADE,
                                              INDEX idx_status_history_image_id (image_id),
                                              INDEX idx_status_history_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8) image_sequence: reference 단위 순번(예: 게시글 내 이미지 순서)
CREATE TABLE IF NOT EXISTS image_sequence (
                                              id INT AUTO_INCREMENT PRIMARY KEY,
                                              image_id VARCHAR(128) NOT NULL,
                                              reference_type_id INT NOT NULL,    -- reference_types.id
                                              reference_id VARCHAR(128) NOT NULL,
                                              seq_number INT NOT NULL,
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                              CONSTRAINT fk_image_sequence_images FOREIGN KEY (image_id)
                                                  REFERENCES images(image_id)
                                                  ON DELETE CASCADE
                                                  ON UPDATE CASCADE,
                                              CONSTRAINT fk_image_sequence_reference_types FOREIGN KEY (reference_type_id)
                                                  REFERENCES reference_types(id)
                                                  ON DELETE RESTRICT
                                                  ON UPDATE CASCADE,
    -- DB에서 (reference_type_id, reference_id, seq_number) 유니크 제약 권장
                                              UNIQUE KEY uk_image_sequence_ref_seq (reference_type_id, reference_id, seq_number),
                                              INDEX idx_image_sequence_reference (reference_type_id, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9) extensions: 확장자<->mime 매핑
CREATE TABLE IF NOT EXISTS extensions (
                                          id INT AUTO_INCREMENT PRIMARY KEY,
                                          name VARCHAR(64) UNIQUE NOT NULL,   -- ex: 'jpg', 'png', 'webp'
                                          mime VARCHAR(255) NOT NULL,
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10) shedlock: 분산 락(예: ShedLock 같은 구현용)
CREATE TABLE IF NOT EXISTS shedlock (
                                        name VARCHAR(64) PRIMARY KEY,
                                        lock_until TIMESTAMP(3) NULL,
                                        locked_at TIMESTAMP(3) NULL,
                                        locked_by VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 11) 참조: image_variants.origin_format -> extensions.name (논리적 매핑)
--     DB 레벨에서 직접 FK로 걸기는 힘들기 때문에(extensions.name이 PK가 아니므로)
--     만약 원한다면 extensions.name을 PK로 바꿔서 FK 가능. (아래는 옵션)
-- ALTER TABLE extensions DROP PRIMARY KEY, ADD PRIMARY KEY (name); -- 신중히 적용

-- =========================
-- 초기 DML (초기 설정 데이터)
-- =========================

-- reference_types 초기값
INSERT INTO reference_types (code, name, active, meta)
VALUES
    ('POST', '게시글', 1, JSON_OBJECT('description','게시글에 첨부된 이미지')),
    ('PRODUCT', '상품', 1, JSON_OBJECT('description','상품 이미지')),
    ('USER_AVATAR', '사용자 아바타', 1, JSON_OBJECT('description','유저 프로필 이미지')),
    ('BANNER', '배너', 1, JSON_OBJECT('description','웹/앱 배너 이미지'))
ON DUPLICATE KEY UPDATE name=VALUES(name), active=VALUES(active), meta=VALUES(meta);

-- extensions 초기값 (일반적인 이미지 확장자 + mime)
INSERT INTO extensions (name, mime) VALUES
                                        ('jpg', 'image/jpeg'),
                                        ('jpeg', 'image/jpeg'),
                                        ('png', 'image/png'),
                                        ('gif', 'image/gif'),
                                        ('webp', 'image/webp'),
                                        ('avif', 'image/avif')
ON DUPLICATE KEY UPDATE mime=VALUES(mime);

-- 예시: 샘플 이미지 및 상태 (테스트용)
INSERT INTO images (image_id, reference_type_id, reference_id, uploader_id, uploaded_at)
VALUES
    ('img-0001', (SELECT id FROM reference_types WHERE code='POST' LIMIT 1), 'post-100', 'user-1', NOW())
ON DUPLICATE KEY UPDATE uploaded_at=VALUES(uploaded_at);

INSERT INTO image_status (image_id, status, updated_by, updated_at)
VALUES
    ('img-0001', 'UPLOADED', 'system', NOW())
ON DUPLICATE KEY UPDATE status=VALUES(status), updated_by=VALUES(updated_by), updated_at=VALUES(updated_at);

-- 샘플 variant 및 storage
INSERT INTO image_variants (image_id, seq_number, is_thumbnail, origin_format, converted_format, origin_width, origin_height, origin_size)
VALUES
    ('img-0001', 1, 0, 'jpg', 'webp', 1920, 1080, 450000)
ON DUPLICATE KEY UPDATE origin_size=VALUES(origin_size);

INSERT INTO storage_objects (variant_id, storage_location, storage_size, content_hash, mime_type)
VALUES
    ((SELECT variant_id FROM image_variants WHERE image_id='img-0001' LIMIT 1),
     's3://my-bucket/images/img-0001-1.webp', 120000, 'sha256:examplehash', 'image/webp');

-- image_sequence 예시
INSERT INTO image_sequence (image_id, reference_type_id, reference_id, seq_number)
VALUES
    ('img-0001', (SELECT id FROM reference_types WHERE code='POST' LIMIT 1), 'post-100', 1)
ON DUPLICATE KEY UPDATE seq_number=VALUES(seq_number);

-- 상태 이력 예시
INSERT INTO status_history (image_id, old_status, new_status, updated_by, reason)
VALUES
    ('img-0001', NULL, 'UPLOADED', 'system', '초기 업로드');

-- 끝
