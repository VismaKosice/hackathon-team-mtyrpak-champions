FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies 2>/dev/null || true
COPY src/ src/
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8080 9090
CMD ["java", \
     "-XX:+UseParallelGC", \
     "-Xms4g", "-Xmx4g", \
     "-XX:NewRatio=1", \
     "-XX:+AlwaysPreTouch", \
     "-XX:+TieredCompilation", \
     "-XX:CompileThreshold=500", \
     "-XX:CICompilerCount=2", \
     "-XX:-UseCounterDecay", \
     "-XX:InlineSmallCode=4000", \
     "-XX:MaxInlineLevel=15", \
     "-XX:FreqInlineSize=6000", \
     "-XX:MetaspaceSize=64m", \
     "-XX:MaxMetaspaceSize=64m", \
     "-XX:ThreadStackSize=512", \
     "-jar", "/app/app.jar"]
