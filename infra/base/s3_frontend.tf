# 프론트엔드 정적 자산 저장용 S3 버킷
resource "aws_s3_bucket" "frontend" {
  bucket = "codeit-team7-${var.project_name}-frontend"

  tags = {
    Name = "${var.project_name}-frontend"
  }
}

# 서버 측 암호화(SSE-S3) 적용: 저장 데이터 암호화 기본값 설정
resource "aws_s3_bucket_server_side_encryption_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 퍼블릭 액세스 전면 차단
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
