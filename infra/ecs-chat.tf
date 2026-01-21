locals {
  chat_name          = "mopl-chat"
  chat_container_name = "mopl-chat"
}

resource "aws_ecs_task_definition" "chat" {
  family                   = local.chat_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu    = 512
  memory = 1024

  execution_role_arn = aws_iam_role.tools_broker_ssm_role.arn
  task_role_arn      = null

  container_definitions = jsonencode([
    {
      name      = local.chat_container_name
      image     = var.chat_image_uri
      essential = true

      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = data.aws_ssm_parameter.db_password.arn
        },
        {
          name = "JWT_ACCESS_SECRET"
          valueFrom = data.aws_ssm_parameter.access_secret.arn
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
          awslogs-group         = aws_cloudwatch_log_group.ecs_chat.name
          awslogs-region        = "ap-northeast-2"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "chat" {
  name            = local.chat_name
  cluster         = aws_ecs_cluster.main.arn
  task_definition = aws_ecs_task_definition.chat.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_a.id, aws_subnet.private_c.id]
    security_groups = [aws_security_group.main.id]
    assign_public_ip = false
  }

  # Chat도 ALB 뒤에 붙일 거면 사용
  load_balancer {
    target_group_arn = aws_lb_target_group.chat.arn
    container_name   = local.chat_container_name
    container_port   = 8080
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200
}
