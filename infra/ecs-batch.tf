locals {
  batch_name           = "mopl-batch"
  batch_container_name = "mopl-batch"
}

resource "aws_ecs_task_definition" "batch" {
  family                   = local.batch_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu    = 1024
  memory = 2048

  container_definitions = jsonencode([
    {
      name      = local.batch_container_name
      image     = var.batch_image_uri
      essential = true

      command = ["java", "-jar", "app.jar"] # 배치 전용 엔트리포인트

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs_batch.name
          awslogs-region        = "ap-northeast-2"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}


# EventBridge Schedule Rule
resource "aws_cloudwatch_event_rule" "batch_schedule" {
  name                = "mopl-batch-daily-7am"
  description         = "Run batch task every day at 07:00"
  schedule_expression = "cron(0 7 * * ? *)"
}

resource "aws_cloudwatch_event_target" "batch_target" {
  rule      = aws_cloudwatch_event_rule.batch_schedule.name
  target_id = "mopl-batch"
  arn       = aws_ecs_cluster.main.arn

  ecs_target {
    task_definition_arn = aws_ecs_task_definition.batch.arn
    launch_type         = "FARGATE"
    task_count          = 1

    network_configuration {
      subnets         = aws_subnet.private.id
      security_groups = [aws_security_group.main.id]
      assign_public_ip = false
    }
  }
}
