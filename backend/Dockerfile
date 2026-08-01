# Build fat JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
    && mv target/opportunity-board-backend-*.jar target/app.jar

# Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
ENV PORT=8080
EXPOSE 8080
# CMD (not only ENTRYPOINT) so App Platform detects a default process
CMD ["java", "-jar", "app.jar"]
