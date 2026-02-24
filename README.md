# Vaadin Create '23 demo app (Vaadin 8)

This is a showcase application for advanced Vaadin 8 topics. The application focuses on UI design and the architecture of the UI code. These techniques help to keep existing Vaadin 8 applications up to date. The application is built with Vaadin 8.30.1, which is fully compatible with Java 11 and 21.

This project is my dogfooding test application for legacy Vaadin 8, as we are still maintaining it under our commercial extended maintenance program. Although it is somewhat fabricated, I have included many details that you would find in a real production application.

Despite the application using the old Vaadin 8, many of the examples featured here are applicable to more current Vaadin versions.

## Covered topics

Despite being a somewhat artificial internal purchase tool, this demo app covers various use cases you may encounter in a real-life application. The demonstrated cases are based on actual customer questions I have seen during my career as a software consultant.

As a result, the project has a wiki that collects practical notes, examples, and best practices derived from the vaadin-create23 demo project. While new articles will be added over time, the current content reflects the main focus of the wiki: documenting Vaadin 8 patterns and techniques that tend to be underrepresented or only briefly mentioned in the official documentation. The emphasis is on real‑world usage, maintainability, and architectural clarity, with each article grounded in concrete code from the demo project rather than isolated snippets or theoretical examples.

### Vaadin 8 Howtos

