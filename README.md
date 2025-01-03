# Vaadin Create '23 demo app (Vaadin 8)

This is show case application for advanced Vaadin 8 topics. The application focuses on UI design and architecture of the UI code. These techniques help to keep old Vaadin 8 applications upto date. The application is built with Vaadin 8.27, which is fully compatible with Java 11 and 17.

## Covered topics

Despites being somewhat artificial this demo app covers various use cases you may encounter in real life application. Source of the demonstrated cases has been actual customer questions I have seen during my career as a software consultant.

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
	- Automatically update the charts when someone saves or deletes book
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
	- Use shared presenter with BooksView as BookForm is sub-component of BooksView
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
- AboutView
	- Demo how to correctly sanitize user input with Jsoup in order to avoid XSS,
	- CustomLayout example
- How to use logger in UI module
- How to create and use application scoped EventBus
	- Implementation shows how to avoid common caveats of memory leaks, see unit test for proof
	- This EventBus is used in many ways in the application
- The custom theme is using BEM (Block Element Modifier) naming scheme to avoid class name conflicts in CSS
- Example of how to localize / provide translations for texts used in UI
- Comprehensive set of unit, compomnent, integration and end to end tests
    - The project demonstrates testing approach I would recommend for typical Vaadin application:
    - Test custom components in isolation
    - Test as much as possible with unit tests
    - Prefer UI Unit Testing for testing UI logic including non-happy paths
    - Use UI Unit Testing for verifying various concurrent user actions scenarios
    - Verify the most important parts with end to end tests using TestBench
    - Include few Screenshot tests to verify visuals
- Accessiblility
    - In most of the views some additional attributes are set for better accessibility
    - In StatsView Charts are having role "figure" and "aria-label" set for audible version of data
    - In BooksView the BookGrid has rows set "aria-live" for better situational awarenes
    - The loading indicator in BooksView has audible alert
    - The label that appears when there is no books matching filter is audible
    - VaadinCreateUI offers "announce" method, that produces audible notification, which used e.g. when view is opened or form is opened
    - AppLayout menu has role "navigation" and the navigation buttons role "link". The buttons also have "aria-label" set according to whether the view is a current view or not.
    - In some places tooltips are used as audible hints
    - Keyboard navigation has been adjusted for better usability with assistive technologies
    - Tabindex and visual focus ring is used to improve situational awateness
    - Using focus color in focused fields labels

- Components module has examples of GWT and JavaScript extensions
	- Reset button for text field extension with client side GWT,
	- Character counter for TextArea and TextField showing remaining characters extension with GWT,
	- Attribute extension using JavaScript. It used in form's number input, and is an essential instrument to adjust the accessibility attributes in various places.
	- Java 11 code used in widgets,
	- ConfirmDialog server side composition component,
	- Suite of unit and integration tests for the components and standalone test UI for them

Notes

- Backend module has in memory data service built with H2 database and Hibernate and has simulated latency on service layer.
- Dependency injection framework such as CDI or Spring is not being used, the demo has neutral stance on purpose and demonstrates that especially small applications can be built without them.

## This project uses commercial Vaadin products

This is intentional to demonstrate the current state of Vaadin 8 extended maintenance
The following commercial products are used.

- Vaadin 8.27.2. The application uses some features it provides. (The latest free version 8.14.3)
  - Eager UI cleanup
  - Binder: change tracking
  - Binder: draft saving
  - Grid: read only state
  - ValoMenu: improved API
- Vaadin Charts in stats dashboard view
- TestBench and TestBench UI Unit Test add-on for testing 
- AppSecKit for SBOM vulnerability analysis

## Building and running the application

git clone <url of the repository>

mvn clean install

    (or use "mvn clean install -Pit" to run also the TestBench tests)

cd vaadincreate-ui

mvn jetty:run

Alternatively run application with AppSecKit

mvn jetty:run -Pappsec

To see the demo, navigate to http://localhost:8080/

## Building and running the component tests

The components module has test UI of its own for running the integration tests of the components.

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

