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