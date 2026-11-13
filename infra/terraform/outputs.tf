output "service_url" {
  description = "URL publica del backend en Render."
  value       = render_web_service.backend.url
}

output "health_url" {
  description = "Healthcheck usado por Render."
  value       = "${render_web_service.backend.url}/actuator/health"
}
