# EC2에 접근한 SSM 접속용 IAM Role 생성
resource "aws_iam_role" "ec2_ssm_role" {

  name = "${var.project_name}-tools-broker-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = [
          "ec2.amazonaws.com"
        ]
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "${var.project_name}-ecs-task-execution-role"
    Env  = var.environment
  }
}

# ec2_ssm_core에 AmazonSSMManagedInstanceCore 권한 부여
resource "aws_iam_role_policy_attachment" "ec2_ssm_core" {
  role       = aws_iam_role.ec2_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}