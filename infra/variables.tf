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

# -----------------------------
# 기존 리소스 참조(필수)
# -----------------------------
variable "vpc_id" {
  type        = string
  description = "기존 VPC ID"
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway"
  type        = bool
  default     = false
}

# -----------------------------
# ALB
# -----------------------------
variable "alb_enable" {
  type    = bool
  default = true
}

variable "api_container_port" {
  type    = number
  default = 8080
}

# -----------------------------
# RDS(MySQL)
# -----------------------------
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

variable "db_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}

variable "db_multi_az" {
  type    = bool
  default = false
}

variable "db_deletion_protection" {
  type    = bool
  default = false
}

# -----------------------------
# EC2 Tools (Prometheus/Grafana + Redis + Kafka)
# -----------------------------
variable "tools_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "grafana_admin_password" {
  type        = string
  sensitive   = true
  description = "Grafana 관리자 비밀번호(주의: user_data에 포함되므로 Terraform state에 남을 수 있음)"
}

# -----------------------------
# 태그
# -----------------------------
variable "tags" {
  type    = map(string)
  default = {}
}

variable "admin_ip_cidr" {
  description = "Admin IP for SSH access"
  type        = string
}

variable "ec2_ami_id" {
  description = "AMI ID for EC2"
  type        = string
}

variable "ec2_key_name" {
  description = "EC2 key pair name"
  type        = string
}

# ECS
variable "ecs_task_cpu" {
  description = "Fargate task CPU units to provision (1 vCPU = 1024 CPU units)"
  type        = number
  default     = 256
}

variable "api_security_group_id" {
  type        = string
  description = "Security Group ID attached to ECS API tasks"
}

variable "api_target_group_arn" {
  type        = string
  description = "ALB Target Group ARN for ECS API service"
}

variable "api_task_cpu" {
  type        = number
  description = "CPU units for ECS API task (e.g. 256, 512, 1024)"
}

variable "api_task_memory" {
  type        = number
  description = "Memory (MB) for ECS API task (e.g. 512, 1024, 2048)"
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
