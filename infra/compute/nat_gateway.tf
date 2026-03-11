resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = data.terraform_remote_state.base.outputs.public_subnet_ids[0]

  tags = {
    Name = "${var.project_name}-nat-gateway"
    Env  = var.environment
  }

  depends_on = [aws_internet_gateway.main]
}

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-nat-eip"
    Env  = var.environment
  }
}
