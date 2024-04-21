# Vaadin Create '23 demo app (Vaadin 8)

This is show case application for advanced Vaadin 8 topics. The application focuses on UI design and architecture of the UI code. These techniques help to keep old Vaadin 8 applications upto date. The application is built with Vaadin 8.21.0, which is fully compatible with Java 11.

## Covered topics

The demo app covers various use cases you may encounter in real life application.

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
- BooksView
	- Responsive Grid using BrowserResizeListener, 
	- Responsive CSS in Grid column
	- Responsive CRUD editor design
	- Highlight last edited item
	- Implement beforeLeave to confirm unsaved changes
	- Highlight changed fields
	- Bean level validation example
	- Bookmarkable editor
	- Description generator showing details in compact mode
- Simplified example of access control
- AdminView
	- Example of nested sub-navigation using url-parameters and TabSheet component,
	- Category management view, use Grid as list editor
	- User management view, use FormLayout light variant
- AboutView
	- Demo how to correctly sanitize user input with Jsoup in order to avoid XSS,
	- CustomLayout example
- How to use logger in UI module
- How to create and use application scoped EventBus
	- Implementation shows how to avoid common caveats of memory leaks, see unit test for proof
- The custom theme is using BEM (Block Element Modifier) naming scheme to avoid class name conflicts in CSS
- Example of how to localize / provide translations for texts used in UI
- Comprehensive set of unit, compomnent, integration and end to end tests

- Components module has examples of GWT and JavaScript extensions
	- Reset button for text field extension with client side GWT,
	- Character counter for TextArea and TextField showing remaining characters extension with GWT,
	- Attribute extensions (used in form's number input) using JavaScript,
	- Java 11 code used in widgets,
	- ConfirmDialog server side composition component,
	- Suite of unit and integration tests for the components and standalone test UI for them

Notes

- Backend module has mock data service only with simulated latency
- Dependency injection framework such as CDI or Spring is not being used, the demo has neutral stance on purpose

## This project uses commercial Vaadin products

This is intentional to demonstrate the current state of Vaadin 8 extended maintenance
The following commercial products are used.

- Vaadin 8.25.0, The latest free version 8.14.3
- Vaadin Charts in stats dashboard view
- TestBench and TestBench UI Unit Test add-on for testing 
- AppSecKit for SBOM vulnerability analysis

## Building and running the application

git clone <url of the repository>
mvn clean install
    (or use "mvn clean install -Pit" to run also the TestBench tests)
cd vaadincreate-ui
mvn jetty:run

To see the demo, navigate to http://localhost:8080/

## Building and running the component tests

git clone <url of the repository>
mvn clean install
    (or use "mvn clean install -Pit" to run also the TestBench tests)
cd vaadincreate-components
mvn jetty:run

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
 

## License & Author

Project itself is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Note, the project uses Vaadin commercial artifacts which require appropriate subscription and license from Vaadin.

