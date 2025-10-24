# Image Server

이미지 업로드, 처리, 확정 워크플로우를 관리하는 Spring Boot 마이크로서비스입니다.

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [주요 기능](#주요-기능)
3. [아키텍처](#아키텍처)
4. [데이터베이스 스키마](#데이터베이스-스키마)
5. [API 엔드포인트](#api-엔드포인트)
6. [기술 스택](#기술-스택)
7. [테스트](#테스트)
8. [성능](#성능)
9. [설정 및 실행](#설정-및-실행)
10. [배포](#배포)

---

## 프로젝트 개요

### 기본 정보

- **프로젝트명**: Image Server
- **타입**: Spring Boot REST API 마이크로서비스
- **Java**: 21
- **빌드**: Gradle 8.x
- **버전**: 2.0.1

### 핵심 목적

마이크로서비스 아키텍처 환경에서 이미지 라이프사이클을 전담 관리하는 서버입니다.

- 임시 업로드 → 확정 → 이벤트 발행의 워크플로우 제공
- 다중/단일 이미지 타입 지원
- 이미지 순서 관리 (ImageSequence)
- WebP 변환 및 최적화
- 비동기 Task Queue 기반 대량 이미지 처리

---

## 주요 기능

### 1. 이미지 업로드 및 저장

#### 동기 방식 (기본)
- MultipartFile 업로드 (단일/배치)
- WebP 자동 변환 (80% 품질)
- 폴백 메커니즘 (변환 실패 시 원본 저장)
- 카테고리별 디렉토리 구조 (`/uploads/{CATEGORY}/{YYYY}/{MM}/{DD}/`)
- 즉시 처리 및 응답 반환

#### 비동기 방식 (v2.0.1 신규)

- Task Queue 기반 이미지 처리
- TEMP 상태로 즉시 응답 (빠른 응답 속도)
- 백그라운드에서 WebP 변환 및 READY 상태 전환
- 스레드 풀 고갈 방지 (최대 500개 큐잉)
- **성능**: 동기 방식 대비 약 7배 빠른 처리량 (208 vs 28 개/초)

### 2. 이미지 확정 워크플로우

- TEMP → READY → CONFIRMED 상태 전환
- 단일 이미지 타입 (PROFILE, CATEGORY): 기존 이미지 자동 교체
- 다중 이미지 타입 (PRODUCT, POST): ImageSequence 자동 생성
- 상태 변경 이력 추적 (StatusHistory)

### 3. 도메인 이벤트 패턴

- 이미지 확정 시 도메인 이벤트 발행 (ImagesConfirmedEvent)
- `@TransactionalEventListener(AFTER_COMMIT)` 적용 (v2.0.1 개선)
- 트랜잭션 커밋 후 이벤트 발행으로 데이터 정합성 보장
- ImageSequence 자동 생성 및 외부 이벤트 발행
- 이벤트 처리 실패 시 전체 롤백

### 4. 이벤트 기반 통합

- Kafka 통합 (이미지 변경 이벤트 발행)
- 토픽: `{reference-type}-image-changed`
- 하위 서비스로 이미지 URL 전파
- 재시도 로직 (최대 3회, 지수 백오프)
- 발행 실패 시 로깅 및 알림

### 5. 스케줄링

#### 임시 이미지 정리 스케줄러
- 만료된 임시 이미지 자동 정리 (매일 오전 3:30)
- ShedLock 분산 락 (다중 인스턴스 환경)
- 2일 이상 TEMP 상태 이미지 삭제

#### 실패 이미지 정리 스케줄러 (v2.0.1 신규)

- 매일 새벽 3시, 오전 9시 실행
- FAILED 상태 7일 이상 이미지 삭제
- TEMP 상태 24시간 이상 이미지 삭제
- 스토리지 공간 최적화

### 6. 성능 최적화

- DB 조회 최적화 (최대 50% 감소)
- preloadedImage 재사용 패턴
- 인메모리 캐시 (Extension, ReferenceType)
- 배치 처리 지원
- 비동기 Task Queue (ThreadPoolTaskExecutor)
	- Core Pool Size: 10
	- Max Pool Size: 20
	- Queue Capacity: 500

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
│    Async Task Queue (v2.0.1)            │
│  (ThreadPoolTaskExecutor)                │
│  - Image Processing                      │
│  - WebP Conversion                       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Event Layer (도메인 이벤트)          │
│  (ImagesConfirmedEvent)                  │
│  (ImageSequenceEventHandler)             │
│  @TransactionalEventListener             │
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
- AFTER_COMMIT 단계에서 ImageSequence 생성 및 외부 이벤트 발행
- 트랜잭션 안전성 보장

#### 2. 비동기 Task Queue 패턴 (v2.0.1 신규)

- ThreadPoolTaskExecutor 기반 작업 큐
- 즉시 응답 후 백그라운드 처리
- 스레드 풀 고갈 방지
- 대량 이미지 처리 최적화

#### 3. 이벤트 기반 아키텍처

- Kafka를 통한 비동기 이벤트 발행
- 느슨한 결합 (Loose Coupling)
- 확장 가능한 구조

#### 4. Service Locator 패턴

- 시작 시 Extension/ReferenceType 인메모리 캐싱
- InitialSetup 클래스를 통한 빠른 조회

#### 5. Strategy 패턴

- LocalImageStorage를 통한 플러그 가능한 저장소
- WebP 변환 실패 시 폴백 전략

#### 6. Template Method 패턴

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
status            VARCHAR(32)              -- TEMP, READY, CONFIRMED, DELETED, FAILED
reference_type_id VARCHAR(32)              -- FK to reference_types.code
reference_id      VARCHAR(200)             -- 참조 대상 ID
image_url         VARCHAR(500)
uploader_id       VARCHAR(255)
is_deleted        TINYINT(1)
created_at        DATETIME
updated_at        DATETIME
```

**상태 전이 (v2.0.1):**

- **TEMP**: 초기 업로드 상태
- **READY**: WebP 변환 완료 (비동기 처리 완료)
- **CONFIRMED**: 확정 완료
- **DELETED**: 삭제 상태
- **FAILED**: 처리 실패

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

#### POST /api/v1/images (동기 방식)

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

#### POST /api/v1/images/async (비동기 방식, v2.0.1 신규)

**단일 이미지 비동기 업로드**

```http
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (필수)
- uploaderId: String (필수)
- category: String (필수)

Response:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "photo.jpg",
  "status": "TEMP"
}

동작:
1. TEMP 상태로 즉시 응답 반환
2. 백그라운드에서 WebP 변환 처리
3. 처리 완료 시 READY 상태로 전환
4. 약 7배 빠른 처리량 (대량 업로드 시 유리)
```

#### POST /api/v1/images/batch

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

#### POST /api/v1/images/confirm/{referenceId}

**단일 이미지 확정**

```http
Query Parameters:
- imageId: String (필수)

동작:
1. 단일 이미지 타입 (PROFILE):
   - 기존 이미지 DELETED 처리
   - 새 이미지 CONFIRMED 처리
   - 직접 외부 이벤트 발행

Response: 204 No Content
```

#### POST /api/v1/images/confirm

**다중 이미지 배치 확정**

```http
Content-Type: application/json

Request Body:
{
  "imageIds": ["id1", "id2", "id3"],
  "referenceId": "ref-456"
}

동작:
1. 다중 이미지 타입 (PRODUCT, POST):
   - 모든 이미지 CONFIRMED 처리
   - 도메인 이벤트 발행 (ImagesConfirmedEvent)
   - ImageSequence 자동 생성 (순서 유지)
   - 외부 이벤트 발행 (Kafka)

Response: 204 No Content
```

### Enums 조회

#### GET /api/v1/enums/extensions

**지원 확장자 조회**

```json
{
  "JPG": "JPEG Image",
  "PNG": "Portable Network Graphics",
  "WEBP": "WebP Image",
  ...
}
```

#### GET /api/v1/enums/referenceType

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

#### GET /actuator/health

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MariaDB",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "kafka": {
      "status": "UP"
    },
    "fileSystem": {
      "status": "UP",
      "details": {
        "baseDir": "/uploads",
        "writable": true
      }
    }
  }
}
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

### Async Processing (v2.0.1)

- **ThreadPoolTaskExecutor**: 비동기 Task Queue
	- Core: 10, Max: 20, Queue: 500
- **@Async**: Spring 비동기 처리

### Monitoring

- **Spring Boot Actuator**: 헬스 체크 및 메트릭
- **Custom Health Indicators**: Kafka, FileSystem

### Development

- **Lombok**: 코드 간소화
- **Slf4j**: 로깅

### Testing

- **JUnit 5**: 단위 테스트 프레임워크
- **Mockito**: 5.2.0 (Static mocking 포함)
- **Spring Boot Test**: 통합 테스트 지원
- **MockMvc**: Controller 레이어 테스트
- **AssertJ**: 유창한 assertion 라이브러리
- **H2 Database**: 인메모리 테스트 데이터베이스
- **TestEntityManager**: JPA 슬라이스 테스트

---

## 테스트

### 테스트 전략

이 프로젝트는 **포괄적인 단위 테스트**를 통해 코드 품질과 안정성을 보장합니다.

#### 테스트 커버리지

```
총 테스트 수: 203+
성공률: 100%
```

### 테스트 구조

#### 1. Validator Layer (100+ 테스트)

**파일**: `src/test/java/com/teambind/image_server/util/validator/`

- `CategoryValidatorTest`: 카테고리 유효성 검증
- `ExtensionValidatorTest`: 파일 확장자 검증
- `ReferenceTypeValidatorTest`: 참조 타입 검증
- `RequestDtoValidatorTest`: 요청 DTO 검증

**실행**:
```bash
./gradlew test --tests "*validator*"
```

#### 2. Entity Layer (25 테스트)

**파일**: `src/test/java/com/teambind/image_server/entity/`

- `ImageTest`: Image 엔티티
- `ReferenceTypeTest`: ReferenceType 엔티티

**실행**:
```bash
./gradlew test --tests "*entity*"
```

#### 3. DTO Layer (10 테스트)

**파일**: `src/test/java/com/teambind/image_server/dto/`

- 요청/응답 DTO 빌더 패턴 테스트
- 유효성 검증 어노테이션 테스트

**실행**:
```bash
./gradlew test --tests "*dto*"
```

#### 4. Controller Layer (33 테스트)

**파일**: `src/test/java/com/teambind/image_server/controller/`

**ImageSaveControllerTest** (22 테스트):

- 단일/다중 이미지 업로드
- 필수 파라미터 검증
- MockMvc 기반 API 테스트

**ImageConfirmControllerTest** (11 테스트):

- 단일/다중 이미지 확정
- 엣지 케이스 처리

**실행**:
```bash
./gradlew test --tests "*controller*"
```

#### 5. Service Layer (20 테스트)

**파일**: `src/test/java/com/teambind/image_server/service/`

**ImageSaveServiceTest** (10 테스트):
- WebP 변환 성공/실패 시나리오
- Rosetta 에러 폴백 메커니즘

**ImageConfirmServiceTest** (10 테스트):
- 신규 이미지 확정
- 기존 이미지 교체
- MONO/MULTI 타입 처리

**실행**:
```bash
./gradlew test --tests "*service*"
```

#### 6. Repository Layer (9 테스트)

**파일**: `src/test/java/com/teambind/image_server/repository/`

**ImageRepositoryTest**:
- JPA 쿼리 메서드 테스트
- 복합 키 조회
- 배치 처리

**실행**:
```bash
./gradlew test --tests "*repository*"
```

#### 7. Performance Tests (6 테스트, v2.0.1 신규)

**파일**: `src/test/java/com/teambind/image_server/performance/`

**ImageProcessingPerformanceTest**:

- 동기 vs 비동기 성능 비교 (100개, 200개)
- 동시성 테스트 (50개 동시 요청)
- 부하 테스트

**실행**:

```bash
./gradlew test --tests "*performance*"
```

### 테스트 환경 설정

#### application-test.yaml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

images:
  upload:
    dir: ${java.io.tmpdir}/test-images
  base-url: http://localhost:8080/images/
```

### 테스트 픽스처

**TestFixtureFactory**: 테스트 데이터 생성 팩토리

```java
// 엔티티 생성
Image image = TestFixtureFactory.createTempImage("image-123");
Image readyImage = TestFixtureFactory.createReadyImage("image-456");
ReferenceType profile = TestFixtureFactory.createProfileReferenceType();
Extension jpg = TestFixtureFactory.createJpgExtension();

// MockMultipartFile 생성
MultipartFile file = TestFixtureFactory.createValidImageFile();
List<MultipartFile> files = TestFixtureFactory.createValidImageFiles(3);
```

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 레이어 테스트
./gradlew test --tests "*controller*"
./gradlew test --tests "*service*"
./gradlew test --tests "*performance*"

# 테스트 리포트 생성
./gradlew test jacocoTestReport

# 빌드 with 테스트
./gradlew clean build
```

### 테스트 베스트 프랙티스

1. **Given-When-Then 패턴**: 모든 테스트는 명확한 구조를 따름
2. **DisplayName 사용**: 한글로 테스트 의도 명확히 표현
3. **Mock 분리**: `@Mock`, `@InjectMocks`로 의존성 분리
4. **Static Mocking**: ImageUtil.toWebp() 등 정적 메서드 모킹
5. **테스트 격리**: `@BeforeEach`로 초기화, 각 테스트 독립 실행
6. **AssertJ 활용**: 유창한 assertion으로 가독성 향상

---

## 성능

### 성능 테스트 결과 (v2.0.1)

상세한 성능 분석은 [docs/ASYNC_PERFORMANCE_COMPARISON.md](docs/ASYNC_PERFORMANCE_COMPARISON.md) 참조

#### 1. 동기 방식 부하 테스트 (200개 이미지)

- **총 처리 시간**: 7.211초
- **성공**: 200개 (100%)
- **처리량**: 27.74개/초

#### 2. 비동기 방식 부하 테스트 (200개 이미지)

- **작업 제출 시간**: 0.961초
- **성공**: 200개 (100%)
- **제출 처리량**: 208.12개/초
- **성능 향상**: **약 7.5배**

#### 3. 동시성 테스트 비교 (50개 동시 요청)

| 방식  | 처리 시간  | 처리량       | 성공률  |
|-----|--------|-----------|------|
| 동기  | 0.252초 | 198.41개/초 | 100% |
| 비동기 | 0.166초 | 301.20개/초 | 100% |

### 권장 사용 시나리오

#### 동기 방식 사용 시

- 소량 이미지 업로드 (1~10개)
- 즉시 변환 결과 필요
- 순차 처리 보장 필요

#### 비동기 방식 사용 시

- 대량 이미지 업로드 (50개 이상)
- 빠른 응답 속도 필요
- 백그라운드 처리 허용 가능
- 배치 작업

### 성능 최적화 기법

1. **ThreadPoolTaskExecutor 설정**
	- Core: 10, Max: 20, Queue: 500
	- 스레드 풀 고갈 방지

2. **데이터베이스 최적화**
	- 인메모리 캐시 (Extension, ReferenceType)
	- preloadedImage 재사용 패턴
	- 배치 처리 지원

3. **이미지 처리 최적화**
	- WebP 변환 (80% 품질)
	- 폴백 메커니즘
	- 비동기 변환

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

  task:
    execution:
      pool:
        core-size: 10
        max-size: 20
        queue-capacity: 500
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

  kafka:
    bootstrap-servers: localhost:9092
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

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
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
    image: ddingsh9/image-server:2.0.1
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
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8080/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3

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
    apt-get install -y libwebp7 ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 배포 단계

```bash
# 1. 빌드
./gradlew clean build

# 2. Docker 이미지 생성
docker build -t ddingsh9/image-server:2.0.1 .

# 3. 푸시
docker push ddingsh9/image-server:2.0.1

# 4. 배포
docker-compose up -d

# 5. 헬스 체크
curl http://localhost:9200/actuator/health
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
│   ├── ImageSaveService.java           # 동기/비동기 처리
│   ├── ImageConfirmService.java
│   ├── ImageSequenceService.java
│   └── ScheduleService.java
│
├── config/
│   ├── AsyncConfig.java                # v2.0.1 신규
│   └── DataInitializer.java
│
├── scheduler/
│   ├── TempImageCleanupScheduler.java
│   └── FailedImageCleanupScheduler.java # v2.0.1 신규
│
├── repository/
│   ├── ImageRepository.java
│   ├── ImageSequenceRepository.java
│   └── ...
│
├── entity/
│   ├── Image.java
│   ├── ImageSequence.java
│   ├── ReferenceType.java
│   ├── Extension.java
│   └── StorageObject.java
│
├── event/
│   ├── events/
│   │   ├── ImagesConfirmedEvent.java
│   │   ├── ImageChangeEvent.java
│   │   └── SequentialImageChangeEvent.java
│   ├── handler/
│   │   └── ImageSequenceEventHandler.java
│   └── publish/
│       └── ImageChangeEventPublisher.java # AFTER_COMMIT
│
└── util/
    ├── InitialSetup.java
    ├── convertor/ImageUtil.java
    ├── store/LocalImageStorage.java
    ├── statuschanger/StatusChanger.java
    └── validator/

src/test/java/com/teambind/image_server/
├── performance/                         # v2.0.1 신규
│   └── ImageProcessingPerformanceTest.java
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── util/

docs/
└── ASYNC_PERFORMANCE_COMPARISON.md      # v2.0.1 신규
```

---

## 주요 개선사항

### v2.0.1 (2025-10-24)

#### 1. 비동기 Task Queue 구현

- ThreadPoolTaskExecutor 기반 이미지 처리
- `saveImageAsync()` API 추가
- TEMP → READY 상태 전환
- 약 7배 성능 향상 (208 vs 28 개/초)

#### 2. 이미지 처리 상태 관리 개선

- ImageStatus.READY 추가 (변환 완료 상태)
- ImageStatus.FAILED 추가 (처리 실패 상태)
- 처리 단계별 상태 전환: TEMP → READY → CONFIRMED

#### 3. Kafka 이벤트 발행 개선

- `@TransactionalEventListener(AFTER_COMMIT)` 적용
- 트랜잭션 커밋 후 이벤트 발행으로 데이터 정합성 보장
- 재시도 로직 추가 (최대 3회, 지수 백오프)

#### 4. 실패 이미지 자동 정리

- FailedImageCleanupScheduler 추가
- 매일 새벽 3시, 오전 9시 실행
- FAILED 7일 이상 + TEMP 24시간 이상 삭제

#### 5. 성능 테스트 구현

- 동기 vs 비동기 성능 비교 테스트
- 동시성 테스트 (50개)
- 부하 테스트 (200개)
- 성능 비교 문서 작성

#### 6. Spring Boot Actuator 추가

- 헬스 체크 엔드포인트
- Kafka, FileSystem Health Indicator
- 운영 모니터링 강화

### v2.0.0 (2025-10-22)

#### 1. 도메인 이벤트 패턴 적용
- ImagesConfirmedEvent 도입
- ImageSequenceEventHandler 자동 처리
- 트랜잭션 안전성 보장

#### 2. ImageSequence 구조 개선
- 복합키 → Auto Increment 단일키
- reference_id 컬럼 추가

#### 3. 엔티티 키 변경

- ReferenceType: id → code
- Extension: extension_id → code

#### 4. 성능 최적화

- DB 조회 최적화 (50% 감소)
- preloadedImage 재사용 패턴

#### 5. 포괄적인 테스트 구현

- 총 203+ 테스트
- 100% 성공률

---

## 문서

### 관련 문서

- [성능 비교 문서](docs/ASYNC_PERFORMANCE_COMPARISON.md)

### 메타 정보
- **작성일**: 2025-10-22
- **최종 업데이트**: 2025-10-24
- **버전**: 2.0.1
- **저자**: DDING
