resource "aws_s3_bucket" "image" {
  bucket = "codeit-team7-${var.project_name}-image"

  tags = {
    Name = "${var.project_name}-image"
  }
}

# 서버 측 암호화(SSE-S3) 적용: 저장 데이터 암호화 기본값 설정
resource "aws_s3_bucket_server_side_encryption_configuration" "image" {
  bucket = aws_s3_bucket.image.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 퍼블릭 액세스 전면 차단
resource "aws_s3_bucket_public_access_block" "image" {
  bucket = aws_s3_bucket.image.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
