variable "aws_region" {
  type        = string
  description = "AWS region to deploy bootstrap resources into."
  default     = "eu-west-1"
}

variable "project_name" {
  type        = string
  description = "Project name used for tags and naming."
  default     = "order-platform"
}

variable "env" {
  type        = string
  description = "Environment name (even if you only have one)."
  default     = "dev"
}

variable "state_bucket_name" {
  type        = string
  description = "Globally-unique S3 bucket name for Terraform state."
}

variable "lock_table_name" {
  type        = string
  description = "DynamoDB table name used for Terraform state locking."
  default     = "order-platform-tf-locks"
}