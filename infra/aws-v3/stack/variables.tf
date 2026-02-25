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

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC."
  default     = "10.30.0.0/16"
}

variable "public_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for public subnets (one per AZ)."
  default     = ["10.30.1.0/24", "10.30.2.0/24"]
}