# RDS Subnet Group
resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = [
    aws_subnet.private_a.id,
    aws_subnet.private_c.id
  ]

  tags = {
    Name = "${var.project_name}-db-subnet-group"
    Env  = var.environment
  }
}

# RDS MySQL Instance
resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-mysql"

  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = "mopldb"
  username = "mopl_admin"
  password = data.aws_ssm_parameter.db_password.value

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  parameter_group_name = aws_db_parameter_group.mopl_mysql.name

  apply_immediately = true

  publicly_accessible = false
  multi_az            = false

  skip_final_snapshot = true
  deletion_protection = false

  backup_retention_period = 0

  tags = {
    Name = "${var.project_name}-mysql"
    Env  = var.environment
  }
}

# RDS 파라미터 그룹 리소스
resource "aws_db_parameter_group" "mopl_mysql" {
  name        = "mopl-mysql-params"
  family      = "mysql8.0"
  description = "Custom parameter group for MOPL (Timezone: Seoul)"

  # 타임존 설정
  parameter {
    name  = "time_zone"
    value = "Asia/Seoul"
  }

  # 한글 깨짐 방지용 문자셋 설정
  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }
  parameter {
    name  = "character_set_client"
    value = "utf8mb4"
  }

  # 정렬 설정 및 이모지 지원
  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }
  parameter {
    name  = "collation_connection"
    value = "utf8mb4_unicode_ci"
  }
}