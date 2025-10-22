# Support Server

사용자 문의, 신고 관리, FAQ를 통합 운영하는 마이크로서비스 기반 서포트 시스템입니다.

## 프로젝트 개요

Support Server는 대규모 서비스 환경에서 사용자 지원 업무를 효율적으로 처리하기 위한 백엔드 시스템입니다. 신고 처리, 1:1 문의 관리, FAQ 서비스를 독립적인 도메인으로 구성하여 높은 확장성과
유지보수성을 제공합니다.

### 핵심 기능

- **신고 관리**: 게시글, 프로필 등 다양한 대상에 대한 신고 접수 및 처리
- **1:1 문의**: 사용자 문의 등록, 답변 관리, 상태 추적
- **FAQ 관리**: 자주 묻는 질문 카테고리별 제공 및 캐싱

## 기술 스택

### Backend

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- QueryDSL
- Hibernate

### Database

- H2 (개발/테스트)
- MySQL (운영)
- Redis (캐싱)

### Build & Test

- Gradle 8.x
- JUnit 5
- AssertJ
- Mockito

### Infrastructure

- Kafka (이벤트 기반 통신)
- Docker
- GitHub Actions (CI/CD)

## 아키텍처

### 도메인 중심 설계

프로젝트는 독립적인 3개의 도메인으로 구성되어 있으며, 각 도메인은 완전한 계층 구조를 갖습니다.

```
com.teambind.supportserver
├── common/              # 공통 인프라 컴포넌트
│   ├── config/          # QueryDSL, ID 생성 설정
│   └── utils/           # Snowflake ID Generator
├── report/              # 신고 관리 도메인
│   ├── entity/          # Report, ReportCategory, Sanction
│   ├── repository/      # JPA + QueryDSL Repository
│   ├── service/         # 비즈니스 로직
│   ├── controller/      # RESTful API
│   └── dto/             # 요청/응답 객체
├── inquiries/           # 1:1 문의 도메인
│   ├── entity/          # Inquiry, Answer
│   ├── repository/      # JPA Repository
│   ├── service/         # 문의/답변 처리 로직
│   ├── controller/      # RESTful API
│   └── dto/             # 요청/응답 객체
└── faq/                 # FAQ 도메인
    ├── entity/          # Faq
    ├── repository/      # JPA Repository
    ├── service/         # 캐싱 전략 포함
    └── controller/      # RESTful API
```

### 주요 설계 패턴

**1. 도메인 주도 설계 (DDD)**

- 각 도메인이 독립적인 엔티티, 리포지토리, 서비스를 보유
- 도메인 간 결합도 최소화로 변경 영향도 제한

**2. 계층화 아키텍처**

- Presentation Layer: Controller (REST API)
- Business Layer: Service (비즈니스 로직)
- Persistence Layer: Repository, Entity (데이터 접근)

**3. 이벤트 기반 아키텍처**

- Kafka를 통한 비동기 이벤트 발행/구독
- 제재 적용, 알림 전송 등 도메인 간 통신

**4. 캐싱 전략**

- FAQ 전체 데이터 인메모리 캐싱
- ReadWriteLock 동시성 제어
- 스케줄러 기반 자동 갱신

## 시작하기

### 사전 요구사항

- JDK 17 이상
- Docker (선택사항)
- Gradle 8.x

### 로컬 환경 설정

1. 저장소 클론

```bash
git clone https://github.com/DDINGJOO/SUPPORT_SERVER.git
cd SUPPORT_SERVER/SupportServer
```

2. 빌드

```bash
./gradlew clean build
```

3. 테스트 실행

```bash
./gradlew test
```

4. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

### 환경 설정

`application.yml` 파일에서 다음 항목을 설정할 수 있습니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/support_db
    username: your_username
    password: your_password

  redis:
    host: localhost
    port: 6379

  kafka:
    bootstrap-servers: localhost:9092
```

## API 문서

### 신고 관리 API

**신고 등록**

```http
POST /api/v1/reports
Content-Type: application/json

