# Image Server - 프로젝트 분석 문서

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [아키텍처 및 디자인 패턴](#2-아키텍처-및-디자인-패턴)
3. [데이터베이스 스키마](#3-데이터베이스-스키마)
4. [API 엔드포인트](#4-api-엔드포인트)
5. [서비스 계층 로직](#5-서비스-계층-로직)
6. [이미지 처리 및 변환](#6-이미지-처리-및-변환)
7. [기술 스택 및 의존성](#7-기술-스택-및-의존성)
8. [설정 및 배포](#8-설정-및-배포)
9. [도커 및 배포 아키텍처](#9-도커-및-배포-아키텍처)
10. [이벤트 시스템 및 Kafka 통합](#10-이벤트-시스템-및-kafka-통합)
11. [예외 처리](#11-예외-처리)
12. [검증 및 보안](#12-검증-및-보안)
13. [데이터 초기화](#13-데이터-초기화)
14. [상태 관리](#14-상태-관리)
15. [테스트 전략](#15-테스트-전략)
16. [주요 기능 및 패턴](#16-주요-기능-및-패턴)
17. [성능 최적화](#17-성능-최적화)
18. [프로젝트 구조](#18-프로젝트-구조)

---

## 1. 프로젝트 개요

### 프로젝트 정보

- **프로젝트명**: Image Server
- **목적**: 이미지 업로드, 저장, 처리 및 확인 워크플로우를 처리하는 전문 마이크로서비스
- **타입**: Spring Boot 3.x REST API 마이크로서비스
- **Java 버전**: 21
- **빌드 도구**: Gradle 8.x

### 핵심 기능

Image Server는 다음 기능을 담당합니다:

- 이미지 업로드 및 임시 저장
- 이미지 포맷 변환 (WebP 변환 및 폴백 지원)
- 이미지 확인/승인 워크플로우
- 이미지 시퀀스 관리
- 상태 이력 추적
- 만료된 임시 이미지 자동 정리 스케줄링
- Kafka 통합을 통한 이벤트 기반 아키텍처

---

## 2. 아키텍처 및 디자인 패턴

### 아키텍처 스타일

**계층화 아키텍처 + 도메인 주도 설계 (DDD)**

- **프레젠테이션 계층**: REST 컨트롤러 (`ImageSaveController`, `ImageConfirmController`, `enumsController`)
- **서비스 계층**: 비즈니스 로직 서비스 (`ImageSaveService`, `ImageConfirmService`, `ScheduleService`)
- **데이터 액세스 계층**: 데이터베이스 작업을 위한 JPA 리포지토리
- **유틸리티/공통**: 검증기, 헬퍼, 변환기, 저장소 핸들러

### 구현된 디자인 패턴

#### 1. 이벤트 기반 아키텍처

- Kafka 기반 비동기 이벤트 발행
- `ImageChangeEventPublisher`가 이미지 상태 변경을 하위 서비스에 발행
- 두 가지 이벤트 타입: `ImageChangeEvent` (단일 이미지), `SequentialImageChangeEvent` (배치)

#### 2. Service Locator 패턴

- `ImageServerApplication`의 정적 맵을 통한 확장자 및 참조 타입 빠른 조회
- 시작 시 `DataInitializer`를 통해 초기화

#### 3. Strategy 패턴

- 플러그 가능한 구현이 있는 파일 저장용 `LocalImageStorage`
- 이미지 변환 실패에 대한 폴백 메커니즘

#### 4. Decorator 패턴

- `StorageObject`가 저장소별 메타데이터로 `Image`를 래핑

#### 5. Template Method 패턴

- `StatusChanger`가 이력 추적과 함께 상태 전환 관리

### 계층 분리

```
Controller 계층 (REST 엔드포인트)
    ↓
Service 계층 (비즈니스 로직)
    ↓
Repository 계층 (JPA)
    ↓
Entity 계층 (도메인 모델)
```

---

## 3. 데이터베이스 스키마

### 핵심 엔티티 관계

#### 1. Images (Root Aggregate)

- **테이블**: `images`
- **기본 키**: `image_id` (UUID 기반 문자열)
- **주요 필드**:
	- `status` (ENUM: TEMP, CONFIRMED, READY, DELETED, FAILED)
	- `reference_type_id` (FK to reference_types)
	- `reference_id` (이미지가 연결된 대상 엔티티 ID)
	- `image_url` (서빙용 전체 URL)
	- `uploader_id` (업로드한 사용자)
	- `is_deleted` (소프트 삭제 플래그)
	- 타임스탬프: `created_at`, `updated_at`

#### 2. StorageObject (Image와 1:1 관계)

- **테이블**: `storage_objects`
- **기본 키**: `image_id` (Image와 공유)
- **목적**: 실제 파일 저장에 대한 메타데이터
- **주요 필드**:
	- `storage_location` (파일시스템의 상대 경로)
	- `origin_size` (원본 파일 크기, 바이트)
	- `converted_size` (WebP 변환 크기)
	- `origin_format_id` (FK to extensions)
	- `converted_format_id` (FK to extensions, nullable)

#### 3. StatusHistory (Image와 N:1 관계)

- **테이블**: `status_history`
- **목적**: 상태 변경 감사 추적
- **주요 필드**:
	- `old_status` (이전 상태)
	- `new_status` (새 상태)
	- `updated_at` (타임스탬프)
	- `updated_by` (변경 수행자)
	- `reason` (변경 이유, 선택 사항)

#### 4. ImageSequence (복합 키)

- **테이블**: `image_sequence`
- **기본 키**: 복합 (`id`, `image_id`)
- **목적**: 컬렉션 내 이미지 순서 지정 (예: 캐러셀)
- **주요 필드**:
	- `seq_number` (순서 위치)

#### 5. Extension (참조 데이터)

- **테이블**: `extensions`
- **목적**: 지원하는 파일 형식 카탈로그
- **지원 형식**: JPG, JPEG, PNG, GIF, WEBP, AVIF, BMP, SVG, MP4, MOV

#### 6. ReferenceType (참조 데이터)

- **테이블**: `reference_types`
- **목적**: 이미지가 참조할 수 있는 객체 카테고리
- **지원 타입**: PRODUCT, USER, POST, BANNER, CATEGORY, PROFILE

#### 7. 사용 중단: ImageVariants

- **테이블**: `image_variants` (스키마에 정의되어 있지만 사용되지 않음)
- **목적**: 향후 썸네일/변형 생성을 위해 예약

### ERD 다이어그램

```
┌──────────────────┐
│     Images       │
│──────────────────│
│ image_id (PK)    │───┐
│ status           │   │
│ reference_type_id│──┐│
│ reference_id     │  ││
│ image_url        │  ││
│ uploader_id      │  ││
└──────────────────┘  ││
         │            ││
         │1:1         ││
         ▼            ││
┌──────────────────┐  ││
│ StorageObject    │  ││
│──────────────────│  ││
│ image_id (PK/FK) │◄─┘│
│ storage_location │   │
│ origin_size      │   │
│ converted_size   │   │
└──────────────────┘   │
                       │
         ┌─────────────┘
         │
         ▼
┌──────────────────┐
│ ReferenceType    │
│──────────────────│
│ id (PK)          │
│ code             │
│ name             │
└──────────────────┘
```

---

## 4. API 엔드포인트

### 이미지 업로드 엔드포인트

#### POST /api/images

**단일 이미지 업로드**

**요청**:

```
Content-Type: multipart/form-data

Parameters:
- file (MultipartFile, 필수): 이미지 파일
- uploaderId (String, 필수): 업로드하는 사용자 ID
- category (String, 필수): 참조 타입 코드 (PRODUCT, USER, POST 등)
```

**응답**:

```json
// 200 OK
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "photo.jpg"
}

// 오류 응답:
// 400: FILE_EXTENSION_NOT_FOUND (잘못된 파일 타입)
// 400: REFERENCE_TYPE_NOT_FOUND (잘못된 카테고리)
// 500: IMAGE_SAVE_FAILED (변환/저장 실패)
// 500: IOException (파일 I/O 문제)
```

#### POST /api/images/batch

**배치 이미지 업로드**

**요청**:

```
Content-Type: multipart/form-data

Parameters:
- files (List<MultipartFile>, 필수): 여러 이미지 파일
- uploaderId (String, 필수): 사용자 ID
- category (String, 필수): 참조 타입 코드
```

**응답**:

```json
// 200 OK
[
  {
    "imageFileName": "photo1.jpg",
    "imageId": "550e8400-e29b-41d4-a716-446655440000"
  },
  {
    "imageFileName": "photo2.png",
    "imageId": "550e8400-e29b-41d4-a716-446655440001"
  }
]
```

### 이미지 확인 엔드포인트

#### PATCH /api/images/{referenceId}/confirm

**참조에 대한 이미지 확인**

**요청**:

```
Query Parameters:
- imageId (List<String>, 필수): 쉼표로 구분되거나 배열 형태의 이미지 ID

동작:
- 단일 이미지: TEMP/READY 상태에서 CONFIRMED로 변경
  - PROFILE 타입의 경우: 이전 프로필 이미지 자동 삭제
- 다중 이미지:
  - referenceId의 기존 이미지를 모두 DELETED로 표시
  - 지정된 새 이미지들을 확인
  - 순서 유지

응답: 204 No Content

검증:
- 요청당 최대 100개 이미지
- 빈 imageId 리스트 거부
- 이미 확인된 경우 IMAGE_ALREADY_CONFIRMED 발생
- 이미지가 없는 경우 IMAGE_NOT_FOUND 발생
```

### 열거형 엔드포인트

#### GET /api/enums/extensions

**지원되는 파일 확장자 조회**

**응답**:

```json
{
  "JPG": "JPEG Image",
  "JPEG": "JPEG Image",
  "PNG": "Portable Network Graphics",
  "WEBP": "WebP Image",
  ...
}
```

#### GET /api/enums/referenceType

**참조 타입 조회**

**응답**:

```json
{
  "PRODUCT": "상품",
  "USER": "사용자",
  "POST": "게시글",
  "BANNER": "배너",
  "CATEGORY": "카테고리",
  "PROFILE": "프로필"
}
```

### 헬스 체크 엔드포인트

#### GET /health

**서버 상태 확인**

**응답**:

```
200 OK
"Server is up"
```

### 스케줄러 엔드포인트

#### GET /api/schedule/cleanup

**이미지 정리 수동 트리거**

- 스케줄된 정리 작업을 수동으로 트리거
- 2일 이상 된 TEMP/FAILED 이미지 제거
- 크론으로 자동 실행: `0 30 3 * * *` (서울 시간 오전 3:30)

---

## 5. 서비스 계층 로직

### ImageSaveService

**주요 책임**: WebP 변환과 함께 업로드된 이미지 처리 및 저장

**주요 메서드**:

1. `saveImage(MultipartFile, String uploaderId, String category)` - 단일 이미지
2. `saveImages(List<MultipartFile>, String uploaderId, String category)` - 배치

**처리 흐름**:

```
1. 파일 확장자 검증
2. 참조 타입(카테고리) 검증
3. 확장자 추출 및 UUID 생성
4. WebP 변환 시도 (80% 품질)
   - 성공: WebP 저장, converted_format을 WEBP로 기록
   - 폴백: Rosetta/Docker 아키텍처 문제 감지
     - 아키텍처 관련: 원본 형식 저장
     - 그 외: IMAGE_SAVE_FAILED 발생
5. TEMP 상태로 Image 엔티티 생성
6. 크기/형식 메타데이터와 함께 StorageObject 생성
7. 데이터베이스에 모두 저장
8. 이미지 ID 및 파일명 반환
```

**이미지 경로 구조**:

```
/uploads/{CATEGORY}/{YYYY}/{MM}/{DD}/{UUID}.{format}
예시: /uploads/PROFILE/2025/09/22/550e8400-e29b-41d4-a716-446655440000.webp
```

### ImageConfirmService

**주요 책임**: 이미지를 TEMP → CONFIRMED 상태로 전환

**주요 메서드**:

1. `confirmImage(String imageId, String referenceId)` - 단일 이미지 확인
2. `confirmImages(List<String> imageIds, String referenceId)` - 배치 확인
3. `deleteOldProfileImg(String imageId, String uploaderId, ReferenceType)` - 프로필 정리

**특별한 프로필 처리**:

- PROFILE 이미지 확인 시, 이전 프로필 이미지를 자동으로 DELETED로 표시
- 사용자당 하나의 활성 프로필 이미지만 유지

### ScheduleService

**스케줄**: 서울 시간 매일 오전 3:30 (cron: `0 30 3 * * *`)
**락 설정**: ShedLock을 통한 최대 락 시간 10분, 최소 1분

**작업**:

- status != CONFIRMED인 모든 이미지 찾기
- 2일 이상 생성된 이미지 필터링
- 데이터베이스 및 파일시스템에서 삭제

---

## 6. 이미지 처리 및 변환

### ImageUtil

강력한 이미지 처리를 위해 **Scrimage** 라이브러리 사용

**메서드**:

#### 1. toWebp(MultipartFile file, float quality)

- 모든 이미지 형식을 WebP로 변환
- 품질: 0.0-1.0 (0-100 스케일로 변환)
- 기본 변환: 80% 품질

#### 2. toWebpThumbnail(MultipartFile file, int width, int height, float quality)

- WebP 썸네일 생성
- 종횡비 유지와 함께 center-crop을 위해 cover() 사용

**오류 처리**:

- 아키텍처별 오류 감지 (Rosetta, Docker의 glibc 문제)
- 환경 문제로 WebP 변환 실패 시 원본 형식으로 폴백
- 실제 이미지 손상/잘못된 형식에 대해 IMAGE_SAVE_FAILED 발생

### LocalImageStorage

**책임**: 이미지 파일의 안전한 파일시스템 작업

**보안 기능**:

1. **경로 탐색 방지**: `..`을 포함하는 경로 거부
2. **기본 디렉토리 강제**: 모든 경로가 설정된 기본 디렉토리 내에 있어야 함
3. **경로 정규화**: 심볼릭 링크 공격 방지

**메서드**:

- `store(MultipartFile file, String relativePath)` - MultipartFile에서 저장
- `store(byte[] imageBytes, String relativePath)` - 바이트 배열에서 저장
- `delete(String relativePath)` - 검증과 함께 파일 삭제

**설정**:

- 기본 디렉토리: application properties의 `${images.upload.dir}`
- 부모 디렉토리 자동 생성
- 초기화 시 권한 상태 로깅

---

## 7. 기술 스택 및 의존성

### 핵심 의존성

```gradle
Spring Boot 3.5.5
  - spring-boot-starter-data-jpa
  - spring-boot-starter-web

Spring Kafka
  - spring-kafka
  - spring-kafka-test (테스트)

Database
  - mariadb-java-client (런타임)
  - h2 (테스트)

Image Processing
  - scrimage-core:4.0.32
  - scrimage-webp:4.0.32

Scheduling
  - shedlock-spring:5.14.0 (스케줄된 작업용 분산 락)

Testing
  - spring-boot-starter-test
  - mockito-inline:5.2.0

Development
  - lombok
  - slf4j (Spring 통해)

JVM
  - Eclipse Temurin JRE 21
```

---

## 8. 설정 및 배포

### 애플리케이션 프로파일

#### 1. dev (기본값)

- 로컬 데이터베이스 설정
- 로컬 이미지 업로드 디렉토리: `/Users/ddingjoo/IdeaProjects/.../nginx/images/uploads`
- 기본 URL: `http://localhost:9200/images/`
- 콘솔 SQL 로깅 활성화

#### 2. prod

- Docker 기반 설정
- 업로드 디렉토리: `/uploads`
- 기본 URL: `${BASE_URL}` 환경 변수에서 (일반적으로 `https://teambind.co.kr:9200/images/`)
- 모든 민감한 설정에 환경 변수 사용

### 환경 변수 (프로덕션)

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_HOST=mariadb
DATABASE_PORT=3306
DATABASE_NAME=images
DATABASE_USER_NAME=root
DATABASE_PASSWORD=pass123#
KAFKA_URL1=kafka1:9091
KAFKA_URL2=kafka2:9092
KAFKA_URL3=kafka3:9093
REDIS_HOST=redis-image
REDIS_PORT=6379
BASE_URL=https://teambind.co.kr:9200/images/
```

### 데이터베이스 설정

- **개발**: MariaDB (환경 변수를 통해 설정)
- **테스트**: H2 in-memory
- **드라이버**: org.mariadb.jdbc.Driver
- **Dialect**: org.hibernate.dialect.MariaDBDialect
- **DDL Auto**: `none` (외부 관리 스키마)
- **SQL Init Mode**: `never` (초기화 스크립트를 통한 수동 제어)

### Kafka 설정

- **Consumer Group**: `image-consumer-group`
- **Auto Offset Reset**: `earliest`
- **ACK Mode**: `record`
- **Producer**: 배치 크기 16KB, 버퍼 32MB, 3회 재시도
- **발행 토픽**: `{reference-type}-image-changed` (예: `product-image-changed`)

### Redis 설정

- **Repositories Enabled**: false (리포지토리 캐싱에 Redis 사용 안 함)
- **연결 목적**: 향후 캐싱 구현

---

## 9. 도커 및 배포 아키텍처

### Docker Compose 설정

**다중 인스턴스 로드 밸런싱 배포**

#### 서비스

**1. image-server-1, image-server-2, image-server-3**

- 이미지: `ddingsh9/image-server:1.9.1`
- 포트: 8080 (내부)
- 볼륨: 공유 `images-data` 볼륨
- 헬스 체크: 15초마다 `curl http://localhost:8080/health`
- 재시작 정책: `unless-stopped`
- 네트워크: `image-network` (내부) + `infra-network` (다른 서비스와 공유)

**2. nginx (로드 밸런서)**

- 이미지: `nginx:alpine`
- 포트: 9200:80 (호스트에 노출)
- Upstream: 3개 이미지 서버에 라운드 로빈
- 프록시 설정: 적절한 헤더와 함께 `/api` 요청 포워딩
- 볼륨: 설정, 로그, 공유 images-data

**3. image-log-cleaner**

- 이미지: `alpine:3.19`
- 기능: 1일 이상 된 로그 삭제
- 시간당 정리 체크 실행

### Dockerfile

```dockerfile
기본 이미지: eclipse-temurin:21-jre-jammy
의존성: libwebp7 (WebP 런타임 라이브러리), ca-certificates
JAR 위치: /app/app.jar
진입점: java -jar /app/app.jar
```

### 볼륨 구조

```
images-data/
├── PROFILE/2025/09/22/
│   ├── 550e8400-e29b-41d4-a716-446655440000.webp
│   └── ...
├── PRODUCT/2025/09/22/
│   └── ...
└── ...
```

### 네트워크 아키텍처

```
┌─────────────────────────────────────┐
│   외부 클라이언트 (HTTPS)             │
└──────────────┬──────────────────────┘
               │
         ┌─────▼──────┐
         │  Nginx:9200│ (로드 밸런서)
         └─────┬──────┘
               │
        ┌──────┴─────────────┐
        │                    │
   ┌────▼────┐         ┌────▼────┐ ┌────────┐
   │Server-1 │         │Server-2 │ │Server-3│
   │:8080    │─────────│:8080    │─│:8080   │
   └────┬────┘         └────┬────┘ └───┬────┘
        │                   │          │
        └───────────────────┼──────────┘
                            │
                   ┌────────▼────────┐
                   │   MariaDB       │
                   │   Redis         │
                   │   Kafka Cluster │
                   │ (infra-network) │
                   └─────────────────┘
```

---

## 10. 이벤트 시스템 및 Kafka 통합

### 이벤트 발행

#### ImageChangeEventPublisher

이미지 상태 변경을 하위 서비스에 발행

**메서드**: `imageChangeEvent(Image image)`

```
토픽: {reference-type-code}-image-changed
예시 토픽:
  - product-image-changed
  - user-image-changed
  - post-image-changed
  - banner-image-changed
  - profile-image-changed

이벤트 페이로드 (ImageChangeEvent):
{
  "referenceId": "user-123",
  "imageId": "550e8400-e29b-41d4-a716-446655440000",
  "imageUrl": "https://teambind.co.kr:9200/images/PROFILE/2025/09/22/550e8400.webp"
}
```

**메서드**: `imagesChangeEvent(List<Image> images)`

```
토픽: {reference-type-code}-image-changed (단일과 동일)

이벤트 페이로드 (List<SequentialImageChangeEvent>):
[
  {
    "imageId": "550e8400-...",
    "imageUrl": "https://...",
    "referenceId": "post-456"
  },
  ...
]
```

### EventPublisher (범용 Kafka 핸들러)

- Jackson ObjectMapper를 사용하여 메시지를 JSON으로 직렬화
- 설정된 Kafka 토픽으로 전송
- JsonProcessingException을 RuntimeException 래퍼로 처리

---

## 11. 예외 처리

### CustomException 프레임워크

**커스텀 예외 계층**

```java
CustomException extends RuntimeException
├──ErrorCode

enum 포함
├──
HTTP 상태에
매핑
└──
에러 메시지
캡슐화
```

### 에러 코드

| 코드                         | 메시지               | HTTP 상태 | 원인                       |
|----------------------------|-------------------|---------|--------------------------|
| `INVALID_REFERENCE`        | Invalid Reference | 400     | 참조 타입을 찾을 수 없음           |
| `FILE_EXTENSION_NOT_FOUND` | 지원하지 않는 파일 확장자    | 400     | Extension 카탈로그에 없는 파일 형식 |
| `REFERENCE_TYPE_NOT_FOUND` | 지원하지 않는 카테고리      | 400     | 잘못된 참조 타입 코드             |
| `IMAGE_NOT_FOUND`          | 이미지가 존재하지 않음      | 404     | 데이터베이스에 이미지 ID 없음        |
| `IMAGE_ALREADY_CONFIRMED`  | 이미 확인된 이미지        | 400     | 확인된 이미지를 재확인할 수 없음       |
| `IMAGE_SAVE_FAILED`        | 이미지 처리 실패         | 500     | WebP 변환 또는 저장 오류         |
| `IOException`              | 파일 I/O 오류         | 500     | 파일시스템 액세스 실패             |

---

## 12. 검증 및 보안

### ExtensionValidator

- Extension 데이터베이스에 대해 파일 확장자 검증
- 확장자가 `extensionMap`에 있는지 확인
- 지원: JPG, JPEG, PNG, GIF, WEBP, AVIF, BMP, SVG, MP4, MOV

### ExtensionParser

- 파일명에서 확장자 추출
- 메서드: `substring(lastIndexOf(".") + 1).toUpperCase()`
- 예시: "photo.jpg" → "JPG"

### ReferenceValidator

- 참조 타입 코드 검증
- 코드가 `referenceTypeMap`에 있는지 확인
- 대소문자 구분 없음 (대문자로 변환)

### 경로 탐색 방지

- `LocalImageStorage`가 `..`을 포함하는 경로 거부
- 경로를 정규화하고 기본 디렉토리 내에 있는지 확인
- 디렉토리 이스케이프 공격 방지

### 요청 검증

- 배치 확인: 요청당 최대 100개 이미지
- imageId 리스트의 공백 제거
- null 값 필터링

---

## 13. 데이터 초기화

### DataInitializer (ApplicationRunner)

애플리케이션 시작 시 실행하여 인메모리 캐시 채우기

**프로세스**:

1. 데이터베이스에서 모든 확장자 로드 → `extensionMap` 채우기
2. 데이터베이스에서 모든 참조 타입 로드 → `referenceTypeMap` 채우기
3. 디버깅을 위해 각 항목 로깅

**시드 데이터** (data-mariadb.sql에서):

**참조 타입:**

```sql
PRODUCT
→ 상품
USER → 사용자
POST → 게시글
BANNER → 배너
CATEGORY → 카테고리
PROFILE → 프로필 (코드에 추가됨)
```

**확장자:**

```sql
JPG
, JPEG → JPEG Image
PNG → Portable Network Graphics
GIF → Graphics Interchange Format
WEBP → WebP Image
AVIF → AV1 Image File Format
BMP → Bitmap Image
SVG → Scalable Vector Graphics
MP4 → MPEG-4 Video
MOV → QuickTime Movie
```

---

## 14. 상태 관리

### 이미지 상태 라이프사이클

```
TEMP (초기)
  ↓ (imageConfirm 또는 2일간 활동 없음)
CONFIRMED (승인됨)
  ↓
READY (추가 처리 완료)

TEMP/READY/CONFIRMED
  ↓ (명시적 삭제 또는 정리)
DELETED (제거 대상)

모든 상태
  ↓ (처리 오류)
FAILED (오류 상태)
```

### StatusChanger 유틸리티

- `status_history` 테이블에 모든 상태 전환 기록
- 캡처: old_status, new_status, 타임스탬프, 작업자, 이유
- 이미지 라이프사이클의 완전한 감사 추적 생성

---

## 15. 테스트 전략

### 테스트 커버리지 (11개 테스트 클래스)

- **통합 테스트**: Repository, Service, Spring Boot 테스트
- **단위 테스트**: 유틸리티 검증 및 변환
- **프로파일**:
	- 전체 컨텍스트 테스트용 `@SpringBootTest`
	- 리포지토리 테스트용 `@DataJpaTest`
	- 파일시스템 작업용 `@TempDir`

### 테스트 클래스

1. `ImageSaveServiceSpringBootTest` - 업로드 및 변환 시나리오
2. `ImageConfirmServiceSpringBootTest` - 상태 확인 로직
3. `ScheduleServiceSpringBootTest` - 정리 작업 테스트
4. `ImageRepositoryDataJpaTest` - 리포지토리 쿼리 테스트
5. `ImageUtilSpringBootTest` - WebP 변환 테스트
6. `LocalImageStorageSpringBootTest` - 파일시스템 저장 테스트
7. `ValidatorSpringBootTest` - Extension/Reference 검증
8. 기타 리포지토리 및 유틸리티 테스트

---

## 16. 주요 기능 및 패턴

### 1. Rosetta/아키텍처 폴백

- 아키텍처 문제로 WebP 변환 실패 감지
- 폴백: 오류 발생 대신 원본 형식 저장
- 이기종 환경(Mac, Linux, Docker)에서 안정성 보장

### 2. 공유 기본 키 패턴

- `StorageObject.id`가 `Image.id`와 공유됨
- `@MapsId` 어노테이션 사용
- 데이터베이스 수준에서 1:1 관계 강제

### 3. 복합 기본 키

- `ImageSequence`가 복합 키 (id, image_id) 사용
- FK 관계 유지하면서 시퀀스 순서 지정 가능
- 커스텀 `ImageSequenceId` 직렬화 가능 클래스 구현

### 4. 순서 유지 배치 작업

- 여러 이미지 확인 시 요청 순서 유지
- Map 기반 조회 및 리스트 재구성으로 시퀀스 유지

### 5. 정적 초기화 맵

- 시작 시 로드되는 스레드 안전 읽기 전용 맵
- enum 같은 조회를 위한 데이터베이스 쿼리 제거
- `ApplicationRunner` 인터페이스를 통해 채워짐

### 6. 스케줄된 작업용 분산 락

- 다중 인스턴스 안전성을 위해 ShedLock 사용
- 3개 서버 인스턴스에서 중복 정리 실행 방지
- `shedlock` 테이블에 락 저장

---

## 17. 성능 최적화

### 1. 인메모리 캐시

- 시작 시 Extension 및 ReferenceType 맵 로드
- 검증 조회를 위한 데이터베이스 쿼리 제로
- 빠른 문자열 키 액세스: 평균 O(1)

### 2. 지연 로딩

- `fetch = FetchType.LAZY`로 설정된 JPA 관계
- 불필요한 데이터 검색 감소

### 3. 배치 처리

- 여러 업로드를 위한 `/api/images/batch` 엔드포인트
- 여러 파일에 대한 초기화 비용 분산

### 4. 스케줄된 정리

- 주기적으로 만료된 이미지 제거 (오전 3:30)
- 데이터베이스 비대화 방지
- 분산 락으로 인스턴스 간 단일 실행 보장

### 5. WebP 압축

- JPEG 대비 이미지 크기 ~30-50% 감소
- 80% 품질 설정으로 크기와 시각적 품질 균형
- 저장 및 대역폭 비용 절감

---

## 18. 프로젝트 구조

```
src/main/java/com/teambind/image_server/
├── ImageServerApplication.java (진입점, 정적 맵)
├── config/
│   ├── InitialConfig.java (설정 홀더)
│   └── DataInitializer.java (시작 초기화)
├── controller/
│   ├── ImageSaveController.java (업로드 엔드포인트)
│   ├── ImageConfirmController.java (확인 엔드포인트)
│   ├── enumsController.java (Enum 조회 엔드포인트)
│   ├── HealthController.java (헬스 체크)
│   └── sehdule/
│       └── ScheduleController.java (수동 정리 트리거)
├── service/
│   ├── ImageSaveService.java (업로드 로직)
│   ├── ImageConfirmService.java (확인 로직)
│   ├── ImageService.java (인터페이스 - 미구현)
│   └── ScheduleService.java (정리 작업)
├── repository/
│   ├── ImageRepository.java
│   ├── ExtensionRepository.java
│   ├── ReferenceTypeRepository.java
│   ├── ImageSequenceRepository.java
│   ├── StatusHistoryRepository.java
│   └── StorageObjectRepository.java
├── entity/
│   ├── Image.java (루트 집합체)
│   ├── StorageObject.java (Image와 1:1)
│   ├── Extension.java (참조 데이터)
│   ├── ReferenceType.java (참조 데이터)
│   ├── ImageSequence.java (복합 키 엔티티)
│   ├── StatusHistory.java (감사 추적)
│   └── key/
│       └── ImageSequenceId.java (복합 키 클래스)
├── enums/
│   └── ImageStatus.java (TEMP, CONFIRMED, READY, DELETED, FAILED)
├── event/
│   ├── EventPublisher.java (범용 Kafka 발행자)
│   ├── events/
│   │   ├── ImageChangeEvent.java (단일 이미지 이벤트)
│   │   └── SequentialImageChangeEvent.java (배치 이벤트)
│   └── publish/
│       └── ImageChangeEventPublisher.java (도메인별 발행자)
├── exception/
│   ├── CustomException.java (예외 래퍼)
│   └── ErrorCode.java (에러 코드 enum)
├── dto/
│   └── response/
│       └── SequentialImageResponse.java (배치 업로드 응답)
└── util/
    ├── convertor/
    │   └── ImageUtil.java (Scrimage를 사용한 WebP 변환)
    ├── helper/
    │   ├── UrlHelper.java (URL 구성)
    │   └── SequenceHelper.java (미구현)
    ├── store/
    │   └── LocalImageStorage.java (파일시스템 작업)
    ├── statuschanger/
    │   └── StatusChanger.java (이력이 있는 상태 전환)
    └── validator/
        ├── ExtensionParser.java (파일명에서 확장자 추출)
        ├── ExtensionValidator.java (카탈로그에 대해 검증)
        └── ReferenceValidator.java (참조 타입 검증)

src/main/resources/
├── application.yaml (프로파일 선택기)
├── application-dev.yaml (개발 설정)
├── application-prod.yaml (프로덕션 설정)
└── sql/
    ├── schema-mariadb.sql (테이블 정의)
    └── data-mariadb.sql (시드 데이터)

src/test/java/... (11개 테스트 클래스)
```

---

## 요약

Image Server는 이미지 업로드, 처리 및 워크플로우 관리를 위한 잘 구조화된 Spring Boot 마이크로서비스입니다. WebP 변환, 이벤트 기반 아키텍처, 로드 밸런싱된 배포와 같은 최신 기능을 활용하여
확장 가능하고 안정적인 이미지 처리 솔루션을 제공합니다.

### 핵심 장점

- 계층화된 아키텍처로 명확한 관심사 분리
- 이벤트 기반 통합으로 느슨한 결합
- WebP 변환으로 성능 최적화
- 분산 락으로 강력한 스케줄링
- 포괄적인 에러 처리 및 검증
- 다중 인스턴스 배포 지원
- 완전한 감사 추적 (상태 이력)

### 기술 하이라이트

| 항목          | 세부사항                                          |
|-------------|-----------------------------------------------|
| **프로젝트 타입** | Spring Boot 3.5.5 마이크로서비스                     |
| **언어/버전**   | Java 21                                       |
| **빌드 도구**   | Gradle 8.x                                    |
| **주요 목적**   | 이미지 업로드, 처리 및 워크플로우 관리                        |
| **핵심 기능**   | WebP 변환, 배치 업로드, 상태 추적, 이벤트 발행                |
| **데이터베이스**  | MariaDB (프로덕션) / H2 (테스트)                     |
| **메시지 큐**   | Kafka                                         |
| **캐싱**      | 시작 시 정적 맵, Redis 설정됨                          |
| **아키텍처**    | 계층화 + 이벤트 기반                                  |
| **배포**      | Docker (Nginx를 통한 3-인스턴스 로드 밸런싱)              |
| **스케줄링**    | 분산 정리를 위한 ShedLock + Cron                     |
| **이미지 형식**  | JPG, PNG, GIF, WEBP, AVIF, BMP, SVG, MP4, MOV |
| **업로드 제한**  | 배치 확인 요청당 100개 이미지                            |
| **모니터링**    | 헬스 체크 엔드포인트, 구조화된 로깅                          |

---

**문서 작성일**: 2025-10-22
**프로젝트 버전**: 1.9.1
**분석 기준**: /Users/ddingjoo/IdeaProjects/BanderProject/SERVER/Image_Server
