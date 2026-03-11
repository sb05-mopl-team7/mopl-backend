# NS 확인 -> 가비아 네임서버 변경
output "route53_name_servers" {
  value = aws_route53_zone.main.name_servers
}

output "route53_zone_id" {
  value = aws_route53_zone.main.zone_id
}

# CloudWatch Log Group Names (ECS 용)
output "cloudwatch_log_group_api_name" {
  description = "API 모듈 로그 그룹 이름"
  value       = aws_cloudwatch_log_group.ecs_api.name
}

output "cloudwatch_log_group_batch_name" {
  description = "Batch 모듈 로그 그룹 이름"
  value       = aws_cloudwatch_log_group.ecs_batch.name
}

output "cloudwatch_log_group_chat_name" {
  description = "Chat 모듈 로그 그룹 이름"
  value       = aws_cloudwatch_log_group.ecs_chat.name
}