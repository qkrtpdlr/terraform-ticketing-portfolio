output "redis_endpoint" {
  value = aws_elasticache_replication_group.main.primary_endpoint_address
}
output "redis_cluster_id" {
  value = aws_elasticache_replication_group.main.id
}
