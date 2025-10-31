# Produce the WAR file for the Vaadin 8 application
FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY . /app/
# Set the working directory
WORKDIR /app
# Set Env variable VAADIN_PRO_KEY
ENV VAADIN_PRO_KEY=
# Build the WAR file
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -Prelease
RUN mv /app/vaadincreate-ui/target/*.war /app/vaadincreate-ui/target/ROOT.war
# Create the final image
FROM tomcat:9-jre21
# Ensure the WAR file is available
COPY --from=build /app/vaadincreate-ui/target/ROOT.war /usr/local/tomcat/webapps/

# Copy and set permissions for setenv.sh
COPY setenv.sh /usr/local/tomcat/bin/
RUN chmod +x /usr/local/tomcat/bin/setenv.sh

# Replace the default context.xml with a custom one
COPY context.xml /usr/local/tomcat/conf/context.xml

# Expose the port on which Tomcat is running
EXPOSE 8080
