# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /source
COPY . .
ARG SOURCE_COMMIT=unknown
RUN printf 'git.commit.id=%s\n' "$SOURCE_COMMIT" > src/main/resources/git.properties
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --max-workers=2 \
    test --tests 'com.maple.api.alrim.application.command.AlrimFcm*Test' bootJar

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 1001 mapleland \
    && useradd --uid 1002 --gid 1001 --no-create-home mapleland \
    && mkdir -p /workspace/logs \
    && chown -R 1002:1001 /workspace
WORKDIR /workspace
COPY --from=build --chown=1002:1001 /source/build/libs/*-SNAPSHOT.jar /workspace/app.jar
USER 1002:1001
EXPOSE 8080 18080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/workspace/app.jar"]
