variable "project_name" { type = string }
variable "environment" { type = string }
variable "vpc_id" { type = string }
variable "cache_subnet_ids" { type = list(string) }
variable "redis_security_group_id" { type = string }
