#!/bin/bash

# 로컬 테스트용 빌드 스크립트 (푸시하지 않음)

set -e

# 색상 코드 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}로컬 Docker 이미지 빌드 (테스트용)${NC}"
echo -e "${BLUE}========================================${NC}"

# docker-compose.yml에서 이미지 정보 추출
COMPOSE_FILE="docker-compose.yml"
IMAGE_LINE=$(grep -m 1 "image: ddingsh9/image-server:" $COMPOSE_FILE)
CURRENT_IMAGE=$(echo $IMAGE_LINE | awk '{print $2}')
REPOSITORY=$(echo $CURRENT_IMAGE | cut -d: -f1)
VERSION=$(echo $CURRENT_IMAGE | cut -d: -f2)

echo -e "${YELLOW}빌드할 이미지: $REPOSITORY:local${NC}"

# Gradle 빌드
echo -e "${YELLOW}Gradle 빌드 실행 중...${NC}"
./gradlew clean bootJar

# 로컬 아키텍처용 이미지 빌드 (푸시 없음)
echo -e "${YELLOW}로컬 이미지 빌드 중...${NC}"
docker build -t $REPOSITORY:local .

echo ""
echo -e "${GREEN}✓ 로컬 빌드 완료!${NC}"
echo -e "${GREEN}테스트 실행:${NC}"
echo "  docker run --rm -p 8080:8080 $REPOSITORY:local"
echo ""
echo -e "${GREEN}또는 docker-compose-local.yml 사용:${NC}"
echo "  docker-compose -f docker-compose-local.yml up"