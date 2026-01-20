# VPC
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

# Security Group
output "security_group_id" {
  description = "Shared security group ID"
  value       = aws_security_group.main.id
}


# ALB
output "alb_dns_name" {
  description = "ALB DNS name (public endpoint)"
  value       = aws_lb.this.dns_name
}

output "alb_arn" {
  description = "ALB ARN"
  value       = aws_lb.this.arn
}


# ECS
output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  description = "ECS cluster ARN"
  value       = aws_ecs_cluster.main.arn
}

output "ecs_api_service_name" {
  description = "ECS API service name"
  value       = aws_ecs_service.api.name
}

output "ecs_chat_service_name" {
  description = "ECS Chat service name"
  value       = aws_ecs_service.chat.name
}


# CloudWatch
output "cloudwatch_log_group_api" {
  description = "CloudWatch log group for API"
  value       = aws_cloudwatch_log_group.ecs_api.name
}

output "cloudwatch_log_group_chat" {
  description = "CloudWatch log group for Chat"
  value       = aws_cloudwatch_log_group.ecs_chat.name
}

output "cloudwatch_log_group_batch" {
  description = "CloudWatch log group for Batch"
  value       = aws_cloudwatch_log_group.ecs_batch.name
}
