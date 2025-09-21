-- 테스트용 기본 데이터 세팅 (H2 메모리 DB)
-- 확장자
INSERT INTO extensions (code, name) VALUES ('WEBP', 'WEBP');
INSERT INTO extensions (code, name) VALUES ('PNG', 'PNG');
INSERT INTO extensions (code, name) VALUES ('JPG', 'JPG');

-- 레퍼런스 타입
INSERT INTO reference_types (code, name) VALUES ('USER', '사용자');
INSERT INTO reference_types (code, name) VALUES ('PROFILE', '프로필');
