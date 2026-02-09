# VaadinCreate Product Requirements Document (PRD)

## 1. Overview

VaadinCreate is a sample inventory and administration application for managing books, users, categories, and system messages, with supporting statistics and basic multi-user coordination. This PRD captures the functional and behavioral requirements inferred from the existing automated tests, in a framework-agnostic way, so the application can be reimplemented or migrated (for example to newer Vaadin versions) while preserving observable behavior.

This document deliberately avoids framework-specific APIs, focusing instead on views, flows, events, and user-visible behavior.


## 2. Goals and Non-Functional Requirements

- Provide a small but realistic back-office UI for:
  - Managing a catalog of books/products.
  - Managing users and their roles.
  - Managing categories used to organize the catalog.
  - Viewing statistics about the catalog.
  - Broadcasting and viewing system messages.
- Demonstrate:
  - Proper authentication and role-based access control.
  - Internationalization and localization.
  - Accessibility support (assistive notifications, aria attributes, keyboard operation).
  - Event-driven updates across views (user updates, locks, category and product changes).
  - Basic multi-user coordination with optimistic locking.

Non-functional requirements (from tests and conventions):
- The UI must be fully operable with keyboard, including shortcuts for navigation, filtering, and form actions.
- The UI must provide assistive/a11y notifications for major navigation and state changes.
- User input that becomes rendered as HTML must be sanitized to prevent XSS.
- Core views and components must be serializable / safe for server-side session storage.
- The application must be resilient to concurrent edits and surface conflicts clearly to the user.


## 3. High-Level Architecture

- **Application Shell / Layout**
  - A single main layout ("App Layout") wraps all views and provides:
    - A navigation menu with buttons for main sections.
    - A content area where views are displayed.
    - Optional status/assistive notifications.

- **Views / Sections**
  - Login view (unauthenticated entry point).
  - About view (default landing view after login).
  - Inventory (Books) view.
  - Statistics view.
  - Admin view, containing:
    - User Management sub-view.
    - Category Management sub-view.

- **Services & Infrastructure (conceptual)**
  - Authentication & access control service (current user, role checks).
  - Product service (books CRUD, categories, statistics data).
  - User service (users CRUD, role changes).
  - Category service (categories CRUD and events).
  - Event bus for domain events (message events, user updates, locking, category updates, product updates).
  - Locking service for object-level locks and remote lock propagation.

- **State & Events**
  - A "current user" object is kept in session and updated when user data changes.
  - A central event bus dispatches events to all registered listeners, including those triggered by a distributed pub/sub backend.


## 4. Authentication and Localization

### 4.1 Login Flow

- The system shall present an unauthenticated login view with:
  - Username input field.
  - Password input field.
  - Language selector for choosing UI locale.
  - Login action (button).
  - Optional "hint" action for demo credentials.

- When the user submits correct credentials:
  - The authentication service shall mark the user as signed-in.
  - A "login succeeded" event shall be fired so other application parts can react.
  - The selected language shall be applied to the session and to the process default locale.

- When the user submits incorrect credentials:
  - No login event shall be fired.
  - The user shall remain unauthenticated.
  - A visible notification with text equivalent to "Login failed" shall be shown.

### 4.2 Language Selection and Persistence

- The login view shall allow selecting a locale (at least English and German).
- When the language is changed:
  - All captions in the login view (field labels, button caption, language selector caption) shall update to the selected language.
  - The session locale and the process default locale shall be updated.
- The currently selected locale shall be persisted (e.g., in a cookie) and used to initialize locale for subsequent sessions.

### 4.3 Responsive Login Behavior

- On larger viewports:
  - Text fields shall display captions (labels) for username and password.
  - An informational text area shall be visible.
  - Placeholders for these fields shall be empty.

- On smaller viewports:
  - The informational text area shall be hidden.
  - Text fields shall use placeholders ("Username", "Password") instead of captions.
  - Captions shall be empty.

### 4.4 Login Hint

- The login view shall provide a hint action (e.g., "Forgot?" button).
- When the hint action is activated:
  - A notification shall be shown with example credentials, e.g., "Hint: Try User0 / user0 or Admin / admin".


## 5. Default Routing and I18N

### 5.1 Default Route on Existing Session

- If a request is associated with an existing session that already has an authenticated user:
  - The router shall navigate directly to the default view (About view) when the base route is requested.

