output "alb_dns_name" {
  value       = try(aws_lb.alb[0].dns_name, null)
  description = "ALB DNS (alb_enable=true일 때만 값 존재)"
}

output "api_target_group_arn" {
  value       = try(aws_lb_target_group.api_tg[0].arn, null)
  description = "기존 ECS api 서비스에 연결할 Target Group ARN"
}

output "chat_target_group_arn" {
  value       = try(aws_lb_target_group.chat_tg[0].arn, null)
  description = "기존 ECS chat 서비스에 연결할 Target Group ARN"
}

output "rds_endpoint" {
  value       = aws_db_instance.mysql.address
  description = "RDS endpoint"
}

output "db_secret_arn" {
  value       = aws_secretsmanager_secret.db.arn
  description = "Secrets Manager ARN(DB 계정/비번 저장)"
}

output "tools_instance_id" {
  value       = aws_instance.tools.id
  description = "Tools EC2 instance id"
}

output "tools_private_ip" {
  value       = aws_instance.tools.private_ip
  description = "Tools EC2 private ip (Kafka advertised listener 사용)"
}