# Build stage: compiles the jar inside Docker, so neither the server nor your PC needs Maven for the image
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Dependencies first, so they stay cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime stage: JRE only
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/uploads

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8081 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
