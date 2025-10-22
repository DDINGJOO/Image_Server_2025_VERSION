-- =====================================================
-- ImageSequence 테이블 구조 변경
-- 버전: 2.0
-- 작성일: 2025-10-22
-- 목적: 복합 키 구조에서 단순 구조로 변경, referenceId 필드 추가
-- =====================================================

-- 1. 기존 데이터 백업 (선택사항)
CREATE TABLE IF NOT EXISTS image_sequence_backup AS
SELECT *
FROM image_sequence;

-- 2. 기존 테이블 삭제
DROP TABLE IF EXISTS image_sequence;

-- 3. 새 구조로 테이블 생성
CREATE TABLE image_sequence
(
    -- 기본 키 (Auto Increment)
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기본 키',

    -- 참조 ID (상품 ID, 게시글 ID 등)
    reference_id VARCHAR(255) NOT NULL COMMENT '참조 ID (상품, 게시글 등)',

    -- 이미지 ID
    image_id     VARCHAR(255) NOT NULL COMMENT '이미지 ID',

    -- 순서 번호 (0부터 시작)
    seq_number   INT          NOT NULL COMMENT '순서 번호 (0부터 시작)',

    -- 생성/수정 시간
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',

    -- 복합 유니크 제약 조건
    CONSTRAINT uk_reference_image UNIQUE (reference_id, image_id) COMMENT '같은 참조에 같은 이미지 중복 방지',
    CONSTRAINT uk_reference_seq UNIQUE (reference_id, seq_number) COMMENT '같은 참조에 같은 순서 번호 중복 방지',

    -- 인덱스
    INDEX idx_reference_seq (reference_id, seq_number) COMMENT '참조 ID + 순서로 빠른 조회',
    INDEX idx_image_id (image_id) COMMENT '이미지 ID로 역방향 조회',

    -- 외래 키 (이미지가 삭제되면 시퀀스도 자동 삭제)
    CONSTRAINT fk_image_sequence_image
        FOREIGN KEY (image_id)
            REFERENCES images (image_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='이미지 순서 관리 테이블';

-- 4. 기존 데이터 마이그레이션 (백업이 있는 경우)
-- 주의: 기존 데이터 구조가 달라서 자동 마이그레이션이 어려울 수 있음
-- 필요한 경우 애플리케이션 코드에서 마이그레이션 로직 실행

-- 5. 마이그레이션 완료 확인
SELECT 'Migration completed. Table structure:' AS status;

-- 6. 테이블 구조 확인
DESCRIBE image_sequence;
