data "aws_iam_policy_document" "api_sqs_send" {
  statement {
    effect = "Allow"
    actions = [
      "sqs:SendMessage"
    ]
    resources = [
      aws_sqs_queue.order_events.arn
    ]
  }
}

resource "aws_iam_policy" "api_sqs_send" {
  name   = "${local.name_prefix}-api-sqs-send"
  policy = data.aws_iam_policy_document.api_sqs_send.json
}

resource "aws_iam_role_policy_attachment" "api_sqs_send" {
  role       = aws_iam_role.api_task.name
  policy_arn = aws_iam_policy.api_sqs_send.arn
}