resource "aws_sqs_queue" "order_events_dlq" {
  name                       = "${local.name_prefix}-order-events-dlq"
  message_retention_seconds  = 1209600 # 14 days
  visibility_timeout_seconds = 30
  sqs_managed_sse_enabled    = true

  tags = {
    Name = "${local.name_prefix}-order-events-dlq"
  }
}

resource "aws_sqs_queue" "order_events" {
  name                       = "${local.name_prefix}-order-events"
  message_retention_seconds  = 345600 # 4 days
  visibility_timeout_seconds = 30
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_events_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name = "${local.name_prefix}-order-events"
  }
}