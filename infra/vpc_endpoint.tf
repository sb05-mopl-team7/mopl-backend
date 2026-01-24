# S3 VPC Endpoint
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"

  route_table_ids = [aws_route_table.private.id]

  tags = {
    Name = "${var.project_name}-s3-endpoint"
    Env  = var.environment
  }
}

# ECR API Endpoint
resource "aws_vpc_endpoint" "ecr" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.aws_region}.ecr.api"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [
    aws_subnet.private_a.id,
    aws_subnet.private_c.id
  ]
  security_group_ids  = [aws_security_group.ecr_endpoint.id]  # ecr_endpoint를 가진 리소스만 ECR API 호출 가능
  private_dns_enabled = true                                  # 코드 설정 변경 없이 바로 ECR 접근 가능

  tags = {
    Name = "${var.project_name}-ecr-api-endpoint"
    Env  = var.environment
  }
}

# ECR DKR(Docker Registry Endpoint) Interface Endpoint
# - 컨테이너 이미지 레이어를 실제로 다운로드하는 통로
resource "aws_vpc_endpoint" "ecr_dkr" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.aws_region}.ecr.dkr"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [
    aws_subnet.private_a.id,
    aws_subnet.private_c.id
  ]
  security_group_ids  = [aws_security_group.ecr_endpoint.id]
  private_dns_enabled = true

  tags = {
    Name = "${var.project_name}-ecr-dkr-endpoint"
    Env  = var.environment
  }
}


# CloudWatch Logs Interface Endpoint
resource "aws_vpc_endpoint" "logs" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.aws_region}.logs"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [aws_subnet.private_a.id, aws_subnet.private_c.id]
  security_group_ids  = [aws_security_group.ecr_endpoint.id]
  private_dns_enabled = true

  tags = {
    Name = "${var.project_name}-logs-endpoint"
    Env  = var.environment
  }
}

resource "aws_vpc_endpoint" "ssm" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.aws_region}.ssm"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [aws_subnet.private_a.id, aws_subnet.private_c.id]
  security_group_ids  = [aws_security_group.ecr_endpoint.id]
  private_dns_enabled = true

  tags = {
    Name = "${var.project_name}-ssm-endpoint"
  }
}