output "ecr_api_repository_url" {
  value       = aws_ecr_repository.api.repository_url
  description = "ECR repository URL for the API image."
}

output "ecr_worker_repository_url" {
  value       = aws_ecr_repository.worker.repository_url
  description = "ECR repository URL for the Worker image."
}

output "ecs_cluster_name" {
  value       = aws_ecs_cluster.this.name
  description = "ECS cluster name."
}

output "log_group_api" {
  value       = aws_cloudwatch_log_group.api.name
  description = "CloudWatch log group name for API."
}

output "log_group_worker" {
  value       = aws_cloudwatch_log_group.worker.name
  description = "CloudWatch log group name for Worker."
}

output "vpc_id" {
  value       = aws_vpc.main.id
  description = "VPC ID for V3."
}

output "public_subnet_ids" {
  value       = [for s in aws_subnet.public : s.id]
  description = "Public subnet IDs for V3."
}

output "public_route_table_id" {
  value       = aws_route_table.public.id
  description = "Public route table ID."
}

output "alb_dns_name" {
  value       = aws_lb.api.dns_name
  description = "Public DNS name of the Application Load Balancer."
}

output "alb_security_group_id" {
  value       = aws_security_group.alb.id
  description = "Security group ID for the ALB."
}

output "api_security_group_id" {
  value       = aws_security_group.api.id
  description = "Security group ID for API tasks."
}

output "api_target_group_arn" {
  value       = aws_lb_target_group.api.arn
  description = "Target group ARN for the API."
}

output "private_subnet_ids" {
  value       = [for s in aws_subnet.private : s.id]
  description = "Private subnet IDs for V3."
}

output "rds_endpoint" {
  value       = aws_db_instance.postgres.address
  description = "RDS endpoint address."
}

output "rds_port" {
  value       = aws_db_instance.postgres.port
  description = "RDS port."
}

output "rds_db_name" {
  value       = aws_db_instance.postgres.db_name
  description = "Database name."
}