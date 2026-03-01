resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${local.name_prefix}-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "SQS Backlog (Visible / InFlight)"
          region = var.aws_region
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.order_events.name],
            [".", "ApproximateNumberOfMessagesNotVisible", "QueueName", aws_sqs_queue.order_events.name]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "DLQ Messages (Visible)"
          region = var.aws_region
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.order_events_dlq.name]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "HTTP 5XX (ALB + TargetGroup)"
          region = var.aws_region
          period = 60
          stat   = "Sum"
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", aws_lb.api.arn_suffix],
            [".", "HTTPCode_Target_5XX_Count", "LoadBalancer", aws_lb.api.arn_suffix, "TargetGroup", aws_lb_target_group.api.arn_suffix]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "ALB Latency p95 (TargetResponseTime)"
          region = var.aws_region
          period = 60
          stat   = "p95"
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.api.arn_suffix]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          title  = "RDS CPUUtilization"
          region = var.aws_region
          period = 60
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.postgres.identifier]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          title  = "RDS FreeStorageSpace"
          region = var.aws_region
          period = 300
          stat   = "Average"
          metrics = [
            ["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", aws_db_instance.postgres.identifier]
          ]
        }
      },

      # App metrics (truth-based): ahora mismo CloudWatch solo tiene WorkerActivity en este namespace.
      {
        type   = "metric"
        x      = 0
        y      = 18
        width  = 12
        height = 6
        properties = {
          title  = "App: WorkerActivity (Sum)"
          region = var.aws_region
          period = 60
          stat   = "Sum"
          metrics = [
            ["${local.name_prefix}/App", "WorkerActivity"]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 18
        width  = 12
        height = 6
        properties = {
          title  = "App: WorkerActivity (Average)"
          region = var.aws_region
          period = 60
          stat   = "Average"
          metrics = [
            ["${local.name_prefix}/App", "WorkerActivity"]
          ]
        }
      },

      {
        type   = "metric"
        x      = 0
        y      = 24
        width  = 12
        height = 6
        properties = {
          title  = "ECS Worker: Desired vs Running Tasks"
          region = var.aws_region
          period = 60
          stat   = "Average"
          metrics = [
            ["ECS/ContainerInsights", "DesiredTaskCount", "ServiceName", aws_ecs_service.worker.name, "ClusterName", aws_ecs_cluster.this.name],
            [".", "RunningTaskCount", "ServiceName", aws_ecs_service.worker.name, "ClusterName", aws_ecs_cluster.this.name]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 24
        width  = 12
        height = 6
        properties = {
          title  = "ALB RequestCount"
          region = var.aws_region
          period = 60
          stat   = "Sum"
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.api.arn_suffix]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 30
        width  = 12
        height = 6
        properties = {
          title  = "SQS Messages Sent"
          region = var.aws_region
          period = 60
          stat   = "Sum"
          metrics = [
            ["AWS/SQS", "NumberOfMessagesSent", "QueueName", aws_sqs_queue.order_events.name]
          ]
        }
      }
    ]
  })
}

output "cloudwatch_dashboard_name" {
  value = aws_cloudwatch_dashboard.main.dashboard_name
}