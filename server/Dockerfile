FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY core ./core
COPY server ./server
RUN chmod +x gradlew && ./gradlew -PserverOnly=true :server:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/server/build/install/server /app
ENV HOST=0.0.0.0 PORT=8080
USER 10001
EXPOSE 8080
ENTRYPOINT ["/app/bin/server"]
