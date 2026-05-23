# Project-Wide Copilot Instructions

## Repo structure

Three Maven modules under root `pom.xml`:

- **vaadincreate-backend** — JPA entities (Hibernate), DAOs, service interfaces + implementations, Redis pub/sub infrastructure, domain event records. Packaging: `jar`.
- **vaadincreate-components** — Reusable Vaadin components with GWT client-side code (connectors). Packaging: `jar`.
- **vaadincreate-ui** — Vaadin UI layer (views, presenters, MVP), theme SCSS, widgetset compilation, servlet. Packaging: `war`. Depends on both other modules.

Call flow: `View → Presenter → Service → DAO → HibernateUtil → Database`

No DI frameworks. Services are singletons accessed via static `get()` methods (e.g., `PurchaseServiceImpl.getInstance()`, `ProductDataService.get()`).

## Commands

```
mvn clean install                          # build everything, skip tests
mvn verify -Pit                            # run unit + integration tests (TestBench)
mvn spotless:apply                         # format code (Eclipse Vaadin conventions)
cd vaadincreate-ui && mvn jetty:run        # start dev server on :8080
cd vaadincreate-components && mvn jetty:run # component test UI on :8080
mvn vaadin:run-codeserver                  # GWT Super Dev Mode (separate console)
```

Maven profiles: `it` (integration tests with Jetty start/stop + Failsafe), `release` (optimized widgetset), `appsec` (CycloneDX SBOM via AppSecKit).

CI runs `mvn install -DskipTests` then `mvn verify -Pit -DghActions=true`. Chrome is required for TestBench ITs.

## Testing pyramid

Three test tiers, each with its own base class:

1. **Unit tests** — JUnit 4 + Mockito in `src/test/java`. Backend DAOs use H2 (in-memory). Name ending: `Test`.
2. **UI Unit Tests** — `AbstractUITest` extends `UIUnitTest` (browserless). Set up `VaadinCreateUI` in `@Before`, call `mockVaadin(ui)`, navigate via `navigate(View.NAME, View.class)`. Use fluent `$` query API and `test(component)` helpers.
3. **Integration Tests** — `AbstractViewTest` extends `TestBenchTestCase`. Runs against headless Chrome on a running Jetty server (started by Failsafe in `-Pit`). Name ending: `IT`.

All new business logic needs JUnit 4 tests. All new view components need UI Unit Tests. Custom components with client-side code need TestBench ITs.

### UI Unit test patterns

```java
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SomeViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private SomeView view;

    @Before public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login(); // or login("Username", "password")
    }

    @After public void cleanUp() { logout(); tearDown(); }

    @Test public void some_behavior() {
        view = navigate(SomeView.VIEW_NAME, SomeView.class);
        // Query: $(ComponentClass.class).id("component-id").first()
        // Interact: test(component).click() / .setValue(...) / .isFocused()
        // Grid rows: test(grid).size(), test(grid).item(rowIndex)
    }
}
```

- `login()` defaults to Admin/admin. Use `login("User5", "user5")` for other users.
- Switching users mid-test: call `logout()`, `tearDown()`, create new `VaadinCreateUI`, `mockVaadin(ui)`, then `login(newUser, newPassword)`.
- Grid row ordering: most rows are ordered by `createdAt desc` (newest first). Row 0 = most recent.
- Async charts: wait for `.loaded` CSS class via `waitForCharts(dashboard)` helper.

### Mock data behavior

On first startup, if no purchases exist, mock data is auto-generated for users Customer11–Customer20 with approvers User5/User6. Disable with `-Dgenerate.data=false`. This affects test expectations — tests should not assume empty database state unless they explicitly clear it.

## Architecture enforcement

ArchUnit tests in `ArchitectureTest.java` enforce strict dependency boundaries at compile time:

- Only presenters, `VaadinCreateUI`, auth classes, and event bus internals may call `*Service`.
- Only services (and other DAOs) may access `*Dao`.
- Only DAOs may touch `SessionFactory` / `HibernateUtil`.
- Presenters must not import any `com.vaadin.*` class.
- Service impls (`*ServiceImpl`) are backend-internal — accessed only via interfaces from outside the backend package.
- Views ending with `View` must have `@AllPermitted` or `@RolesPermitted` and implement `HasI18N`.

These tests run as part of normal Maven test lifecycle. If they fail, the build fails.

## Vaadin 8 specifics

- All views extend `VaadinCreateView` (interface) — provides i18n + accessibility defaults.
- Navigator-based routing (not Flow). Views registered programmatically in `AppLayout.addView()`. URL fragment format: `#!viewName`.
- GWT widgetset compiles during `prepare-package`. For dev iteration use `mvn vaadin:run-codeserver` in a separate console + `?superdevmode` URL parameter.
- Client-side components live in `vaadincreate-components` under `client/` package (connectors). State classes go in `shared/`.
- Theme SCSS lives in `vaadincreate-ui/src/main/webapp/VAADIN/themes/vaadincreate/`. CSS class name constants belong in `VaadinCreateTheme`.
- Push enabled via WebSocket/XHR transport (`@Push` on `VaadinCreateUI`).
- CurrentUser stored as session attribute (`CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY`). Access via `CurrentUser.get()` or `Utils.getCurrentUserOrThrow()`.

## Code conventions

- `@NullMarked` on all public classes; `@Nullable` on parameters/returns that may be null. No Lombok.
- Use Java records for DTOs, events posted to EventBus, and immutable data holders.
- Entity `equals()` / `hashCode()` based on `id` field only.
- Public method validation: `Objects.requireNonNull`. Private method validation: `assert`.
- Utility methods in `util.*` package must be static.
- I18n keys defined as `static final` constants in `I18n.java`; message bundles in resources.
- JSoup HTML sanitization with `content` mode for user-supplied HTML display.

## Key domain patterns

- **Purchase** entity: has `requester` (User), optional `approver` (User), embedded `Address`, and a list of `PurchaseLine`. Status flows PENDING → APPROVED/REJECTED.
- **EventBus**: post events via `EventBus.get().post(new SomeEvent(...))`; listeners register in constructors or init methods. Events include `PurchaseStatusChangedEvent`, `MessageEvent`, `UserUpdatedEvent`, `ShutdownEvent`.
- Mock data generation: on first startup, if no purchases exist, mock data is auto-generated for users Customer11–Customer20 with approvers User5/User6. Disable with `-Dgenerate.data=false`.

## Docker / local environment

Docker Compose sets up a full cluster: 2 app nodes, Postgres DB, Redis (EventBus), Nginx (load balancer), OpenTelemetry collector + Jaeger. Requires `VAADIN_PRO_KEY` env var for commercial Vaadin artifacts during build.

DB init: load `vaadincreate.sql` into the Postgres container before starting the app.

## Docs references

- Architecture: `docs/ApplicationArchitecture.md` (includes Mermaid diagram)
- Data model: `docs/DataModel.md`
- Style guide / theme tokens: `docs/StyleGuide.md`
- Product requirements: `docs/VaadinCreate.PRD.md`
- Ubiquitous language / domain glossary: `docs/UbiquitousLanguage.md`
