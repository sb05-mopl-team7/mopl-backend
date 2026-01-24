# Application Load Balancer
resource "aws_lb" "this" {
  name               = "${var.project_name}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]

  subnets            = [aws_subnet.public_a.id, aws_subnet.public_c.id]

  tags = {
    Name = "${var.project_name}-alb"
    Env  = var.environment
  }
}

# HTTP Listener (Direct -> API)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}



########################################


# Target Groups (서비스별 포트 분리)

resource "aws_lb_target_group" "api" {
  name        = "${var.project_name}-tg-api"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30    # 20 초마다 검사
    timeout             = 15    # 10초 안에 200 OK 해야함
    healthy_threshold   = 2     # 2번 연속 성공
    unhealthy_threshold = 3     # 3번: 안 될 놈은 빨리 실패 처리
  }

  tags = {
    Name = "${var.project_name}-tg-api"
    Env  = var.environment
  }
}

resource "aws_lb_target_group" "batch" {
  name        = "${var.project_name}-tg-batch"
  port        = 8081
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 35
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }

  tags = {
    Name = "${var.project_name}-tg-batch"
    Env  = var.environment
  }
}

resource "aws_lb_target_group" "chat" {
  name        = "${var.project_name}-tg-chat"
  port        = 8082
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 35
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }

  tags = {
    Name = "${var.project_name}-tg-chat"
    Env  = var.environment
  }
}



########################################
# Listener Rules (경로 기반 라우팅)
########################################

# /api/* -> 8080
resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.http.arn
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

# /chat/* -> 8082
resource "aws_lb_listener_rule" "chat" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 20

  condition {
    path_pattern {
      values = ["/ws/*"]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.chat.arn
  }
}
