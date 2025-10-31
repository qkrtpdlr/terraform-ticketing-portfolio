terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# VPC 모듈
module "vpc" {
  source = "./modules/vpc"

  project_name = var.project_name
  environment  = var.environment
  vpc_cidr     = var.vpc_cidr
  azs          = var.azs
}

# Security Groups 모듈
module "security" {
  source = "./modules/security"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
}

# RDS Aurora 모듈
module "database" {
  source = "./modules/database"

  project_name        = var.project_name
  environment         = var.environment
  vpc_id              = module.vpc.vpc_id
  db_subnet_ids       = module.vpc.database_subnet_ids
  db_security_group_id = module.security.db_security_group_id
  db_master_username  = var.db_master_username
  db_master_password  = var.db_master_password
}

# ElastiCache Redis 모듈
module "cache" {
  source = "./modules/cache"

  project_name           = var.project_name
  environment            = var.environment
  vpc_id                 = module.vpc.vpc_id
  cache_subnet_ids       = module.vpc.database_subnet_ids
  redis_security_group_id = module.security.redis_security_group_id
}

# EC2 Auto Scaling 모듈
module "compute" {
  source = "./modules/compute"

  project_name         = var.project_name
  environment          = var.environment
  vpc_id               = module.vpc.vpc_id
  public_subnet_ids    = module.vpc.public_subnet_ids
  private_subnet_ids   = module.vpc.private_subnet_ids
  alb_security_group_id = module.security.alb_security_group_id
  ec2_security_group_id = module.security.ec2_security_group_id
  
  # Application config
  db_host        = module.database.db_endpoint
  redis_host     = module.cache.redis_endpoint
  
  # Auto Scaling config
  min_size       = var.min_size
  max_size       = var.max_size
  desired_size   = var.desired_size
}

# CloudWatch Monitoring 모듈
module "monitoring" {
  source = "./modules/monitoring"

  project_name       = var.project_name
  environment        = var.environment
  alarm_email        = var.alarm_email
  autoscaling_group_name = module.compute.autoscaling_group_name
  alb_arn_suffix     = module.compute.alb_arn_suffix
  target_group_arn_suffix = module.compute.target_group_arn_suffix
  db_cluster_id      = module.database.db_cluster_id
  redis_cluster_id   = module.cache.redis_cluster_id
}