{
  "reporterId": "USER-001",
  "reportedId": "TARGET-001",
  "referenceType": "PROFILE",
  "reportCategory": "SPAM",
  "reason": "스팸 프로필입니다"
}
```

**신고 조회**

```http
GET /api/v1/reports?status=PENDING&page=0&size=20
```

**신고 상태 변경**

```http
PATCH /api/v1/reports/{reportId}
Content-Type: application/json

{
  "status": "APPROVED",
  "adminId": "ADMIN-001",
  "comment": "제재 조치 완료"
}
```

### 1:1 문의 API

**문의 등록**

```http
POST /api/v1/inquiries
Content-Type: application/json

{
  "title": "예약 취소 문의",
  "contents": "예약을 취소하고 싶습니다",
  "category": "RESERVATION",
  "writerId": "USER-001"
}
```

**문의 조회**

```http
GET /api/v1/inquiries?writerId=USER-001&status=UNANSWERED
```

**답변 등록**

```http
POST /api/v1/inquiries/answers
Content-Type: application/json

{
  "inquiryId": "inquiry-uuid",
  "contents": "예약 취소는 마이페이지에서 가능합니다",
  "writerId": "ADMIN-001"
}
```

### FAQ API

**FAQ 전체 조회**

```http
GET /api/v1/faqs
```

**카테고리별 조회**

```http
GET /api/v1/faqs/category/PAYMENT
```

## 테스트

### 테스트 구조

프로젝트는 285개 이상의 테스트로 높은 커버리지를 유지합니다.

```bash
# 전체 테스트 실행
./gradlew test

# 특정 도메인 테스트
./gradlew test --tests "com.teambind.supportserver.report.*"
./gradlew test --tests "com.teambind.supportserver.inquiries.*"
./gradlew test --tests "com.teambind.supportserver.faq.*"

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

### 테스트 분류

- **단위 테스트**: 엔티티 비즈니스 로직 검증
- **통합 테스트**: Repository와 DB 연동 테스트
- **슬라이스 테스트**: Controller, Service 계층 테스트

## 배포

### Docker를 이용한 배포

```bash
# Docker 이미지 빌드
docker build -t support-server:latest .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/support_db \
  support-server:latest
```

### Docker Compose

```bash
docker-compose up -d
```

## 성능 최적화

### 1. QueryDSL을 활용한 동적 쿼리

- 복잡한 검색 조건을 타입 세이프하게 처리
- N+1 문제 해결을 위한 Fetch Join 전략

### 2. 인메모리 캐싱

- FAQ 데이터 전체를 메모리에 캐싱
- ReadWriteLock으로 동시성 제어
- 스케줄러 기반 자동 갱신(매일 새벽 3시)

### 3. Snowflake ID 생성

- 분산 환경에서 고유 ID 생성 보장
- 타임스탬프 기반 정렬 가능한 ID

### 4. 커서 기반 페이징

- 대용량 데이터 효율적 조회
- Offset 방식 대비 성능 우수

## 모니터링

### 성능 메트릭

- AOP 기반 실행 시간 측정
- 캐시 히트율 모니터링
- DB 쿼리 성능 추적

### 로깅

```yaml
logging:
  level:
    com.teambind.supportserver: DEBUG
    org.hibernate.SQL: DEBUG
```

## 기여 가이드

### 브랜치 전략

- `main`: 프로덕션 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `bugfix/*`: 버그 수정 브랜치
- `hotfix/*`: 긴급 수정 브랜치

### 커밋 메시지 규칙

```
[TYPE] 작업 내용 요약

상세 설명 (선택사항)

refs #이슈번호
```

타입:

- `FEAT`: 새로운 기능
- `REFACTOR`: 리팩토링
- `TEST`: 테스트 추가/수정
- `DOC`: 문서 수정
- `CHORE`: 빌드, 설정 변경

### Pull Request

1. 기능 브랜치에서 작업
2. 테스트 작성 및 통과 확인
3. PR 생성 시 관련 이슈 연결 (`Closes #N`)
4. 코드 리뷰 후 머지

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 문의

프로젝트 관련 문의사항은 이슈를 통해 제출해주세요.

- GitHub Issues: https://github.com/DDINGJOO/SUPPORT_SERVER/issues
- 프로젝트 위키: https://github.com/DDINGJOO/SUPPORT_SERVER/wiki
