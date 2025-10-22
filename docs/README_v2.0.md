# Image Server

이미지 업로드, 처리, 확정 워크플로우를 관리하는 Spring Boot 마이크로서비스입니다.

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [주요 기능](#주요-기능)
3. [아키텍처](#아키텍처)
4. [데이터베이스 스키마](#데이터베이스-스키마)
5. [API 엔드포인트](#api-엔드포인트)
6. [기술 스택](#기술-스택)
7. [설정 및 실행](#설정-및-실행)
8. [배포](#배포)

---

## 프로젝트 개요

### 기본 정보

- **프로젝트명**: Image Server
- **타입**: Spring Boot REST API 마이크로서비스
- **Java**: 21
- **빌드**: Gradle 8.x
- **버전**: 2.0.0

### 핵심 목적

마이크로서비스 아키텍처 환경에서 이미지 라이프사이클을 전담 관리하는 서버입니다.

- 임시 업로드 → 확정 → 이벤트 발행의 워크플로우 제공
- 다중/단일 이미지 타입 지원
- 이미지 순서 관리 (ImageSequence)
- WebP 변환 및 최적화

---

## 주요 기능

### 1. 이미지 업로드 및 저장

- MultipartFile 업로드 (단일/배치)
- WebP 자동 변환 (80% 품질)
- 폴백 메커니즘 (변환 실패 시 원본 저장)
- 카테고리별 디렉토리 구조 (`/uploads/{CATEGORY}/{YYYY}/{MM}/{DD}/`)

### 2. 이미지 확정 워크플로우

- TEMP → CONFIRMED 상태 전환
- 단일 이미지 타입 (PROFILE, CATEGORY): 기존 이미지 자동 교체
- 다중 이미지 타입 (PRODUCT, POST): ImageSequence 자동 생성
- 상태 변경 이력 추적 (StatusHistory)

### 3. 도메인 이벤트 패턴

- 이미지 확정 시 도메인 이벤트 발행 (ImagesConfirmedEvent)
- TransactionalEventListener를 통한 트랜잭션 안전성
- ImageSequence 자동 생성 및 외부 이벤트 발행
- 이벤트 처리 실패 시 전체 롤백

### 4. 이벤트 기반 통합

- Kafka 통합 (이미지 변경 이벤트 발행)
- 토픽: `{reference-type}-image-changed`
- 하위 서비스로 이미지 URL 전파

### 5. 스케줄링

- 만료된 임시 이미지 자동 정리 (매일 오전 3:30)
- ShedLock 분산 락 (다중 인스턴스 환경)
- 2일 이상 TEMP 상태 이미지 삭제

### 6. 성능 최적화

- DB 조회 최적화 (최대 50% 감소)
- preloadedImage 재사용 패턴
- 인메모리 캐시 (Extension, ReferenceType)
- 배치 처리 지원

---

## 아키텍처

### 계층 구조

```
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  (ImageSaveController, ImageConfirm...)  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Service Layer                   │
│  (ImageSaveService, ImageConfirm...)     │
│  (ImageSequenceService)                  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Event Layer (도메인 이벤트)          │
│  (ImagesConfirmedEvent)                  │
│  (ImageSequenceEventHandler)             │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Repository Layer (JPA)             │
│  (ImageRepository, ImageSequence...)     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Entity Layer                    │
│  (Image, ImageSequence, StorageObject)   │
└─────────────────────────────────────────┘
```

### 디자인 패턴

#### 1. 도메인 이벤트 패턴

- 이미지 확정 완료 시 ImagesConfirmedEvent 발행
- ImageSequenceEventHandler가 이벤트 수신
- BEFORE_COMMIT 단계에서 ImageSequence 생성 및 외부 이벤트 발행
- 트랜잭션 안전성 보장

#### 2. 이벤트 기반 아키텍처

- Kafka를 통한 비동기 이벤트 발행
- 느슨한 결합 (Loose Coupling)
- 확장 가능한 구조

#### 3. Service Locator 패턴

- 시작 시 Extension/ReferenceType 인메모리 캐싱
- InitialSetup 클래스를 통한 빠른 조회

#### 4. Strategy 패턴

- LocalImageStorage를 통한 플러그 가능한 저장소
- WebP 변환 실패 시 폴백 전략

#### 5. Template Method 패턴

- StatusChanger를 통한 상태 전환 표준화
- 상태 이력 자동 기록

---

## 데이터베이스 스키마

### 핵심 엔티티

#### 1. reference_types (참조 타입)

```sql
code
VARCHAR
    (32)
    PRIMARY KEY  -- PRODUCT, POST, PROFILE 등
name            VARCHAR(64)              -- 표시명
allows_multiple TINYINT(1)               -- 다중 이미지 허용 여부
max_images      INT                      -- 최대 이미지 개수
description     VARCHAR(255) -- 설명
```

**초기 데이터:**

- PRODUCT (상품, 다중, 최대 10개)
- POST (게시글, 다중, 최대 20개)
- BANNER (배너, 다중, 무제한)
- CATEGORY (카테고리, 단일, 1개)
- PROFILE (프로필, 단일, 1개)
- USER_BACKGROUND (배경, 단일, 1개)

#### 2. extensions (확장자)

```sql
code
VARCHAR
    (16)
    PRIMARY KEY  -- JPG, PNG, WEBP 등
name VARCHAR(64) -- 표시명
```

**지원 형식:** JPG, JPEG, PNG, GIF, WEBP, AVIF, BMP, SVG, MP4, MOV, HEIC, HEIF

#### 3. images (이미지)

```sql
image_id
VARCHAR
    (255)
    PRIMARY KEY
status            VARCHAR(32)              -- TEMP, CONFIRMED, DELETED
reference_type_id VARCHAR(32)              -- FK to reference_types.code
reference_id      VARCHAR(200)             -- 참조 대상 ID
image_url         VARCHAR(500)
uploader_id       VARCHAR(255)
is_deleted        TINYINT(1)
created_at        DATETIME
updated_at        DATETIME
```

#### 4. image_sequence (이미지 순서)

```sql
id
BIGINT AUTO_INCREMENT PRIMARY KEY  -- 신규: Auto Increment
reference_id VARCHAR(255)                       -- 참조 ID
image_id     VARCHAR(255)                       -- FK to images
seq_number   INT                                -- 순서 (0부터 시작)
created_at   DATETIME
updated_at   DATETIME

-- 인덱스
idx_reference_seq (reference_id, seq_number)
idx_image_id (image_id)

-- 유니크 제약
uk_reference_image (reference_id, image_id)
uk_reference_seq (reference_id, seq_number)
```

**변경 사항 (v2.0):**

- 복합키 → Auto Increment 단일키
- reference_id 컬럼 추가
- created_at, updated_at 추가

#### 5. storage_objects (스토리지 정보)

```sql
image_id
VARCHAR
    (255)
    PRIMARY KEY
storage_location    VARCHAR(1000)
origin_size         BIGINT
converted_size      BIGINT
origin_format_id    VARCHAR(16)  -- FK to extensions.code
converted_format_id VARCHAR(16) -- FK to extensions.code
```

#### 6. status_history (상태 이력)

```sql
id
BIGINT AUTO_INCREMENT PRIMARY KEY
image_id   VARCHAR(255)
old_status VARCHAR(32)
new_status VARCHAR(32)
updated_at DATETIME
updated_by VARCHAR(255)
reason     VARCHAR(1000)
```

### ERD

```
┌──────────────────┐
│ reference_types  │
│──────────────────│
│ code (PK)        │───┐
│ name             │   │
│ allows_multiple  │   │
│ max_images       │   │
└──────────────────┘   │
                       │
┌──────────────────┐   │
│   extensions     │   │
│──────────────────│   │
│ code (PK)        │──┐│
│ name             │  ││
└──────────────────┘  ││
                      ││
┌──────────────────┐  ││
│     images       │  ││
│──────────────────│  ││
│ image_id (PK)    │◄─┼┘
│ reference_type_id│◄─┘
│ reference_id     │
│ status           │
└─────┬────────────┘
      │ 1:1
      ▼
┌──────────────────┐
│ storage_objects  │
│──────────────────│
│ image_id (PK/FK) │
│ storage_location │
│ origin_format_id │──┐
│ converted_format │◄─┘
└──────────────────┘

      │ 1:N
      ▼
┌──────────────────┐
│ status_history   │
│──────────────────│
│ id (PK)          │
│ image_id (FK)    │
│ old_status       │
│ new_status       │
└──────────────────┘

      │ 1:N
      ▼
┌──────────────────┐
│ image_sequence   │
│──────────────────│
│ id (PK)          │
│ reference_id     │
│ image_id (FK)    │
│ seq_number       │
└──────────────────┘
```

---

## API 엔드포인트

### 이미지 업로드

#### POST /api/images

**단일 이미지 업로드**

```http
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (필수)
- uploaderId: String (필수)
- category: String (필수) - PRODUCT, POST, PROFILE 등

Response:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "photo.jpg"
}
```

#### POST /api/images/batch

**배치 이미지 업로드**

```http
Content-Type: multipart/form-data

Parameters:
- files: List<MultipartFile> (필수)
- uploaderId: String (필수)
- category: String (필수)

Response:
[
  {
    "imageFileName": "photo1.jpg",
    "imageId": "550e8400-..."
  },
  ...
]
```

### 이미지 확정

#### PATCH /api/images/{referenceId}/confirm

**이미지 확정 처리**

```http
Query Parameters:
- imageId: List<String> (필수)

동작:
1. 단일 이미지 타입 (PROFILE):
   - 기존 이미지 DELETED 처리
   - 새 이미지 CONFIRMED 처리
   - 직접 외부 이벤트 발행

2. 다중 이미지 타입 (PRODUCT, POST):
   - 모든 이미지 CONFIRMED 처리
   - 도메인 이벤트 발행 (ImagesConfirmedEvent)
   - ImageSequence 자동 생성 (순서 유지)
   - 외부 이벤트 발행 (Kafka)

Response: 204 No Content
```

### Enums 조회

#### GET /api/enums/extensions

**지원 확장자 조회**

```json
{
  "JPG": "JPEG Image",
  "PNG": "Portable Network Graphics",
  "WEBP": "WebP Image",
  ...
}
```

#### GET /api/enums/referenceType

**참조 타입 조회**

```json
{
  "PRODUCT": "상품",
  "POST": "게시글",
  "PROFILE": "프로필",
  ...
}
```

### 헬스 체크

#### GET /health

```
200 OK
"Server is up"
```

---

## 기술 스택

### Core

- **Spring Boot**: 3.5.5
- **Java**: 21 (Eclipse Temurin)
- **Gradle**: 8.x

### Database

- **Production**: MariaDB
- **Test**: H2 (in-memory)
- **JPA**: Hibernate

### Messaging

- **Kafka**: spring-kafka
- **ShedLock**: 5.14.0 (분산 락)

### Image Processing

- **Scrimage**: 4.0.32 (WebP 변환)
- **libwebp7**: Native 라이브러리

### Development

- **Lombok**: 코드 간소화
- **Slf4j**: 로깅

### Testing

- **JUnit 5**
- **Mockito**: 5.2.0
- **Spring Boot Test**
- **@DataJpaTest**

---

## 설정 및 실행

### 로컬 실행 (dev 프로파일)

```bash
# 1. 환경 변수 설정
export SPRING_PROFILES_ACTIVE=dev

# 2. 데이터베이스 준비
mysql -u root -p < src/main/resources/sql/schema-mariadb.sql
mysql -u root -p < src/main/resources/sql/data-mariadb.sql

# 3. 실행
./gradlew bootRun
```

### 설정 파일

#### application.yaml

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

#### application-dev.yaml

```yaml
images:
  upload:
    dir: /path/to/uploads
    base-url: http://localhost:9200/images/

spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/images
    username: root
    password: pass123#
```

#### application-prod.yaml

```yaml
images:
  upload:
    dir: /uploads
    base-url: ${BASE_URL}

spring:
  datasource:
    url: jdbc:mariadb://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}
    username: ${DATABASE_USER_NAME}
    password: ${DATABASE_PASSWORD}

  kafka:
    bootstrap-servers:
      - ${KAFKA_URL1}
      - ${KAFKA_URL2}
      - ${KAFKA_URL3}
```

---

## 배포

### Docker Compose

#### 아키텍처

```
┌─────────────────┐
│  Nginx:9200     │ (로드 밸런서)
└────────┬────────┘
         │
    ┌────┴────┬────────┐
    │         │        │
┌───▼──┐  ┌───▼──┐  ┌──▼───┐
│Server│  │Server│  │Server│
│  1   │  │  2   │  │  3   │
└──┬───┘  └──┬───┘  └──┬───┘
   └─────────┼─────────┘
             │
    ┌────────▼────────┐
    │   MariaDB       │
    │   Redis         │
    │   Kafka Cluster │
    └─────────────────┘
```

#### docker-compose.yml

```yaml
services:
  image-server-1:
    image: ddingsh9/image-server:2.0.0
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_HOST=mariadb
      - KAFKA_URL1=kafka1:9091
      # ...
    volumes:
      - images-data:/uploads
    networks:
      - image-network
      - infra-network

  nginx:
    image: nginx:alpine
    ports:
      - "9200:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - images-data:/usr/share/nginx/html/images
    depends_on:
      - image-server-1
      - image-server-2
      - image-server-3

volumes:
  images-data:

networks:
  image-network:
  infra-network:
    external: true
```

#### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
    apt-get install -y libwebp7 ca-certificates && \
    rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 배포 단계

```bash
# 1. 빌드
./gradlew clean build

# 2. Docker 이미지 생성
docker build -t ddingsh9/image-server:2.0.0 .

# 3. 푸시
docker push ddingsh9/image-server:2.0.0

# 4. 배포
docker-compose up -d
```

---

## 프로젝트 구조

```
src/main/java/com/teambind/image_server/
├── controller/
│   ├── ImageSaveController.java
│   ├── ImageConfirmController.java
│   └── enumsController.java
│
├── service/
│   ├── ImageSaveService.java
│   ├── ImageConfirmService.java
│   ├── ImageSequenceService.java          # 신규
│   └── ScheduleService.java
│
├── repository/
│   ├── ImageRepository.java
│   ├── ImageSequenceRepository.java
│   └── ...
│
├── entity/
│   ├── Image.java
│   ├── ImageSequence.java                 # 개선
│   ├── ReferenceType.java                 # 개선
│   ├── Extension.java                     # 개선
│   └── StorageObject.java
│
├── event/
│   ├── events/
│   │   ├── ImagesConfirmedEvent.java      # 신규
│   │   ├── ImageChangeEvent.java
│   │   └── SequentialImageChangeEvent.java
│   ├── handler/
│   │   └── ImageSequenceEventHandler.java # 신규
│   └── publish/
│       └── ImageChangeEventPublisher.java
│
├── util/
│   ├── InitialSetup.java                  # 신규
│   ├── convertor/ImageUtil.java
│   ├── store/LocalImageStorage.java
│   ├── statuschanger/StatusChanger.java
│   └── validator/
│
└── config/
    └── DataInitializer.java

src/main/resources/
├── application*.yaml
└── sql/
    ├── schema-mariadb.sql                 # 업데이트
    └── data-mariadb.sql                   # 업데이트
```

---

## 주요 개선사항 (v2.0)

### 1. 도메인 이벤트 패턴 적용

- ImagesConfirmedEvent 도입
- ImageSequenceEventHandler를 통한 자동 처리
- 트랜잭션 안전성 보장 (BEFORE_COMMIT)

### 2. ImageSequence 구조 개선

- 복합키 → Auto Increment 단일키
- reference_id 컬럼 추가
- 인덱스 및 제약 최적화

### 3. 엔티티 키 변경

- ReferenceType: id (INT) → code (VARCHAR)
- Extension: extension_id (INT) → code (VARCHAR)
- 자연키(Natural Key) 사용으로 가독성 향상

### 4. ReferenceType 메타데이터 확장

- allows_multiple: 다중 이미지 허용 여부
- max_images: 최대 이미지 개수
- description: 설명

### 5. 성능 최적화

- DB 조회 최적화 (최대 50% 감소)
- preloadedImage 재사용 패턴
- 스마트 조회 (기존 이미지 먼저 조회 후 필터링)

### 6. 서비스 계층 책임 분리

- ImageSequenceService 신규 추가
- SRP(Single Responsibility Principle) 준수

---

## 문서

- **작성일**: 2025-10-22
- **버전**: 2.0.0
- **저자**: DDING
