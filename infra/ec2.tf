############################
# EC2 Security Group
############################
resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-ec2-sg"
  description = "Security group for EC2 (monitoring / infra)"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "SSH access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_ip_cidr]
  }

  ingress {
    description = "Monitoring ports"
    from_port   = 9090
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.this.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ec2-sg"
    Env  = var.environment
  }
}

############################
# EC2 Instance
############################
resource "aws_instance" "this" {
  ami           = var.ec2_ami_id
  instance_type = "t3.micro"

  subnet_id              = aws_subnet.private.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false
  key_name                    = var.ec2_key_name

  tags = {
    Name = "${var.project_name}-ec2"
    Env  = var.environment
  }
}
