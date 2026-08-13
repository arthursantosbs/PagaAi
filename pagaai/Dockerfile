# ---------- build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Baixa as dependencias em uma camada separada: so refaz quando o pom muda.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:17-jre

# curl serve para o healthcheck do docker compose.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Nao roda como root.
RUN groupadd --system pagaai && useradd --system --gid pagaai pagaai
WORKDIR /app

COPY --from=build /build/target/paga-ai-*.jar app.jar
RUN chown -R pagaai:pagaai /app
USER pagaai

# A hospedagem normalmente injeta PORT; 8080 e o padrao.
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
# Respeita o limite de memoria do container em vez de assumir a RAM da maquina.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
