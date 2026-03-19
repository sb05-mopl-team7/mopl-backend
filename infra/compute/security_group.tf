# ALB SG
resource "aws_security_group" "alb" {
  name   = "mopl-alb-sg"
  vpc_id = aws_vpc.main.id

  # 인바운드 트래픽
  # HTTP:80
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS:443
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 아웃바운드 트래픽 (전부 허용)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ECS Security Group
resource "aws_security_group" "ecs" {
  name        = "${var.project_name}-ecs-sg"
  description = "Security group for ECS (api / batch / chat)"
  vpc_id      = aws_vpc.main.id

  # 인바운드
  # 이 ecs SG을 가진 리소스들끼리는 모든 포트/모든 프로토콜로 서로 통신을 허용
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  # 외부(ALB, EC2) → ECS(api)
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # 외부(ALB) → ECS(batch)
  ingress {
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  # 외부(ALB) → ECS(chat)
  ingress {
    from_port       = 8082
    to_port         = 8082
    protocol        = "tcp"
    self = true
    security_groups = [aws_security_group.alb.id]
  }

  # Kafka, Redis, S3, ECR, ...
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# EC2(모니터링)가 8080~8082로 ECS에 들어올 수 있도록 허용
resource "aws_security_group_rule" "allow_prometheus_to_ecs" {
  type                     = "ingress"
  from_port                = 8080
  to_port                  = 8082
  protocol                 = "tcp"

  # 규칙을 붙일 보안 그룹 (ECS)
  security_group_id        = aws_security_group.ecs.id

  # 허용할 대상 (EC2 모니터링 서버)
  source_security_group_id = aws_security_group.ec2.id

  description              = "Allow Prometheus(EC2) to access ECS Actuator"
}

resource "aws_security_group_rule" "prometheus_egress_to_ecs" {
  type                     = "egress"
  from_port                = 8080
  to_port                  = 8082
  protocol                 = "tcp"
  security_group_id        = aws_security_group.ec2.id
  source_security_group_id = aws_security_group.ecs.id
  description              = "Allow Prometheus outbound to ECS tasks"
}

# EC2 Security Group
resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-ec2-sg"
  description = "Security group for EC2 (monitoring / Redis / Kafka)"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  # Grafana (ECS → Grafana)
  ingress {
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  # Prometheus (ECS → Prometheus scrape or UI)
  ingress {
    from_port       = 9090
    to_port         = 9090
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  # Kafka (ECS → Kafka)
  ingress {
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
    description     = "Allow ECS tasks to access Kafka"
  }

  # Redis (ECS → Redis)
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
    description     = "Allow ECS tasks to access Redis"
  }

  # Kafka UI (EC2 -> Kafka UI)
  ingress {
    from_port   = 9000
    to_port     = 9000
    protocol    = "tcp"
    self = true
    description = "Allow access to Kafka UI from browser"
  }

  # Outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# RDS Security Group
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "MySQL from ECS"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [
      aws_security_group.ecs.id,
      aws_security_group.ec2.id
    ]   # ECS만 접근 허용
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ----------------------------------------------------------------------------------

# aws_vpc_endpoint의 private_dns_enabled = true일 때 자동으로 사용
resource "aws_security_group" "ecr_endpoint" {
  name        = "${var.project_name}-ecr-endpoint-sg"
  description = "Security group for ECR VPC Interface Endpoints (api, dkr)"
  vpc_id      = aws_vpc.main.id

  # ECS → ECR Endpoint (HTTPS)
  # EC2(Redis/Kafka/Monitoring) 허용
  ingress {
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [
      aws_security_group.ecs.id,
      aws_security_group.ec2.id
    ]
    description     = "Allow ECS tasks to access ECR via VPC Endpoint. And Allow EC2 instances to access SSM/ECR endpoints"
  }

  # Endpoint → AWS 내부 통신
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ecr-endpoint-sg"
    Env  = var.environment
  }
}