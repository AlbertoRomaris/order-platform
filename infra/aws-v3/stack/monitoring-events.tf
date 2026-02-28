resource "aws_sns_topic_policy" "alerts_allow_eventbridge" {
  arn = aws_sns_topic.alerts.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowEventBridgePublish"
        Effect    = "Allow"
        Principal = { Service = "events.amazonaws.com" }
        Action    = "sns:Publish"
        Resource  = aws_sns_topic.alerts.arn
      }
    ]
  })
}

resource "aws_cloudwatch_event_rule" "ecs_worker_task_stopped" {
  name        = "${local.name_prefix}-ecs-worker-task-stopped"
  description = "Notify when worker tasks stop"

  event_pattern = jsonencode({
    "source": ["aws.ecs"],
    "detail-type": ["ECS Task State Change"],
    "detail": {
      "clusterArn": [aws_ecs_cluster.this.arn],
      "lastStatus": ["STOPPED"],
      "group": ["service:${aws_ecs_service.worker.name}"]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_worker_task_stopped_to_sns" {
  rule      = aws_cloudwatch_event_rule.ecs_worker_task_stopped.name
  target_id = "sns-alerts"
  arn       = aws_sns_topic.alerts.arn
}

# Regla: deployment del worker falla / rollback / no llega a steady state (eventos de servicio)
resource "aws_cloudwatch_event_rule" "ecs_worker_deployment_events" {
  name        = "${local.name_prefix}-ecs-worker-deployment-events"
  description = "Notify on ECS deployment/service events for worker"

  event_pattern = jsonencode({
    "source": ["aws.ecs"],
    "detail-type": ["ECS Deployment State Change", "ECS Service Action"],
    "detail": {
      "clusterArn": [aws_ecs_cluster.this.arn],
      "serviceArn": [aws_ecs_service.worker.arn]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_worker_deployment_events_to_sns" {
  rule      = aws_cloudwatch_event_rule.ecs_worker_deployment_events.name
  target_id = "sns-alerts"
  arn       = aws_sns_topic.alerts.arn
}