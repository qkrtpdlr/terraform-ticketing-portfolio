output "db_endpoint" {
  value = aws_rds_cluster.main.endpoint
}
output "db_reader_endpoint" {
  value = aws_rds_cluster.main.reader_endpoint
}
output "db_cluster_id" {
  value = aws_rds_cluster.main.id
}
