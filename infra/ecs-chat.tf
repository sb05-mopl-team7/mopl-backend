locals {
  chat_name          = "mopl-chat"
  chat_container_name = "mopl-chat"
}

resource "aws_ecs_task_definition" "chat" {
  family                   = local.chat_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu    = 1024
  memory = 2048

  container_definitions = jsonencode([
    {
      name      = local.chat_container_name
      image     = var.chat_image_uri
      essential = true

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
    subnets         = aws_subnet.private.id
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
