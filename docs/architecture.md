# 🏗️ 아키텍처 상세 문서

티켓팅 시스템의 전체 아키텍처와 설계 철학을 상세히 설명합니다.

---

## 📋 목차

1. [시스템 개요](#시스템-개요)
2. [아키텍처 원칙](#아키텍처-원칙)
3. [인프라 아키텍처](#인프라-아키텍처)
4. [애플리케이션 아키텍처](#애플리케이션-아키텍처)
5. [데이터 아키텍처](#데이터-아키텍처)
6. [보안 아키텍처](#보안-아키텍처)
7. [모니터링 아키텍처](#모니터링-아키텍처)
8. [확장성 설계](#확장성-설계)

---

## 시스템 개요

### 비즈니스 요구사항

티켓팅 시스템은 다음 요구사항을 만족해야 합니다:

| 요구사항 | 목표 | 달성 방법 |
|---------|------|----------|
| **대규모 트래픽 처리** | 10,000+ 동시 접속 | Auto Scaling + Redis |
| **빠른 응답 시간** | 평균 50ms 이하 | Redis 캐싱 + RDS Read Replica |
| **정확한 재고 관리** | 오버부킹 0건 | Redis 분산 락 + 비관적 락 |
| **고가용성** | 99.9% Uptime | Multi-AZ + Auto Failover |
| **비용 효율성** | $400/월 이하 | t3 인스턴스 + 최적화 |

### 기술 스택 선정 이유

| 기술 | 선정 이유 |
|------|----------|
| **Terraform** | IaC로 재현 가능한 인프라, 멀티 클라우드 지원 |
| **AWS** | 높은 안정성, 풍부한 서비스, 한국 리전 |
| **Spring Boot** | 생산성, 풍부한 생태계, 엔터프라이즈급 안정성 |
| **Aurora MySQL** | MySQL 호환, Multi-AZ, 자동 백업, 고성능 |
| **ElastiCache Redis** | 인메모리 캐싱, 분산 락, 세션 관리, 고속 |
| **Docker** | 컨테이너화, 환경 일관성, 빠른 배포 |

---

## 아키텍처 원칙

### 1. 고가용성 (High Availability)

**원칙**: Single Point of Failure(SPOF) 제거

**구현**:
- ✅ Multi-AZ 배포 (AZ-2a, AZ-2c)
- ✅ RDS Aurora Multi-AZ (Writer + Reader)
- ✅ ElastiCache Redis Replication (Primary + Replica)
- ✅ Auto Scaling Group (최소 2대)
- ✅ ALB Health Check (자동 Failover)

### 2. 확장성 (Scalability)

**원칙**: 수평 확장 가능한 아키텍처

**구현**:
- ✅ Stateless 애플리케이션 (세션은 Redis에 저장)
- ✅ Auto Scaling (2-20대)
- ✅ RDS Read Replica (읽기 부하 분산)
- ✅ Redis 캐싱 (DB 부하 감소)

### 3. 보안 (Security)

**원칙**: Defense in Depth (심층 방어)

**구현**:
- ✅ Private Subnet (EC2, RDS, Redis)
- ✅ Security Group (최소 권한 원칙)
- ✅ IAM Role (EC2 → ECR, CloudWatch)
- ✅ Secrets Manager (비밀번호 암호화, 향후)
- ✅ SSL/TLS (ALB)

### 4. 관찰 가능성 (Observability)

**원칙**: 모든 것을 측정하고 모니터링

**구현**:
- ✅ CloudWatch Metrics (CPU, Memory, Disk)
- ✅ CloudWatch Logs (애플리케이션 로그)
- ✅ CloudWatch Alarms (임계치 기반 알람)
- ✅ X-Ray Tracing (향후)

### 5. 비용 최적화 (Cost Optimization)

**원칙**: 필요한 만큼만 사용

**구현**:
- ✅ t3 인스턴스 (Burstable Performance)
- ✅ Auto Scaling (수요에 따라 조정)
- ✅ Reserved Instances (장기 사용 시)
- ✅ Spot Instances (개발 환경)

---

## 인프라 아키텍처

### 네트워크 아키텍처

```
Region: ap-northeast-2
│
├── VPC (10.0.0.0/16)
│   │
│   ├── Public Subnets (인터넷 접근 가능)
│   │   ├── AZ-2a: 10.0.1.0/24
│   │   │   ├── NAT Gateway
│   │   │   └── Bastion Host (향후)
│   │   │
│   │   └── AZ-2c: 10.0.2.0/24
│   │       └── NAT Gateway
│   │
│   ├── Private Subnets (애플리케이션 계층)
│   │   ├── AZ-2a: 10.0.11.0/24
│   │   │   ├── EC2 Auto Scaling Group (2-10대)
│   │   │   └── ElastiCache Redis Primary
│   │   │
│   │   └── AZ-2c: 10.0.12.0/24
│   │       ├── EC2 Auto Scaling Group (2-10대)
│   │       └── ElastiCache Redis Replica
│   │
│   └── DB Subnets (데이터베이스 계층)
│       ├── AZ-2a: 10.0.21.0/24
│       │   └── RDS Aurora Writer
│       │
│       └── AZ-2c: 10.0.22.0/24
│           └── RDS Aurora Reader
│
├── Internet Gateway
│   └── ALB (Application Load Balancer)
│
└── Route Tables
    ├── Public Route Table
    │   └── 0.0.0.0/0 → Internet Gateway
    │
    ├── Private Route Table (AZ-2a)
    │   └── 0.0.0.0/0 → NAT Gateway (AZ-2a)
    │
    └── Private Route Table (AZ-2c)
        └── 0.0.0.0/0 → NAT Gateway (AZ-2c)
```

### 컴퓨트 아키텍처

**Auto Scaling Group 설정**:
```hcl
min_size     = 2   # 최소 2대 (Multi-AZ)
max_size     = 20  # 최대 20대
desired_size = 2   # 희망 2대

# Scale Up Trigger
CPU > 70% for 2분 → +2대 증가

# Scale Down Trigger
CPU < 20% for 5분 → -1대 감소
```

**Launch Template**:
- **AMI**: Amazon Linux 2
- **Instance Type**: t3.medium (2 vCPU, 4GB RAM)
- **User Data**: Docker 설치 + ECR 로그인 + 앱 실행
- **IAM Role**: ECR 읽기, CloudWatch Logs 쓰기

**배치 전략**:
- **AZ 분산**: 각 AZ에 최소 1대씩 배치
- **균등 분산**: 가능한 한 AZ 간 인스턴스 수 균등

---

## 애플리케이션 아키텍처

### Layered Architecture

```
┌─────────────────────────────────────┐
│       Presentation Layer            │
│  (REST API Controllers)             │
│  - EventController                  │
│  - ReservationController            │
│  - UserController                   │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│       Service Layer                 │
│  (Business Logic)                   │
│  - EventService                     │
│  - ReservationService               │
│  - CacheService                     │
│  - DistributedLockService           │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│       Repository Layer              │
│  (Data Access)                      │
│  - EventRepository                  │
│  - ReservationRepository            │
│  - RedisTemplate                    │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│       Persistence Layer             │
│  - RDS Aurora MySQL                 │
│  - ElastiCache Redis                │
└─────────────────────────────────────┘
```

### 주요 컴포넌트

#### 1. 분산 락 서비스 (DistributedLockService)

**목적**: 동시성 제어, 오버부킹 방지

**구현**:
```java
public class DistributedLockService {
    
    // Redis SETNX를 이용한 분산 락
    public boolean acquireLock(String lockKey, long ttlSeconds) {
        return redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", ttlSeconds, TimeUnit.SECONDS);
    }
    
    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
    
    // 락 획득 대기 (최대 10초)
    public boolean acquireLockWithRetry(String lockKey, long ttlSeconds, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            if (acquireLock(lockKey, ttlSeconds)) {
                return true;
            }
            Thread.sleep(100); // 100ms 대기
        }
        return false;
    }
}
```

**특징**:
- ✅ TTL 설정으로 데드락 방지 (10초 자동 해제)
- ✅ 비동기 처리 가능
- ✅ 고성능 (Redis 인메모리)

#### 2. 캐시 서비스 (CacheService)

**목적**: DB 부하 감소, 응답 시간 단축

**구현**:
```java
@Service
public class CacheService {
    
    // Cache-Aside 패턴
    @Cacheable(value = "events", key = "#eventId", unless = "#result == null")
    public Event getEvent(Long eventId) {
        // Cache Miss 시 DB 조회
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException());
    }
    
    // 캐시 무효화
    @CacheEvict(value = "events", key = "#eventId")
    public void evictEvent(Long eventId) {
        // 이벤트 수정/삭제 시 캐시 삭제
    }
    
    // 전체 캐시 무효화
    @CacheEvict(value = "events", allEntries = true)
    public void evictAllEvents() {
        // 대량 업데이트 시 전체 캐시 삭제
    }
}
```

**캐시 전략**:
| 데이터 유형 | TTL | Eviction Policy |
|-----------|-----|-----------------|
| 이벤트 정보 | 5분 | allkeys-lru |
| 사용자 정보 | 10분 | allkeys-lru |
| 예매 내역 | 30초 | allkeys-lru |

#### 3. 예매 서비스 (ReservationService)

**목적**: 티켓 예매 핵심 로직

**트랜잭션 흐름**:
```
1. Redis 분산 락 획득 (lock:event:{eventId})
   ├─ 성공 → 2단계 진행
   └─ 실패 → ConcurrentReservationException

2. 이벤트 조회 (SELECT FOR UPDATE)
   ├─ 존재 → 3단계 진행
   └─ 없음 → EventNotFoundException

3. 좌석 확인
   ├─ 충분 → 4단계 진행
   └─ 부족 → InsufficientSeatsException

4. 좌석 차감 (UPDATE events SET available_seats = available_seats - ?)
   └─ 5단계 진행

5. 예매 생성 (INSERT INTO reservations)
   └─ 6단계 진행

6. 캐시 무효화 (EVICT event:{eventId})
   └─ 7단계 진행

7. Redis 분산 락 해제 (DEL lock:event:{eventId})
   └─ 완료
```

**에러 처리**:
- **ConcurrentReservationException**: 다른 사용자가 예매 중 → 재시도 안내
- **InsufficientSeatsException**: 좌석 부족 → 대기 안내
- **EventNotFoundException**: 이벤트 없음 → 404 반환

---

## 데이터 아키텍처

### RDS Aurora MySQL 스키마

#### events 테이블

```sql
CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_name VARCHAR(255) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    event_date DATETIME NOT NULL,
    venue_name VARCHAR(255),
    venue_address VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_event_date (event_date),
    INDEX idx_status (status),
    CHECK (available_seats >= 0),
    CHECK (total_seats > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**인덱스 전략**:
- `idx_event_date`: 날짜별 이벤트 조회 최적화
- `idx_status`: 상태별 필터링 최적화

#### reservations 테이블

```sql
CREATE TABLE reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    seat_type VARCHAR(10),
    total_price DECIMAL(10,2),
    status VARCHAR(50) DEFAULT 'CONFIRMED',
    reserved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    cancelled_at DATETIME,
    expires_at DATETIME,
    
    FOREIGN KEY (event_id) REFERENCES events(id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_status (status),
    INDEX idx_reserved_at (reserved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**인덱스 전략**:
- `idx_user_id`: 사용자별 예매 내역 조회
- `idx_event_id`: 이벤트별 예매 통계
- `idx_reserved_at`: 시간별 예매 분석

### Redis 데이터 구조

#### 1. 캐시 데이터

**Key 패턴**:
```
event:{eventId}              # 이벤트 상세 정보
user:{userId}                # 사용자 정보
reservation:{reservationId}  # 예매 내역
```

**예시**:
```redis
# 이벤트 캐시
redis> GET event:1
"{\"eventId\":1,\"eventName\":\"콘서트\",\"availableSeats\":9998}"

# TTL 확인
redis> TTL event:1
(integer) 298  # 남은 시간 (초)
```

#### 2. 분산 락

**Key 패턴**:
```
lock:event:{eventId}         # 이벤트별 분산 락
```

**예시**:
```redis
# 락 획득
redis> SET lock:event:1 "locked" EX 10 NX
OK

# 락 확인
redis> GET lock:event:1
"locked"

# 락 해제
redis> DEL lock:event:1
(integer) 1
```

#### 3. 세션 관리 (향후)

**Key 패턴**:
```
session:{sessionId}          # 사용자 세션
```

### 데이터 백업 전략

| 백업 유형 | 주기 | 보관 기간 | 용도 |
|----------|------|----------|------|
| **자동 백업** | 매일 03:00 | 7일 | 일상적인 복구 |
| **수동 스냅샷** | 주요 변경 전 | 30일 | 롤백 포인트 |
| **바이너리 로그** | 실시간 | 7일 | Point-in-Time Recovery |

---

## 보안 아키텍처

### Security Group 설계

#### ALB Security Group

```hcl
resource "aws_security_group" "alb" {
  name = "ticketing-alb-sg"
  
  # Inbound: 인터넷에서 HTTP/HTTPS 허용
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  # Outbound: EC2로 8080 허용
  egress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }
}
```

#### EC2 Security Group

```hcl
resource "aws_security_group" "ec2" {
  name = "ticketing-ec2-sg"
  
  # Inbound: ALB에서만 8080 허용
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  
  # Outbound: RDS 3306, Redis 6379 허용
  egress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.rds.id]
  }
  
  egress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.redis.id]
  }
}
```

#### RDS Security Group

```hcl
resource "aws_security_group" "rds" {
  name = "ticketing-rds-sg"
  
  # Inbound: EC2에서만 3306 허용
  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }
}
```

#### Redis Security Group

```hcl
resource "aws_security_group" "redis" {
  name = "ticketing-redis-sg"
  
  # Inbound: EC2에서만 6379 허용
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }
}
```

### IAM 역할 설계

#### EC2 Instance Profile

```hcl
resource "aws_iam_role" "ec2" {
  name = "ticketing-ec2-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

# ECR 읽기 권한
resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# CloudWatch Logs 쓰기 권한
resource "aws_iam_role_policy_attachment" "cloudwatch_logs" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}
```

---

## 모니터링 아키텍처

### CloudWatch Dashboard

**주요 메트릭**:
| 메트릭 | 설명 | 임계치 |
|-------|------|--------|
| **CPU Utilization** | EC2 CPU 사용률 | 70% (Scale Up) |
| **Memory Usage** | EC2 메모리 사용률 | 80% (경고) |
| **Request Count** | ALB 요청 수 | 5000/min (Scale Up) |
| **Target Response Time** | 평균 응답 시간 | 100ms (경고) |
| **Unhealthy Hosts** | Unhealthy 인스턴스 수 | 0 (정상) |
| **DB Connections** | RDS 연결 수 | 400 (경고) |
| **Cache Hit Rate** | Redis 캐시 히트율 | >90% (목표) |

### CloudWatch Alarms

**Critical Alarms** (즉시 대응):
- ALB Unhealthy Hosts > 0
- RDS CPU > 90%
- Redis Memory > 90%

**Warning Alarms** (모니터링):
- EC2 CPU > 70%
- Response Time > 100ms
- DB Connections > 400

---

## 확장성 설계

### 수평 확장 전략

**현재 (Phase 1)**:
- EC2: 2-20대
- RDS: Writer 1대 + Reader 1대
- Redis: Primary 1대 + Replica 1대

**미래 (Phase 2)**:
- EC2: 2-50대 (리전 확장)
- RDS: Writer 1대 + Reader 3대
- Redis: Cluster Mode (3 Shards × 2 Replicas)

### 데이터베이스 샤딩 (향후)

**샤딩 키**: `event_date`

```
Shard 1: events WHERE event_date < '2024-06-01'
Shard 2: events WHERE event_date BETWEEN '2024-06-01' AND '2024-12-31'
Shard 3: events WHERE event_date >= '2025-01-01'
```

### 글로벌 확장 (향후)

**멀티 리전 배포**:
```
Primary Region: ap-northeast-2 (Seoul)
Replica Region: us-east-1 (Virginia)

Route 53 Geo-Routing:
- 한국/일본 → Seoul Region
- 미국/유럽 → Virginia Region
```

---

**작성일**: 2024-11-03  
**버전**: 1.0.0  
**작성자**: rlagudfo1223@gmail.com