### 5.2 Locale from Cookie

- On initial request, the application shall:
  - Read a locale value from a cookie if present.
  - Use that locale (if valid) as the session locale; otherwise, fall back to a default locale (e.g., English).

### 5.3 I18N Provider

- The UI shall use a translation provider with the following contract:
  - A call `getTranslation(key)` (or equivalent) returns a localized string for the current session locale.
  - At minimum, keys like "save" must exist and produce localized strings for supported locales (e.g., "Save" in English, "Tallenna" in Finnish).


## 6. Application Layout and Navigation

### 6.1 Navigation Structure

- The main layout shall expose menu entries for at least:
  - About.
  - Inventory (Books).
  - Statistics.
  - Admin.

- Selecting a menu entry shall navigate to the associated view and update the visible content accordingly.

### 6.2 Active View Indication

- Exactly one menu item shall appear as "selected" (active) at a time.
- When a view is navigated to (e.g., by route or programmatically), its corresponding menu button shall be marked as selected.

### 6.3 Menu Toggle

- A menu toggle control shall show and hide the navigation menu.
- When the menu is opened:
  - An assistive notification shall be shown announcing "navigation menu opened".
- When the menu is closed:
  - An assistive notification shall be shown announcing "navigation menu closed".

### 6.4 Keyboard Navigation Shortcut

- The main layout shall define a global keyboard shortcut (e.g., Alt+Shift+N) that moves keyboard focus to the About menu button.

### 6.5 Route Protection and Error View

- Views shall declare their required roles via metadata (e.g., a roles-permitted mechanism).
- When navigation is attempted to a view for which the current user lacks the necessary role:
  - The navigation shall be blocked.
  - An error view shall be shown instead.

- When navigation is attempted to an unknown route:
  - An error view shall be shown indicating that the view is not available.


## 7. Current User and Role Reactivity

### 7.1 Current User Representation

- The application shall maintain a "current user" object in session, including at least:
  - Unique identifier.
  - Username.
  - Role.

### 7.2 Reacting to User Updates

- The application shall listen to "user updated" events that reference a user identifier.
- When such an event is observed for the current user:
  - The current user information shall be reloaded from the user service.
  - The in-memory representation of current user (name, role) shall be updated.

### 7.3 UI Behavior on Role Changes

- If the current user role changes such that they lose editing permissions for books:
  - Clicking rows in the inventory grid shall no longer open the book edit form.

- If the current user role changes such that they no longer have admin permissions:
  - Navigation to the admin view shall be denied; the error view shall be shown instead.


## 8. Inventory / Books View

### 8.1 Grid Display

- The inventory view shall display a grid of products with at least the following columns:
  - Product name.
  - Price (formatted as currency, using the current locale’s formatting).
  - Availability (rendered status with icon and text).
  - Stock count.
  - Categories (one or more).

- On entering the inventory view:
  - The system shall fetch the list of products from the product service.
  - The grid shall be populated with the fetched products.
  - An assistive notification equivalent to "Inventory opened" shall be announced.

### 8.2 Row Selection and Form Toggle

- When the user clicks a row in the grid:
  - An edit form for the corresponding product shall open.
  - Focus shall remain in the grid for keyboard navigation.
  - An assistive notification announcing that the particular product was opened shall be shown.

- When the user clicks the same row again:
  - The edit form shall close.
  - The "new product" button shall be re-enabled.
  - Form fields shall be reset to their default/empty state.

### 8.3 Keyboard Navigation within Grid and Form

- When the form is open for a selected row:
  - Pressing Page Down shall move the selection to the next row and update the form with that row’s data (if a next row exists).
  - Pressing Page Up shall move the selection to the previous row and update the form (if a previous row exists).
  - Pressing Page Up while on the first row shall keep selection on the first row and keep the same form contents.

- A keyboard shortcut (e.g., Esc via an associated button) shall close the form when invoked.

### 8.4 Filter Field

- The inventory view shall provide a filter field controlling the grid’s content.
- A global shortcut (e.g., Ctrl+F) shall focus the filter field.
- When a filter value is entered:
  - The grid shall filter products (at minimum by product name) so that only matching products remain visible.

### 8.5 New Product Form Behavior

- The view shall provide a "new product" button.
- When the "new product" button is clicked:
  - An empty product form shall be shown.
  - The "new product" button shall be disabled while the form is open.
  - Focus shall move to the product name field.

