# ECS Service (API)

resource "aws_ecs_service" "api" {
  name            = local.api_name
  cluster         = aws_ecs_cluster.main.arn
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private.id
    security_groups  = [aws_security_group.ec2]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api
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
      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment  # 여기서 프로필을 결정
        },
        {
          name  = "DB_HOST"
          value = aws_db_instance.this.address
        },
        {
          name  = "DB_USER"
          value = var.db_username
        },
        # ... 기타 필요한 변수들
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