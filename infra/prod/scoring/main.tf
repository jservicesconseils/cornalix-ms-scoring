terraform {
  required_version = ">= 1.7.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Ressources partagees (VPC, cluster ECS, ALB, role d'execution, ECR...)
# creees par infra/prod/platform (SCRUM-34/35/36, repo cornalix-ms-identity)
# -- ce stack ne fait que les lire, jamais les modifier.
data "terraform_remote_state" "platform" {
  backend = "s3"

  config = {
    bucket = "cornalix-tfstate-591859078355"
    key    = "prod/platform/terraform.tfstate"
    region = "ca-central-1"
  }
}

locals {
  platform = data.terraform_remote_state.platform.outputs
}

############################################
# Task definition -- pas de datasource propre (ce service calcule le
# score a la volee en appelant cornalix-ms-diagnostic, voir SCRUM-25),
# donc pas de secrets RDS a injecter, juste DIAGNOSTIC_BASE_URL.
############################################
resource "aws_ecs_task_definition" "scoring" {
  family                   = "${var.project}-${var.environment}-${var.service_name}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = local.platform.ecs_task_execution_role_arn

  container_definitions = jsonencode([
    {
      name      = var.service_name
      image     = "${local.platform.ecr_repository_urls[var.service_name]}:${var.image_tag}"
      essential = true

      portMappings = [{
        containerPort = var.container_port
        protocol      = "tcp"
      }]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "CORNALIX_CORS_ALLOWED_ORIGINS", value = "https://cornalix.ca" },
        # Appel serveur-a-serveur vers diagnostic -- ne resoudra/ne
        # fonctionnera qu'une fois le domaine cornalix.ca branche
        # (SCRUM-38), le routage de l'ALB se fait par en-tete Host.
        { name = "DIAGNOSTIC_BASE_URL", value = "https://${var.diagnostic_hostname}" },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = local.platform.ecs_log_group_name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = var.service_name
        }
      }
    }
  ])

  tags = {
    Name = "${var.project}-${var.environment}-${var.service_name}"
  }
}

############################################
# Target group + regle d'ecoute ALB (routage par nom d'hote, SCRUM-36)
############################################
resource "aws_lb_target_group" "scoring" {
  name        = "${var.project}-${var.environment}-${var.service_name}"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = local.platform.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
    matcher             = "200"
  }

  tags = {
    Name = "${var.project}-${var.environment}-${var.service_name}"
  }
}

resource "aws_lb_listener_rule" "scoring" {
  listener_arn = local.platform.alb_http_listener_arn
  priority     = var.alb_rule_priority

  condition {
    host_header {
      values = [var.public_hostname]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.scoring.arn
  }
}

############################################
# Service ECS
############################################
resource "aws_ecs_service" "scoring" {
  name            = "${var.project}-${var.environment}-${var.service_name}"
  cluster         = local.platform.ecs_cluster_id
  task_definition = aws_ecs_task_definition.scoring.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = local.platform.public_subnet_ids
    security_groups  = [local.platform.ecs_tasks_security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.scoring.arn
    container_name    = var.service_name
    container_port    = var.container_port
  }

  depends_on = [aws_lb_listener_rule.scoring]
}
