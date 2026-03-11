
# Private DNS 네임 스페이스 생성
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "mopl.local"
  description = "Service discovery for MOPL services"
  vpc         = aws_vpc.main.id
}

# API 서비스용 디스커버리 등록 = Cloud Map
resource "aws_service_discovery_service" "api" {
  name = "api" # 호출 주소: api.mopl.local
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A" # A 레코드 (도메인 -> IP 변환)
    }
    routing_policy = "MULTIVALUE"
  }
}

# Chat 서비스용 디스커버리 등록
resource "aws_service_discovery_service" "chat" {
  name = "chat" # 호출 주소: chat.mopl.local
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}