# resource "aws_route53_record" "mopl_shop" {
#   zone_id = data.aws_route53_zone.main.zone_id
#   name    = "mopl.shop"
#   type    = "A"
#
#   alias {
#     name                   = data.aws_cloudfront_distribution.front.domain_name
#     zone_id                = "Z2FDTNDATAQYW2"
#     evaluate_target_health = false
#   }
# }