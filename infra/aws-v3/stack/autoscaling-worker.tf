# Autoscaling target: ECS worker desired count
resource "aws_appautoscaling_target" "worker" {
  service_namespace  = "ecs"
  scalable_dimension = "ecs:service:DesiredCount"

  # Formato requerido por Application Auto Scaling:
  # service/<cluster-name>/<service-name>
  resource_id = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.worker.name}"

  min_capacity = 1
  max_capacity = 5
}

# Scale OUT policy (step scaling)
resource "aws_appautoscaling_policy" "worker_scale_out" {
  name               = "${local.name_prefix}-worker-scale-out"
  policy_type        = "StepScaling"
  service_namespace  = aws_appautoscaling_target.worker.service_namespace
  scalable_dimension = aws_appautoscaling_target.worker.scalable_dimension
  resource_id        = aws_appautoscaling_target.worker.resource_id

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 30
    metric_aggregation_type = "Average"

    # Con backlog alto, subimos 1 task
    step_adjustment {
      metric_interval_lower_bound = 0
      scaling_adjustment          = 1
    }
  }
}

# Scale IN policy (step scaling)
resource "aws_appautoscaling_policy" "worker_scale_in" {
  name               = "${local.name_prefix}-worker-scale-in"
  policy_type        = "StepScaling"
  service_namespace  = aws_appautoscaling_target.worker.service_namespace
  scalable_dimension = aws_appautoscaling_target.worker.scalable_dimension
  resource_id        = aws_appautoscaling_target.worker.resource_id

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 120
    metric_aggregation_type = "Average"

    # Con backlog vacío de forma sostenida, bajamos 1 task (hasta min_capacity)
    step_adjustment {
      metric_interval_upper_bound = 0
      scaling_adjustment          = -1
    }
  }
}

# Alarm: backlog alto => scale out
resource "aws_cloudwatch_metric_alarm" "sqs_backlog_high" {
  alarm_name          = "${local.name_prefix}-sqs-backlog-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  period              = 60
  statistic           = "Average"
  threshold           = 10
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/SQS"
  metric_name = "ApproximateNumberOfMessagesVisible"

  dimensions = {
    QueueName = aws_sqs_queue.order_events.name
  }

  alarm_actions = [aws_appautoscaling_policy.worker_scale_out.arn]
}

# Alarm: backlog vacío => scale in (conservador para evitar flapping)
resource "aws_cloudwatch_metric_alarm" "sqs_backlog_low" {
  alarm_name          = "${local.name_prefix}-sqs-backlog-low"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 5
  period              = 60
  statistic           = "Average"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/SQS"
  metric_name = "ApproximateNumberOfMessagesVisible"

  dimensions = {
    QueueName = aws_sqs_queue.order_events.name
  }

  alarm_actions = [aws_appautoscaling_policy.worker_scale_in.arn]
}