variable "project" {
  description = "Name prefix for every resource."
  type        = string
  default     = "citizen-services"
}

variable "aws_region" {
  description = "AWS region. me-south-1 (Bahrain) is the closest to Saudi Arabia."
  type        = string
  default     = "me-south-1"
}

variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "image_tag" {
  description = "Tag of the container image in ECR to run."
  type        = string
  default     = "latest"
}

variable "desired_count" {
  description = "Number of API tasks. Set to 0 to stop paying for compute."
  type        = number
  default     = 1
}

variable "task_cpu" {
  type    = number
  default = 512
}

variable "task_memory" {
  type    = number
  default = 1024
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "expose_otp_code" {
  description = "Demo mode: return one-time codes in API responses. Never enable with real users."
  type        = bool
  default     = true
}

variable "certificate_arn" {
  description = "ACM certificate for HTTPS. Leave empty to serve plain HTTP (demo only)."
  type        = string
  default     = ""
}
