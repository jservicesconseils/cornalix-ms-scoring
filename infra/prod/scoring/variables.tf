variable "project" {
  type    = string
  default = "cornalix"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "aws_region" {
  type    = string
  default = "ca-central-1"
}

variable "service_name" {
  type    = string
  default = "scoring"
}

variable "container_port" {
  type    = number
  default = 8083
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "public_hostname" {
  type    = string
  default = "scoring.api.cornalix.ca"
}

variable "diagnostic_hostname" {
  description = "Nom d'hote public de cornalix-ms-diagnostic -- doit correspondre a public_hostname de infra/prod/diagnostic (cornalix-ms-diagnostic)"
  type        = string
  default     = "diagnostic.api.cornalix.ca"
}

variable "alb_rule_priority" {
  type    = number
  default = 103
}
