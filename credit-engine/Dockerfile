# =============================================================================
# Multi-stage build — mantém a imagem final enxuta (só o JAR, sem Maven/SDK)
# =============================================================================

# Stage 1: build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia o wrapper Maven e o pom.xml primeiro para aproveitar o cache de layers
# (as dependências só são re-baixadas se o pom.xml mudar)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixa dependências sem compilar o código (cache eficiente)
RUN ./mvnw dependency:go-offline -q

# Copia o código-fonte e compila
COPY src src
RUN ./mvnw package -DskipTests -q

# =============================================================================
# Stage 2: imagem final — apenas JRE + JAR
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S srm && adduser -S srm -G srm
USER srm

# Copia apenas o JAR gerado no stage anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
