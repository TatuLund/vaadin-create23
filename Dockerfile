# Produce the WAR file for the Vaadin 8 application
FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY . /app/
# Set the working directory
WORKDIR /app
# Set Env variable VAADIN_PRO_KEY
ARG VAADIN_PRO_KEY
ENV VAADIN_PRO_KEY=${VAADIN_PRO_KEY}
# Build the WAR file
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -Prelease
RUN mv /app/vaadincreate-ui/target/*.war /app/vaadincreate-ui/target/ROOT.war
# Create the final image
FROM tomcat:9-jre21
# Add OpenTelemetry Java agent (version can be updated)
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.22.0/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

# Configure agent
ENV OTEL_SERVICE_NAME="vaadincreate"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="none"
ENV OTEL_LOGS_EXPORTER="none"
ENV OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317"
ENV OTEL_TRACES_SAMPLER="parentbased_traceidratio"
ENV OTEL_TRACES_SAMPLER_ARG="1.0"

# Ensure the WAR file is available
COPY --from=build /app/vaadincreate-ui/target/ROOT.war /usr/local/tomcat/webapps/

# Copy and set permissions for setenv.sh
COPY setenv.sh /usr/local/tomcat/bin/
RUN chmod +x /usr/local/tomcat/bin/setenv.sh

# Replace the default context.xml with a custom one
COPY context.xml /usr/local/tomcat/conf/context.xml

# Expose the port on which Tomcat is running
EXPOSE 8080
