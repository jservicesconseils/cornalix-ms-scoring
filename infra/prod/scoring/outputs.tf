output "target_group_arn" {
  value = aws_lb_target_group.scoring.arn
}

output "ecs_service_name" {
  value = aws_ecs_service.scoring.name
}
