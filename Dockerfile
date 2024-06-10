FROM openjdk:21-slim as build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x ./gradlew

RUN ./gradlew dependencies

COPY src src

RUN ./gradlew bootJar

FROM openjdk:21-slim

WORKDIR /app

ENV GOOGLE_APPLICATION_CREDENTIALS=/app/gcp_service_account.json

COPY gcp_service_account.json /app/gcp_service_account.json

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
