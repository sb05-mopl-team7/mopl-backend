# Application Load Balancer
resource "aws_lb" "this" {
  name               = "${var.project_name}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.main.id]

  subnets            = [aws_subnet.public.id]

  tags = {
    Name = "${var.project_name}-alb"
    Env  = var.environment
  }
}

# Route53
resource "aws_route53_zone" "main" {
  name = "mopl.shop"

  tags = {
    Name = "${var.project_name}-zone"
    Env  = var.environment
  }
}

resource "aws_route53_record" "alb" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "mopl.shop"
  type    = "A"

  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = true
  }
}

# ACM Certificate
resource "aws_acm_certificate" "this" {
  domain_name       = "mopl.shop"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project_name}-cert"
    Env  = var.environment
  }
}

# HTTP Listener (Redirect -> HTTPS)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# HTTPS Listener (Default -> API)
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = aws_acm_certificate.this.arn

  # 기본은 API로 보냄 ("/" 같은 요청)
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

########################################
# Target Groups (서비스별 포트 분리)
########################################

resource "aws_lb_target_group" "api" {
  name        = "${var.project_name}-tg-api"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  tags = {
    Name = "${var.project_name}-tg-api"
    Env  = var.environment
  }
}

resource "aws_lb_target_group" "chat" {
  name        = "${var.project_name}-tg-chat"
  port        = 8081
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  tags = {
    Name = "${var.project_name}-tg-chat"
    Env  = var.environment
  }
}

resource "aws_lb_target_group" "batch" {
  name        = "${var.project_name}-tg-batch"
  port        = 8082
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  tags = {
    Name = "${var.project_name}-tg-batch"
    Env  = var.environment
  }
}

########################################
# Listener Rules (경로 기반 라우팅)
########################################

# /api/* -> 8080
resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

# /chat/* -> 8081
resource "aws_lb_listener_rule" "chat" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 20

  condition {
    path_pattern {
      values = ["/chat/*"]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.chat.arn
  }
}
