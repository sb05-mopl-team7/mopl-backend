# EC2 Instance
resource "aws_instance" "tools_broker" {
  ami           = "ami-03fd85ef2fae79c05"
  instance_type = "t3.micro"

  subnet_id              = aws_subnet.private_a.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false

  # SSM 접속용
  iam_instance_profile = aws_iam_instance_profile.tools_broker_ssm_profile.name

  tags = {
    Name = "${var.project_name}-ec2"
  }
}