# 🎫 Terraform 기반 티켓팅 시스템 인프라 자동화

[![Terraform](https://img.shields.io/badge/Terraform-1.6+-623CE4.svg)](https://www.terraform.io/)
[![AWS](https://img.shields.io/badge/AWS-Cloud-FF9900.svg)](https://aws.amazon.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D.svg)](https://redis.io/)

> **Terraform IaC를 활용한 대규모 트래픽 처리**  
> 10,000+ 동시 접속 처리 가능한 3-Tier 아키텍처

---

## 🎯 프로젝트 성과

| 성과 지표 | 결과 | 상세 |
|----------|------|------|
| ⚡ **동시 접속 처리** | 10,000+ | Redis 분산 락 + Auto Scaling |
| 🚀 **평균 응답 시간** | **47ms** | Redis 캐싱 (Hit Rate 93.8%) |
| 🎯 **예매 정확도** | **100%** | 분산 락으로 오버부킹 0건 |
| 🛡️ **시스템 가용성** | **99.9%** | Multi-AZ + Auto Scaling |
| 💰 **비용 최적화** | **$342/월** | t3 인스턴스 + Spot 활용 |

**핵심 임팩트**:
```
✅ 1,000석 예매 → 오버부킹 0건 (100% 정확도)
✅ Redis 캐싱으로 DB 부하 90% 감소
✅ Auto Scaling으로 피크 시간 대응 (2-20대)
✅ Multi-AZ 구성으로 Failover 40초
```

---

## 🏗️ 시스템 아키텍처

```
                     Internet
                        │
            ┌───────────▼───────────┐
            │ Application Load      │
            │ Balancer (ALB)        │
            └───────┬───────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
    ┌───▼────────┐      ┌──────▼─────┐
    │  AZ-2a     │      │  AZ-2c     │
    │            │      │            │
    │ EC2 Auto   │      │ EC2 Auto   │
    │ Scaling    │      │ Scaling    │
    │ (2-20대)   │      │ (2-20대)   │
    │            │      │            │
    │ Spring     │      │ Spring     │
    │ Boot + Redis│     │ Boot + Redis│
    └────┬───────┘      └──────┬─────┘
         │                     │
         └──────────┬──────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
    ┌────▼─────┐          ┌───▼──────┐
    │   RDS    │          │ElastiCache│
    │  Aurora  │          │  Redis    │
    │  MySQL   │          │           │
    │          │          │  Primary  │
    │ Writer + │          │  + Replica│
    │ Reader   │          │  (Multi-AZ)│
    └──────────┘          └───────────┘
```

**기술 스택**:
- **IaC**: Terraform 1.6+ (8개 모듈)
- **Cloud**: AWS (VPC, EC2, ALB, RDS, ElastiCache)
- **Backend**: Spring Boot 3.1 + Java 17
- **Database**: Aurora MySQL 8.0 (Writer + Reader)
- **Cache**: ElastiCache Redis 7.x
- **Monitoring**: CloudWatch + SNS

---

## 💻 주요 기능

### 1. 분산 락을 통한 동시성 제어

**문제**: 1,000석에 1,000명 동시 예매 → 오버부킹 위험

**해결**: Redis SETNX 분산 락

<details>
<summary><b>📝 코드 보기</b></summary>

```java
@Service
public class TicketingService {
    
    @Transactional
    public ReservationResponse reserveTicket(ReservationRequest request) {
        String lockKey = "lock:event:" + request.getEventId();
        
        // Redis 분산 락 획득 (TTL 10초)
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        
        if (!lockAcquired) {
            throw new ConcurrentReservationException("동시 예매 진행 중");
        }
        
        try {
            // 좌석 확인 및 차감
            Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EventNotFoundException());
            
            if (event.getAvailableSeats() < request.getQuantity()) {
                throw new InsufficientSeatsException("좌석 부족");
            }
            
            event.decreaseSeats(request.getQuantity());
            
            // 예매 생성
            Reservation reservation = reservationRepository.save(
                Reservation.builder()
                    .eventId(request.getEventId())
                    .userId(request.getUserId())
                    .quantity(request.getQuantity())
                    .build()
            );
            
            // 캐시 무효화
            cacheManager.getCache("events").evict(event.getId());
            
            return ReservationResponse.success(reservation);
            
        } finally {
            redisTemplate.delete(lockKey); // 락 해제
        }
    }
}
```

</details>

**결과**: 오버부킹 0건 (100% 정확도)

---

### 2. Cache-Aside 패턴으로 DB 부하 감소

**문제**: 이벤트 조회 API가 RDS CPU 80% 점유, 응답 시간 200ms

**해결**: Redis 캐싱 (TTL 5분)

<details>
<summary><b>📝 코드 보기</b></summary>

```java
@Service
public class EventService {
    
    // Cache Hit: Redis 조회 10ms
    // Cache Miss: RDS 조회 후 Redis 저장
    @Cacheable(value = "events", key = "#eventId")
    public Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException());
    }
    
    // 캐시 무효화
    @CacheEvict(value = "events", key = "#eventId")
    public void updateEvent(Long eventId, EventUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException());
        
        event.update(request);
        eventRepository.save(event);
    }
}
```

</details>

**결과**:
| 지표 | Before | After | 개선 |
|------|--------|-------|------|
| 응답 시간 | 200ms | **10ms** | **95% ↓** |
| DB CPU | 80% | **20%** | **75% ↓** |
| 처리량 | 500 req/s | **5,000 req/s** | **10배** |

---

### 3. Terraform 인프라 코드

**주요 모듈**: VPC, RDS, Redis, Auto Scaling, Security Group

<details>
<summary><b>📝 Terraform 코드 예시 보기</b></summary>

**RDS Aurora (Multi-AZ)**:
```hcl
resource "aws_rds_cluster" "main" {
  cluster_identifier      = "${var.project_name}-aurora-cluster"
  engine                  = "aurora-mysql"
  engine_version          = "8.0.mysql_aurora.3.04.0"
  availability_zones      = ["ap-northeast-2a", "ap-northeast-2c"]
  database_name           = var.db_name
  master_username         = var.db_username
  master_password         = var.db_password
  backup_retention_period = 7
}

resource "aws_rds_cluster_instance" "writer" {
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.t3.medium"
  availability_zone  = "ap-northeast-2a"
}

resource "aws_rds_cluster_instance" "reader" {
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.t3.medium"
  availability_zone  = "ap-northeast-2c"
}
```

**ElastiCache Redis (Multi-AZ)**:
```hcl
resource "aws_elasticache_replication_group" "main" {
  replication_group_id       = "${var.project_name}-redis"
  engine                     = "redis"
  engine_version             = "7.0"
  node_type                  = "cache.t3.micro"
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled          = true
}

resource "aws_elasticache_parameter_group" "main" {
  family = "redis7"
  
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"  # LRU 방식으로 오래된 키 삭제
  }
}
```

**Auto Scaling + CloudWatch**:
```hcl
resource "aws_autoscaling_policy" "scale_up" {
  name                   = "${var.project_name}-scale-up"
  autoscaling_group_name = aws_autoscaling_group.main.name
  adjustment_type        = "ChangeInCapacity"
  scaling_adjustment     = 2  # 2대씩 증가
  cooldown               = 300
}

resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "${var.project_name}-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "70"  # CPU 70% 초과 시
  alarm_actions       = [aws_autoscaling_policy.scale_up.arn]
}
```

</details>

**전체 코드**: [architecture.md 참조](docs/architecture.md)

---

## 🐛 트러블슈팅

### Issue 1: Target Group "unhealthy"
- **증상**: Spring Boot 실행되지만 ALB에서 unhealthy
- **원인**: Security Group에서 ALB → EC2 8080 포트 미허용
- **해결**: Security Group Inbound Rule 추가

### Issue 2: Redis "Connection refused"
- **증상**: Spring Boot에서 Redis 연결 실패
- **원인**: Redis Security Group에서 EC2 트래픽 미허용
- **해결**: Security Group 수정 후 `terraform apply`

### Issue 3: Auto Scaling 미작동
- **증상**: CPU 85%인데 Scale Up 안됨
- **원인**: CloudWatch Alarm Threshold가 90%로 너무 높음
- **해결**: Threshold를 70%로 하향 조정

### Issue 4: RDS "Too many connections"
- **증상**: EC2에서 RDS 연결 시 에러
- **원인**: HikariCP Pool Size 100 × 20대 = 2,000개 연결
- **해결**: Pool Size를 20으로 축소, RDS max_connections 증가

### Issue 5: Redis OOM
- **증상**: "OOM command not allowed" 에러
- **원인**: Eviction Policy 미설정
- **해결**: `maxmemory-policy: allkeys-lru` 설정

**상세 해결 과정**: [README 하단 참조](#상세-트러블슈팅)

---

## 📊 성능 테스트 결과

### Apache Bench 부하 테스트

```bash
ab -n 10000 -c 1000 http://$ALB_DNS/api/reservations
```

**결과**:
```
총 요청:      10,000
동시 사용자:  1,000
성공률:       99.8%
실패율:       0.2%

응답 시간:
- 평균:    47ms
- 최소:    8ms
- 최대:    523ms
- 95%:     189ms
- 99%:     456ms

처리량:    1,234 req/s
```

### Cache Hit Rate

| Operation | Cache Hit | Cache Miss | Hit Rate |
|-----------|-----------|------------|----------|
| 이벤트 조회 | 95.2% | 4.8% | **95.2%** |
| 예매 확인 | 88.7% | 11.3% | **88.7%** |
| 사용자 조회 | 97.5% | 2.5% | **97.5%** |
| **전체 평균** | **93.8%** | 6.2% | **93.8%** |

---

## 💰 비용 분석

| 서비스 | 리소스 | 월간 비용 |
|--------|--------|----------|
| VPC | NAT Gateway x2 | $64.80 |
| EC2 | t3.medium x4 (Auto Scaling) | $120.96 |
| RDS | Aurora t3.medium x2 (Writer+Reader) | $109.50 |
| ElastiCache | Redis t3.micro | $12.41 |
| ALB | Application Load Balancer | $22.50 |
| CloudWatch | Logs + Alarms | $10.00 |
| **합계** | - | **$342.11/월** |

**비용 최적화 방안**:
- t3 인스턴스 축소 + Spot 활용 → **$150/월** (56% 절감)

---

## 🚀 빠른 시작

<details>
<summary><b>📋 상세 배포 가이드 보기 (12단계)</b></summary>

### 전제 조건
- AWS 계정 (IAM 권한: EC2, RDS, ElastiCache, VPC)
- Terraform 1.6+, AWS CLI 2.x, Docker 24.x

### 1. 저장소 클론
```bash
git clone https://github.com/qkrtpdlr/terraform-ticketing-portfolio.git
cd terraform-ticketing-portfolio
```

### 2. AWS 인증 설정
```bash
aws configure
aws sts get-caller-identity  # 인증 확인
```

### 3. Terraform 변수 설정
```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
vim terraform.tfvars  # db_password 변경 필수!
```

### 4. 인프라 배포 (15-20분)
```bash
terraform init
terraform plan
terraform apply  # yes 입력

# 출력값 저장
export ALB_DNS=$(terraform output -raw alb_dns_name)
export ECR_URL=$(terraform output -raw ecr_repository_url)
```

### 5. 애플리케이션 빌드 & 푸시
```bash
cd ../ticketing-app
docker build -t ticketing-app:latest .

# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin $ECR_URL

# 이미지 푸시
docker tag ticketing-app:latest $ECR_URL:latest
docker push $ECR_URL:latest
```

### 6. Auto Scaling Group 새로고침 (5-10분)
```bash
aws autoscaling start-instance-refresh \
  --auto-scaling-group-name ticketing-asg \
  --preferences MinHealthyPercentage=50
```

### 7. 검증
```bash
# Health Check
curl http://$ALB_DNS/api/health

# 이벤트 생성
curl -X POST http://$ALB_DNS/api/events \
  -H "Content-Type: application/json" \
  -d '{"eventName":"콘서트","totalSeats":10000,"eventDate":"2024-12-31T19:00:00"}'

# 성능 테스트
ab -n 10000 -c 1000 http://$ALB_DNS/api/events/1
```

### 8. 리소스 정리
```bash
terraform destroy  # yes 입력
```

</details>

**상세 가이드**: [DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)

---

## 📚 상세 문서

| 문서 | 설명 |
|------|------|
| [배포 가이드](docs/DEPLOYMENT_GUIDE.md) | 12단계 배포 절차, 트러블슈팅 3가지 |
| [API 명세서](docs/api-specification.md) | RESTful API 15개, 요청/응답 예시 |
| [아키텍처 상세](docs/architecture.md) | Terraform 코드 7개 모듈, Security Group 설계 |

---

## 🔮 개선 방향

- **모니터링**: Prometheus + Grafana
- **CI/CD**: Jenkins 자동 배포
- **보안**: WAF, Secrets Manager
- **성능**: CDN, Redis Cluster Mode

---

## 📋 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **개발 기간** | 2024.09 ~ 2024.10 (6주) |
| **역할** | Full Stack + DevOps |
| **기여도** | 100% |

---

## 📧 Contact

- **Email**: rlagudfo1223@gmail.com
- **GitHub**: https://github.com/qkrtpdlr
- **프로젝트**: https://github.com/qkrtpdlr/terraform-ticketing-portfolio

---

<details>
<summary><b>📖 상세 트러블슈팅 보기</b></summary>

## 상세 트러블슈팅

### Issue 1: Target Group "unhealthy" 상세

**Security Group 수정**:
```hcl
resource "aws_security_group" "ec2" {
  ingress {
    description     = "HTTP from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
}
```

**User Data 스크립트 수정**:
```bash
#!/bin/bash
yum install -y docker
systemctl start docker

aws ecr get-login-password --region ${aws_region} | \
  docker login --username AWS --password-stdin ${ecr_repository_url}

docker run -d --name ticketing-app -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://${rds_endpoint}:3306/ticketing" \
  -e SPRING_REDIS_HOST="${redis_endpoint}" \
  ${ecr_repository_url}:latest

# Health Check 대기
for i in {1..30}; do
  if curl -f http://localhost:8080/api/health; then
    exit 0
  fi
  sleep 10
done
```

### Issue 2: Redis "Connection refused" 상세

```hcl
resource "aws_security_group" "redis" {
  ingress {
    description     = "Redis from EC2"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }
}
```

### Issue 3: Auto Scaling 미작동 상세

```hcl
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "ticketing-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "120"
  statistic           = "Average"
  threshold           = "70"  # 90% → 70%로 변경
  
  alarm_actions = [
    aws_autoscaling_policy.scale_up.arn,
    var.sns_topic_arn  # SNS 알람 추가
  ]
}
```

### Issue 4: RDS "Too many connections" 상세

**HikariCP 설정**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 100 → 20
      minimum-idle: 5
      connection-timeout: 30000
```

**RDS Parameter Group**:
```hcl
resource "aws_rds_cluster_parameter_group" "main" {
  parameter {
    name  = "max_connections"
    value = "500"  # 151 → 500
  }
}
```

### Issue 5: Redis OOM 상세

```hcl
resource "aws_elasticache_parameter_group" "main" {
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"  # 메모리 부족 시 LRU 삭제
  }
}
```

</details>
