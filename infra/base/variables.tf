variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project_name" {
  type    = string
  default = "mopl"
}

variable "alb_domain_name" {
  type = string
  default = "mopl-alb-896405909.ap-northeast-2.elb.amazonaws.com"
}