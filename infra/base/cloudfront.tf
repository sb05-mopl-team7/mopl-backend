# S3 접근을 OAC로 제한: CloudFront만 서명된 요청으로 접근
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project_name}-frontend-oac"
  description                       = "OAC for ${aws_s3_bucket.frontend.bucket}"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# 프론트엔드 정적 사이트용 CloudFront 배포
resource "aws_cloudfront_distribution" "front" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.project_name}-frontend"
  default_root_object = "index.html"


  # S3 오리진(OAC 사용)
  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "s3-frontend-origin"
    origin_access_control_id = "E1VZAEF7USP49X"
  }

  # 기본 캐시 정책: 정적 파일 중심, HTTPS 강제
  default_cache_behavior {
    target_origin_id       = "s3-frontend-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    allowed_methods = ["GET", "HEAD", "OPTIONS"]
    cached_methods  = ["GET", "HEAD", "OPTIONS"]

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }
  }

  # 자산 경로 캐시 정책(필요 시 TTL/헤더 정책 분리 가능)
  ordered_cache_behavior {
    path_pattern           = "/assets/*"
    target_origin_id       = "s3-frontend-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    allowed_methods = ["GET", "HEAD", "OPTIONS"]
    cached_methods  = ["GET", "HEAD", "OPTIONS"]

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }
  }

  # SPA 라우팅 지원: 403/404 -> index.html
  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 0
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 0
  }

  # 지역 제한 없음
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # 커스텀 도메인용 ACM 인증서 연결(us-east-1)
  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.main.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  # 요금 등급(북미/유럽/아시아 일부)
  price_class = "PriceClass_200"

  tags = {
    Name = "${var.project_name}-front"
  }
}

# CloudFront에서만 S3 읽기 허용(OAC + SourceArn 조건)
resource "aws_s3_bucket_policy" "frontend_cloudfront" {
  bucket = aws_s3_bucket.frontend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontServicePrincipalReadOnly"
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = ["s3:GetObject"]
        Resource = "${aws_s3_bucket.frontend.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.front.arn
          }
        }
      }
    ]
  })
}
