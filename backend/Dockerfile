FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/service-marketplace-1.0.0.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
