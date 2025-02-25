# Produce the WAR file for the Vaadin 8 application
FROM maven:3.9.1-eclipse-temurin-17 AS build
COPY . /app/
# Set the working directory
WORKDIR /app
# Set Env variable VAADIN_PRO_KEY
ENV VAADIN_PRO_KEY=<insert-your-key-here>
# Build the WAR file
RUN mvn clean package -DskipTests
RUN mv /app/vaadincreate-ui/target/*.war /app/vaadincreate-ui/target/ROOT.war
# Create the final image
ENV JAVA_OPTS="-Dvaadin.productionMode=true"
FROM tomcat:9-jre17
# Ensure the WAR file is available
COPY --from=build /app/vaadincreate-ui/target/ROOT.war /usr/local/tomcat/webapps/
# Expose the port on which Tomcat is running

# Copy and set permissions for setenv.sh
COPY setenv.sh /usr/local/tomcat/bin/
RUN chmod +x /usr/local/tomcat/bin/setenv.sh

EXPOSE 8080
