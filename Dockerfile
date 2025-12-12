FROM gradle:8.14.3-jdk21 AS builder
USER root
WORKDIR /home/gradle/src
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java21
ENV JAVA_TOOL_OPTIONS=-XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/src/build/libs/fint-kontroll-azure-ad-gateway-*.jar /data/app.jar
CMD ["/data/app.jar"]
