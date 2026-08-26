FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
# Download dependencies first so this layer is cached
RUN mvn dependency:go-offline -B
COPY src ./src
# Build application using the cached dependencies (allows downloading missing plugins/dependencies)
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-Xss256k", "-XX:+UseG1GC", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=64m", "-XX:+UseStringDeduplication", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
