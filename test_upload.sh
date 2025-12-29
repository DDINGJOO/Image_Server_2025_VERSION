#!/bin/bash

# 테스트 이미지 업로드 스크립트
# 사용법: ./test_upload.sh

API_URL="http://localhost:9200/api/images"

echo "======================================="
echo "이미지 업로드 API 테스트"
echo "======================================="

# 단일 이미지 업로드 테스트
echo ""
echo "1. 단일 이미지 업로드 테스트"
echo "-------------------------------"
echo "요청: POST $API_URL"
echo "파라미터: file, uploaderId=user123, category=BOOK"

curl -X POST $API_URL \
  -F "file=@test_image.jpg" \
  -F "uploaderId=user123" \
  -F "category=BOOK" | json_pp

echo ""
echo ""

# 다중 이미지 업로드 테스트
echo "2. 다중 이미지 업로드 테스트"
echo "-------------------------------"
echo "요청: POST $API_URL"
echo "파라미터: files[], uploaderId=user456, category=PROFILE"

curl -X POST $API_URL \
  -F "files=@test_image1.jpg" \
  -F "files=@test_image2.jpg" \
  -F "files=@test_image3.jpg" \
  -F "uploaderId=user456" \
  -F "category=PROFILE" | json_pp

echo ""
echo ""

# 대용량 파일 테스트 (10MB 이하)
echo "3. 대용량 파일 업로드 테스트 (8MB)"
echo "-------------------------------"
echo "요청: POST $API_URL"
echo "파라미터: file, uploaderId=user789, category=BOOK"

# 8MB 테스트 파일 생성 (없으면)
if [ ! -f "large_test.jpg" ]; then
    echo "8MB 테스트 파일 생성 중..."
    dd if=/dev/zero of=large_test.jpg bs=1M count=8 2>/dev/null
fi

curl -X POST $API_URL \
  -F "file=@large_test.jpg" \
  -F "uploaderId=user789" \
  -F "category=BOOK" | json_pp

echo ""
echo "======================================="
echo "테스트 완료!"
echo "======================================="