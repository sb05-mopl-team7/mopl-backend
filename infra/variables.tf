variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project_name" {
  type    = string
  default = "mopl"
}

# -----------------------------
# 기존 리소스 참조(필수)
# -----------------------------
variable "vpc_id" {
  type        = string
  description = "기존 VPC ID"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "기존 Public Subnet ID 목록(ALB용)"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "기존 Private Subnet ID 목록(RDS/EC2용)"
}

variable "ecs_tasks_sg_id" {
  type        = string
  description = "기존 ECS Tasks가 사용하는 Security Group ID (ALB->ECS 인바운드 허용 및 내부 접근용)"
}

# -----------------------------
# ALB
# -----------------------------
variable "alb_enable" {
  type    = bool
  default = true
}

variable "alb_ingress_cidrs" {
  type        = list(string)
  description = "ALB 인바운드 허용 CIDR 목록(기본 전체 오픈). 운영은 회사 IP로 제한 권장"
  default     = ["0.0.0.0/0"]
}

variable "api_container_port" {
  type    = number
  default = 8080
}

variable "chat_container_port" {
  type    = number
  default = 8080
}

variable "api_healthcheck_path" {
  type    = string
  default = "/actuator/health"
}

variable "chat_healthcheck_path" {
  type    = string
  default = "/actuator/health"
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