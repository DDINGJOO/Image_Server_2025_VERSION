#!/bin/bash

# 빠른 빌드 스크립트 - 자동으로 패치 버전을 증가시킵니다

set -e

# 색상 코드 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# docker-compose.yml에서 현재 버전 추출
COMPOSE_FILE="docker-compose.yml"
IMAGE_LINE=$(grep -m 1 "image: ddingsh9/image-server:" $COMPOSE_FILE)
CURRENT_IMAGE=$(echo $IMAGE_LINE | awk '{print $2}')
REPOSITORY=$(echo $CURRENT_IMAGE | cut -d: -f1)
CURRENT_VERSION=$(echo $CURRENT_IMAGE | cut -d: -f2)

# 버전 자동 증가 (패치 버전)
IFS='.' read -r -a version_parts <<< "$CURRENT_VERSION"
major="${version_parts[0]}"
minor="${version_parts[1]}"
patch="${version_parts[2]}"
patch=$((patch + 1))
NEW_VERSION="$major.$minor.$patch"

echo -e "${GREEN}자동 버전 업그레이드${NC}"
echo -e "${YELLOW}$CURRENT_VERSION → $NEW_VERSION${NC}"

# docker-compose.yml 업데이트
sed -i.bak "s|$REPOSITORY:$CURRENT_VERSION|$REPOSITORY:$NEW_VERSION|g" $COMPOSE_FILE

# Gradle 빌드
./gradlew clean bootJar

# 멀티 아키텍처 빌드 및 푸시
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag $REPOSITORY:$NEW_VERSION \
    --tag $REPOSITORY:latest \
    --push \
    .

# 백업 파일 삭제
rm -f $COMPOSE_FILE.bak

echo -e "${GREEN}✓ 완료! $REPOSITORY:$NEW_VERSION${NC}"