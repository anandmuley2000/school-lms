FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src

RUN mvn --batch-mode --no-transfer-progress -f backend/pom.xml clean verify

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=production
ENV FILE_UPLOAD_PATH=/app/uploads

RUN mkdir -p /app/uploads

COPY --from=build /workspace/backend/target/school-lms-api-0.1.0.jar /app/app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
