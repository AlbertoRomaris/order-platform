resource "aws_sqs_queue" "dlq" {
  name                      = "${var.name_prefix}-order-events-dlq"
  message_retention_seconds = 1209600 # 14 days
}

resource "aws_sqs_queue" "main" {
  name                       = "${var.name_prefix}-order-events"
  visibility_timeout_seconds = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_receive_count
  })
}
