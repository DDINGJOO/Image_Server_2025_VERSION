# Image Server 프로젝트 개선 로드맵

## Phase 1: 긴급 수정 (P0 - Critical)

### 1.1 보안 및 안정성 핵심 이슈

- **Global Exception Handler 구현** - 예외 정보 노출 방지 및 표준 에러 응답
- **InitialSetup 버그 수정** - `@PostConstruct init()` 메서드가 비어있어 데이터 로드 실패
- **ExtensionParser 안정화** - null/빈 문자열 처리 및 확장자 없는 파일 예외 처리
- **@EnableScheduling 추가** - 스케줄 작업이 실행되지 않는 문제 해결

### 1.2 입력 검증 강화

- Controller에 `@Valid` 적용 및 Request DTO 검증
- 파일 업로드 검증 (크기, 확장자, 이중 확장자, path traversal)
- uploaderId, category 필수 값 검증

---

## Phase 2: 운영 안정성 강화 (P1 - High)

### 2.1 모니터링 및 로깅

- 실제 상태 체크하는 Health Check 구현 (DB, Kafka, File System)
- Request/Response 로깅 및 Correlation ID
- Kafka 이벤트 발행 실패 로깅
- Spring Boot Actuator metrics 추가

### 2.2 이벤트 처리 안정화

- Kafka 발행 실패 핸들링 및 재시도 로직
- Dead Letter Queue 구성
- Topic 존재 여부 검증

### 2.3 파일 관리 최적화

- 파일 I/O를 트랜잭션 외부로 분리
- Orphaned 파일 정리 메커니즘
- 파일 저장 실패 시 롤백 처리

---

## Phase 3: 성능 및 확장성 (P1-P2 - High to Medium)

### 3.1 데이터베이스 최적화

- N+1 쿼리 해결 (fetch join 적용)
- 인덱스 추가 (`uploader_id`, `created_at`)
- ScheduleService 쿼리를 DB 레벨로 이동 (날짜 비교 최적화)

### 3.2 비동기 처리

- 파일 업로드 및 WebP 변환을 비동기로 처리
- Task Queue 도입으로 스레드 풀 고갈 방지

### 3.3 스토리지 전략

- 로컬 파일 시스템의 한계 극복 (S3/GCS 마이그레이션 계획)
- 멀티 인스턴스 환경 지원
- 오래된 파일 자동 정리

---

## Phase 4: 코드 품질 및 유지보수성 (P2 - Medium)

### 4.1 테스트 작성

- Service layer 단위 테스트
- Controller 통합 테스트
- 파일 업로드 엣지 케이스 테스트
- Kafka 이벤트 발행 테스트
- 스케줄 작업 테스트

### 4.2 API 설계 개선

- 표준 Response DTO 래퍼 도입
- Swagger/OpenAPI 문서화
- RESTful 원칙 준수 (ScheduleController GET → POST 변경)
- API 버저닝 전략 (`/api/v1/...`)

### 4.3 리팩토링

- ImageConfirmService 복잡도 감소 (lines 144-169)
- Controller에 DTO 적용 (Entity 직접 노출 방지)
- Magic number를 설정값으로 이동 (WebP 품질, 정리 주기 등)
- 관심사 분리 (파일 I/O와 비즈니스 로직 분리)

### 4.4 코드 정리

- 네이밍 규칙 수정 (`enumsController` → `EnumsController`, `idDeleted` → `isDeleted`)
- 빈 클래스 제거 (InitialConfig)
- 주석 처리된 코드 정리 또는 구현 (ImageConfirmController)

---

## Phase 5: 인프라 및 DevOps (P2-P3 - Medium to Low)

### 5.1 데이터베이스 마이그레이션

- Flyway/Liquibase 도입
- 스키마 버전 관리
- 롤백 전략 수립

### 5.2 설정 관리

- Environment별 설정 분리 (로깅 레벨, 커넥션 풀, 타임아웃)
- 개발 환경 경로 하드코딩 제거
- Redis 사용 또는 설정 제거

### 5.3 배포 및 운영

- 백업 전략 수립 (파일 및 DB)
- 재해 복구 계획
- Circuit Breaker 패턴 적용

### 5.4 관찰성 강화

- Distributed Tracing (Sleuth/OpenTelemetry)
- 업로드 성공률, WebP 변환율 메트릭
- 스토리지 사용량 모니터링

---

## Phase 6: 추가 기능 및 개선 (P3 - Low)

### 6.1 컨텐츠 보안

- 업로드 파일 안티바이러스 스캔
- 이미지 컨텐츠 검증 (실제 이미지 파일인지 확인)
- EXIF 데이터 제거 (개인정보 보호)

### 6.2 CORS 설정

- CORS 설정 추가 (프론트엔드 연동)

### 6.3 데이터 일관성

- Soft Delete 구현 (`is_deleted` 플래그 활용)
- StatusHistory와 is_deleted 플래그 일관성 유지

---

## 우선순위별 타임라인 제안

| Phase   | Priority | 예상 기간  | 핵심 목표                     |
|---------|----------|--------|---------------------------|
| Phase 1 | P0       | 1주     | 운영 중단 방지 및 즉각적 안정성 이슈 해결  |
| Phase 2 | P1       | 2-3주   | 프로덕션 준비 완료 (모니터링, 이벤트 처리) |
| Phase 3 | P1-P2    | 3-4주   | 성능 및 확장성 확보               |
| Phase 4 | P2       | 4-6주   | 코드 품질 및 유지보수성 개선          |
| Phase 5 | P2-P3    | 2-3주   | 인프라 및 운영 자동화              |
| Phase 6 | P3       | 지속적 개선 | 추가 편의 기능                  |

---

## 핵심 개선 영역 요약

> **참고**: 인증/인가는 다른 서버에서 처리되어 이미지 서버에서는 검증 불필요. 트랜잭션 관리는 이미 `@Transactional` 적용 완료.

1. **안정성 취약점**: 예외 처리, 입력 검증, 파일 업로드 검증 부재
2. **초기화 버그**: InitialSetup이 데이터를 로드하지 않아 첫 요청 실패
3. **성능 이슈**: N+1 쿼리, 동기 파일 I/O, 비효율적 스케줄 작업
4. **모니터링 부재**: 실질적 헬스체크, 로깅, 메트릭 없음
5. **테스트 커버리지**: 거의 모든 코드가 테스트되지 않음
6. **이벤트 처리**: Kafka 발행 실패 핸들링 및 재시도 로직 부족
7. **코드 품질**: 복잡한 로직, magic number, 네이밍 규칙 위반
8. **인프라 준비도**: 로컬 스토리지 한계, DB 마이그레이션 도구 부재
