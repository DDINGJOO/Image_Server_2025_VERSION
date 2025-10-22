-- MariaDB seed data for code tables and basic initializations
-- Execute after schema-mariadb.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Seed reference_types (새로운 구조에 맞춰 업데이트)
INSERT INTO reference_types (code, name, allows_multiple, max_images, description)
VALUES ('PRODUCT', '상품', 1, 10, '상품 이미지 (최대 10개)'),
       ('POST', '게시글', 1, 20, '게시글 이미지 (최대 20개)'),
       ('BANNER', '배너', 1, NULL, '배너 이미지 (무제한)'),
       ('CATEGORY', '카테고리', 0, 1, '카테고리 썸네일 (단일 이미지)'),
       ('PROFILE', '프로필', 0, 1, '사용자 프로필 이미지 (단일 이미지)'),
       ('USER_BACKGROUND', '사용자 배경', 0, 1, '사용자 배경 이미지 (단일 이미지)')
ON DUPLICATE KEY UPDATE name            = VALUES(name),
                        allows_multiple = VALUES(allows_multiple),
                        max_images      = VALUES(max_images),
                        description     = VALUES(description);

-- Seed extensions (common image/video types)
INSERT INTO extensions (code, name)
VALUES ('JPG', 'JPEG Image'),
       ('JPEG', 'JPEG Image'),
       ('PNG', 'Portable Network Graphics'),
       ('GIF', 'Graphics Interchange Format'),
       ('WEBP', 'WebP Image'),
       ('AVIF', 'AV1 Image File Format'),
       ('BMP', 'Bitmap Image'),
       ('SVG', 'Scalable Vector Graphics'),
       ('MP4', 'MPEG-4 Video'),
       ('MOV', 'QuickTime Movie'),
       ('HEIC', 'High Efficiency Image File Format'),
       ('HEIF', 'High Efficiency Image File Format')
ON DUPLICATE KEY UPDATE name = VALUES(name);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 예제 데이터 (주석 처리됨 - 필요시 주석 해제하여 사용)
-- ============================================================

-- 1. 이미지 생성 예제 (상품 이미지)
-- INSERT INTO images(image_id, status, reference_type_id, reference_id, image_url, is_deleted, uploader_id, created_at, updated_at)
-- VALUES ('img_product_001', 'CONFIRMED', 'PRODUCT', 'product_123', 'https://cdn.example.com/products/img_001.jpg', 0, 'user_001', NOW(), NOW()),
--        ('img_product_002', 'CONFIRMED', 'PRODUCT', 'product_123', 'https://cdn.example.com/products/img_002.jpg', 0, 'user_001', NOW(), NOW()),
--        ('img_product_003', 'CONFIRMED', 'PRODUCT', 'product_123', 'https://cdn.example.com/products/img_003.jpg', 0, 'user_001', NOW(), NOW());

-- 2. 스토리지 정보 예제
-- INSERT INTO storage_objects(image_id, storage_location, origin_size, converted_size, origin_format_id, converted_format_id)
-- VALUES ('img_product_001', '/bucket/originals/2025/01/img_product_001.jpg', 512000, 256000, 'JPG', 'WEBP'),
--        ('img_product_002', '/bucket/originals/2025/01/img_product_002.jpg', 480000, 240000, 'JPG', 'WEBP'),
--        ('img_product_003', '/bucket/originals/2025/01/img_product_003.jpg', 550000, 275000, 'PNG', 'WEBP');

-- 3. 이미지 순서 관리 예제 (상품의 이미지 순서)
-- INSERT INTO image_sequence(reference_id, image_id, seq_number, created_at, updated_at)
-- VALUES ('product_123', 'img_product_001', 0, NOW(), NOW()),
--        ('product_123', 'img_product_002', 1, NOW(), NOW()),
--        ('product_123', 'img_product_003', 2, NOW(), NOW());

-- 4. 상태 변경 이력 예제
-- INSERT INTO status_history(image_id, old_status, new_status, updated_at, updated_by, reason)
-- VALUES ('img_product_001', 'TEMP', 'CONFIRMED', NOW(), 'user_001', '상품 등록 완료'),
--        ('img_product_002', 'TEMP', 'CONFIRMED', NOW(), 'user_001', '상품 등록 완료'),
--        ('img_product_003', 'TEMP', 'CONFIRMED', NOW(), 'user_001', '상품 등록 완료');

-- 5. 이미지 변형 예제 (썸네일)
-- INSERT INTO image_variants(image_id, variant_code, is_thumbnail, uploader_id, uploaded_at, width, height, url)
-- VALUES ('img_product_001', 'THUMBNAIL', 1, 'system', NOW(), 200, 200, 'https://cdn.example.com/thumb/img_product_001.jpg'),
--        ('img_product_001', 'MEDIUM', 0, 'system', NOW(), 800, 800, 'https://cdn.example.com/medium/img_product_001.jpg');

-- 6. 단일 이미지 예제 (프로필 이미지)
-- INSERT INTO images(image_id, status, reference_type_id, reference_id, image_url, is_deleted, uploader_id, created_at, updated_at)
-- VALUES ('img_profile_001', 'CONFIRMED', 'PROFILE', 'user_001', 'https://cdn.example.com/profiles/user_001.jpg', 0, 'user_001', NOW(), NOW());
--
-- INSERT INTO storage_objects(image_id, storage_location, origin_size, converted_size, origin_format_id, converted_format_id)
-- VALUES ('img_profile_001', '/bucket/profiles/2025/01/user_001.jpg', 128000, 64000, 'JPG', 'WEBP');
