# Docker 빌드 및 배포 가이드

## 스크립트 소개

이 프로젝트는 ARM64와 AMD64 아키텍처를 모두 지원하는 Docker 이미지를 빌드하고 배포하기 위한 스크립트를 제공합니다.

### 제공되는 스크립트

1. **`build-and-push.sh`** - 전체 빌드 및 푸시 프로세스
2. **`quick-build.sh`** - 자동 버전 증가 및 빠른 배포
3. **`build-local.sh`** - 로컬 테스트용 빌드

## 사용 방법

### 1. 전체 빌드 및 푸시 (`build-and-push.sh`)

멀티 아키텍처 이미지를 빌드하고 Docker Hub에 푸시합니다.

```bash
./build-and-push.sh
```

**특징:**

- ARM64와 AMD64 모두 지원
- 버전 태그와 latest 태그 동시 푸시
- docker-compose.yml 자동 업데이트
- 빌드 실패 시 자동 롤백

**프로세스:**

1. 새 버전 입력 (선택사항)
2. Gradle 빌드 실행
3. 멀티 아키텍처 이미지 빌드
4. Docker Hub에 푸시
5. docker-compose.yml 업데이트

### 2. 빠른 배포 (`quick-build.sh`)

패치 버전을 자동으로 증가시키고 배포합니다.

```bash
./quick-build.sh
```

**특징:**

- 버전 자동 증가 (예: 2.0.0.7 → 2.0.0.8)
- 사용자 입력 없이 자동 진행
- CI/CD 파이프라인에 적합

### 3. 로컬 테스트 (`build-local.sh`)

로컬 테스트용 이미지를 빌드합니다 (푸시하지 않음).

```bash
./build-local.sh
```

**특징:**

- 로컬 아키텍처용으로만 빌드
- Docker Hub에 푸시하지 않음
- `:local` 태그 사용

**테스트 방법:**

```bash
# 단일 컨테이너 실행
docker run --rm -p 8080:8080 ddingsh9/image-server:local

# docker-compose로 전체 스택 실행
docker-compose -f docker-compose-local.yml up
```

## 사전 요구사항

### 1. Docker Buildx 설정

```bash
# buildx 활성화 확인
docker buildx version

# multi-arch builder 생성 (처음 한 번만)
docker buildx create --name multi-arch-builder --use
docker buildx inspect --bootstrap
```

### 2. Docker Hub 로그인

```bash
docker login
```

### 3. Gradle 설치

프로젝트에 Gradle Wrapper가 포함되어 있으므로 별도 설치는 필요없습니다.

## 서버 배포

### 1. 이미지 업데이트 후 배포

```bash
# 최신 이미지 가져오기
docker-compose pull

# 서비스 재시작
docker-compose up -d

# 상태 확인
docker-compose ps
```

### 2. Nginx 설정 적용

nginx 설정을 변경한 경우:

```bash
docker-compose restart nginx
```

## 버전 관리

### 현재 버전 확인

```bash
grep "image: ddingsh9/image-server:" docker-compose.yml
```

### 버전 형식

`major.minor.patch.build`

예: `2.0.0.7`

- **major**: 주요 변경사항
- **minor**: 기능 추가
- **patch**: 버그 수정
- **build**: 빌드 번호

## 주의사항

1. **멀티 아키텍처 빌드는 시간이 걸립니다**
	- ARM64와 AMD64 모두 빌드하므로 일반 빌드보다 2-3배 시간 소요

2. **Docker Hub Rate Limit**
	- 무료 계정은 6시간당 200회 pull 제한
	- 인증된 사용자는 6시간당 200회 pull 제한

3. **디스크 공간**
	- 멀티 아키텍처 빌드는 더 많은 디스크 공간 필요
	- 정기적으로 `docker system prune` 실행 권장

## 문제 해결

### buildx가 작동하지 않을 때

```bash
# Docker 재시작
sudo systemctl restart docker

# buildx 재설정
docker buildx rm multi-arch-builder
docker buildx create --name multi-arch-builder --use
```

### 빌드 실패 시

```bash
# 빌드 캐시 정리
docker buildx prune -f

# 전체 시스템 정리
docker system prune -a -f
```

### push 권한 오류

```bash
# Docker Hub 재로그인
docker logout
docker login
```

## 지원되는 플랫폼

- `linux/amd64` - Intel/AMD 64비트 프로세서
- `linux/arm64` - Apple Silicon (M1/M2), AWS Graviton, Raspberry Pi 4

## 관련 파일

- `Dockerfile` - Docker 이미지 빌드 설정
- `docker-compose.yml` - 프로덕션 배포 설정
- `docker-compose-local.yml` - 로컬 테스트 설정
- `nginx/conf/default.conf` - Nginx 설정
