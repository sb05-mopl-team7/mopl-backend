# EC2 Instance
resource "aws_instance" "tools_broker" {
  ami           = "ami-03fd85ef2fae79c05"
  instance_type = "t3.micro"

  subnet_id              = aws_subnet.private.id
  vpc_security_group_ids = [aws_security_group.ec2.id]

  associate_public_ip_address = false
  key_name                    = var.ec2_key_name

  # SSM 접속용
  iam_instance_profile = aws_iam_instance_profile.tools_broker_ssm_profile.name

  tags = {
    Name = "${var.project_name}-ec2"
  }
}

# EC2에 접근한 SSM 접속용 IAM Role 생성
resource "aws_iam_role" "tools_broker_ssm_role" {

  name = "${var.project_name}-tools-broker-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })
}

# Role에 AmazonSSMManagedInstanceCore 권한 부여
resource "aws_iam_role_policy_attachment" "tools_broker_ssm_core" {
  role       = aws_iam_role.tools_broker_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "tools_broker_ssm_profile" {
  name = "${var.project_name}-${var.environment}-tools-broker-ssm-profile"
  role = aws_iam_role.tools_broker_ssm_role.name
}