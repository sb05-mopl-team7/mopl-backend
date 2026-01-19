locals {
  name_prefix = var.project_name

  common_tags = merge(
    {
      Project = var.project_name
      Managed = "terraform"
    },
    var.tags
  )

  # API/CHAT 포트가 동일할 수 있으니 중복 인바운드 룰 생성을 방지하기 위해 set으로 유니크 처리
  ecs_service_ports = toset([var.api_container_port, var.chat_container_port])
}

# =========================================================
# 1) Security Groups
# =========================================================

# --- ALB SG (Public)
resource "aws_security_group" "alb_sg" {
  count       = var.alb_enable ? 1 : 0
  name        = "${local.name_prefix}-alb-sg"
  description = "ALB security group"
  vpc_id      = var.vpc_id
  tags        = local.common_tags
}

# ALB 인바운드(80)
resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  count             = var.alb_enable ? length(var.alb_ingress_cidrs) : 0
  security_group_id = aws_security_group.alb_sg[0].id
  cidr_ipv4         = var.alb_ingress_cidrs[count.index]
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  description       = "HTTP from allowed CIDRs"
}

resource "aws_vpc_security_group_egress_rule" "alb_all" {
  count             = var.alb_enable ? 1 : 0
  security_group_id = aws_security_group.alb_sg[0].id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
  description       = "ALB outbound all"
}

# --- RDS SG (Private)
resource "aws_security_group" "rds_sg" {
  name        = "${local.name_prefix}-rds-sg"
  description = "RDS security group"
  vpc_id      = var.vpc_id
  tags        = local.common_tags
}

resource "aws_vpc_security_group_egress_rule" "rds_all" {
  security_group_id = aws_security_group.rds_sg.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
  description       = "RDS outbound all"
}

# --- Tools EC2 SG (Private)
resource "aws_security_group" "tools_sg" {
  name        = "${local.name_prefix}-tools-sg"
  description = "Tools EC2 SG (prometheus/grafana/redis/kafka)"
  vpc_id      = var.vpc_id
  tags        = local.common_tags
}

resource "aws_vpc_security_group_egress_rule" "tools_all" {
  security_group_id = aws_security_group.tools_sg.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
  description       = "Tools outbound all"
}

# =========================================================
# 2) SG 연결 규칙 (핵심: ALB -> ECS Tasks)
# =========================================================

# --- ALB -> ECS Tasks (API/CHAT 포트 유니크 처리)
resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {
  for_each = var.alb_enable ? local.ecs_service_ports : toset([])

  security_group_id            = var.ecs_tasks_sg_id
  referenced_security_group_id = aws_security_group.alb_sg[0].id

  ip_protocol = "tcp"
  from_port   = each.value
  to_port     = each.value

  description = "Allow ALB to reach ECS tasks on port ${each.value}"
}

# --- ECS Tasks -> RDS(MySQL)
resource "aws_vpc_security_group_ingress_rule" "rds_from_ecs" {
  security_group_id            = aws_security_group.rds_sg.id
  referenced_security_group_id = var.ecs_tasks_sg_id

  ip_protocol = "tcp"
  from_port   = 3306
  to_port     = 3306

  description = "Allow ECS tasks to access RDS MySQL"
}

# --- ECS Tasks -> Tools(Redis/Kafka)
resource "aws_vpc_security_group_ingress_rule" "tools_redis_from_ecs" {
  security_group_id            = aws_security_group.tools_sg.id
  referenced_security_group_id = var.ecs_tasks_sg_id

  ip_protocol = "tcp"
  from_port   = 6379
  to_port     = 6379

  description = "Allow ECS tasks to access Redis on Tools EC2"
}

resource "aws_vpc_security_group_ingress_rule" "tools_kafka_from_ecs" {
  security_group_id            = aws_security_group.tools_sg.id
  referenced_security_group_id = var.ecs_tasks_sg_id

  ip_protocol = "tcp"
  from_port   = 9092
  to_port     = 9092

  description = "Allow ECS tasks to access Kafka on Tools EC2"
}

# --- Tools EC2 -> RDS(MySQL) (운영/테스트/관리 용도)
resource "aws_vpc_security_group_ingress_rule" "rds_from_tools" {
  security_group_id            = aws_security_group.rds_sg.id
  referenced_security_group_id = aws_security_group.tools_sg.id

  ip_protocol = "tcp"
  from_port   = 3306
  to_port     = 3306

  description = "Allow Tools EC2 to access RDS MySQL"
}

# =========================================================
# 3) ALB + Target Groups
# =========================================================

resource "aws_lb" "alb" {
  count              = var.alb_enable ? 1 : 0
  name               = "${local.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg[0].id]
  subnets            = var.public_subnet_ids
  tags               = local.common_tags
}

resource "aws_lb_target_group" "api_tg" {
  count       = var.alb_enable ? 1 : 0
  name        = "${local.name_prefix}-api-tg"
  port        = var.api_container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = var.api_healthcheck_path
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 15
    matcher             = "200"
  }

  tags = local.common_tags
}

