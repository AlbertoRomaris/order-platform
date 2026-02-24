provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      project = var.project_name
      version = "v3"
      stack   = "bootstrap"
      env     = var.env
    }
  }
}