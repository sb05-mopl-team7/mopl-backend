# EC2 Instance
resource "aws_instance" "tools_broker" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.small"

  subnet_id              = aws_subnet.private_a.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false

  # SSM 접속용
  iam_instance_profile = aws_iam_instance_profile.tools_broker_ssm_profile.name

  root_block_device {
    volume_size = 30 # 최소 20~30GB 추천
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project_name}-ec2"
  }
}