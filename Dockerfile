FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY file-sharing/pom.xml .
COPY file-sharing/src ./src
EXPOSE 8082
