resource "aws_security_group" "main" {
  name   = "mopl-main-sg"
  vpc_id = aws_vpc.main.id

  # HTTP / HTTPS (ALB, API)
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # API / ECS 내부 포트
  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"
    self      = true
  }

  # 내부 통신 전부 허용
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  # 외부로 나가는 트래픽 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}


# EC2 Security Group
resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-ec2-sg"
  description = "Security group for EC2 (monitoring / Redis / Kafka)"
  vpc_id      = aws_vpc.main.id

  # Grafana
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  # Prometheus
  ingress {
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ec2-sg"
    Env  = var.environment
  }
}

# main 보안 그룹에 Fargate 태스크가 접근할 수 있도록 규칙 추가
resource "aws_security_group_rule" "endpoint_from_ecs" {
  type                     = "ingress"
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
  security_group_id        = aws_security_group.main.id       # 엔드포인트가 사용하는 SG
  source_security_group_id = aws_security_group.ec2.id        # Fargate 태스크가 사용하는 SG
  description              = "Allow ECS tasks to access VPC Endpoints"
}

# ALB(main SG)가 ECS 태스크(ec2 SG)의 8080 포트에 접근 허용
resource "aws_security_group_rule" "alb_to_ecs" {
  type                     = "ingress"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  security_group_id        = aws_security_group.ec2.id        # 태스크 SG
  source_security_group_id = aws_security_group.main.id       # ALB SG
  description              = "Allow ALB to access ECS tasks on 8080"
}

# ALB -> Chat 태스크 허용
resource "aws_security_group_rule" "alb_to_chat" {
  type                     = "ingress"
  from_port                = 8081
  to_port                  = 8081
  protocol                 = "tcp"
  security_group_id        = aws_security_group.ec2.id
  source_security_group_id = aws_security_group.main.id
  description              = "Allow ALB to access Chat tasks on 8081"
}