# 호스트 생성
resource "aws_route53_zone" "main" {
  name = "mopl.shop"

  tags = {
    Name = "mopl-shop"
    Env  = "prod"
  }
}

# ACM 인증서
resource "aws_acm_certificate" "main" {
  provider = aws.use1

  domain_name       = "mopl.shop"
  subject_alternative_names = ["*.mopl.shop"]
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "mopl-cert"
    Env  = "prod"
  }
}

# ACM 검증용 CNAME 레코드 생성
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.main.domain_validation_options :
    dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id = aws_route53_zone.main.zone_id
  name    = each.value.name
  type    = each.value.type
  ttl     = 60
  records = [each.value.record]
  allow_overwrite = true
}

# ACM 인증서 DNS 검증 완료 대기
resource "aws_acm_certificate_validation" "main" {
  provider = aws.use1

  certificate_arn         = aws_acm_certificate.main.arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}
