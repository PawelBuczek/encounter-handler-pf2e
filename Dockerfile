FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/encounter-handler-pf2e-back-0.0.1-SNAPSHOT.jar /app
EXPOSE 8080
ENTRYPOINT ["java","-jar","encounter-handler-pf2e-back-0.0.1-SNAPSHOT.jar"]