# ECS Service (API)
locals {
  api_name           = "${var.project_name}-api"
  api_container_name = "${var.project_name}-api"
}

# ----------------------------------------------------------------------

resource "aws_ecs_service" "api" {
  name            = local.api_name
  cluster         = aws_ecs_cluster.main.arn
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_a.id, aws_subnet.private_c.id]
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = local.api_container_name
    container_port   = 8080
  }

  # ECS의 IP 고정
  service_registries {
    registry_arn = aws_service_discovery_service.api.arn
  }

  # 애플리케이션 가동 후 첫 3분 동안은 헬스체크 실패를 무시
  health_check_grace_period_seconds = 180
  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200
}

# ----------------------------------------------------------------------

# ECS Task Definition (API)
resource "aws_ecs_task_definition" "api" {
  family                   = local.api_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 1024
  memory                   = 2048

  execution_role_arn = aws_iam_role.ecs_execution_role.arn
  task_role_arn      = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = local.api_container_name
      image     = "${data.aws_ecr_repository.mopl_api.repository_url}:release-${var.api_image_uri}"
      essential = true

      # 컨테이너 실행 시 주입될 환경변수
      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = data.aws_ssm_parameter.db_password.arn
        },
        {
          name = "GOOGLE_MAIL_PASSWORD"
          valueFrom = data.aws_ssm_parameter.gmail_password.arn
        },
        {
          name = "JWT_ACCESS_SECRET"
          valueFrom = data.aws_ssm_parameter.access_secret.arn
        },
        {
          name = "JWT_REFRESH_SECRET"
          valueFrom = data.aws_ssm_parameter.refresh_secret.arn
        }
      ]

      environment = [
        {
          name  = "JAVA_OPTS"
          value = "-Xms1536M -Xmx1536M"
        },
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment  # 여기서 프로필을 결정
        },
        {
          name  = "DB_URL"
          value = aws_db_instance.main.address
        },
        {
          name  = "DB_USERNAME"
          value = aws_db_instance.main.username
        },
        {
          name = "REDIS_HOST_PROD"
          value = aws_instance.redis.private_ip
        },
        {
          name = "REDIS_PORT"
          value = "6379"
        },
        {
          name = "GOOGLE_MAIL_USERNAME"
          value = "isylsy166@gmail.com"
        },
        {
          name = "AWS_REGION"
          value = data.aws_ssm_parameter.aws_region.value
        },
        {
          name = "AWS_S3_BUCKET"
          value = data.aws_ssm_parameter.aws_s3_bucket.value
        },
        {
          name = "KAFKA_BOOTSTRAP_SERVERS_PROD"
          value = "${aws_instance.kafka.private_ip}:9092"
        }
      ]

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs_api.name
          awslogs-region        = "ap-northeast-2"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}