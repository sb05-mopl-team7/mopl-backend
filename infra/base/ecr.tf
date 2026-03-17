locals {
  ecr_repositories = ["api", "batch", "chat"]
}

resource "aws_ecr_repository" "mopl-repos" {
  for_each = toset(local.ecr_repositories)
  name = "${var.project_name}-${each.value}"
  image_tag_mutability = "MUTABLE"

  tags = {
    Project = "Mopl"
    Service = each.key
  }

  # lifecycle {
  #   prevent_destroy = true
  # }
}