resource "aws_lb_target_group" "chat_tg" {
  count       = var.alb_enable ? 1 : 0
  name        = "${local.name_prefix}-chat-tg"
  port        = var.chat_container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = var.chat_healthcheck_path
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 15
    matcher             = "200"
  }

  tags = local.common_tags
}

resource "aws_lb_listener" "http" {
  count             = var.alb_enable ? 1 : 0
  load_balancer_arn = aws_lb.alb[0].arn
  port              = 80
  protocol          = "HTTP"

  # 기본은 api로 보냄
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api_tg[0].arn
  }
}

# /chat*, /ws*, /sse* 는 chat으로 라우팅
resource "aws_lb_listener_rule" "chat_path_rule" {
  count        = var.alb_enable ? 1 : 0
  listener_arn = aws_lb_listener.http[0].arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.chat_tg[0].arn
  }

  condition {
    path_pattern {
      values = ["/chat*", "/ws*", "/sse*"]
    }
  }
}

# =========================================================
# 4) RDS MySQL (Private)
# =========================================================

resource "random_password" "db" {
  length  = 24
  special = true
}

resource "aws_secretsmanager_secret" "db" {
  name = "${local.name_prefix}/rds/mysql"
  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
    dbname   = var.db_name
  })
}

resource "aws_db_subnet_group" "db" {
  name       = "${local.name_prefix}-db-subnet-group"
  subnet_ids = var.private_subnet_ids
  tags       = local.common_tags
}

resource "aws_db_instance" "mysql" {
  identifier              = "${local.name_prefix}-mysql"
  engine                  = "mysql"
  engine_version          = "8.0"
  instance_class          = var.db_instance_class
  allocated_storage       = var.db_allocated_storage
  db_name                 = var.db_name
  username                = var.db_username
  password                = random_password.db.result
  db_subnet_group_name    = aws_db_subnet_group.db.name
  vpc_security_group_ids  = [aws_security_group.rds_sg.id]
  publicly_accessible     = false
  multi_az                = var.db_multi_az
  deletion_protection     = var.db_deletion_protection
  skip_final_snapshot     = true
  backup_retention_period = 7

  tags = local.common_tags
}

# =========================================================
# 5) EC2 Tools (Private) - Prometheus/Grafana + Redis + Kafka
# =========================================================

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

resource "aws_iam_role" "ec2_ssm_role" {
  name = "${local.name_prefix}-ec2-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Action = "sts:AssumeRole",
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.ec2_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${local.name_prefix}-ec2-profile"
  role = aws_iam_role.ec2_ssm_role.name
}

resource "aws_instance" "tools" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.tools_instance_type
  subnet_id                   = var.private_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.tools_sg.id]
  iam_instance_profile        = aws_iam_instance_profile.ec2_profile.name
  associate_public_ip_address = false

  metadata_options {
    http_tokens = "required"
  }

  # 주의: user_data는 state에 남을 수 있음(비밀번호 포함 가능)
  user_data = <<-EOF
    #!/bin/bash
    set -euo pipefail

    dnf update -y
    dnf install -y docker docker-compose-plugin curl

    systemctl enable --now docker

    mkdir -p /opt/mopl/stack
    cd /opt/mopl/stack

    HOST_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)

    cat > .env <<EOT
    HOST_IP=${HOST_IP}
    GRAFANA_ADMIN_PASSWORD=${var.grafana_admin_password}
    EOT

    cat > prometheus.yml <<'EOT'
    global:
      scrape_interval: 15s

    scrape_configs:
      - job_name: 'prometheus'
        static_configs:
          - targets: ['localhost:9090']
    EOT

    cat > docker-compose.yml <<'EOT'
    services:
      redis:
        image: redis:7
        ports:
          - "6379:6379"
        restart: always

      zookeeper:
        image: bitnami/zookeeper:3.9
        environment:
          - ALLOW_ANONYMOUS_LOGIN=yes
        ports:
          - "2181:2181"
        restart: always

      kafka:
        image: bitnami/kafka:3.7
        depends_on:
          - zookeeper
        environment:
          - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
          - ALLOW_PLAINTEXT_LISTENER=yes
          - KAFKA_CFG_LISTENERS=PLAINTEXT://0.0.0.0:9092
          - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://${HOST_IP}:9092
        ports:
          - "9092:9092"
        restart: always

      prometheus:
        image: prom/prometheus:latest
        command:
          - "--config.file=/etc/prometheus/prometheus.yml"
        volumes:
          - "./prometheus.yml:/etc/prometheus/prometheus.yml:ro"
        ports:
          - "9090:9090"
        restart: always

      grafana:
        image: grafana/grafana:latest
        environment:
          - GF_SECURITY_ADMIN_USER=admin
          - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
        ports:
          - "3000:3000"
        restart: always
    EOT

    docker compose up -d
  EOF

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-tools" })
}
