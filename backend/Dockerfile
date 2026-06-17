FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/streamfolio-v1-1.4.0.jar app.jar
RUN ffmpeg -version >/dev/null && ffprobe -version >/dev/null
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
