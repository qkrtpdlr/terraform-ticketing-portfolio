# 🚀 배포 가이드

티켓팅 시스템 인프라를 AWS에 배포하는 상세 가이드입니다.

---

## 📋 목차

1. [전제 조건](#전제-조건)
2. [로컬 환경 설정](#로컬-환경-설정)
3. [AWS 인증 설정](#aws-인증-설정)
4. [Terraform 인프라 배포](#terraform-인프라-배포)
5. [애플리케이션 배포](#애플리케이션-배포)
6. [검증 및 테스트](#검증-및-테스트)
7. [모니터링 설정](#모니터링-설정)
8. [트러블슈팅](#트러블슈팅)
9. [리소스 정리](#리소스-정리)

---

## 전제 조건

### 필수 소프트웨어

| 소프트웨어 | 버전 | 다운로드 |
|-----------|------|----------|
| **Terraform** | 1.6+ | https://www.terraform.io/downloads |
| **AWS CLI** | 2.x | https://aws.amazon.com/cli/ |
| **Docker** | 24.x | https://www.docker.com/get-started |
| **Git** | 2.x+ | https://git-scm.com/downloads |
| **MySQL Client** | 8.0+ | https://dev.mysql.com/downloads/ |

### AWS 계정 권한

필요한 IAM 권한:
- EC2: Full Access
- RDS: Full Access
- ElastiCache: Full Access
- VPC: Full Access
- CloudWatch: Full Access
- IAM: Role 생성 권한
- ECR: Full Access

### 예상 비용

| 환경 | 월간 비용 | 용도 |
|------|----------|------|
| **개발** | $150 | 테스트, 학습 |
| **스테이징** | $250 | Pre-production |
| **프로덕션** | $442 | 실제 서비스 |

---

## 로컬 환경 설정

### 1. 저장소 클론

```bash
# 저장소 클론
git clone https://github.com/qkrtpdlr/terraform-ticketing-portfolio.git
cd terraform-ticketing-portfolio

# 브랜치 확인
git branch -a

# 최신 버전으로 업데이트
git pull origin main
```

### 2. 디렉토리 구조 확인

```bash
tree -L 2

# 예상 출력:
# .
# ├── terraform/
# │   ├── main.tf
# │   ├── variables.tf
# │   ├── outputs.tf
# │   └── modules/
# ├── ticketing-app/
# │   ├── src/
# │   ├── build.gradle
# │   └── Dockerfile
# ├── docs/
# └── README.md
```

### 3. 필수 소프트웨어 설치 확인

```bash
# Terraform 버전 확인
terraform version
# Terraform v1.6.0 이상이어야 함

# AWS CLI 버전 확인
aws --version
# aws-cli/2.x.x 이상이어야 함

# Docker 버전 확인
docker --version
# Docker version 24.x.x 이상이어야 함

# Git 버전 확인
git --version
# git version 2.x.x 이상이어야 함
```

---

## AWS 인증 설정

### 1. IAM 사용자 생성 (처음인 경우)

```bash
# AWS Console 접속
# https://console.aws.amazon.com/iam/

# IAM > Users > Create user
# - User name: terraform-user
# - Access type: Programmatic access
# - Permissions: AdministratorAccess (또는 커스텀 정책)
# - Access Key ID와 Secret Access Key 저장
```

### 2. AWS CLI 인증 설정

```bash
# AWS 인증 정보 설정
aws configure

# 입력 예시:
# AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
# AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
# Default region name [None]: ap-northeast-2
# Default output format [None]: json
```

### 3. 인증 확인

```bash
# 현재 인증 정보 확인
aws sts get-caller-identity

# 예상 출력:
# {
#     "UserId": "AIDAXXXXXXXXXXXXXXXXX",
#     "Account": "123456789012",
#     "Arn": "arn:aws:iam::123456789012:user/terraform-user"
# }

# 리전 확인
aws configure get region
# ap-northeast-2
```

### 4. 프로파일 설정 (여러 계정 사용 시)

```bash
# 프로파일 추가
aws configure --profile terraform-dev

# 프로파일 사용
export AWS_PROFILE=terraform-dev

# 또는 Terraform 실행 시
terraform apply -var="aws_profile=terraform-dev"
```

---

## Terraform 인프라 배포

### 1. Terraform 디렉토리로 이동

```bash
cd terraform
```

### 2. 변수 파일 설정

```bash
# 변수 파일 복사
cp terraform.tfvars.example terraform.tfvars

# 변수 파일 편집
vim terraform.tfvars
```

**terraform.tfvars 예시**:
```hcl
# 프로젝트 기본 설정
project_name = "ticketing"
environment  = "dev"
region       = "ap-northeast-2"

# VPC 설정
vpc_cidr            = "10.0.0.0/16"
availability_zones  = ["ap-northeast-2a", "ap-northeast-2c"]

# RDS 설정
db_username         = "admin"
db_password         = "ChangeThisPassword123!"  # ⚠️ 반드시 변경!
db_name             = "ticketing"
db_instance_class   = "db.t3.medium"

# ElastiCache 설정
redis_node_type     = "cache.t3.micro"

# EC2 Auto Scaling 설정
instance_type       = "t3.medium"
min_size            = 2
max_size            = 10
desired_size        = 2

# 태그 설정
tags = {
  Project     = "Ticketing"
  Environment = "Dev"
  ManagedBy   = "Terraform"
  Owner       = "rlagudfo1223@gmail.com"
}
```

### 3. Terraform 초기화

```bash
# 플러그인 다운로드 및 백엔드 초기화
terraform init

# 예상 출력:
# Initializing the backend...
# Initializing provider plugins...
# - Finding hashicorp/aws versions matching "~> 5.0"...
# - Installing hashicorp/aws v5.30.0...
# 
# Terraform has been successfully initialized!
```

### 4. 실행 계획 확인

```bash
# 실행 계획 확인 (dry-run)
terraform plan

# 예상 출력 요약:
# Plan: 45 to add, 0 to change, 0 to destroy.
# 
# Changes to Outputs:
#   + alb_dns_name         = (known after apply)
#   + ecr_repository_url   = (known after apply)
#   + rds_endpoint         = (known after apply)
#   + redis_endpoint       = (known after apply)

# 계획을 파일로 저장 (권장)
terraform plan -out=tfplan
```

### 5. 인프라 배포 (15-20분 소요)

```bash
# 인프라 배포 시작
terraform apply

# 또는 저장된 계획 사용
terraform apply tfplan

# 확인 프롬프트
# Do you want to perform these actions?
#   Terraform will perform the actions described above.
#   Only 'yes' will be accepted to approve.
# 
#   Enter a value: yes

# 배포 진행 상황
# module.vpc.aws_vpc.main: Creating...
# module.vpc.aws_vpc.main: Creation complete after 3s
# module.vpc.aws_subnet.public[0]: Creating...
# module.vpc.aws_subnet.public[0]: Creation complete after 2s
# ...
# module.database.aws_rds_cluster.main: Still creating... [10m0s elapsed]
# module.database.aws_rds_cluster.main: Still creating... [15m0s elapsed]
# module.database.aws_rds_cluster.main: Creation complete after 18m23s
```

### 6. 출력값 확인 및 저장

```bash
# 모든 출력값 확인
terraform output

# 예상 출력:
# alb_dns_name = "ticketing-alb-123456789.ap-northeast-2.elb.amazonaws.com"
# ecr_repository_url = "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/ticketing-app"
# rds_endpoint = "ticketing-aurora-cluster.cluster-abc123.ap-northeast-2.rds.amazonaws.com"
# redis_endpoint = "ticketing-redis.abc123.ng.0001.apne2.cache.amazonaws.com"

# 특정 출력값만 확인
terraform output alb_dns_name
terraform output -raw alb_dns_name  # 따옴표 없이 출력

# 환경 변수로 저장
export ALB_DNS=$(terraform output -raw alb_dns_name)
export RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
export REDIS_ENDPOINT=$(terraform output -raw redis_endpoint)
export ECR_URL=$(terraform output -raw ecr_repository_url)

# 저장된 변수 확인
echo "ALB: $ALB_DNS"
echo "RDS: $RDS_ENDPOINT"
echo "Redis: $REDIS_ENDPOINT"
echo "ECR: $ECR_URL"
```

### 7. 배포 상태 확인

```bash
# Terraform state 확인
terraform show

# 특정 리소스 상태 확인
terraform state list
terraform state show module.vpc.aws_vpc.main

# 리소스 그래프 생성
terraform graph | dot -Tpng > graph.png
```

---

## 애플리케이션 배포

### 1. 애플리케이션 디렉토리로 이동

```bash
cd ../ticketing-app
```

### 2. application.yml 설정

```bash
# application.yml 편집
vim src/main/resources/application.yml
```

**application.yml 예시**:
```yaml
spring:
  application:
    name: ticketing-app
  
  profiles:
    active: prod
  
  datasource:
    url: jdbc:mysql://${RDS_ENDPOINT}:3306/ticketing?useSSL=true&serverTimezone=Asia/Seoul
    username: admin
    password: ${DB_PASSWORD}  # 환경 변수 또는 Secrets Manager에서 가져오기
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: TicketingHikariPool
      connection-test-query: SELECT 1
  
  redis:
    host: ${REDIS_ENDPOINT}
    port: 6379
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
  
  jpa:
    hibernate:
      ddl-auto: update  # prod에서는 validate 권장
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        show_sql: false
        use_sql_comments: false
    open-in-view: false
  
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5분
      cache-null-values: false

server:
  port: 8080
  shutdown: graceful
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    max-connections: 10000

logging:
  level:
    root: INFO
    com.ticketing: DEBUG
    org.springframework.cache: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/ticketing/application.log
    max-size: 10MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 3. Docker 이미지 빌드

```bash
# Dockerfile 확인
cat Dockerfile

# Docker 이미지 빌드
docker build -t ticketing-app:latest .

# 빌드 진행 (약 3-5분 소요)
# [+] Building 245.2s (14/14) FINISHED
# => [internal] load build definition
# => => transferring dockerfile: 456B
# => [internal] load .dockerignore
# => [1/8] FROM docker.io/library/openjdk:17-jdk-slim
# => [2/8] WORKDIR /app
# => [3/8] COPY build.gradle settings.gradle ./
# => [4/8] RUN ./gradlew dependencies
# => [5/8] COPY src ./src
# => [6/8] RUN ./gradlew build
# => [7/8] COPY build/libs/*.jar app.jar
# => exporting to image
# => => naming to docker.io/library/ticketing-app:latest

# 빌드된 이미지 확인
docker images | grep ticketing-app
# ticketing-app   latest   abc123def456   2 minutes ago   387MB

# 로컬에서 테스트 (선택사항)
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://${RDS_ENDPOINT}:3306/ticketing" \
  -e SPRING_REDIS_HOST="${REDIS_ENDPOINT}" \
  ticketing-app:latest
```

### 4. ECR에 이미지 푸시

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin $ECR_URL

# 로그인 성공 출력:
# WARNING! Your password will be stored unencrypted in /home/user/.docker/config.json.
# Configure a credential helper to remove this warning. See
# https://docs.docker.com/engine/reference/commandline/login/#credentials-store
# 
# Login Succeeded

# 이미지 태깅
docker tag ticketing-app:latest $ECR_URL:latest
docker tag ticketing-app:latest $ECR_URL:v1.0.0
docker tag ticketing-app:latest $ECR_URL:$(git rev-parse --short HEAD)

# 태그 확인
docker images | grep ticketing-app

# ECR에 푸시
docker push $ECR_URL:latest
docker push $ECR_URL:v1.0.0

# 푸시 진행 상황:
# The push refers to repository [123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/ticketing-app]
# abc123def456: Pushed
# def456ghi789: Pushed
# ghi789jkl012: Pushed
# ...
# latest: digest: sha256:abc123def456... size: 2841
# v1.0.0: digest: sha256:abc123def456... size: 2841

# ECR에서 이미지 확인
aws ecr describe-images \
  --repository-name ticketing-app \
  --region ap-northeast-2

# 예상 출력:
# {
#   "imageDetails": [
#     {
#       "imageTags": ["latest", "v1.0.0"],
#       "imageSizeInBytes": 387654321,
#       "imagePushedAt": "2024-11-03T12:00:00+00:00"
#     }
#   ]
# }
```

### 5. Auto Scaling Group 인스턴스 새로고침

```bash
# Auto Scaling Group 이름 확인
aws autoscaling describe-auto-scaling-groups \
  --query 'AutoScalingGroups[?contains(AutoScalingGroupName, `ticketing`)].AutoScalingGroupName' \
  --output text

# 인스턴스 새로고침 시작
aws autoscaling start-instance-refresh \
  --auto-scaling-group-name ticketing-asg \
  --preferences MinHealthyPercentage=50,InstanceWarmup=300

# 예상 출력:
# {
#   "InstanceRefreshId": "abc123-def4-5678-9012-abc123def456"
# }

# 새로고침 상태 확인
aws autoscaling describe-instance-refreshes \
  --auto-scaling-group-name ticketing-asg

# 상태 확인 반복 (완료까지 약 5-10분)
watch -n 30 'aws autoscaling describe-instance-refreshes \
  --auto-scaling-group-name ticketing-asg \
  --query "InstanceRefreshes[0].[Status,PercentageComplete]" \
  --output text'

# 완료 시 출력:
# Successful    100
```

### 6. 배포 확인

```bash
# Auto Scaling Group 인스턴스 확인
aws autoscaling describe-auto-scaling-groups \
  --auto-scaling-group-names ticketing-asg \
  --query 'AutoScalingGroups[0].Instances[*].[InstanceId,HealthStatus,LifecycleState]' \
  --output table

# 예상 출력:
# -----------------------------------------------
# |        DescribeAutoScalingGroups           |
# +----------------+----------+-----------------+
# |  i-0abc123456  | Healthy  | InService       |
# |  i-0def789012  | Healthy  | InService       |
# +----------------+----------+-----------------+

# Target Group 상태 확인
ALB_TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
  --names ticketing-tg \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

aws elbv2 describe-target-health \
  --target-group-arn $ALB_TARGET_GROUP_ARN

# 예상 출력:
# {
#   "TargetHealthDescriptions": [
#     {
#       "Target": {"Id": "i-0abc123456", "Port": 8080},
#       "HealthCheckPort": "8080",
#       "TargetHealth": {"State": "healthy"}
#     },
#     {
#       "Target": {"Id": "i-0def789012", "Port": 8080},
#       "HealthCheckPort": "8080",
#       "TargetHealth": {"State": "healthy"}
#     }
#   ]
# }
```

---

## 검증 및 테스트

### 1. Health Check

```bash
# ALB를 통한 Health Check
curl http://$ALB_DNS/api/health

# 예상 출력:
# {
#   "status": "UP",
#   "components": {
#     "db": {
#       "status": "UP",
#       "details": {
#         "database": "MySQL",
#         "validationQuery": "isValid()"
#       }
#     },
#     "redis": {
#       "status": "UP",
#       "details": {
#         "version": "7.0.0"
#       }
#     },
#     "diskSpace": {
#       "status": "UP",
#       "details": {
#         "total": 10737418240,
#         "free": 8589934592,
#         "threshold": 10485760
#       }
#     }
#   }
# }

# jq를 사용한 포맷팅 (jq 설치 필요)
curl -s http://$ALB_DNS/api/health | jq .
```

### 2. API 테스트

#### 2.1 이벤트 생성

```bash
# 이벤트 생성 API 호출
curl -X POST http://$ALB_DNS/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "콘서트 티켓 판매",
    "totalSeats": 10000,
    "eventDate": "2024-12-31T19:00:00"
  }' | jq .

# 예상 출력:
# {
#   "eventId": 1,
#   "eventName": "콘서트 티켓 판매",
#   "totalSeats": 10000,
#   "availableSeats": 10000,
#   "eventDate": "2024-12-31T19:00:00",
#   "createdAt": "2024-11-03T12:00:00",
#   "status": "ACTIVE"
# }
```

#### 2.2 이벤트 조회 (캐시 테스트)

```bash
# 첫 번째 요청 (Cache Miss - DB 조회)
time curl -s http://$ALB_DNS/api/events/1 | jq .

# 실행 시간: 약 200ms (DB 조회)

# 두 번째 요청 (Cache Hit - Redis 조회)
time curl -s http://$ALB_DNS/api/events/1 | jq .

# 실행 시간: 약 10ms (캐시 조회)
# ⚠️ 응답 시간이 95% 감소!
```

#### 2.3 티켓 예매

```bash
# 예매 요청
curl -X POST http://$ALB_DNS/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "userId": "user123",
    "quantity": 2
  }' | jq .

# 예상 출력:
# {
#   "reservationId": 1,
#   "eventId": 1,
#   "userId": "user123",
#   "quantity": 2,
#   "status": "CONFIRMED",
#   "reservedAt": "2024-11-03T12:05:00",
#   "expiresAt": "2024-11-03T12:20:00"
# }
```

#### 2.4 예매 확인

```bash
# 예매 내역 조회
curl http://$ALB_DNS/api/reservations/1 | jq .

# 예상 출력:
# {
#   "reservationId": 1,
#   "eventId": 1,
#   "userId": "user123",
#   "quantity": 2,
#   "status": "CONFIRMED",
#   "reservedAt": "2024-11-03T12:05:00",
#   "event": {
#     "eventId": 1,
#     "eventName": "콘서트 티켓 판매",
#     "availableSeats": 9998
#   }
# }
```

### 3. 데이터베이스 직접 확인

```bash
# RDS에 MySQL 클라이언트로 연결
mysql -h $RDS_ENDPOINT -u admin -p ticketing

# 비밀번호 입력 후
mysql> USE ticketing;

# 테이블 목록 확인
mysql> SHOW TABLES;
# +---------------------+
# | Tables_in_ticketing |
# +---------------------+
# | events              |
# | reservations        |
# | users               |
# +---------------------+

# 이벤트 데이터 확인
mysql> SELECT * FROM events;
# +----+-------------------------+-------------+------------------+---------------------+
# | id | event_name              | total_seats | available_seats  | event_date          |
# +----+-------------------------+-------------+------------------+---------------------+
# |  1 | 콘서트 티켓 판매         |      10000  |            9998  | 2024-12-31 19:00:00 |
# +----+-------------------------+-------------+------------------+---------------------+

# 예매 데이터 확인
mysql> SELECT * FROM reservations LIMIT 5;
# +----+----------+---------+----------+-----------+---------------------+
# | id | event_id | user_id | quantity | status    | reserved_at         |
# +----+----------+---------+----------+-----------+---------------------+
# |  1 |        1 | user123 |        2 | CONFIRMED | 2024-11-03 12:05:00 |
# +----+----------+---------+----------+-----------+---------------------+

mysql> EXIT;
```

### 4. Redis 직접 확인

```bash
# Redis CLI 설치 (없는 경우)
sudo apt-get update
sudo apt-get install redis-tools -y

# Redis 서버 연결
redis-cli -h $REDIS_ENDPOINT

# PING 테스트
127.0.0.1:6379> PING
PONG

# 캐시 키 목록 확인
127.0.0.1:6379> KEYS event:*
1) "event:1"

# 캐시 데이터 확인
127.0.0.1:6379> GET event:1
"{\"eventId\":1,\"eventName\":\"콘서트 티켓 판매\",\"totalSeats\":10000,\"availableSeats\":9998,\"eventDate\":\"2024-12-31T19:00:00\"}"

# TTL 확인
127.0.0.1:6379> TTL event:1
(integer) 298  # 남은 TTL (초)

# Redis 메모리 사용량 확인
127.0.0.1:6379> INFO memory
# used_memory:1234567
# used_memory_human:1.18M
# maxmemory:2147483648
# maxmemory_human:2.00G
# maxmemory_policy:allkeys-lru

127.0.0.1:6379> EXIT
```

### 5. 성능 테스트 (Apache Bench)

```bash
# Apache Bench 설치 (없는 경우)
sudo apt-get install apache2-utils -y

# 간단한 부하 테스트 (100 요청, 10 동시)
ab -n 100 -c 10 http://$ALB_DNS/api/health

# 예상 출력:
# Concurrency Level:      10
# Time taken for tests:   0.523 seconds
# Complete requests:      100
# Failed requests:        0
# Requests per second:    191.20 [#/sec] (mean)
# Time per request:       52.300 [ms] (mean)

# 본격적인 부하 테스트 (10,000 요청, 1,000 동시)
# ⚠️ 주의: 실제 예매 API는 비용 발생 가능
ab -n 10000 -c 1000 \
   -p reservation.json \
   -T "application/json" \
   http://$ALB_DNS/api/events/1

# reservation.json 파일 내용:
# (없어도 GET 요청으로 테스트 가능)
```

---

## 모니터링 설정

### 1. CloudWatch Dashboard 확인

```bash
# CloudWatch Dashboard URL 생성
echo "https://console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#dashboards:name=ticketing-dashboard"

# 브라우저에서 URL 열기
```

### 2. CloudWatch Alarms 확인

```bash
# 모든 Alarms 확인
aws cloudwatch describe-alarms \
  --alarm-name-prefix "ticketing" \
  --query 'MetricAlarms[*].[AlarmName,StateValue,MetricName,Threshold]' \
  --output table

# 예상 출력:
# -------------------------------------------------------
# |                  DescribeAlarms                    |
# +------------------------+----------+--------+--------+
# |  ticketing-high-cpu    | OK       | CPU    |   70   |
# |  ticketing-low-cpu     | OK       | CPU    |   20   |
# |  ticketing-unhealthy   | OK       | Health |    0   |
# +------------------------+----------+--------+--------+
```

### 3. CloudWatch Logs 확인

```bash
# Log Group 확인
aws logs describe-log-groups \
  --log-group-name-prefix "/aws/ticketing"

# 최근 로그 스트림 확인
aws logs describe-log-streams \
  --log-group-name "/aws/ticketing/application" \
  --order-by LastEventTime \
  --descending \
  --max-items 5

# 최근 로그 이벤트 확인
aws logs tail /aws/ticketing/application --follow
```

---

## 트러블슈팅

### 문제 1: Terraform apply 실패

**증상**: `Error creating VPC: VpcLimitExceeded`

**해결**:
```bash
# VPC 한도 확인
aws ec2 describe-account-attributes \
  --attribute-names vpc-max-elastic-ips

# 사용 중인 VPC 확인
aws ec2 describe-vpcs --query 'Vpcs[*].[VpcId,Tags]'

# 불필요한 VPC 삭제 후 재시도
terraform apply
```

### 문제 2: ECR 푸시 실패

**증상**: `denied: Your authorization token has expired`

**해결**:
```bash
# ECR 재로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin $ECR_URL

# 이미지 다시 푸시
docker push $ECR_URL:latest
```

### 문제 3: Target Group Unhealthy

**증상**: ALB Target Group에서 "unhealthy" 상태

**해결**:
```bash
# 1. Security Group 확인
aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=ticketing-ec2-sg"

# 2. EC2 인스턴스 로그 확인
aws ssm start-session --target <INSTANCE_ID>
sudo docker logs ticketing-app

# 3. Health Check 경로 수정
# ALB Target Group Health Check 경로 확인: /api/health
```

---

## 리소스 정리

### 주의사항

⚠️ **경고**: 아래 명령어는 모든 리소스를 삭제합니다. 프로덕션 환경에서는 신중히 실행하세요!

### 1. 애플리케이션 중지 (선택사항)

```bash
# Auto Scaling Group 인스턴스 수 0으로 설정
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name ticketing-asg \
  --min-size 0 \
  --max-size 0 \
  --desired-capacity 0
```

### 2. Terraform Destroy

```bash
# Terraform 디렉토리로 이동
cd terraform

# 삭제 계획 확인
terraform plan -destroy

# 모든 리소스 삭제
terraform destroy

# 확인 프롬프트
# Do you really want to destroy all resources?
#   Terraform will destroy all your managed infrastructure, as shown above.
#   There is no undo. Only 'yes' will be accepted to confirm.
# 
#   Enter a value: yes

# 삭제 진행 (약 10-15분 소요)
# module.compute.aws_autoscaling_group.main: Destroying...
# module.loadbalancer.aws_lb.main: Destroying...
# module.database.aws_rds_cluster.main: Destroying...
# module.cache.aws_elasticache_replication_group.main: Destroying...
# ...
# Destroy complete! Resources: 45 destroyed.
```

### 3. ECR 이미지 삭제 (수동)

```bash
# ECR 이미지 삭제
aws ecr batch-delete-image \
  --repository-name ticketing-app \
  --image-ids imageTag=latest imageTag=v1.0.0

# ECR Repository 삭제
aws ecr delete-repository \
  --repository-name ticketing-app \
  --force
```

### 4. CloudWatch Logs 삭제 (선택사항)

```bash
# Log Group 삭제
aws logs delete-log-group \
  --log-group-name /aws/ticketing/application
```

### 5. 삭제 확인

```bash
# VPC 확인 (ticketing VPC가 없어야 함)
aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ticketing-vpc"

# RDS 확인
aws rds describe-db-clusters \
  --db-cluster-identifier ticketing-aurora-cluster

# 예상 출력: DBClusterNotFoundFault (정상)

# ElastiCache 확인
aws elasticache describe-replication-groups \
  --replication-group-id ticketing-redis

# 예상 출력: ReplicationGroupNotFoundFault (정상)
```

---

## 추가 리소스

- [Terraform AWS Provider 문서](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Spring Boot 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Redis 모범 사례](https://redis.io/docs/management/optimization/)

---

**작성일**: 2024-11-03  
**버전**: 1.0.0  
**작성자**: rlagudfo1223@gmail.com
