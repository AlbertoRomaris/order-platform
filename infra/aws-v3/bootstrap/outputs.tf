output "state_bucket_name" {
  value       = aws_s3_bucket.tf_state.bucket
  description = "S3 bucket used for Terraform remote state."
}

output "lock_table_name" {
  value       = aws_dynamodb_table.tf_lock.name
  description = "DynamoDB table used for Terraform state locking."
}

output "aws_region" {
  value       = var.aws_region
  description = "AWS region used for bootstrap."
}