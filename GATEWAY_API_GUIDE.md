# 이미지 서버 API 스펙 가이드 (게이트웨이 연동용)

## 개요

이 문서는 게이트웨이에서 이미지 서버와 통신하기 위한 API 스펙을 정의합니다.
이미지 업로드 API는 제외하고, 이미지 확정, 메타데이터 조회, 헬스체크 등의 API만 포함합니다.

---

## Base URL

```
http://image-server:8080
```

---

## 1. 헬스 체크 API

### 1.1 서버 상태 확인

서버가 정상적으로 동작하는지 확인합니다.

**Endpoint**

```
GET /health
```

**Response**

- **Success (200 OK)**

```
Server is up
```

---

## 2. 이미지 확정 API

### 2.1 단일 이미지 확정

업로드된 이미지를 특정 참조 ID(예: 상품 ID, 게시글 ID)에 연결하여 확정합니다.

**Endpoint**

```
POST /api/v1/images/confirm/{referenceId}
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| referenceId | String | O | 이미지를 연결할 참조 ID (예: 상품 ID, 게시글 ID) |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| imageId | String | O | 확정할 이미지 ID |

**Request Example**

```http
POST /api/v1/images/confirm/PRODUCT_12345?imageId=img_abc123
```

**Response**

- **Success (200 OK)**

```
빈 응답
```

- **Error (400 Bad Request)**

```json
{
  "timestamp": "2025-10-25T10:30:00",
  "status": 400,
  "code": "INVALID_IMAGE_ID",
  "message": "Invalid Image Id",
  "path": "/api/v1/images/confirm/PRODUCT_12345"
}
```

**주요 에러 코드**
| 에러 코드 | HTTP 상태 | 설명 |
|----------|-----------|------|
| INVALID_IMAGE_ID | 400 | 유효하지 않은 이미지 ID |
| IMAGE_NOT_FOUND | 404 | 이미지를 찾을 수 없음 |
| IMAGE_ALREADY_CONFIRMED | 400 | 이미 확정된 이미지 |
| IMAGE_PROCESSING_IN_PROGRESS | 409 | 이미지가 아직 처리 중 |
| IMAGE_PROCESSING_FAILED | 500 | 이미지 처리 실패 |

---

### 2.2 다중 이미지 확정 (배치)

여러 이미지를 한 번에 특정 참조 ID에 연결하여 확정합니다.

**Endpoint**

```
POST /api/v1/images/confirm
```

**Request Body**

```json
{
  "imageIds": [
    "img_abc123",
    "img_def456",
    "img_ghi789"
  ],
  "referenceId": "PRODUCT_12345"
}
```

**Request Body Schema**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| imageIds | List\<String\> | O | 확정할 이미지 ID 리스트 (빈 배열 허용) |
| referenceId | String | O | 이미지를 연결할 참조 ID |

**특수 케이스**

- `imageIds`가 빈 배열 `[]`인 경우: 해당 `referenceId`의 모든 이미지를 삭제합니다.

**Request Example**

```http
POST /api/v1/images/confirm
Content-Type: application/json

