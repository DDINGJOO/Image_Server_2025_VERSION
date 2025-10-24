# Kafka 이벤트 발행 명세서

## 목차

1. [개요](#개요)
2. [이벤트 발행 시점](#이벤트-발행-시점)
3. [토픽 네이밍 규칙](#토픽-네이밍-규칙)
4. [이벤트 타입별 명세](#이벤트-타입별-명세)
5. [재시도 정책](#재시도-정책)
6. [트러블슈팅](#트러블슈팅)

---

## 개요

Image Server는 이미지 상태 변경 시 Kafka를 통해 외부 시스템에 이벤트를 발행합니다.
모든 이벤트는 **트랜잭션 커밋 이후(AFTER_COMMIT)** 발행되어 데이터 일관성을 보장합니다.

### 주요 특징

- **트랜잭션 안전성**: DB 커밋 성공 후에만 Kafka 이벤트 발행
- **재시도 메커니즘**: 지수 백오프(Exponential Backoff) 기반 재시도
- **비동기 처리**: Kafka 발행 실패가 DB 트랜잭션에 영향을 주지 않음
- **순서 보장**: 순차 이미지의 경우 sequence 정보 포함

---

## 이벤트 발행 시점

### 1. 단일 이미지 확정

**시점**: `confirmImage()` API 호출 시

**발생 케이스**:

- 새 이미지 등록 (기존 이미지 없음)
- 기존 이미지 교체 (단일 이미지 타입)
- 전체 삭제 (빈 문자열 전달 시)

**처리 흐름**:

```
1. DB 트랜잭션 시작
2. 이미지 상태 변경 (CONFIRMED)
3. DB 커밋
4. Kafka 이벤트 발행 (ImageChangeEvent)
```

### 2. 다중 이미지 확정

**시점**: `confirmImages()` API 호출 시

**발생 케이스**:

- 복수 이미지 일괄 확정
- 이미지 순서 변경
- 일부 이미지 추가/삭제

**처리 흐름**:

```
1. DB 트랜잭션 시작
2. 이미지 상태 변경 (CONFIRMED)
3. 도메인 이벤트 발행 (ImagesConfirmedEvent)
4. DB 커밋
5. TransactionalEventListener 실행
   - ImageSequence 재생성
   - Kafka 이벤트 발행 (SequentialImageChangeEvent 리스트)
```

### 3. 전체 삭제

**시점**: 빈 리스트 또는 빈 문자열 전달 시

**처리 흐름**:

```
1. DB 트랜잭션 시작
2. 기존 이미지 상태 DELETED로 변경
3. DB 커밋
4. Kafka 삭제 이벤트 발행 (imageId=null, imageUrl=null)
```

---

## 토픽 네이밍 규칙

토픽 이름은 **ReferenceType 코드**를 기반으로 동적 생성됩니다.

### 형식

```
{referenceType}-image-changed
```

### 예시

| ReferenceType | Topic 이름                |
|---------------|-------------------------|
| PRODUCT       | `product-image-changed` |
| USER          | `user-image-changed`    |
| POST          | `post-image-changed`    |
| PROFILE       | `profile-image-changed` |
| BANNER        | `banner-image-changed`  |

### 구현 위치

`ImageChangeEventPublisher.java:24`, `ImageChangeEventPublisher.java:31`, `ImageChangeEventPublisher.java:44`

---

## 이벤트 타입별 명세

### 1. ImageChangeEvent (단일 이미지 변경)

#### 이벤트 정보

- **클래스**: `com.teambind.image_server.event.events.ImageChangeEvent`
- **발행 메서드**: `ImageChangeEventPublisher.imageChangeEvent()`
- **사용 케이스**: 단일 이미지 타입의 이미지 확정/교체

#### 페이로드 구조

```json
{
  "referenceId": "string",
  "imageId": "string",
  "imageUrl": "string"
}
```

#### 필드 설명

| 필드          | 타입     | 필수 | 설명                    |
|-------------|--------|----|-----------------------|
| referenceId | String | Y  | 참조 ID (상품ID, 게시글ID 등) |
| imageId     | String | Y  | 이미지 고유 ID             |
| imageUrl    | String | Y  | 이미지 접근 URL            |

#### 페이로드 예시

##### 1) 새 이미지 등록

```json
{
  "referenceId": "PROD-12345",
  "imageId": "img_abc123xyz",
  "imageUrl": "https://cdn.example.com/images/product/img_abc123xyz.jpg"
}
```

**발행 시점**: PROFILE 등 단일 이미지 타입에 처음 이미지 등록 시

**토픽**: `profile-image-changed`

---

##### 2) 이미지 교체

```json
{
  "referenceId": "USER-98765",
  "imageId": "img_new456def",
  "imageUrl": "https://cdn.example.com/images/user/img_new456def.jpg"
}
```

**발행 시점**: 기존 이미지를 새 이미지로 교체 시

**토픽**: `user-image-changed`

**비고**:

- 기존 이미지는 자동으로 `DELETED` 상태로 변경
- 교체된 새 이미지 정보만 발행

---

### 2. ImageDeletedEvent (이미지 삭제)

#### 이벤트 정보

- **클래스**: `com.teambind.image_server.event.events.ImageChangeEvent` (동일 구조)
- **발행 메서드**: `ImageChangeEventPublisher.imageDeletedEvent()`
- **사용 케이스**: 단일 이미지 타입의 전체 삭제

#### 페이로드 구조

```json
{
  "referenceId": "string",
  "imageId": null,
  "imageUrl": null
}
```

#### 페이로드 예시

##### 전체 삭제

```json
{
  "referenceId": "PROFILE-11111",
  "imageId": null,
  "imageUrl": null
}
```

**발행 시점**:

- `confirmImage("", referenceId)` 호출 시 (빈 문자열)
- 단일 이미지 타입의 이미지 전체 삭제

**토픽**: `profile-image-changed`

**처리 가이드**:

- Consumer는 `imageId == null && imageUrl == null`을 체크하여 삭제로 인식
- referenceId에 연결된 모든 이미지 정보를 제거해야 함

---

### 3. ImagesChangeEventWrapper (다중 이미지 변경)

#### 이벤트 정보

- **클래스**: `com.teambind.image_server.event.events.ImagesChangeEventWrapper`
- **발행 메서드**: `ImageChangeEventPublisher.imagesChangeEvent()`
- **사용 케이스**: 다중 이미지 타입의 일괄 확정/순서 변경/전체 삭제

#### 페이로드 구조

```json
{
  "referenceId": "string",
  "images": [
    {
      "imageId": "string",
      "imageUrl": "string",
      "referenceId": "string",
      "sequence": integer
    }
  ]
}
```

#### 필드 설명

| 필드                   | 타입      | 필수 | 설명                               |
|----------------------|---------|----|----------------------------------|
| referenceId          | String  | Y  | 참조 ID (빈 배열일 때 삭제 대상 식별용)        |
| images               | Array   | Y  | 이미지 변경 이벤트 배열 (빈 배열 = 전체 삭제)     |
| images[].imageId     | String  | Y  | 이미지 고유 ID                        |
| images[].imageUrl    | String  | Y  | 이미지 접근 URL                       |
| images[].referenceId | String  | Y  | 참조 ID (Wrapper의 referenceId와 동일) |
| images[].sequence    | Integer | Y  | 이미지 순서 (1부터 시작)                  |

#### 페이로드 예시

##### 1) 다중 이미지 일괄 등록

```json
{
  "referenceId": "PROD-99999",
  "images": [
    {
      "imageId": "img_prod_001",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_001.jpg",
      "referenceId": "PROD-99999",
      "sequence": 1
    },
    {
      "imageId": "img_prod_002",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_002.jpg",
      "referenceId": "PROD-99999",
      "sequence": 2
    },
    {
      "imageId": "img_prod_003",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_003.jpg",
      "referenceId": "PROD-99999",
      "sequence": 3
    }
  ]
}
```

**발행 시점**: PRODUCT 등 다중 이미지 타입에 여러 이미지 확정 시

**토픽**: `product-image-changed`

**비고**:

- sequence는 1부터 시작하는 순차 번호
- 배열 순서는 sequence 순서와 동일하게 정렬됨

---

##### 2) 이미지 순서 변경

```json
{
  "referenceId": "PROD-99999",
  "images": [
    {
      "imageId": "img_prod_003",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_003.jpg",
      "referenceId": "PROD-99999",
      "sequence": 1
    },
    {
      "imageId": "img_prod_001",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_001.jpg",
      "referenceId": "PROD-99999",
      "sequence": 2
    },
    {
      "imageId": "img_prod_002",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_002.jpg",
      "referenceId": "PROD-99999",
      "sequence": 3
    }
  ]
}
```

**발생 케이스**:

- 기존 이미지의 순서만 변경
- 이미지 ID는 동일하지만 sequence가 재배치됨

**토픽**: `product-image-changed`

---

##### 3) 일부 이미지 추가/삭제

```json
{
  "referenceId": "PROD-99999",
  "images": [
    {
      "imageId": "img_prod_001",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_001.jpg",
      "referenceId": "PROD-99999",
      "sequence": 1
    },
    {
      "imageId": "img_prod_004",
      "imageUrl": "https://cdn.example.com/images/product/img_prod_004.jpg",
      "referenceId": "PROD-99999",
      "sequence": 2
    }
  ]
}
```

**발생 케이스**:

- `img_prod_002`, `img_prod_003` 삭제
- `img_prod_004` 추가
- 최종 2개 이미지만 남음

**토픽**: `product-image-changed`

**처리 가이드**:

- Consumer는 해당 referenceId의 **전체 이미지를 교체**해야 함
- 이벤트에 포함되지 않은 이미지는 삭제된 것으로 간주

---

##### 4) 전체 삭제 (빈 배열)

```json
{
  "referenceId": "PROD-99999",
  "images": []
}
```

**발행 시점**:

- `confirmImages([], referenceId)` 호출 시
- 다중 이미지 타입의 전체 삭제

**토픽**: `product-image-changed`

**처리 가이드**:

- `images` 배열이 비어있으면 `referenceId`의 모든 이미지를 삭제
- `referenceId` 필드를 통해 어떤 대상의 이미지를 삭제할지 명확히 알 수 있음
- 전체 교체 로직과 동일하게 처리 가능 (빈 배열로 교체 = 삭제)

**비고**:

- 단일 이미지 타입의 삭제: `ImageDeletedEvent` (null 페이로드) 사용
- 다중 이미지 타입의 삭제: `ImagesChangeEventWrapper` 빈 배열 사용

---

## Consumer 구현 가이드

### 단일 이미지 타입 처리 (PROFILE, USER 등)

```java
// Topic: profile-image-changed, user-image-changed 등

@KafkaListener(topics = "profile-image-changed")
public void handleProfileImageChange(String message) {
	ImageChangeEvent event = objectMapper.readValue(message, ImageChangeEvent.class);
	
	if (event.getImageId() == null && event.getImageUrl() == null) {
		// 이미지 삭제
		removeProfileImage(event.getReferenceId());
	} else {
		// 이미지 등록/교체
		updateProfileImage(event.getReferenceId(), event.getImageId(), event.getImageUrl());
	}
}
```

### 다중 이미지 타입 처리 (PRODUCT, POST 등)

```java
// Topic: product-image-changed, post-image-changed 등

@KafkaListener(topics = "product-image-changed")
public void handleProductImageChange(String message) {
	ImagesChangeEventWrapper wrapper = objectMapper.readValue(
			message,
			ImagesChangeEventWrapper.class
	);
	
	String referenceId = wrapper.getReferenceId();
	List<SequentialImageChangeEvent> images = wrapper.getImages();
	
	if (images.isEmpty()) {
		// 빈 배열 = 전체 삭제
		removeAllProductImages(referenceId);
	} else {
		// 전체 교체 (기존 삭제 + 새로 추가)
		replaceProductImages(referenceId, images);
	}
}

private void replaceProductImages(String referenceId, List<SequentialImageChangeEvent> images) {
	// 1. 기존 이미지 전체 삭제
	deleteProductImagesByReferenceId(referenceId);
	
	// 2. 새 이미지 저장 (sequence 순서대로)
	for (SequentialImageChangeEvent image : images) {
		saveProductImage(
				image.getReferenceId(),
				image.getImageId(),
				image.getImageUrl(),
				image.getSequence()
		);
	}
}
```

### 중요 사항

1. **전체 교체 방식**: 다중 이미지 이벤트는 항상 "전체 교체" 방식으로 처리
	- 기존 이미지 전체 삭제 후 새로 추가
	- 부분 업데이트가 아님에 주의

2. **순서 보장**: `sequence` 필드를 사용하여 이미지 순서 유지

3. **멱등성 보장**: 같은 이벤트가 중복 처리되어도 결과가 동일하도록 구현

4. **빈 배열 처리**:
	- 다중 이미지: 빈 배열 = 전체 삭제
	- 단일 이미지: null 페이로드 = 삭제

---

## 재시도 정책

### 설정 위치

`KafkaConfig.java`

### 재시도 설정

```yaml
최대 재시도 횟수: 3회
초기 대기 시간: 1000ms (1초)
백오프 배율: 2.0 (지수 백오프)
최대 대기 시간: 10000ms (10초)
```

### 재시도 시나리오

| 시도 | 대기 시간  | 누적 시간  |
|----|--------|--------|
| 1차 | 0ms    | 0ms    |
| 2차 | 1000ms | 1000ms |
| 3차 | 2000ms | 3000ms |
| 4차 | 4000ms | 7000ms |

### 실패 처리

- 모든 재시도 실패 시: 로그만 기록하고 계속 진행
- DB 트랜잭션은 이미 커밋된 상태이므로 롤백하지 않음
- Kafka 발행 실패는 외부 시스템에 로그를 남기고 수동 복구 필요

### 로그 예시

```
[WARN] Kafka publish failed (attempt 1): topic=product-image-changed, error=...
[WARN] Kafka publish failed (attempt 2): topic=product-image-changed, error=...
[ERROR] All retry attempts exhausted for Kafka publish: topic=product-image-changed, totalAttempts=3
```

---

## 트러블슈팅

### 1. Kafka 브로커 연결 실패

**증상**: 모든 이벤트 발행 실패

**확인 사항**:

```bash
# Kafka 브로커 상태 확인
curl http://localhost:8080/actuator/health
```

**해결 방법**:

- Kafka 브로커 재시작
- `application.yml`의 `spring.kafka.bootstrap-servers` 설정 확인

---

### 2. 직렬화 오류

**증상**: `JsonProcessingException` 발생

**원인**: 이벤트 객체의 JSON 변환 실패

**해결 방법**:

- 이벤트 클래스에 기본 생성자 추가
- Lombok `@Data` 또는 `@Getter` 확인
- ObjectMapper 설정 점검

---

### 3. 토픽 자동 생성 실패

**증상**: Unknown topic 에러

**확인 사항**:

```yaml
# application.yml
spring.kafka.producer.properties:
  allow.auto.create.topics: true
```

**해결 방법**:

- Kafka 브로커에서 `auto.create.topics.enable=true` 설정
- 또는 토픽 수동 생성:

```bash
kafka-topics.sh --create --topic product-image-changed \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1
```

---

### 4. 이벤트 중복 발행

**증상**: 같은 이벤트가 여러 번 발행됨

**원인**:

- 재시도 중 일부 성공한 경우
- Consumer의 중복 처리 미흡

**해결 방법**:

- Consumer에서 멱등성(Idempotency) 보장
- `imageId` 또는 `referenceId`를 키로 사용하여 중복 체크

---

### 5. 순서 보장 문제

**증상**: 이미지 순서가 뒤바뀜

**원인**: Kafka 파티션 분산 전송

**해결 방법**:

- 같은 `referenceId`는 같은 파티션으로 전송되도록 Key 설정
- Producer 설정에서 `key`를 `referenceId`로 지정

**예시**:

```java
kafkaTemplate.send(topic, referenceId, json);
```

---

## 참고 자료

### 관련 소스 코드

- `EventPublisher.java`: Kafka 발행 로직
- `ImageChangeEventPublisher.java`: 이벤트 발행 래퍼
- `ImageSequenceEventHandler.java`: TransactionalEventListener
- `ImageConfirmService.java`: 비즈니스 로직

### 관련 문서

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Transactional Event Listeners](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)

---

**문서 버전**: 1.0
**최종 수정일**: 2025-10-24
**작성자**: Image Server Team
