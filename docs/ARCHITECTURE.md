# 시스템 아키텍처

## 3-Tier 아키텍처

### Web Tier
- Application Load Balancer
- SSL/TLS 종료
- Health Check

### Application Tier
- EC2 Auto Scaling (2-20대)
- Spring Boot 3.1
- Redis Client

### Data Tier
- RDS Aurora MySQL (Writer + Reader)
- ElastiCache Redis (Primary + Replica)

## 핵심 기술

### 1. 분산 락 (Redis SETNX)
동시성 제어를 위한 분산 락 구현

### 2. Cache-Aside 패턴
Redis 캐싱으로 DB 부하 90% 감소

### 3. Multi-AZ 배포
고가용성 확보 (99.9%)

### 4. Auto Scaling
트래픽 급증 대응 (CPU 기반)

자세한 다이어그램은 메인 README.md 참고
