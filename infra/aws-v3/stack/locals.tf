locals {
  name_prefix = "${var.project_name}-${var.env}-v3"

  tags = {
    project = var.project_name
    env     = var.env
    version = "v3"
    stack   = "stack"
  }
}