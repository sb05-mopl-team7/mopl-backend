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

# [1] 소유권 인증용 TXT 레코드 (문제가 해결될 때까지 임시로 필요)
resource "aws_route53_record" "cloudfront_verification" {
  zone_id = aws_route53_zone.main.zone_id #
  name    = "_cloudfront" # 자동으로 _cloudfront.mopl.shop이 됩니다.
  type    = "TXT"
  ttl     = 300
  records = ["d1ocfp6g80vipy.cloudfront.net"]
}

# [2] 실제 서비스용 mopl.shop 레코드 (Alias 사용)
resource "aws_route53_record" "root_domain" {
  zone_id = aws_route53_zone.main.zone_id #
  name    = "mopl.shop"
  type    = "A"

  alias {
    name                   = "d1ocfp6g80vipy.cloudfront.net" # 내 CloudFront 주소
    zone_id                = "Z2FDTNDATAQYW2" # CloudFront의 고정 Hosted Zone ID
    evaluate_target_health = false
  }
}

# [3] 실제 서비스용 www.mopl.shop 레코드
resource "aws_route53_record" "www_domain" {
  zone_id = aws_route53_zone.main.zone_id #
  name    = "www"
  type    = "A"

  alias {
    name                   = "d1ocfp6g80vipy.cloudfront.net"
    zone_id                = "Z2FDTNDATAQYW2"
    evaluate_target_health = false
  }
}