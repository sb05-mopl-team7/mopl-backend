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

variable "domain_name" {}

variable "vpc_id" {
  type        = string
  description = "기존 VPC ID"
}

variable "api_container_port" {
  type    = number
  default = 8080
}

variable "db_name" {
  type    = string
  default = "mopl"
}

variable "db_username" {
  type    = string
  default = "mopl_admin"
}

variable "db_password" {
  type        = string
  description = "Database master password"
  sensitive   = true
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
