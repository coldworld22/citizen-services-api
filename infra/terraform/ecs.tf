resource "aws_ecr_repository" "api" {
  name                 = var.project
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "api" {
  repository = aws_ecr_repository.api.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the 10 most recent images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${var.project}"
  retention_in_days = 14
}

resource "aws_ecs_cluster" "main" {
  name = var.project

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# Used by ECS itself to pull the image, write logs and read the two secrets the task needs.
resource "aws_iam_role" "execution" {
  name               = "${var.project}-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions = ["secretsmanager:GetSecretValue"]
    resources = [
      aws_secretsmanager_secret.jwt.arn,
      aws_db_instance.main.master_user_secret[0].secret_arn,
    ]
  }
}

resource "aws_iam_role_policy" "execution_secrets" {
  name   = "read-app-secrets"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.read_secrets.json
}

# The application's own role. It needs no AWS permissions today.
resource "aws_iam_role" "task" {
  name               = "${var.project}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

locals {
  db_secret_arn = aws_db_instance.main.master_user_secret[0].secret_arn
}

resource "aws_ecs_task_definition" "api" {
  family                   = var.project
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name         = "api"
    image        = "${aws_ecr_repository.api.repository_url}:${var.image_tag}"
    essential    = true
    portMappings = [{ containerPort = 8080, protocol = "tcp" }]
    environment = [
      { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.main.endpoint}/${aws_db_instance.main.db_name}" },
      { name = "OTP_EXPOSE_CODE", value = tostring(var.expose_otp_code) },
    ]
    secrets = [
      { name = "DB_USERNAME", valueFrom = "${local.db_secret_arn}:username::" },
      { name = "DB_PASSWORD", valueFrom = "${local.db_secret_arn}:password::" },
      { name = "JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt.arn },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.api.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "api"
      }
    }
  }])
}

resource "aws_ecs_service" "api" {
  name            = "api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  health_check_grace_period_seconds = 90

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.api.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "api"
    container_port   = 8080
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.http]
}
