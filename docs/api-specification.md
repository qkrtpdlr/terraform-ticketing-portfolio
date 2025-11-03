# 📡 API 명세서

티켓팅 시스템 RESTful API 상세 명세서입니다.

---

## 📋 목차

1. [개요](#개요)
2. [인증](#인증)
3. [응답 형식](#응답-형식)
4. [에러 코드](#에러-코드)
5. [API 엔드포인트](#api-엔드포인트)
   - [Health Check](#health-check)
   - [이벤트 관리](#이벤트-관리)
   - [예매 관리](#예매-관리)
   - [사용자 관리](#사용자-관리)

---

## 개요

### Base URL

```
Production: https://ticketing-alb-123456789.ap-northeast-2.elb.amazonaws.com
Development: http://localhost:8080
```

### 버전

- **현재 버전**: v1.0
- **최종 업데이트**: 2024-11-03

### 통신 프로토콜

- **Protocol**: HTTP/1.1, HTTP/2
- **Content-Type**: `application/json`
- **Character Encoding**: UTF-8

---

## 인증

### JWT 토큰 기반 인증 (향후 구현 예정)

현재 버전은 인증이 없으며, 향후 JWT 토큰 기반 인증이 추가될 예정입니다.

**예상 형식**:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "message": "Success",
  "timestamp": "2024-11-03T12:00:00"
}
```

### 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": "상세 정보"
  },
  "timestamp": "2024-11-03T12:00:00"
}
```

---

## 에러 코드

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|----------|------|
| 400 | `INVALID_REQUEST` | 잘못된 요청 형식 |
| 404 | `EVENT_NOT_FOUND` | 이벤트를 찾을 수 없음 |
| 404 | `RESERVATION_NOT_FOUND` | 예매 내역을 찾을 수 없음 |
| 409 | `INSUFFICIENT_SEATS` | 좌석 부족 |
| 409 | `CONCURRENT_RESERVATION` | 동시 예매 진행 중 |
| 409 | `ALREADY_RESERVED` | 이미 예매 완료 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
| 503 | `SERVICE_UNAVAILABLE` | 서비스 일시 중단 |

---

## API 엔드포인트

## Health Check

### Health Check

시스템 상태 확인

**엔드포인트**:
```
GET /api/health
```

**파라미터**: 없음

**응답 예시** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 10737418240,
        "free": 8589934592,
        "threshold": 10485760
      }
    }
  }
}
```

**응답 예시** (503 Service Unavailable - DB 다운):
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused"
      }
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

---

## 이벤트 관리

### 1. 이벤트 목록 조회

모든 이벤트 목록 조회

**엔드포인트**:
```
GET /api/events
```

**쿼리 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| `page` | Integer | N | 페이지 번호 (0부터 시작) | 0 |
| `size` | Integer | N | 페이지당 항목 수 | 20 |
| `sort` | String | N | 정렬 기준 (eventDate,desc) | eventDate,asc |
| `status` | String | N | 이벤트 상태 (ACTIVE, CLOSED, CANCELLED) | 전체 |

**요청 예시**:
```bash
curl "http://localhost:8080/api/events?page=0&size=10&sort=eventDate,desc"
```

**응답 예시** (200 OK):
```json
{
  "content": [
    {
      "eventId": 1,
      "eventName": "콘서트 티켓 판매",
      "totalSeats": 10000,
      "availableSeats": 9998,
      "eventDate": "2024-12-31T19:00:00",
      "status": "ACTIVE",
      "createdAt": "2024-11-03T12:00:00",
      "updatedAt": "2024-11-03T12:05:00"
    },
    {
      "eventId": 2,
      "eventName": "뮤지컬 티켓 판매",
      "totalSeats": 500,
      "availableSeats": 450,
      "eventDate": "2024-12-25T15:00:00",
      "status": "ACTIVE",
      "createdAt": "2024-11-02T10:00:00",
      "updatedAt": "2024-11-02T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false
}
```

---

### 2. 이벤트 상세 조회

특정 이벤트의 상세 정보 조회 (Redis 캐싱 적용)

**엔드포인트**:
```
GET /api/events/{eventId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `eventId` | Long | Y | 이벤트 ID |

**요청 예시**:
```bash
curl http://localhost:8080/api/events/1
```

**응답 예시** (200 OK):
```json
{
  "eventId": 1,
  "eventName": "콘서트 티켓 판매",
  "totalSeats": 10000,
  "availableSeats": 9998,
  "eventDate": "2024-12-31T19:00:00",
  "venue": {
    "name": "서울 올림픽 주경기장",
    "address": "서울특별시 송파구 올림픽로 424",
    "capacity": 10000
  },
  "status": "ACTIVE",
  "price": {
    "vip": 150000,
    "r": 100000,
    "s": 70000
  },
  "createdAt": "2024-11-03T12:00:00",
  "updatedAt": "2024-11-03T12:05:00",
  "metadata": {
    "cacheHit": true,
    "ttl": 298
  }
}
```

**에러 응답** (404 Not Found):
```json
{
  "success": false,
  "error": {
    "code": "EVENT_NOT_FOUND",
    "message": "이벤트를 찾을 수 없습니다.",
    "details": "eventId: 999"
  },
  "timestamp": "2024-11-03T12:00:00"
}
```

---

### 3. 이벤트 생성

새로운 이벤트 생성 (관리자 전용)

**엔드포인트**:
```
POST /api/events
```

**요청 헤더**:
```
Content-Type: application/json
```

**요청 바디**:
```json
{
  "eventName": "콘서트 티켓 판매",
  "totalSeats": 10000,
  "eventDate": "2024-12-31T19:00:00",
  "venue": {
    "name": "서울 올림픽 주경기장",
    "address": "서울특별시 송파구 올림픽로 424",
    "capacity": 10000
  },
  "price": {
    "vip": 150000,
    "r": 100000,
    "s": 70000
  }
}
```

**요청 예시**:
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "콘서트 티켓 판매",
    "totalSeats": 10000,
    "eventDate": "2024-12-31T19:00:00"
  }'
```

**응답 예시** (201 Created):
```json
{
  "eventId": 1,
  "eventName": "콘서트 티켓 판매",
  "totalSeats": 10000,
  "availableSeats": 10000,
  "eventDate": "2024-12-31T19:00:00",
  "status": "ACTIVE",
  "createdAt": "2024-11-03T12:00:00",
  "updatedAt": "2024-11-03T12:00:00"
}
```

**에러 응답** (400 Bad Request):
```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "잘못된 요청 형식입니다.",
    "details": {
      "eventName": "이벤트 이름은 필수입니다.",
      "totalSeats": "좌석 수는 1 이상이어야 합니다."
    }
  },
  "timestamp": "2024-11-03T12:00:00"
}
```

---

### 4. 이벤트 수정

기존 이벤트 정보 수정 (관리자 전용)

**엔드포인트**:
```
PUT /api/events/{eventId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `eventId` | Long | Y | 이벤트 ID |

**요청 바디**:
```json
{
  "eventName": "콘서트 티켓 판매 (수정)",
  "eventDate": "2024-12-31T20:00:00",
  "status": "ACTIVE"
}
```

**요청 예시**:
```bash
curl -X PUT http://localhost:8080/api/events/1 \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "콘서트 티켓 판매 (수정)",
    "eventDate": "2024-12-31T20:00:00"
  }'
```

**응답 예시** (200 OK):
```json
{
  "eventId": 1,
  "eventName": "콘서트 티켓 판매 (수정)",
  "totalSeats": 10000,
  "availableSeats": 9998,
  "eventDate": "2024-12-31T20:00:00",
  "status": "ACTIVE",
  "createdAt": "2024-11-03T12:00:00",
  "updatedAt": "2024-11-03T13:00:00"
}
```

---

### 5. 이벤트 삭제

이벤트 삭제 (관리자 전용)

**엔드포인트**:
```
DELETE /api/events/{eventId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `eventId` | Long | Y | 이벤트 ID |

**요청 예시**:
```bash
curl -X DELETE http://localhost:8080/api/events/1
```

**응답 예시** (204 No Content):
```
(응답 바디 없음)
```

**에러 응답** (409 Conflict - 예매 건이 있는 경우):
```json
{
  "success": false,
  "error": {
    "code": "CANNOT_DELETE_EVENT",
    "message": "예매 건이 있는 이벤트는 삭제할 수 없습니다.",
    "details": "eventId: 1, reservations: 245"
  },
  "timestamp": "2024-11-03T12:00:00"
}
```

---

## 예매 관리

### 1. 티켓 예매

티켓 예매 (Redis 분산 락 적용)

**엔드포인트**:
```
POST /api/reservations
```

**요청 헤더**:
```
Content-Type: application/json
```

**요청 바디**:
```json
{
  "eventId": 1,
  "userId": "user123",
  "quantity": 2,
  "seatType": "R"
}
```

**필드 설명**:
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `eventId` | Long | Y | 이벤트 ID |
| `userId` | String | Y | 사용자 ID |
| `quantity` | Integer | Y | 예매 수량 (1-10) |
| `seatType` | String | N | 좌석 등급 (VIP, R, S) |

**요청 예시**:
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "userId": "user123",
    "quantity": 2
  }'
```

**응답 예시** (201 Created):
```json
{
  "reservationId": 1,
  "eventId": 1,
  "userId": "user123",
  "quantity": 2,
  "seatType": "R",
  "totalPrice": 200000,
  "status": "CONFIRMED",
  "reservedAt": "2024-11-03T12:05:00",
  "expiresAt": "2024-11-03T12:20:00",
  "event": {
    "eventId": 1,
    "eventName": "콘서트 티켓 판매",
    "eventDate": "2024-12-31T19:00:00"
  },
  "lockAcquisitionTime": 8.34
}
```

**에러 응답** (409 Conflict - 좌석 부족):
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_SEATS",
    "message": "좌석이 부족합니다.",
    "details": {
      "requested": 2,
      "available": 0
    }
  },
  "timestamp": "2024-11-03T12:05:00"
}
```

**에러 응답** (409 Conflict - 동시 예매):
```json
{
  "success": false,
  "error": {
    "code": "CONCURRENT_RESERVATION",
    "message": "동시 예매가 진행 중입니다. 잠시 후 다시 시도해주세요.",
    "details": "lockKey: lock:event:1, waitTime: 10s"
  },
  "timestamp": "2024-11-03T12:05:00"
}
```

---

### 2. 예매 내역 조회

특정 예매 내역 상세 조회

**엔드포인트**:
```
GET /api/reservations/{reservationId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `reservationId` | Long | Y | 예매 ID |

**요청 예시**:
```bash
curl http://localhost:8080/api/reservations/1
```

**응답 예시** (200 OK):
```json
{
  "reservationId": 1,
  "eventId": 1,
  "userId": "user123",
  "quantity": 2,
  "seatType": "R",
  "totalPrice": 200000,
  "status": "CONFIRMED",
  "reservedAt": "2024-11-03T12:05:00",
  "expiresAt": "2024-11-03T12:20:00",
  "event": {
    "eventId": 1,
    "eventName": "콘서트 티켓 판매",
    "eventDate": "2024-12-31T19:00:00",
    "availableSeats": 9998
  },
  "payment": {
    "paymentId": 1,
    "amount": 200000,
    "method": "CARD",
    "status": "COMPLETED",
    "paidAt": "2024-11-03T12:06:00"
  }
}
```

**에러 응답** (404 Not Found):
```json
{
  "success": false,
  "error": {
    "code": "RESERVATION_NOT_FOUND",
    "message": "예매 내역을 찾을 수 없습니다.",
    "details": "reservationId: 999"
  },
  "timestamp": "2024-11-03T12:00:00"
}
```

---

### 3. 사용자별 예매 목록 조회

특정 사용자의 예매 목록 조회

**엔드포인트**:
```
GET /api/reservations/user/{userId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `userId` | String | Y | 사용자 ID |

**쿼리 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| `page` | Integer | N | 페이지 번호 | 0 |
| `size` | Integer | N | 페이지당 항목 수 | 20 |
| `status` | String | N | 예매 상태 필터 | 전체 |

**요청 예시**:
```bash
curl "http://localhost:8080/api/reservations/user/user123?page=0&size=10"
```

**응답 예시** (200 OK):
```json
{
  "content": [
    {
      "reservationId": 1,
      "eventId": 1,
      "userId": "user123",
      "quantity": 2,
      "status": "CONFIRMED",
      "reservedAt": "2024-11-03T12:05:00",
      "event": {
        "eventName": "콘서트 티켓 판매",
        "eventDate": "2024-12-31T19:00:00"
      }
    },
    {
      "reservationId": 2,
      "eventId": 2,
      "userId": "user123",
      "quantity": 1,
      "status": "CANCELLED",
      "reservedAt": "2024-11-02T10:00:00",
      "cancelledAt": "2024-11-02T11:00:00",
      "event": {
        "eventName": "뮤지컬 티켓 판매",
        "eventDate": "2024-12-25T15:00:00"
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 12,
  "totalPages": 2
}
```

---

### 4. 예매 취소

예매 취소 (좌석 복원)

**엔드포인트**:
```
DELETE /api/reservations/{reservationId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `reservationId` | Long | Y | 예매 ID |

**요청 예시**:
```bash
curl -X DELETE http://localhost:8080/api/reservations/1
```

**응답 예시** (200 OK):
```json
{
  "reservationId": 1,
  "status": "CANCELLED",
  "cancelledAt": "2024-11-03T13:00:00",
  "refund": {
    "refundAmount": 200000,
    "refundMethod": "CARD",
    "refundStatus": "PROCESSING",
    "expectedAt": "2024-11-06T13:00:00"
  }
}
```

**에러 응답** (409 Conflict - 이미 취소됨):
```json
{
  "success": false,
  "error": {
    "code": "ALREADY_CANCELLED",
    "message": "이미 취소된 예매입니다.",
    "details": "reservationId: 1, cancelledAt: 2024-11-03T13:00:00"
  },
  "timestamp": "2024-11-03T14:00:00"
}
```

---

## 사용자 관리

### 1. 사용자 정보 조회

사용자 기본 정보 조회

**엔드포인트**:
```
GET /api/users/{userId}
```

**경로 파라미터**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `userId` | String | Y | 사용자 ID |

**요청 예시**:
```bash
curl http://localhost:8080/api/users/user123
```

**응답 예시** (200 OK):
```json
{
  "userId": "user123",
  "username": "홍길동",
  "email": "user123@example.com",
  "phoneNumber": "010-1234-5678",
  "createdAt": "2024-01-01T00:00:00",
  "lastLoginAt": "2024-11-03T12:00:00",
  "statistics": {
    "totalReservations": 12,
    "totalSpent": 2400000,
    "cancelledReservations": 2
  }
}
```

---

## 부록

### Rate Limiting

현재 버전은 Rate Limiting이 적용되지 않았으나, 향후 다음과 같이 적용될 예정입니다:

- **일반 사용자**: 100 요청/분
- **관리자**: 1000 요청/분

### Pagination

모든 목록 API는 페이지네이션을 지원합니다:

```
GET /api/events?page=0&size=20&sort=eventDate,desc
```

| 파라미터 | 설명 | 기본값 |
|---------|------|--------|
| `page` | 페이지 번호 (0부터 시작) | 0 |
| `size` | 페이지당 항목 수 | 20 |
| `sort` | 정렬 기준 (field,direction) | id,asc |

### Caching Headers

Redis 캐싱이 적용된 API는 다음 헤더를 포함합니다:

```
X-Cache-Hit: true
X-Cache-TTL: 298
```

---

**작성일**: 2024-11-03  
**버전**: 1.0.0  
**작성자**: rlagudfo1223@gmail.com
