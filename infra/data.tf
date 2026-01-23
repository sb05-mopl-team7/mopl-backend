# 운영 DB 비밀번호
data "aws_ssm_parameter" "db_password" {
  name            = "/mopl/prod/db/password"
  with_decryption = true
}

data "aws_ssm_parameter" "gmail_password" {
  name = "/mopl/prod/gmail/password"
  with_decryption = true
}

data "aws_ssm_parameter" "access_secret" {
  name = "/mopl/prod/jwt/access_secret"
  with_decryption = true
}

data "aws_ssm_parameter" "refresh_secret" {
  name = "/mopl/prod/jwt/refresh_secret"
  with_decryption = true
}

data "aws_ssm_parameter" "aws_access_key" {
  name = "/mopl/prod/aws_access_key"
  with_decryption = true
}

data "aws_ssm_parameter" "aws_refresh_key" {
  name = "/mopl/prod/aws_refresh_key"
  with_decryption = true
}

data "aws_ssm_parameter" "aws_region" {
  name = "/mopl/prod/aws_region"
  with_decryption = true
}

data "aws_ssm_parameter" "aws_s3_bucket" {
  name = "/mopl/prod/aws_s3_bucket"
  with_decryption = true
}

data "aws_ssm_parameter" "tmdb_api_token" {
  name = "/mopl/prod/tmdb_api_token"
  with_decryption = true
}

data "aws_route53_zone" "main" {
  name = "mopl.shop"
}

data "aws_caller_identity" "current" {}

# 최신 Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-x86_64"]
  }
}

data "aws_cloudfront_distribution" "front" {
  id = "E263BP4SCXEFZR"
}