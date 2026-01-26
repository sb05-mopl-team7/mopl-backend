locals {
  batch_name           = "mopl-batch"
  batch_container_name = "mopl-batch"
}

# ----------------------------------------------------------------------

resource "aws_ecs_task_definition" "batch" {
  family                   = local.batch_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu    = 512
  memory = 1024

  execution_role_arn = aws_iam_role.ecs_execution_role.arn
  task_role_arn      = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = local.batch_container_name
      image     = "${data.aws_ecr_repository.mopl_batch.repository_url}:release-${var.batch_image_uri}"
      essential = true

      secrets = [
        {
          name      = "TMDB_API_TOKEN"
          valueFrom = data.aws_ssm_parameter.tmdb_api_token.arn
        }
      ]

      environment = [
        {
          name  = "TZ"
          value = "Asia/Seoul"
        },
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment  # 여기서 프로필을 결정
        },
        {
          name  = "DB_HOST"
          value = aws_db_instance.main.address
        },
        {
          name  = "DB_USER"
          value = aws_db_instance.main.username
        },
        {
          name = "AWS_REGION"
          value = data.aws_ssm_parameter.aws_region.value
        },
        {
          name = "AWS_S3_BUCKET"
          value = data.aws_ssm_parameter.aws_s3_bucket.value
        },
      ]

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


# 7:00 ECS Task 자동으로 실행하도록 설정
resource "aws_cloudwatch_event_rule" "batch_schedule" {
  name                = "mopl-batch-daily-7am"
  description         = "Run batch task every day at 07:00"
  schedule_expression = "cron(0 22 * * ? *)"
}

# 무엇을 실행할지 결정 - batch task
resource "aws_cloudwatch_event_target" "batch_target" {
  rule      = aws_cloudwatch_event_rule.batch_schedule.name
  target_id = "mopl-batch"
  arn       = aws_ecs_cluster.main.arn
  role_arn  = aws_iam_role.eventbridge_ecs_role.arn


  ecs_target {
    task_definition_arn = aws_ecs_task_definition.batch.arn
    launch_type         = "FARGATE"
    task_count          = 1

    network_configuration {
      subnets         = [aws_subnet.private_a.id, aws_subnet.private_c.id]
      security_groups = [aws_security_group.ecs.id]
      assign_public_ip = false
    }
  }
}
