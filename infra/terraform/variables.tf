variable "render_api_key" {
  description = "API key de Render. Vacio = se toma RENDER_API_KEY del entorno."
  type        = string
  sensitive   = true
  default     = null
}

variable "repo_url" {
  description = "URL del repositorio publico que Render clona y compila."
  type        = string
}

variable "branch" {
  description = "Rama a desplegar."
  type        = string
  default     = "main"
}

variable "service_name" {
  description = "Nombre del web service en Render."
  type        = string
  default     = "acento-api"
}

variable "plan" {
  description = "Plan de Render: free para la demo."
  type        = string
  default     = "free"
}

variable "region" {
  description = "Region de Render: oregon, ohio, virginia, frankfurt o singapore."
  type        = string
  default     = "oregon"
}

variable "auto_deploy" {
  description = "Redesplegar en cada push a la rama."
  type        = bool
  default     = true
}

variable "frontend_origin" {
  description = "Origen del frontend en Netlify, por ejemplo https://acento.netlify.app."
  type        = string
}

variable "database_url" {
  description = "URL reactiva de Neon: r2dbc:postgresql://host/db?sslmode=require."
  type        = string
  sensitive   = true
}

variable "jdbc_url" {
  description = "URL JDBC de Neon para Flyway: jdbc:postgresql://host/db?sslmode=require."
  type        = string
  sensitive   = true
}

variable "database_user" {
  description = "Usuario de la base en Neon."
  type        = string
  sensitive   = true
}

variable "database_password" {
  description = "Contrasena de la base en Neon."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Secreto HS256, minimo 32 caracteres."
  type        = string
  sensitive   = true
}

variable "admin_email" {
  description = "Correo del administrador inicial."
  type        = string
}

variable "admin_password" {
  description = "Contrasena del administrador inicial: 8+, un numero y un simbolo."
  type        = string
  sensitive   = true
}
