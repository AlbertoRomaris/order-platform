resource "aws_cloudwatch_metric_alarm" "sqs_dlq_nonempty" {
  alarm_name          = "${local.name_prefix}-dlq-nonempty"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Average"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/SQS"
  metric_name = "ApproximateNumberOfMessagesVisible"

  dimensions = {
    QueueName = aws_sqs_queue.order_events_dlq.name
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "sqs_backlog_high_sustained" {
  alarm_name          = "${local.name_prefix}-queue-backlog-high-sustained"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 5
  period              = 60
  statistic           = "Average"
  threshold           = 100
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/SQS"
  metric_name = "ApproximateNumberOfMessagesVisible"

  dimensions = {
    QueueName = aws_sqs_queue.order_events.name
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  alarm_name          = "${local.name_prefix}-alb-5xx"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ApplicationELB"
  metric_name = "HTTPCode_ELB_5XX_Count"

  dimensions = {
    LoadBalancer = aws_lb.api.arn_suffix
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}


resource "aws_cloudwatch_metric_alarm" "alb_target_5xx" {
  alarm_name          = "${local.name_prefix}-tg-5xx"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ApplicationELB"
  metric_name = "HTTPCode_Target_5XX_Count"

  dimensions = {
    LoadBalancer = aws_lb.api.arn_suffix
    TargetGroup  = aws_lb_target_group.api.arn_suffix
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "alb_latency_p95" {
  alarm_name          = "${local.name_prefix}-alb-latency-p95"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 5
  period              = 60
  extended_statistic  = "p95"
  threshold           = 1.0
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ApplicationELB"
  metric_name = "TargetResponseTime"

  dimensions = {
    LoadBalancer = aws_lb.api.arn_suffix
    TargetGroup  = aws_lb_target_group.api.arn_suffix
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_free_storage_low" {
  alarm_name          = "${local.name_prefix}-rds-free-storage-low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 2
  period              = 300
  statistic           = "Average"
  threshold           = 5000000000 # 5GB
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/RDS"
  metric_name = "FreeStorageSpace"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgres.id
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "${local.name_prefix}-rds-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  period              = 300
  statistic           = "Average"
  threshold           = 80
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/RDS"
  metric_name = "CPUUtilization"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgres.id
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
}