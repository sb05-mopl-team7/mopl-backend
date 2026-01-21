# ECS Service (API)

resource "aws_ecs_service" "api" {
  name            = local.api_name
  cluster         = aws_ecs_cluster.main.arn
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private.id]
    security_groups  = [aws_security_group.ec2.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = local.api_container_name
    container_port   = 8080
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200
}


locals {
  api_name           = "${var.project_name}-api"
  api_container_name = "${var.project_name}-api"
}

# ECS Task Definition (API)
resource "aws_ecs_task_definition" "api" {
  family                   = local.api_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024

  # IAM Role 사용 안함
  execution_role_arn = null
  task_role_arn      = null

  container_definitions = jsonencode([
    {
      name      = local.api_container_name
      image     = var.api_image_uri # CD에서 입력
      essential = true

      # 컨테이너 실행 시 주입될 환경변수
      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = data.aws_ssm_parameter.db_password.arn
        },
        {
          name = "GOOGLE_MAIL_PASSWORD"
          value = data.aws_ssm_parameter.gmail_password.value
        },
        {
          name = "JWT_ACCESS_SECRET"
          value = data.aws_ssm_parameter.access_secret.value
        },
        {
          name = "JWT_REFRESH_SECRET"
          value = data.aws_ssm_parameter.refresh_secret.value
        },
        {
          name = "AWS_ACCESS_KEY"
          value = data.aws_ssm_parameter.aws_access_key.value
        },
        {
          name = "AWS_SECRET_KEY"
          value = data.aws_ssm_parameter.aws_refresh_key.value
        }
      ]

      environment = [
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
          name = "REDIS_HOST_PROD"
          value = "redis"
        },
        {
          name = "REDIS_PORT"
          value = 6379
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
          value = "kafka:9092"
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
          awslogs-group         = aws_cloudwatch_log_group.ecs_api
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}