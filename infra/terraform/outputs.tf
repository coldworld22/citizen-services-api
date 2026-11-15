output "api_url" {
  description = "Base URL of the API."
  value       = "${local.https_enabled ? "https" : "http"}://${aws_lb.main.dns_name}"
}

output "swagger_url" {
  value = "${local.https_enabled ? "https" : "http"}://${aws_lb.main.dns_name}/swagger-ui.html"
}

output "ecr_repository_url" {
  value = aws_ecr_repository.api.repository_url
}

output "ecs_cluster" {
  value = aws_ecs_cluster.main.name
}

output "github_deploy_role_arn" {
  description = "Set as the AWS_DEPLOY_ROLE_ARN repository variable in GitHub."
  value       = local.github_deploy_enabled ? aws_iam_role.github_deploy[0].arn : null
}
