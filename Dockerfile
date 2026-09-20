FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# Primero solo el pom: así las dependencias se cachean si el código cambia pero el pom no
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# No ejecutamos la app como root
RUN useradd --system --no-create-home fiumplan
COPY --from=builder /app/target/*.jar app.jar
USER fiumplan
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
