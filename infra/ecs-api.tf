# ECS Service (API)

resource "aws_ecs_service" "api" {
  name            = local.api_name
  cluster         = aws_ecs_cluster.main.arn
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private.id
    security_groups  = [var.api_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.api_target_group_arn
    container_name   = local.api_container_name
    container_port   = var.api_container_port
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

      portMappings = [
        {
          containerPort = var.api_container_port
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

