variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project_name" {
  type    = string
  default = "mopl"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "prod"
}

variable "api_image_uri" {
  type        = string
  description = "ECR image URI for ECS API task (e.g. repo:release-7)"
}

variable "chat_image_uri" {
  type        = string
  description = "ECR image URI for ECS API task (e.g. repo:release-7)"
}

variable "batch_image_uri" {
  type        = string
  description = "ECR image URI for ECS API task (e.g. repo:release-7)"
}

variable "acm_certificate_arn" {
  type        = string
  description = "ACM certificate ARN for ALB HTTPS listener"
}