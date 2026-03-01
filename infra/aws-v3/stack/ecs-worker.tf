data "aws_iam_policy_document" "worker_sqs_access" {
  statement {
    effect = "Allow"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:ChangeMessageVisibility"
    ]
    resources = [
      aws_sqs_queue.order_events.arn
    ]
  }
}

resource "aws_iam_policy" "worker_sqs_access" {
  name   = "${local.name_prefix}-worker-sqs-access"
  policy = data.aws_iam_policy_document.worker_sqs_access.json
}

resource "aws_iam_role" "worker_task" {
  name               = "${local.name_prefix}-worker-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

resource "aws_iam_role_policy_attachment" "worker_sqs_access" {
  role       = aws_iam_role.worker_task.name
  policy_arn = aws_iam_policy.worker_sqs_access.arn
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "${local.name_prefix}-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.worker_task.arn

  container_definitions = jsonencode([
    {
      name      = "worker"
      image = "${aws_ecr_repository.worker.repository_url}:worker-main"
      essential = true

      environment = [
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:${aws_db_instance.postgres.port}/${aws_db_instance.postgres.db_name}" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "WORKER_MODE", value = "sqs-consumer" },
        {
          name = "SPRING_APPLICATION_JSON",
          value = jsonencode({
            aws = {
              region = var.aws_region
              sqs = {
                queueUrl = aws_sqs_queue.order_events.url
              }
            }
          })
        },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "AWS_DEFAULT_REGION", value = var.aws_region }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-region        = var.aws_region
          awslogs-group         = aws_cloudwatch_log_group.worker.name
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_security_group" "worker" {
  name        = "${local.name_prefix}-worker-sg"
  description = "Worker tasks security group (no inbound)"
  vpc_id      = aws_vpc.main.id

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-worker-sg"
  }
}

resource "aws_ecs_service" "worker" {
  name            = "${local.name_prefix}-worker-svc"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.worker.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [for s in aws_subnet.public : s.id]
    security_groups  = [aws_security_group.worker.id]
    assign_public_ip = true
  }
}