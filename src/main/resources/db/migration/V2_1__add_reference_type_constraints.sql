-- =====================================================
-- ReferenceType 테이블에 다중/단일 이미지 제약 추가
-- 버전: 2.1
-- 작성일: 2025-10-22
-- 목적: 참조 타입별로 허용되는 이미지 개수 제약 추가
-- =====================================================

-- 1. 새 컬럼 추가
ALTER TABLE reference_types
    ADD COLUMN allows_multiple BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '다중 이미지 허용 여부',
    ADD COLUMN max_images      INT          NULL COMMENT '최대 이미지 개수 (NULL이면 무제한)',
    ADD COLUMN description     VARCHAR(255) NULL COMMENT '설명';

-- 2. 기존 데이터 업데이트
UPDATE reference_types
SET allows_multiple = CASE
                          WHEN code = 'PROFILE' THEN FALSE -- 프로필은 단일 이미지만
                          ELSE TRUE -- 나머지는 다중 허용
    END,
    max_images      = CASE
                          WHEN code = 'PROFILE' THEN 1 -- 프로필 1개
                          WHEN code = 'PRODUCT' THEN 10 -- 상품 최대 10개
                          WHEN code = 'POST' THEN 10 -- 게시글 최대 10개
                          WHEN code = 'BANNER' THEN 5 -- 배너 최대 5개
                          ELSE NULL -- 나머지 무제한
        END,
    description     = CASE
                          WHEN code = 'PROFILE' THEN '프로필 이미지 (1개만 허용)'
                          WHEN code = 'PRODUCT' THEN '상품 이미지 (최대 10개)'
                          WHEN code = 'USER' THEN '사용자 관련 이미지'
                          WHEN code = 'POST' THEN '게시글 이미지 (최대 10개)'
                          WHEN code = 'BANNER' THEN '배너 이미지 (최대 5개)'
                          WHEN code = 'CATEGORY' THEN '카테고리 대표 이미지'
                          ELSE NULL
        END;

-- 3. 제약 조건 추가
ALTER TABLE reference_types
    ADD CONSTRAINT chk_max_images_positive CHECK (max_images IS NULL OR max_images > 0);

-- 4. 결과 확인
SELECT code,
       name,
       allows_multiple,
       max_images,
       description,
       CASE
           WHEN allows_multiple = FALSE THEN '단일'
           WHEN max_images IS NULL THEN '다중 (무제한)'
           ELSE CONCAT('다중 (최대 ', max_images, '개)')
           END AS constraint_summary
FROM reference_types
ORDER BY code;

-- 5. 마이그레이션 완료 메시지
SELECT 'ReferenceType 제약 조건이 추가되었습니다.' AS status;
