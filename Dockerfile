FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies 2>/dev/null || true
COPY src/ src/
RUN ./gradlew --no-daemon bootJar

# Use glibc-based image (musl in alpine is significantly slower for JVM)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar

# CDS training run: capture class data for faster cold start
RUN java -XX:ArchiveClassesAtExit=/app/application.jsa \
    -Dspring.context.exit=onRefresh \
    -Dserver.port=0 \
    -Dgrpc.server.port=-1 \
    -Dspring.jmx.enabled=false \
    -jar /app/app.jar || true

EXPOSE 8080 9090
CMD ["java", \
     "-XX:SharedArchiveFile=/app/application.jsa", \
     "-XX:+UseParallelGC", \
     "-Xms512m", "-Xmx512m", \
     "-XX:NewRatio=1", \
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
     "-Dspring.jmx.enabled=false", \
     "-jar", "/app/app.jar"]
