resource "aws_kms_key" "secrets" {
  description             = "PCIS Secrets Manager CMK (${var.environment_name})"
  deletion_window_in_days = 30
  enable_key_rotation     = true

  tags = merge(local.base_tags, {
    Name = "pcis-secrets-${var.environment_name}"
  })
}

resource "aws_kms_alias" "secrets" {
  name          = "alias/pcis-secrets-${var.environment_name}"
  target_key_id = aws_kms_key.secrets.key_id
}
