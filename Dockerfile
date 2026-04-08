FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN mvn -q package -DskipTests
RUN mv target/bankcards-*.jar /app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