- The form shall contain fields for at least:
  - Product name.
  - Price.
  - Availability.
  - Stock count.
  - Category selection (multi-select).

- Initially, before user input:
  - "Save" and "Discard" actions on the form shall be disabled.

### 8.6 Validation Rules and Error Messages

- The system shall validate consistency between availability and stock count, for example:
  - Raising a validation error when availability indicates not-ready-for-sale (e.g., COMING) but stock count is non-zero.

- When this consistency check fails:
  - The availability field shall be marked invalid with a specific error message (e.g., "Mismatch between availability and stock count").
  - The stock-count field shall also be marked invalid with the same error message.
  - The "save" action shall remain disabled.

- When the user corrects the stock count to a valid value:
  - Validation errors on both fields shall be cleared.
  - The "save" action shall become enabled (assuming other fields are valid).

### 8.7 Saving a New Product

- When the user enters valid information for a new product and activates the "save" action:
  - The product shall be persisted via the product service.
  - The form shall close.
  - Focus shall return to the grid.
  - The "new product" button shall be re-enabled.
  - A notification shall be shown indicating that the product was created or updated (e.g., "\"Filter book\" updated").

- After saving:
  - The new product shall appear at the end of the grid.
  - Its row shall show correct values for name, price, and stock.
  - Filtering the grid by the new product’s name shall result in exactly one matching row with the correct values.

### 8.8 Discard and Cancel Behavior

- When the form has been modified and the user clicks "Cancel":
  - A confirmation dialog shall appear asking whether to discard unsaved changes.

- If the user cancels the confirmation:
  - The edit form shall remain open with its current values.

- If the user confirms discard:
  - Fields shall be reset to empty or default values (e.g., default availability, zero stock, no categories).

- After discarding and then cancelling:
  - The form shall close.
  - The grid shall regain focus.

### 8.9 Availability Rendering and Accessibility

- The availability column shall render as structured, styled content including:
  - An icon element using a theme-defined class and color based on availability state.
  - An element carrying an aria-label describing the availability and, when appropriate, stock count.
  - A visible label with the availability text.

- The theme shall define colors for each availability state (e.g., AVAILABLE, DISCONTINUED, COMING), and these shall be used in the availability rendering.

### 8.10 Category Selection Behavior

- The form’s category selector shall list all categories from the category service.
- When some categories are selected for a product:
  - Those selected categories shall appear first in the selector’s visible order, followed by unselected categories.


## 9. Statistics Dashboard

### 9.1 Dashboard Layout

- The statistics view shall display a dashboard containing multiple charts, including:
  - A chart of product counts grouped by price ranges (e.g., "0 - 10 €", "10 - 20 €", "20 - 30 €", "40 - 50 €").
  - A chart of product counts by availability state (AVAILABLE, COMING, DISCONTINUED).
  - A chart (or combination of charts) showing per-category statistics:
    - Number of titles per category.
    - Total stock count per category.

- When the statistics view is opened:
  - An assistive notification equivalent to "Statistics opened" shall be announced.
  - The availability chart shall receive keyboard focus by default.

### 9.2 Data Population and Zero-Value Handling

- The view shall obtain statistics from the product service based on current product and category data.
- When building chart data:
  - Categories or range buckets with zero count shall be omitted from the chart series.

### 9.3 Reacting to Category Updates

- The statistics view shall observe category update events that indicate changes such as rename or save.
- When a category name is changed elsewhere in the system:
  - The charts shall be updated so that:
    - The new category name appears with the correct counts.
    - The old category name is removed from the chart.

### 9.4 Reacting to Product Save/Delete

- The statistics view shall observe product save and delete events (directly or via a presenter/service).
- When a new product is saved:
  - Price-range counts shall be updated to reflect the new product’s price.
  - Availability counts shall be updated to reflect the new product’s availability state.
  - Per-category title counts and stock counts shall be incremented for each selected category.

- When that product is later deleted:
  - All of the above counts shall revert as if the product never existed.


## 10. Admin – User Management

### 10.1 Initial State

- The user management view shall present:
  - A user-selection control listing existing users.
  - A form with fields: username, password, password confirmation, role.
  - Buttons/actions: New, Save, Delete, Cancel.

- On initial load:
  - The form shall be disabled.
  - Form fields shall be empty.
  - Only the "New" button shall be enabled.
  - Save, Delete, and Cancel buttons shall be disabled.

