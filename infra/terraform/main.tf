resource "render_web_service" "backend" {
  name              = var.service_name
  plan              = var.plan
  region            = var.region
  health_check_path = "/actuator/health"

  runtime_source = {
    docker = {
      branch          = var.branch
      repo_url        = var.repo_url
      auto_deploy     = var.auto_deploy
      context         = "./backend"
      dockerfile_path = "./backend/Dockerfile"
    }
  }

  env_vars = {
    SPRING_PROFILES_ACTIVE = { value = "prod" }
    DATABASE_URL           = { value = var.database_url }
    JDBC_URL               = { value = var.jdbc_url }
    DATABASE_USER          = { value = var.database_user }
    DATABASE_PASSWORD      = { value = var.database_password }
    JWT_SECRET             = { value = var.jwt_secret }
    TOKEN_TTL              = { value = "PT30M" }
    COOKIE_SECURE          = { value = "true" }
    ADMIN_EMAIL            = { value = var.admin_email }
    ADMIN_PASSWORD         = { value = var.admin_password }
    CORS_ALLOWED_ORIGINS   = { value = var.frontend_origin }
    FLYWAY_ENABLED         = { value = "true" }
    JAVA_TOOL_OPTIONS      = { value = "-XX:MaxRAMPercentage=60 -Djava.io.tmpdir=/tmp/franchises" }
  }
}
