# Vaadin Create '23 demo app (Vaadin 8)

This is show case application for advanced Vaadin 8 topics. The application focuses on UI design and architecture of the UI code. These techniques help to keep old Vaadin 8 applications up to date. The application is built with Vaadin 8.30.0, which is fully compatible with Java 11 and 21.

This project is my dogfooding test application for legacy Vaadin 8 as we are still maintaining it under our commercial extended maintenance program. Thus, even it is a bit fabricated, I have added there are many details that you would find in real production application.

Despite the application using the old Vaadin 8, many of the examples featured by this application are applicable for more current Vaadin versions.

## Covered topics

Despite being somewhat artificial this demo app covers various use cases you may encounter in real life application. Source of the demonstrated cases has been actual customer questions I have seen during my career as a software consultant.

- Multi-module project setup (backend, components and ui)
- AppLayout uses ValoMenu to create responsive application shell
	- Setup also navigator and combine building of the menu and registrations of the views.
	- Implements per view role based access control.
- The main application views have been implemented using Model View Presenter pattern
	- VaadinCreateUI acts as a proxy for services and services are used only in the Presenters. This pattern allows to extend and override VaadinCreateUI for alternate set of services for testing.	
- StatsView and BooksView load data from backend asynchronously and use push to update the view
	- BooksView uses FakeGrid component as placeholder while loading.
- StatsView
	- Responsive dashboard layout of Charts using Vaadin responsive CSS.
	- Async loading and showing no data label during loading.
	- Multi-axis Chart example.
	- Automatically update the charts when someone saves or deletes book, as well as adds, removes or updates a category.
	- Enabled Chart export menu
- BooksView
	- Responsive Grid using BrowserResizeListener, 
	- Responsive CSS in Grid column
	- Responsive CRUD editor design
	- Highlight last edited item
	- Implement beforeLeave to confirm unsaved changes
	- Highlight changed fields
	- Bean level validation example
	- Display no matches label on Grid when search did not find matches
	- Bookmarkable editor
	- Description generator showing details in compact mode
	- Pessimistic locking preventing concurrent edits
- BookForm
	- Use event based decoupling with BooksView
	- Demo of how to implement "dirty" state for the fields
	- So previous value of the field when "dirty"
	- Use change tracking in Binder, i.e. when user edits the value back to original Binder state is back hasChanges = false
	- Demo of how to use bean level validation
	- Custom field demo, NumberField
	- Side panel design example, see SidePanel
	- Auto save draft if the browser is closed while form is open
- Simplified example of access control
- AdminView
	- Example of nested sub-navigation using url-parameters and TabSheet component,
	- Category management view, use Grid as list editor
	- User management view, use FormLayout light variant
	- Optimistic locking used for handling concurrent edits
	- Event based decoupling demo in UserForm
	- Indicator in Tab when UserForm has un-saved changes
	- Do not let session end when there is un-saved changes in UserForm
- AboutView
	- Demo how to correctly sanitize user input with Jsoup in order to avoid XSS,
	- CustomLayout example
- How to use logger in UI module
- How to create and use application scoped EventBus
	- Implementation shows how to avoid common caveats of memory leaks, see unit test for proof
	- This EventBus is used in many ways in the application
	- The EventBus uses RedisPubSubService to share the events within the cluster
- The custom theme is using BEM (Block Element Modifier) naming scheme to avoid class name conflicts in CSS
- Example of how to localize / provide translations for texts used in UI
- Observability
    - Simple utility class using OpenTelemetry adds events and attributes of the user actions to trace spans.

### Backend

The backend module features data access layer using Hibernate with a concise utility class for running queries in session and transaction, which is used by DAOs. There are couple of Service classes that are using by the presenters in the UI module. The backend also has RedisPubSubService, which is used by EventBus.  There is a set of unit tests verifying the main functionalities.

### Automated testing

The test suite of the application follows the principles that I think are the best practise for a Vaadin application. 

- Comprehensive set of unit, component, integration and end to end tests
    - The project demonstrates testing approach I would recommend for typical Vaadin application:
    - Test custom components in isolation
    - Test as much as possible with unit tests
    - Prefer UI Unit Testing for testing UI logic including non-happy paths
    - Use UI Unit Testing for verifying various concurrent user actions scenarios
	- ArchUnit tests guarding selected architecure conventions
    - Verify the most important parts with end to end tests using TestBench
    - Include few Screenshot tests to verify visuals

