resource "random_bytes" "jwt_key" {
  length = 32
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.project}/jwt-secret"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_bytes.jwt_key.base64
}
