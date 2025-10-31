# API 명세서

## Base URL
\`\`\`
http://<ALB_DNS>/api
\`\`\`

## Endpoints

### 1. Health Check
\`\`\`
GET /health
\`\`\`

### 2. 이벤트 조회
\`\`\`
GET /events
GET /events/{id}
GET /events/upcoming
GET /events/available
\`\`\`

### 3. 이벤트 생성
\`\`\`
POST /events
Content-Type: application/json

{
  "eventName": "아이유 콘서트",
  "totalSeats": 10000,
  "eventDate": "2024-12-31T19:00:00"
}
\`\`\`

### 4. 티켓 예매
\`\`\`
POST /reservations
Content-Type: application/json

{
  "eventId": 1,
  "userId": "user123",
  "quantity": 2
}
\`\`\`

### 5. 예매 내역 조회
\`\`\`
GET /reservations/user/{userId}
GET /reservations/{id}
\`\`\`

### 6. 예매 취소
\`\`\`
DELETE /reservations/{id}
\`\`\`

자세한 응답 형식은 Spring Boot 코드 참고
