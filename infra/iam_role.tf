# EC2에 접근한 SSM 접속용 IAM Role 생성
resource "aws_iam_role" "tools_broker_ssm_role" {

  name = "${var.project_name}-tools-broker-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "${var.project_name}-ecs-task-execution-role"
    Env  = var.environment
  }
}

# tools_broker_ssm_role에 AmazonSSMManagedInstanceCore 권한 부여
resource "aws_iam_role_policy_attachment" "tools_broker_ssm_core" {
  role       = aws_iam_role.tools_broker_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "tools_broker_ssm_profile" {
  name = "${var.project_name}-${var.environment}-tools-broker-ssm-profile"
  role = aws_iam_role.tools_broker_ssm_role.name
}

# SSM Parameter 읽기 권한
resource "aws_iam_role_policy" "ecs_task_execution_policy" {
  name   = "${var.project_name}-ecs-task-execution-policy"
  role   = aws_iam_role.tools_broker_ssm_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameters",
          "ssm:GetParameter"
        ]
        Resource = "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/mopl/prod/*"
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt"
        ]
        Resource = "*"
      }
    ]
  })
}

# EventBridge → ECS 실행 권한
resource "aws_iam_role" "eventbridge_ecs_role" {
  name = "${var.project_name}-eventbridge-ecs-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "events.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Name = "${var.project_name}-eventbridge-ecs-role"
    Env  = var.environment
  }
}

# ECS 작업 실행 권한
resource "aws_iam_role_policy" "eventbridge_ecs_policy" {
  name   = "${var.project_name}-eventbridge-ecs-policy"
  role   = aws_iam_role.eventbridge_ecs_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecs:RunTask"
        ]
        Resource = aws_ecs_task_definition.batch.arn
      },
      {
        Effect = "Allow"
        Action = [
          "iam:PassRole"
        ]
        Resource = [
          aws_iam_role.tools_broker_ssm_role.arn
        ]
      }
    ]
  })
}
