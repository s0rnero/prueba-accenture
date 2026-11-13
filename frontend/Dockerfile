# syntax=docker/dockerfile:1.7
# Etapa de dependencias con Bun, que respeta bun.lock.
FROM oven/bun:1.3 AS dependencies
# Directorio de trabajo de la instalacion.
WORKDIR /app
# Copia solo los manifiestos para cachear la instalacion.
COPY package.json bun.lock ./
# Instala dependencias exactas con la cache de Bun montada.
RUN --mount=type=cache,id=pa-bun-cache,target=/root/.bun/install/cache \
    bun install --frozen-lockfile

# Etapa del dev server: Node real, porque el CLI de Angular 22 pide Node >= 24.15.
FROM node:26-alpine AS development
# Directorio de trabajo del dev server.
WORKDIR /app
# Reutiliza los node_modules instalados por Bun.
COPY --from=dependencies /app/node_modules ./node_modules
# Copia el codigo del frontend.
COPY . .
# Puerto del dev server de Vite.
EXPOSE 4200
# Levanta el dev server escuchando en todas las interfaces.
CMD ["node", "node_modules/@angular/cli/bin/ng.js", "serve", "--host", "0.0.0.0", "--port", "4200"]
