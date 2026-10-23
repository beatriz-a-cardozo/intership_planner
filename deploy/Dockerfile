# syntax=docker/dockerfile:1
# Imagem única de deploy (Render): o backend serve a API e o frontend compilado no mesmo domínio.
# Contexto = raiz do repositório:  docker build -f deploy/Dockerfile -t interchange-planner .
# (Para desenvolvimento continue usando o docker-compose.yml, com o frontend no nginx.)

FROM node:22-alpine AS frontend
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_APP_NAME="Interchange Planner"
# Vazio = mesma origem (/api)
ENV VITE_APP_NAME=$VITE_APP_NAME VITE_API_BASE_URL=""
RUN npm run build

FROM eclipse-temurin:21-jdk-alpine AS backend
WORKDIR /app
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q dependency:go-offline
COPY backend/src/ src/
# O frontend entra como recursos estáticos do Spring (ativa o SpaConfig)
COPY --from=frontend /web/dist/ src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend /app/target/extracted/dependencies/ ./
COPY --from=backend /app/target/extracted/spring-boot-loader/ ./
COPY --from=backend /app/target/extracted/snapshot-dependencies/ ./
COPY --from=backend /app/target/extracted/application/ ./
USER app
# A plataforma informa a porta em PORT (application.yml: server.port=${PORT:8080})
EXPOSE 8080
# Planos gratuitos têm ~512 MB e CPU fracionada: heap limitado, GC serial e JIT de 1 nível para subir mais rápido
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
