-- MariaDB seed data for code tables and basic initializations
-- Execute after schema-mariadb.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Seed reference_types
INSERT INTO reference_types (code, name)
VALUES ('PRODUCT', '상품'),
       ('USER', '사용자'),
       ('POST', '게시글'),
       ('BANNER', '배너'),
       ('CATEGORY', '카테고리')
ON DUPLICATE KEY UPDATE name = VALUES(name);

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
       ('MOV', 'QuickTime Movie')
ON DUPLICATE KEY UPDATE name = VALUES(name);

SET FOREIGN_KEY_CHECKS = 1;

-- Optional examples (commented out):
-- INSERT INTO images(image_id, status, reference_type_id, image_url, is_deleted, created_at, updated_at)
-- SELECT 'img_001', 'TEMP', rt.id, 'https://cdn.example.com/img/img_001.jpg', 0, NOW(), NOW()
-- FROM reference_types rt WHERE rt.code = 'PRODUCT' LIMIT 1;
--
-- INSERT INTO storage_objects(image_id, storage_location, origin_size, converted_size, origin_format_id, converted_format_id)
-- SELECT 'img_001', '/bucket/originals/2025/09/img_001.jpg', 512000, NULL,
--        (SELECT extension_id FROM extensions WHERE code='JPG' LIMIT 1), NULL;
--
-- INSERT INTO status_history(image_id, old_status, new_status, updated_at, updated_by, reason)
-- VALUES ('img_001', 'TEMP', 'READY', NOW(), 'system', 'initial import');
--
-- INSERT INTO image_variants(image_id, variant_code, is_thumbnail, uploader_id, uploaded_at, width, height, url)
-- VALUES ('img_001', 'THUMBNAIL', 1, 'system', NOW(), 200, 200, 'https://cdn.example.com/img/thumb/img_001.jpg');
--
-- INSERT INTO image_sequence(id, image_id, seq_number) VALUES (1, 'img_001', 1);