- [How to Use OpenTelemetry in a Vaadin 8 Application](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Use-OpenTelemetry-in-a-Vaadin-8-Application)
- [How to Improve Accessibility (a11y) in a Vaadin 8 Application](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Improve-Accessibility-(a11y)-in-a-Vaadin-8-Application)
- [How to Localize a Vaadin 8 Application](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Localize-a-Vaadin-8-Application)
- [How to Validate a Vaadin 8 Project with GitHub Actions](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Validate-a-Vaadin-8-Project-with-GitHub-Actions)
- [How to Create an Accessible Application Shell with Vaadin 8](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Create-an-Accessible-Application-Shell-with-Vaadin-8)
- [How to Write Browserless UI Tests with Vaadin 8](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Write-Browserless-UI-Tests-with-Vaadin-8)
- [How to Use Asynchronous Updates in Vaadin 8](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Use-Asynchronous-Updates-in-Vaadin-8)
- [How to Customize the Valo Theme in Vaadin 8](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Customize-the-Valo-Theme-in-Vaadin-8)
- [How to Test Vaadin 8 Charts with TestBench](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Test-Vaadin-8-Charts-with-TestBench)
- [How to Test Vaadin 8 Components in Isolation with TestBench](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Test-Vaadin-8-Components-in-Isolation-with-TestBench)
- [How to Use the Vaadin 8 Component Event Bus](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Use-the-Vaadin-8-Component-Event-Bus)
- [How To Use the Latest Vaadin 8 Binder Features](https://github.com/TatuLund/vaadin-create23/wiki/How-To-Use-the-Latest-Vaadin-8-Binder-Features)
- [How to Implement an Application Event Bus for a Typical Vaadin Business App](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Implement-an-Application-Event-Bus-for-a-Typical-Vaadin-Business-App)
- [How To Reduce Flaky Tests in Vaadin 8 TestBench ](https://github.com/TatuLund/vaadin-create23/wiki/How-To-Reduce-Flaky-Tests-in-Vaadin-8-TestBench)
- [How To Show Non-Interactive, Accessible Details in a Vaadin 8 Grid](https://github.com/TatuLund/vaadin-create23/wiki/How-To-Show-Non%E2%80%90Interactive,-Accessible-Details-in-a-Vaadin-8-Grid)
- [How To Compose HTML Content in Vaadin 8 Safely](https://github.com/TatuLund/vaadin-create23/wiki/How-To-Compose-HTML-Content-in-Vaadin-8-Safely-(Without-XSS-Surprises))
- [How to implement hierarchical navigation in Vaadin 8](https://github.com/TatuLund/vaadin-create23/wiki/How-to-implement-hierarchical-navigation-in-Vaadin-8)
- [How to Build a Test Pyramid for Vaadin 8 Apps](https://github.com/TatuLund/vaadin-create23/wiki/How-to-Build-a-Test-Pyramid-for-Vaadin-8-Apps)

## Backend

The backend module features a data access layer using Hibernate with a concise utility class for running queries in a session and transaction, which is used by the DAOs. There are a couple of service classes that are used by the presenters in the UI module. The backend also has a RedisPubSubService, which is used by the EventBus. There is a set of unit tests verifying the main functionality.

Dependency injection frameworks such as CDI or Spring are not used. The demo is intentionally framework-neutral and demonstrates that, especially for small applications, you can build a clean architecture without them.

## This project uses commercial Vaadin products

This is intentional to demonstrate the current state of Vaadin 8 extended maintenance.

The following commercial products are used.

- Vaadin 8.30.1. The application uses some features it provides. (The latest free version 8.14.3)
  - Eager UI cleanup
  - Binder: change tracking
  - Binder: draft saving
  - Grid: read only state
  - Grid: accessible navigation mode
  - ValoMenu: improved API
  - ChartElement: For testing StatsView
- Vaadin Charts in the stats dashboard view
- TestBench and TestBench UI Unit Test add-on for testing
- AppSecKit for SBOM vulnerability analysis

## Building and running the application

```
git clone <url of the repository>

mvn clean install
```

  (or use "mvn clean install -Pit" to run also the TestBench tests)

```
cd vaadincreate-ui

mvn jetty:run
```

Alternatively, run the application with AppSecKit

```
mvn jetty:run -Pappsec
```

To see the demo, navigate to http://localhost:8080/

## Building and running the component tests

The components module has its own test UI for running the integration tests of the components.

```
git clone <url of the repository>

mvn clean install
```

(or use "mvn clean install -Pit" to run also the TestBench tests)

```
cd vaadincreate-components

mvn jetty:run
```

To see the demo, navigate to http://localhost:8080/

## Development with Eclipse IDE

For further development of this project, the following toolchain is recommended:
- Eclipse IDE
- m2e wtp plug-in (install it from Eclipse Marketplace)
- Vaadin Eclipse plug-in (install it from Eclipse Marketplace)
- For Java hotswapping setup HotSwapAgent with JBR 17 or use JRebel Eclipse plug-in (install it from Eclipse Marketplace)
- Chrome/Edge/Firefox browser

### Importing project

Choose File > Import... > Existing Maven Projects

Note that Eclipse may show "Plugin execution not covered by lifecycle configuration" errors for pom.xml. Use the "Permanently mark goal resources in pom.xml as ignored in Eclipse build" quick-fix to mark these errors as permanently ignored in your project. The project will still work fine.

### Debugging server-side

If you have not already compiled the widgetset, do it now by running the vaadin:install Maven target for the vaadincreate-root project.

If you have a JRebel license, it makes on-the-fly code changes faster. Just add JRebel nature to your vaadincreate-ui project by right-clicking the project and choosing JRebel > Add JRebel Nature.

To debug project and make code modifications on the fly in the server-side, right-click the vaadincreate-ui project and choose Debug As > Debug on Server. Navigate to http://localhost:8080/ to see the application.

### Debugging client-side

Debugging client side code in the vaadincreate-ui project:
  - run "mvn vaadin:run-codeserver" on a separate console while the application is running
  - activate Super Dev Mode in the debug window of the application or by adding ?superdevmode to the URL
  - You can access Java-sources and set breakpoints inside Chrome if you enable source maps from inspector settings.

### Build simulated production setup with Docker

The project root folder has Dockerfile, Dockerfile.db and docker-compose.yaml files for setting up eight container demonstrator "production" environment. 

Check Dockerfile first, add your license key there into VAADIN_PRO_KEY environment variable, the license checker needs this during build time as it is using commercial assets.

Then use command

```
docker-compose build db
```

The building of the application container can take 5-10 minutes depending on how fast the dependencies are loaded. This build does not run the tests in order to speed up the process. The setenv.sh file in the repository is injected to the application container and has the system properties set for the production mode app.

The other Dockerfile.db file is just for Postgres database.

Then you need to load initial data from the script in this repository, start the database container and use commands:
```
docker-compose up db
docker exec -it vaadin-create23-db-1 mkdir /backup
docker cp vaadincreate.sql vaadin-create23-db-1:/backup/vaadincreate.sql
docker exec -it vaadin-create23-db-1 psql -U creator -d vaadincreate -f /backup/vaadincreate.sql
```

Then stop the container.

If it works thus far, you can start the cluster using:

```
docker-compose up
```

If you change the application build it separately using, as the database container should not be usually rebuild unless you need to start from the scratch again.

```
docker-compose build app-1
docker-compose build app-2
```

The compose file builds also HighCharts export service, Redis and Nginx containers. Redis is used by EventBus for sharing events within the cluster and Nginx is used as load balancer. As the free edition of Nginx does not support real sticky sessions the nginx.conf demonstrates one potential workaround for this problem. Furthermore 
the setup includes OpenTelemetry collector and Jaeger to view the telemetry data.

## License & Author

Project itself is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Note, the project uses Vaadin commercial artifacts which require appropriate subscription and license from Vaadin.