{
  "imageIds": ["img_abc123", "img_def456"],
  "referenceId": "PRODUCT_12345"
}
```

**Response**

- **Success (200 OK)**

```
빈 응답
```

- **Error (400 Bad Request)**

```json
{
  "timestamp": "2025-10-25T10:30:00",
  "status": 400,
  "code": "IMAGE_COUNT_EXCEEDED",
  "message": "Image Count Exceeded",
  "path": "/api/v1/images/confirm"
}
```

**주요 에러 코드**
| 에러 코드 | HTTP 상태 | 설명 |
|----------|-----------|------|
| INVALID_IMAGE_ID | 400 | 유효하지 않은 이미지 ID |
| IMAGE_COUNT_EXCEEDED | 400 | 최대 이미지 개수 초과 |
| NOT_ALLOWED_MULTIPLE_IMAGES | 400 | 다중 이미지가 허용되지 않는 참조 타입 |
| IMAGE_PROCESSING_IN_PROGRESS | 409 | 이미지가 아직 처리 중 |

---

## 3. 메타데이터 조회 API

### 3.1 지원하는 파일 확장자 목록 조회

이미지 서버에서 지원하는 파일 확장자 목록을 조회합니다.

**Endpoint**

```
GET /api/extensions
```

**Response**

- **Success (200 OK)**

```json
{
  "JPG": {
    "code": "JPG",
    "name": "JPEG Image"
  },
  "JPEG": {
    "code": "JPEG",
    "name": "JPEG Image"
  },
  "PNG": {
    "code": "PNG",
    "name": "Portable Network Graphics"
  },
  "GIF": {
    "code": "GIF",
    "name": "Graphics Interchange Format"
  },
  "WEBP": {
    "code": "WEBP",
    "name": "WebP Image"
  },
  "AVIF": {
    "code": "AVIF",
    "name": "AV1 Image File Format"
  },
  "BMP": {
    "code": "BMP",
    "name": "Bitmap Image"
  },
  "SVG": {
    "code": "SVG",
    "name": "Scalable Vector Graphics"
  },
  "MP4": {
    "code": "MP4",
    "name": "MPEG-4 Video"
  },
  "MOV": {
    "code": "MOV",
    "name": "QuickTime Movie"
  },
  "HEIC": {
    "code": "HEIC",
    "name": "High Efficiency Image File Format"
  },
  "HEIF": {
    "code": "HEIF",
    "name": "High Efficiency Image File Format"
  }
}
```

**Response Schema**

```json
{
  "[확장자코드]": {
    "code": "string (확장자 코드, 대문자)",
    "name": "string (확장자 이름)"
  }
}
```

---

### 3.2 참조 타입 목록 조회

이미지를 연결할 수 있는 참조 타입 목록을 조회합니다.

**Endpoint**

```
GET /api/referenceType
```

**Response**

- **Success (200 OK)**

```json
{
  "PRODUCT": {
    "code": "PRODUCT",
    "name": "상품",
    "allowsMultiple": true,
    "maxImages": 10,
    "description": "상품 이미지 (최대 10개)"
  },
  "POST": {
    "code": "POST",
    "name": "게시글",
    "allowsMultiple": true,
    "maxImages": 20,
    "description": "게시글 이미지 (최대 20개)"
  },
  "BANNER": {
    "code": "BANNER",
    "name": "배너",
    "allowsMultiple": true,
    "maxImages": null,
    "description": "배너 이미지 (무제한)"
  },
  "CATEGORY": {
    "code": "CATEGORY",
    "name": "카테고리",
    "allowsMultiple": false,
    "maxImages": 1,
    "description": "카테고리 썸네일 (단일 이미지)"
  },
  "PROFILE": {
    "code": "PROFILE",
    "name": "프로필",
    "allowsMultiple": false,
    "maxImages": 1,
    "description": "사용자 프로필 이미지 (단일 이미지)"
  },
  "USER_BACKGROUND": {
    "code": "USER_BACKGROUND",
    "name": "사용자 배경",
    "allowsMultiple": false,
    "maxImages": 1,
    "description": "사용자 배경 이미지 (단일 이미지)"
  }
}
```

**Response Schema**

```json
{
  "[참조타입코드]": {
    "code": "string (참조 타입 코드)",
    "name": "string (참조 타입 이름)",
    "allowsMultiple": "boolean (다중 이미지 허용 여부)",
    "maxImages": "integer | null (최대 이미지 개수, null이면 무제한)",
    "description": "string (설명)"
  }
}
```

**필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| code | String | 참조 타입 코드 (대문자) |
| name | String | 참조 타입 표시 이름 |
| allowsMultiple | Boolean | 다중 이미지 허용 여부 |
| maxImages | Integer | 최대 이미지 개수 (null = 무제한) |
| description | String | 참조 타입 설명 |

---

## 4. 스케줄 관리 API

### 4.1 미사용 이미지 정리

확정되지 않은 오래된 이미지를 수동으로 정리합니다.

**Endpoint**

```
GET /api/schedule/cleanup
```

**Response**

- **Success (200 OK)**

```
빈 응답
```

**참고**

- 이 API는 주로 관리자 또는 내부 스케줄러에서 호출됩니다.
- 확정되지 않은 채로 일정 시간이 지난 이미지를 삭제합니다.

---

## 5. 공통 에러 응답 형식

모든 에러 응답은 다음 형식을 따릅니다.

**Error Response Schema**

```json
{
  "timestamp": "string (ISO 8601 형식, yyyy-MM-dd'T'HH:mm:ss)",
  "status": "integer (HTTP 상태 코드)",
  "code": "string (에러 코드)",
  "message": "string (에러 메시지)",
  "path": "string (요청 경로)"
}
```

**예시**

```json
{
  "timestamp": "2025-10-25T10:30:00",
  "status": 400,
  "code": "INVALID_REFERENCE",
  "message": "Invalid Reference",
  "path": "/api/v1/images/confirm"
}
```

---

## 6. 주요 에러 코드 목록

| 에러 코드                        | HTTP 상태 | 설명               |
|------------------------------|---------|------------------|
| INVALID_REFERENCE            | 400     | 유효하지 않은 참조 ID    |
| INVALID_EXTENSION            | 400     | 유효하지 않은 파일 확장자   |
| INVALID_IMAGE_ID             | 400     | 유효하지 않은 이미지 ID   |
| IMAGE_NOT_FOUND              | 404     | 이미지를 찾을 수 없음     |
| IMAGE_ALREADY_CONFIRMED      | 400     | 이미 확정된 이미지       |
| IMAGE_COUNT_EXCEEDED         | 400     | 최대 이미지 개수 초과     |
| NOT_ALLOWED_MULTIPLE_IMAGES  | 400     | 다중 이미지 미허용 참조 타입 |
| REFERENCE_TYPE_NOT_FOUND     | 400     | 참조 타입을 찾을 수 없음   |
| FILE_EXTENSION_NOT_FOUND     | 400     | 파일 확장자를 찾을 수 없음  |
| IMAGE_PROCESSING_IN_PROGRESS | 409     | 이미지가 아직 처리 중     |
| IMAGE_PROCESSING_FAILED      | 500     | 이미지 처리 실패        |
| IMAGE_SAVE_FAILED            | 500     | 이미지 저장 실패        |

---

## 7. 사용 시나리오

### 시나리오 1: 상품 이미지 등록

1. **이미지 업로드** (별도 API, 본 문서에서 제외)
	- 사용자가 이미지를 업로드하면 `imageId`를 받음

2. **참조 타입 확인**
   ```http
   GET /api/referenceType
   ```
	- `PRODUCT` 타입이 다중 이미지를 허용하는지, 최대 개수는 몇 개인지 확인

3. **이미지 확정**
   ```http
   POST /api/v1/images/confirm
   Content-Type: application/json

   {
     "imageIds": ["img_abc123", "img_def456"],
     "referenceId": "PRODUCT_12345"
   }
   ```

### 시나리오 2: 프로필 이미지 변경

1. **이미지 업로드** (별도 API)
	- 새 프로필 이미지를 업로드하면 `imageId`를 받음

2. **단일 이미지 확정**
   ```http
   POST /api/v1/images/confirm/USER_67890?imageId=img_xyz999
   ```
	- 기존 프로필 이미지는 자동으로 대체됨

### 시나리오 3: 모든 이미지 삭제

1. **빈 배열로 확정**
   ```http
   POST /api/v1/images/confirm
   Content-Type: application/json

   {
     "imageIds": [],
     "referenceId": "PRODUCT_12345"
   }
   ```
	- 해당 참조 ID의 모든 이미지가 삭제됨

---

## 8. 참고 사항

### 8.1 이미지 처리 상태

- 이미지 업로드 후 비동기로 처리되므로, 즉시 확정할 수 없을 수 있습니다.
- `IMAGE_PROCESSING_IN_PROGRESS` 에러가 발생하면 잠시 후 재시도해야 합니다.

### 8.2 참조 타입별 제약사항

- 각 참조 타입마다 허용되는 이미지 개수가 다릅니다.
- `allowsMultiple: false`인 경우 단일 이미지만 허용됩니다.
- `maxImages`를 초과하면 `IMAGE_COUNT_EXCEEDED` 에러가 발생합니다.

### 8.3 타임스탬프 형식

- 모든 타임스탬프는 ISO 8601 형식을 따릅니다: `yyyy-MM-dd'T'HH:mm:ss`

---

## 9. API 호출 예시 (cURL)

### 9.1 헬스 체크

```bash
curl -X GET http://image-server:8080/health
```

### 9.2 단일 이미지 확정

```bash
curl -X POST "http://image-server:8080/api/v1/images/confirm/PRODUCT_12345?imageId=img_abc123"
```

### 9.3 다중 이미지 확정

```bash
curl -X POST http://image-server:8080/api/v1/images/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "imageIds": ["img_abc123", "img_def456"],
    "referenceId": "PRODUCT_12345"
  }'
```

### 9.4 확장자 목록 조회

```bash
curl -X GET http://image-server:8080/api/extensions
```

### 9.5 참조 타입 목록 조회

```bash
curl -X GET http://image-server:8080/api/referenceType
```

### 9.6 미사용 이미지 정리

```bash
curl -X GET http://image-server:8080/api/schedule/cleanup
```

---

## 변경 이력

| 날짜         | 버전  | 변경 내용 |
|------------|-----|-------|
| 2025-10-25 | 1.0 | 최초 작성 |
