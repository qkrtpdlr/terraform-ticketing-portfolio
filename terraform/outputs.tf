output "alb_dns_name" {
  description = "Application Load Balancer DNS 이름"
  value       = module.compute.alb_dns_name
}

output "db_endpoint" {
  description = "RDS Aurora Cluster 엔드포인트"
  value       = module.database.db_endpoint
}

output "db_reader_endpoint" {
  description = "RDS Aurora Reader 엔드포인트"
  value       = module.database.db_reader_endpoint
}

output "redis_endpoint" {
  description = "ElastiCache Redis 엔드포인트"
  value       = module.cache.redis_endpoint
}

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "autoscaling_group_name" {
  description = "Auto Scaling Group 이름"
  value       = module.compute.autoscaling_group_name
}
