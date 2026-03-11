# ECS API 로그
resource "aws_cloudwatch_log_group" "ecs_api" {
  name              = "/mopl/ecs/api"
  retention_in_days = 7

  tags = {
    Service = "ecs-api"
  }
}

# ECS Chat 로그
resource "aws_cloudwatch_log_group" "ecs_chat" {
  name              = "/mopl/ecs/chat"
  retention_in_days = 7

  tags = {
    Service = "ecs-chat"
  }
}

# ECS Batch 로그
resource "aws_cloudwatch_log_group" "ecs_batch" {
  name              = "/mopl/ecs/batch"
  retention_in_days = 7

  tags = {
    Service = "ecs-batch"
  }
}
