
resource "aws_cloudwatch_log_metric_filter" "api_error_count" {
  name           = "${local.name_prefix}-api-error-count"
  log_group_name = aws_cloudwatch_log_group.api.name
  pattern        = "\" ERROR \""

  metric_transformation {
    name      = "ApiErrorCount"
    namespace = "${local.name_prefix}/App"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "api_error_spike" {
  alarm_name          = "${local.name_prefix}-api-error-spike"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 5
  treat_missing_data  = "notBreaching"

  namespace   = "${local.name_prefix}/App"
  metric_name = "ApiErrorCount"

  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_log_metric_filter" "worker_failed_count" {
  name           = "${local.name_prefix}-worker-failed-count"
  log_group_name = aws_cloudwatch_log_group.worker.name
  pattern        = "FAILED"

  metric_transformation {
    name      = "WorkerFailedCount"
    namespace = "${local.name_prefix}/App"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "worker_failed_alarm" {
  alarm_name          = "${local.name_prefix}-worker-failed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  namespace   = "${local.name_prefix}/App"
  metric_name = "WorkerFailedCount"

  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_log_metric_filter" "worker_activity" {
  name           = "${local.name_prefix}-worker-activity"
  log_group_name = aws_cloudwatch_log_group.worker.name

  pattern = "SQS message received"

  metric_transformation {
    name      = "WorkerActivity"
    namespace = "${local.name_prefix}/App"
    value     = "1"
  }
}
