# 배포 가이드

## 사전 요구사항
- AWS CLI 설치 및 인증
- Terraform 1.6 이상
- Docker 24.x
- Java 17 & Maven 3.9

## 배포 단계

### 1. Terraform 인프라 배포
\`\`\`bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
vim terraform.tfvars  # 설정 수정

terraform init
terraform plan
terraform apply
\`\`\`

### 2. Spring Boot 빌드
\`\`\`bash
cd ticketing-app
mvn clean package
\`\`\`

### 3. Docker 이미지 빌드 및 배포
\`\`\`bash
docker build -t ticketing-app:latest .
# ECR에 푸시 (Terraform output의 ECR URL 사용)
\`\`\`

### 4. 동작 확인
\`\`\`bash
ALB_DNS=$(terraform output -raw alb_dns_name)
curl http://$ALB_DNS/api/health
\`\`\`

자세한 내용은 메인 README.md 참고
