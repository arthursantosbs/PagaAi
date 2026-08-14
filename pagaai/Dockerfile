# ---------- build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Baixa as dependencias em uma camada separada: so refaz quando o pom muda.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# maven.test.skip pula ate a COMPILACAO dos testes, diferente de -DskipTests,
# que compila e nao roda. O container nao precisa deles — os testes rodam na
# maquina de quem desenvolve — e isso deixa o build mais curto e mais leve.
RUN mvn -B -q -Dmaven.test.skip=true package

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

# ---------------------------------------------------------------------------
# Ajuste de memoria para container pequeno (o plano gratuito do Render da 512 MB).
#
# A versao anterior usava so MaxRAMPercentage=75 e o processo era morto com
# status 137 (OOM) antes de abrir a porta: 384 MB de heap deixavam ~128 MB para
# metaspace, pilhas de thread e a propria JVM, que precisa de mais que isso.
#
#   MaxRAMPercentage=55   ~280 MB de heap, sobra folga para o resto do processo
#   MaxMetaspaceSize      teto explicito; o Spring carrega muita classe
#   UseSerialGC           o G1 reserva bem mais memoria; para 2 usuarios o
#                         coletor serial e suficiente e bem mais economico
#   Xss512k               pilha menor por thread (o padrao e 1 MB)
#   TieredStopAtLevel=1   so o compilador rapido: sobe antes e usa menos memoria,
#                         em troca de desempenho de pico que este app nao precisa
#   jmx.enabled=false     desliga o JMX, que ninguem consome aqui
# ---------------------------------------------------------------------------
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -Dspring.jmx.enabled=false"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
