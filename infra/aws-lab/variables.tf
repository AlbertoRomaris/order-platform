variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "name_prefix" {
  type    = string
  default = "order-platform-aws-lab"
}

variable "max_receive_count" {
  type    = number
  default = 5
}