### 10.2 New User Creation

- When the "New" button is clicked:
  - The form shall be enabled for input.
  - Focus shall move to the username field.
  - The Cancel button shall become enabled.
  - Save and Delete remain disabled until inputs are valid.

- The system shall validate that:
  - Password and password confirmation fields match; otherwise, mark the password field invalid and show an error (e.g., "Passwords do not match").
  - A role is selected; otherwise, mark the role field invalid with an error (e.g., "The role is mandatory").

- Save button enablement rules:
  - Save remains disabled until username, passwords, and role are all valid.

- When Save is clicked and validation succeeds:
  - The user shall be persisted via the user service.
  - A notification shall be shown indicating success (e.g., "User \"Tester\" saved.").
  - The form shall be cleared.
  - The form shall return to its initial disabled state.

### 10.3 Selecting and Editing Existing Users

- Selecting a user from the user-selection control shall:
  - Enable the form.
  - Populate the fields with the selected user’s data (except passwords, which may remain empty or masked as appropriate).

- When a user is edited and Save is clicked:
  - The user service shall attempt to persist the changes.

- If a concurrent modification is detected (another actor updated the user since it was loaded):
  - The save operation shall fail.
  - A notification such as "Save conflict, try again." shall be shown.
  - The form shall be cleared and disabled, returning to the initial state.

### 10.4 Deleting Users

- When a user is selected and the Delete button is clicked:
  - A confirmation dialog shall appear, describing which user will be deleted (e.g., "\"User0\" will be deleted.").

- If the deletion is confirmed:
  - The user service shall remove the user.
  - A notification such as "User \"User0\" removed." shall be shown.
  - The form shall be disabled and cleared.

### 10.5 Cancel Behavior

- When editing an existing user and Cancel is clicked:
  - The form shall be cleared back to the initial, disabled state.
  - Save, Delete, and Cancel buttons shall be disabled; New shall be enabled.

### 10.6 Duplicate Username Handling

- When attempting to save a new user with a username that already exists:
  - The save shall be rejected.
  - A notification such as "Username \"Super\" is a duplicate." shall be shown.
  - The form shall be cleared and reset to the initial disabled state.

### 10.7 Role Restrictions for Current Admin

- When the current logged-in admin selects their own account in the user management view:
  - The role field shall be disabled to prevent changing their own role.

### 10.8 Unsaved Changes and Tab Errors

- The user management view resides in a tabbed admin interface.
- When there are unsaved changes in the user form and the admin attempts to switch to another tab:
  - The system shall mark the user management tab with a component-level error (e.g., error indicator and message such as "The form has unsaved changes.").

- When the admin returns to the user management tab and clicks Cancel to discard changes:
  - The tab’s error indication shall be cleared.

- While there are unsaved changes:
  - The UI poll interval shall be set to a non-default (e.g., 60 seconds).
- After the form becomes clean (e.g., via Cancel):
  - The poll interval shall be reset to its default value.


## 11. Admin – Category Management (High-Level)

- The admin view shall include a category management sub-view.
- When the admin view is opened:
  - The category management sub-view shall be the default visible sub-view.
  - Focus shall move to a "New Category" action/button.

- The category management sub-view shall allow basic CRUD for categories (names, possibly other attributes) and shall emit appropriate events for the statistics view and inventory view to react to.


## 12. About View and System Messages

### 12.1 Admin Note Editing

- The About view shall display an admin-maintained note using rendered HTML.
- An "Edit" action shall switch the note into an editable text area and hide the Edit button.
- While in edit mode:
  - The text area shall be visible and focused.

- When the admin finishes editing by moving focus away (e.g., clicking elsewhere) or using a designated action:
  - The new content shall be sanitized to remove unsafe HTML (e.g., stripping script-related attributes while preserving basic formatting like `<b>` tags and safe `<img>` tags without dangerous attributes).
  - The sanitized content shall be applied to the note label.
  - The text area shall be hidden.
  - The Edit button shall become visible again.

- A notification shall be shown with:
  - Caption matching the note’s caption.
  - Description showing the sanitized content.

### 12.2 Keyboard Shortcut for Save

- While editing the note, a keyboard shortcut (e.g., Ctrl+S) shall close the editor and return to display mode, applying the current text (with sanitization).

### 12.3 Message Events

