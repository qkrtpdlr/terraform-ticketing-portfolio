variable "project_name" {
  description = "프로젝트 이름"
  type        = string
  default     = "ticketing"
}

variable "environment" {
  description = "환경 (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "가용 영역 목록"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "db_master_username" {
  description = "RDS 마스터 사용자 이름"
  type        = string
  default     = "admin"
  sensitive   = true
}

variable "db_master_password" {
  description = "RDS 마스터 비밀번호"
  type        = string
  sensitive   = true
}

variable "min_size" {
  description = "Auto Scaling 최소 인스턴스 수"
  type        = number
  default     = 2
}

variable "max_size" {
  description = "Auto Scaling 최대 인스턴스 수"
  type        = number
  default     = 20
}

variable "desired_size" {
  description = "Auto Scaling 희망 인스턴스 수"
  type        = number
  default     = 2
}

variable "alarm_email" {
  description = "알람 수신 이메일"
  type        = string
}

variable "tags" {
  description = "공통 태그"
  type        = map(string)
  default     = {}
}
