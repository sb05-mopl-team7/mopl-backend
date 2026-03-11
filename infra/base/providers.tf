terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.17.0"
    }
  }
}

# 디폴트
provider "aws" {
  region = var.aws_region
}

# CloudFront용 ACM 인증서는 us-east-1 리전에 있어야 함
provider "aws" {
  alias  = "use1"
  region = "us-east-1"
}
