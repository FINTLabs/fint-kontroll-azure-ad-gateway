FROM gradle:8.10.1-jdk as builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java21
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-kontroll-azure-ad-gateway-*.jar /data/app.jar
CMD ["/data/app.jar"]
