#!/bin/bash

# 멀티 아키텍처 Docker 이미지 빌드 및 푸시 스크립트
# ARM64와 AMD64를 모두 지원합니다

set -e  # 오류 발생 시 스크립트 종료

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# docker-compose.yml에서 이미지 정보 추출
COMPOSE_FILE="docker-compose.yml"
IMAGE_LINE=$(grep -m 1 "image: ddingsh9/image-server:" $COMPOSE_FILE)
CURRENT_IMAGE=$(echo $IMAGE_LINE | awk '{print $2}')
REPOSITORY=$(echo $CURRENT_IMAGE | cut -d: -f1)
CURRENT_VERSION=$(echo $CURRENT_IMAGE | cut -d: -f2)

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}멀티 아키텍처 Docker 이미지 빌드 및 푸시${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 버전 입력 받기
echo -e "${YELLOW}현재 버전: $CURRENT_VERSION${NC}"
read -p "새 버전을 입력하세요 (Enter 누르면 현재 버전 유지): " NEW_VERSION

if [ -z "$NEW_VERSION" ]; then
    NEW_VERSION=$CURRENT_VERSION
    echo -e "${YELLOW}현재 버전 유지: $NEW_VERSION${NC}"
else
    # docker-compose.yml 파일 업데이트
    echo -e "${YELLOW}docker-compose.yml 파일 업데이트 중...${NC}"
    sed -i.bak "s|$REPOSITORY:$CURRENT_VERSION|$REPOSITORY:$NEW_VERSION|g" $COMPOSE_FILE
    echo -e "${GREEN}✓ docker-compose.yml 업데이트 완료${NC}"
fi

# Docker buildx 설정 확인
echo -e "${YELLOW}Docker buildx 설정 확인 중...${NC}"

# buildx builder가 있는지 확인
if ! docker buildx ls | grep -q "multi-arch-builder"; then
    echo -e "${YELLOW}multi-arch-builder 생성 중...${NC}"
    docker buildx create --name multi-arch-builder --use
    docker buildx inspect --bootstrap
else
    echo -e "${GREEN}✓ multi-arch-builder 존재함${NC}"
    docker buildx use multi-arch-builder
fi

# 로그인 확인
echo -e "${YELLOW}Docker Hub 로그인 상태 확인 중...${NC}"
if ! docker info 2>/dev/null | grep -q "Username"; then
    echo -e "${YELLOW}Docker Hub에 로그인하세요${NC}"
    docker login
fi

# Gradle 빌드 실행
echo ""
echo -e "${YELLOW}Gradle 빌드 실행 중...${NC}"
./gradlew clean bootJar
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Gradle 빌드 성공${NC}"
else
    echo -e "${RED}✗ Gradle 빌드 실패${NC}"
    exit 1
fi

# Dockerfile 존재 확인
if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}✗ Dockerfile이 존재하지 않습니다${NC}"
    exit 1
fi

# 멀티 아키텍처 이미지 빌드 및 푸시
echo ""
echo -e "${YELLOW}멀티 아키텍처 이미지 빌드 중...${NC}"
echo -e "${YELLOW}대상 플랫폼: linux/amd64, linux/arm64${NC}"
echo -e "${YELLOW}태그: $REPOSITORY:$NEW_VERSION, $REPOSITORY:latest${NC}"

docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag $REPOSITORY:$NEW_VERSION \
    --tag $REPOSITORY:latest \
    --push \
    .

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ 빌드 및 푸시 성공!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Repository: $REPOSITORY${NC}"
    echo -e "${GREEN}버전 태그: $NEW_VERSION${NC}"
    echo -e "${GREEN}Latest 태그: latest${NC}"
    echo -e "${GREEN}지원 플랫폼: linux/amd64, linux/arm64${NC}"
    echo ""
    echo -e "${YELLOW}이미지 확인:${NC}"
    echo "  docker pull $REPOSITORY:$NEW_VERSION"
    echo "  docker pull $REPOSITORY:latest"
    echo ""
    echo -e "${YELLOW}서버 배포:${NC}"
    echo "  docker-compose pull"
    echo "  docker-compose up -d"
else
    echo -e "${RED}✗ 빌드 또는 푸시 실패${NC}"
    # 실패 시 docker-compose.yml 원래대로 복구
    if [ -f "$COMPOSE_FILE.bak" ]; then
        mv $COMPOSE_FILE.bak $COMPOSE_FILE
        echo -e "${YELLOW}docker-compose.yml 파일을 원래 상태로 복구했습니다${NC}"
    fi
    exit 1
fi

# 백업 파일 삭제
if [ -f "$COMPOSE_FILE.bak" ]; then
    rm $COMPOSE_FILE.bak
fi

echo -e "${GREEN}완료!${NC}"