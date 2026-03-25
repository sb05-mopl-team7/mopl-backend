# S3 접근을 OAC로 제한: CloudFront만 서명된 요청으로 접근
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project_name}-frontend-oac"
  description                       = "OAC for ${aws_s3_bucket.frontend.bucket}"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

data "terraform_remote_state" "compute" {
  backend = "local"

  config = {
    path = "${path.module}/../compute/terraform.tfstate"
  }
}

# 프론트엔드 정적 사이트용 CloudFront 배포
resource "aws_cloudfront_distribution" "front" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.project_name}-frontend"
  default_root_object = "index.html"

  #aliases = ["mopl.shop", "www.mopl.shop"]

  # S3 오리진(OAC 사용)
  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "s3-frontend-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  # ALB 오리진 (CloudFront -> ALB)
  origin {
    domain_name = "${var.alb_domain_name}"
    origin_id   = "alb-origin"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id       = "s3-frontend-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }
  }

  ordered_cache_behavior {
    path_pattern           = "/api/*"
    target_origin_id       = "alb-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    allowed_methods = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods  = ["GET", "HEAD", "OPTIONS"]

    forwarded_values {
      query_string = true  # API 파라미터 전달 허용

      cookies {
        forward = "all"    # 인증 쿠키(JSESSIONID 등) 전달 허용
      }

      # Header 전달이 필요하다면 (예: Authorization)
      headers = ["*"]
    }
  }

  ordered_cache_behavior {
    path_pattern     = "/assets/*"
    target_origin_id = "s3-frontend-origin" # S3 오리진 지정

    allowed_methods  = ["GET", "HEAD"]
    cached_methods   = ["GET", "HEAD"]

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
  }

  # WebSocket 또는 /ws 경로용 설정 (ALB 오리진으로 전달)
  ordered_cache_behavior {
    path_pattern           = "/ws/*"
    target_origin_id       = "alb-origin" # ALB를 바라보도록 설정
    viewer_protocol_policy = "redirect-to-https"

    allowed_methods = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods  = ["GET", "HEAD"]

    # WebSocket은 실시간 통신이므로 캐싱을 비활성화하는 것이 일반적입니다.
    forwarded_values {
      query_string = true

      cookies {
        forward = "all"
      }

      # WebSocket 연결을 위한 필수 헤더 전달
      headers = ["*"]
    }
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