### Accessibility

Vaadin 8 is not fully upto date with modern accessibility, but with some relatively simple workarounds acceptable accessibility can be still achieved and those are being demonstrated in this application.

- Accessibility
    - In most of the views some additional attributes are set for better accessibility, AttributeExtension and HasAttributes mixin are used to extend the components
    - In StatsView Charts are having role "figure" and "aria-label" set for audible version of data
	- Using ChatAccessibilityExtension to patch Chart's legends and context menus to be accesible
    - In BooksView the BookGrid using Grid's accessible navigation mode for better situational awareness
    - The loading indicator in BooksView has audible alert
    - The label that appears when there is no books matching filter is audible
    - Using assistive Notification, which used e.g. when view is opened or form is opened
    - AppLayout menu has role "navigation" and the navigation buttons role "link". The buttons also have "aria-label" set according to whether the view is a current view or not.
    - In some places tooltips are used as audible hints
    - Keyboard navigation has been adjusted for better usability with assistive technologies
    - Tabindex and visual focus ring is used to improve situational awareness
    - Using focus color in focused fields labels

### General use components

The general use components have been isolated in their own module. This is to emphasize separation of concern, and also making it possible to have end-to-end tests of the components in isolation against test UIs.

- Components module has examples of GWT and JavaScript extensions
	- Reset button for text field extension with client side GWT,
	- Character counter for TextArea and TextField showing remaining characters extension with GWT,
	- Attribute extension using JavaScript. It used in form's number input, and is an essential instrument to adjust the accessibility attributes in various places. The component has HasAttributes mixin that makes it convenient to use when more attributes are needed to be set.
	- Java 11 code used in widgets,
	- ConfirmDialog server side composition component,
	- ChartAccessibilityExtension patches Chart legend and context menu as well as adss API to localize context menu texts, as those are not in Lang object.
	- Suite of unit and integration tests for the components and standalone test UI for them

### Notes

- Backend module has in memory data service built with H2 database and Hibernate and has simulated latency on service layer.
- Dependency injection framework such as CDI or Spring is not being used, the demo has neutral stance on purpose and demonstrates that especially small applications can be built without them.

## This project uses commercial Vaadin products

This is intentional to demonstrate the current state of Vaadin 8 extended maintenance.

The following commercial products are used.

- Vaadin 8.30.0. The application uses some features it provides. (The latest free version 8.14.3)
  - Eager UI cleanup
  - Binder: change tracking
  - Binder: draft saving
  - Grid: read only state
  - Grid: accessible navigation mode
  - ValoMenu: improved API
- Vaadin Charts in stats dashboard view
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
```

mvn jetty:run

Alternatively run application with AppSecKit

```
mvn jetty:run -Pappsec
```

To see the demo, navigate to http://localhost:8080/

## Building and running the component tests

The components module has test UI of its own for running the integration tests of the components.

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

For further development of this project, the following tool-chain is recommended:
- Eclipse IDE
- m2e wtp plug-in (install it from Eclipse Marketplace)
- Vaadin Eclipse plug-in (install it from Eclipse Marketplace)
- For Java hotswapping setup HotSwapAgent with JBR 17 or use JRebel Eclipse plug-in (install it from Eclipse Marketplace)
- Chrome/Edge/Firefox browser

### Importing project

Choose File > Import... > Existing Maven Projects

Note that Eclipse may give "Plugin execution not covered by lifecycle configuration" errors for pom.xml. Use "Permanently mark goal resources in pom.xml as ignored in Eclipse build" quick-fix to mark these errors as permanently ignored in your project. Do not worry, the project still works fine. 

### Debugging server-side

If you have not already compiled the widgetset, do it now by running vaadin:install Maven target for vaadincreate-root project.

If you have a JRebel license, it makes on the fly code changes faster. Just add JRebel nature to your vaadincreate-ui project by clicking project with right mouse button and choosing JRebel > Add JRebel Nature

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
docker exec -it vaadincreate23-db-1 mkdir /backup
docker cp vaadincreate.sql vaadincreate23-db-1:/backup/vaadincreate.sql
docker exec -it vaadincreate23-db-1 psql -U creator -d vaadincreate -f /backup/vaadincreate.sql
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

