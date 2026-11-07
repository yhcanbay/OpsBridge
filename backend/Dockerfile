FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S opsbridge && adduser -S opsbridge -G opsbridge
COPY --from=build --chown=opsbridge:opsbridge /app/target/*.jar app.jar
USER opsbridge
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