- The About view shall listen for message events from the event bus.
- When a new message event is received:
  - The admin note label shall be updated to show the message text.
  - A notification shall be shown where:
    - Caption matches the note’s caption.
    - Description shows the message text.
  - If the note editor is open, it shall be closed.

### 12.4 Shutdown Flow

- The About (or admin) view shall provide a "Shutdown" action.
- When the Shutdown action is invoked:
  - A confirmation dialog with a caption "Shutdown" shall be shown.

- If the confirmation is accepted:
  - A notification shall be shown with text such as "You will be logged out in 60 seconds.".
  - (Actual process termination / logout mechanics are outside the scope of this UI PRD but must align with this contract.)


## 13. Event Bus

### 13.1 Core Behavior

- The application shall provide a central event bus responsible for dispatching domain events to listeners.
- Components (views, presenters, services) shall be able to register listeners implementing a common event-listener interface.
- Registered listeners shall be invoked when relevant events are posted to the bus.

### 13.2 Listener Lifecycle

- Listeners shall be explicitly unregisterable.
- The event bus shall store listeners in a way that allows unused listeners to be garbage-collected (e.g., weak references).
- When a listener is garbage-collected, it shall no longer receive events.

### 13.3 Integration with Backend Pub/Sub

- The event bus shall integrate with a backend pub/sub mechanism that transmits events between nodes.
- When a message envelope is received from the backend:
  - The event bus shall deserialize or unwrap the contained domain event.
  - The event shall be dispatched locally to all appropriate listeners as if it were posted locally.

### 13.4 Logging / Metrics

- When events are dispatched, the system shall log or otherwise record the number of listeners that received the event (e.g., "event fired for 1 recipients.").


## 14. Locking and Concurrency

### 14.1 Local Locking

- The system shall provide a locking service that can:
  - Lock a given object identified by its type and identifier for a specific user.
  - Record which user currently holds the lock.
  - Report whether a given object is locked and, if so, by which user name.

- When a lock is requested for an object that is already locked by another user:
  - The locking attempt shall fail and raise an error (e.g., IllegalStateException in implementation terms).
  - The original lock ownership shall remain unchanged.

- When an object lock is released by the holder:
  - The locked state shall be cleared for that object.

### 14.2 Locking Events and Remote Propagation

- The locking service shall publish locking and unlocking events via the event bus, including:
  - Object type.
  - Object identifier.
  - User identifier.
  - User name.
  - Boolean flag indicating locked/unlocked.

- The locking service shall observe locking events from the event bus corresponding to operations initiated on other nodes.
- When a remote locking event is received:
  - The local locked state shall be updated to reflect the remote lock or unlock.


## 15. Accessibility and Assistive Notifications

### 15.1 Assistive Notifications

- The application shall provide assistive (screen-reader-oriented) notifications for key state changes, including at least:
  - Opening the Inventory view (e.g., "Inventory opened").
  - Opening the User Management view (e.g., "Users opened").
  - Opening the Statistics view (e.g., "Statistics opened").
  - Opening the About view (e.g., localized messages like "Tietoja avattu" in Finnish).
  - Opening and closing the navigation menu.

- Assistive notifications shall be delivered in a way that does not obscure the main UI (e.g., visually hidden or positioned away from main content but announced to screen readers).

### 15.2 Keyboard Accessibility

- All primary actions (navigation, form actions, filter focus, chart access) shall be accessible via keyboard.
- Focus handling shall be explicit and predictable:
  - Views shall set focus to the most relevant component on entry (e.g., charts in stats, form fields, new-category button).
  - Keyboard shortcuts shall not conflict with common browser shortcuts where avoidable.

### 15.3 ARIA and Error Indicators

- Elements representing availability, unsaved-changes indicators, and other stateful UI aspects shall use appropriate aria attributes and error indicators to convey state to assistive technologies.


## 16. Serialization and Session Safety

- Core views and components (e.g., About view, Statistics view, User Management view) shall be serializable so that the server can safely serialize UI state between requests if needed.
- No non-serializable resource (e.g., open streams, threads) shall be stored directly in view instances or other UI state objects.


---

This PRD intentionally mirrors the behavior validated by the existing automated tests in a framework-neutral manner. Any migration or reimplementation (e.g., to Vaadin 24 or another UI framework) should preserve these requirements unless explicitly superseded by new design decisions.