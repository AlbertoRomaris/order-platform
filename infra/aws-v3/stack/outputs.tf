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