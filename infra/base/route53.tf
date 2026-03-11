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
}

# ACM 인증서가 DNS 검증을 기다림
resource "aws_acm_certificate_validation" "main" {
  provider = aws.use1

  certificate_arn = aws_acm_certificate.main.arn

  validation_record_fqdns = [
    for record in aws_route53_record.cert_validation : record.fqdn
  ]
}

# CloudFront Alias 레코드 (A)
resource "aws_route53_record" "frontend_alias_a" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "mopl.shop"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.front.domain_name
    zone_id                = aws_cloudfront_distribution.front.hosted_zone_id
    evaluate_target_health = false
  }
}

# CloudFront Alias 레코드 (AAAA)
resource "aws_route53_record" "frontend_alias_aaaa" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "mopl.shop"
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.front.domain_name
    zone_id                = aws_cloudfront_distribution.front.hosted_zone_id
    evaluate_target_health = false
  }
}

# www 서브도메인 Alias 레코드 (A)
resource "aws_route53_record" "frontend_www_alias_a" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "www.mopl.shop"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.front.domain_name
    zone_id                = aws_cloudfront_distribution.front.hosted_zone_id
    evaluate_target_health = false
  }
}

# www 서브도메인 Alias 레코드 (AAAA)
resource "aws_route53_record" "frontend_www_alias_aaaa" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "www.mopl.shop"
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.front.domain_name
    zone_id                = aws_cloudfront_distribution.front.hosted_zone_id
    evaluate_target_health = false
  }
}
