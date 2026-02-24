variable "aws_region" {
  type        = string
  description = "AWS region for V3."
  default     = "eu-west-1"
}

variable "project_name" {
  type        = string
  description = "Project name used for naming and tagging."
  default     = "order-platform"
}

variable "env" {
  type        = string
  description = "Environment name."
  default     = "dev"
}