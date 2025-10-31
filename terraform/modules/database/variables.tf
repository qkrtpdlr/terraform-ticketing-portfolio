variable "project_name" { type = string }
variable "environment" { type = string }
variable "vpc_id" { type = string }
variable "db_subnet_ids" { type = list(string) }
variable "db_security_group_id" { type = string }
variable "db_master_username" { type = string }
variable "db_master_password" { type = string }
