# EC2 Instance
resource "aws_instance" "redis" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.small"

  subnet_id              = aws_subnet.private_a.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false

  # SSM 접속용
  iam_instance_profile = aws_iam_instance_profile.ec2_ssm_profile.name

  root_block_device {
    volume_size = 30 # 최소 20~30GB 추천
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project_name}-ec2-redis"
    Role = "Redis"
  }
}

resource "aws_instance" "kafka" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.small"

  subnet_id              = aws_subnet.private_a.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false

  # SSM 접속용
  iam_instance_profile = aws_iam_instance_profile.ec2_ssm_profile.name

  root_block_device {
    volume_size = 30 # 최소 20~30GB 추천
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project_name}-ec2-kafka"
    Role = "Kafka"
  }
}

resource "aws_instance" "monitoring" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.small"

  subnet_id              = aws_subnet.private_a.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false

  # SSM 접속용
  iam_instance_profile = aws_iam_instance_profile.ec2_ssm_profile.name

  root_block_device {
    volume_size = 30 # 최소 20~30GB 추천
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project_name}-ec2-monitoring"
  }
}

# -----------------------------------------------------------------------------------------------

# EC2에 Role 적용
resource "aws_iam_instance_profile" "ec2_ssm_profile" {
  name = "${var.project_name}-${var.environment}-ec2-ssm-profile"
  role = aws_iam_role.ec2_ssm_role.name
}