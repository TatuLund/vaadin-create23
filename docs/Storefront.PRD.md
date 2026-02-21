# Storefront & Purchases Epic – Product Requirements

## 0. Context & Goals

We add an **internal purchase tool** on top of the existing Vaadin demo:

- Employees (role `CUSTOMER`) create purchase requests for books.
- Supervisors (`USER`) and admins (`ADMIN`) approve or reject requests.
- Approval is the **business checkout** point where stock is decremented.
- We maintain a reliable **audit trail** (who requested, who approved, when, what address, what prices).

This epic is implemented in **small, functional steps**. Each step:

- Is shippable on its own.
- Builds on previous steps.
- Explicitly covers concurrency and edge cases discovered in design.

Technologies/constraints:

- Backend: Hibernate + DAOs + singleton services using `HibernateUtil` (same as existing services).
- UI: Vaadin (Navigator, `Composite`, `Grid`, callback `DataProvider`).
- **Components are *not* singletons.** Each view creates its own component instances.
- Services (`*Service`) are singletons.

---

## 1. Step 1 – Purchase Domain Model

### 1.1 Scope

Introduce backend domain model and service interface for purchases, without changing any UI.

### 1.2 Requirements

#### 1.2.1 Entities

1. **`Address`** (value object)
   - JPA `@Embeddable`.
   - Fields (all `String`):
     - `street`
     - `postalCode`
     - `city`
     - `country`
   - Used as:
     - Default address for a `User` (optional, later),
     - Snapshot `deliveryAddress` in `Purchase`.

2. **`PurchaseStatus`** enum
   - Values:
     - `PENDING` – waiting for supervisor decision.
     - `COMPLETED` – approved, stock decremented; workflow finished.
     - `REJECTED` – explicitly rejected, no stock change.
     - `CANCELLED` - there were items missing in the inventory at the time of approval
   - No “reservation” / “pre-booking” statuses in this epic.

3. **`Purchase`** entity
   - Fields:
     - `id` – primary key, inherited from AbstractEntity
     - `requester` – `@ManyToOne User` (employee / `CUSTOMER`).
     - `approver` – `@ManyToOne User` (supervisor / `USER` or `ADMIN`).
       - May be `null` while `PENDING` (not yet decided).
     - `status` – `PurchaseStatus`, default `PENDING`.
     - `createdAt` – `Instant`, set when purchase is first created.
     - `decidedAt` – `Instant`, set when status becomes `COMPLETED` or `REJECTED`.
     - `decisionReason` – `String`, optional; reason for `REJECTED` or comments.
     - `deliveryAddress` – `@Embedded Address`:
       - **Snapshot** of the address used for this specific purchase.
   - Associations:
     - `lines` – `@OneToMany List<PurchaseLine>`:
       - `mappedBy = "purchase"`, `cascade = ALL`, `orphanRemoval = true`.
   - Derived values:
     - `getTotalAmount()`:
       - `@Transient` method.
       - Sums `PurchaseLine.getLineTotal()` over `lines`.
       - **No** `totalAmount` column; avoid denormalized totals.

4. **`PurchaseLine`** entity
   - Fields:
     - `id` – primary key, inherited from AbstractEntity
     - `purchase` – `@ManyToOne Purchase` (non-null).
     - `product` – `@ManyToOne Product` (book).
     - `quantity` – `int`, positive.
     - `unitPrice` – `BigDecimal`:
       - Snapshot of price at *request creation time*.
   - Derived value:
     - `getLineTotal()`:
       - `@Transient`, `unitPrice * quantity`.
   - **Do not** persist `lineTotal`.

5. **`UserSupervisor`** entity (mapping)
   - Purpose: maps **employee → default supervisor**.
   - Fields:
     - `id` – primary key, inherited from AbstractEntity
     - `employee` – `@ManyToOne User` (role `CUSTOMER`).
     - `supervisor` – `@ManyToOne User` (role `USER` or `ADMIN`).
   - Used only to find **current default approver** at request creation time.
   - Historical approver is always read from `Purchase.approver`.

#### 1.2.2 DAO and service

6. **`PurchaseDao`**
   - Follows pattern of existing DAOs (e.g. `ProductDao`).
   - CRUD operations for `Purchase` and queries based on:
     - requester,
     - approver,
     - status,
     - paging (`offset`, `limit`).
   - Additional queries used by the UI:
     - Find default supervisor for an employee (`UserSupervisor` mapping).
     - Find recently decided purchases for login-time notification.

7. **`PurchaseService` interface** (singleton service)
   - Responsibility: **aggregate boundary** for purchases.
   - Methods (actual implemented signatures):
     - `Purchase createPendingPurchase(Cart cart, Address address, User requester, User defaultApproverOrNull)`
       - Creates a `Purchase` with:
         - `status = PENDING`,
         - `createdAt = now` (also defaulted by `Purchase` constructor),
         - `requester` set,
         - `approver` initial value:
           - Either explicitly passed in,
           - Or resolved from `UserSupervisor` mapping when `defaultApproverOrNull == null`.
         - `deliveryAddress` snapshot from `address`.
         - `lines` built from `cart`:
           - `unitPrice` from current `Product` price,
           - `quantity` from cart,
           - Derived totals only in getters.
       - **No stock modification** here.
       - Throws `IllegalArgumentException` if the cart is empty.
     - Simple query methods (paging):
       - `List<Purchase> findMyPurchases(User requester, int offset, int limit)`
       - `long countMyPurchases(User requester)`
       - `List<Purchase> findAll(int offset, int limit)`, `long countAll()`
       - `List<Purchase> findPendingForApprover(User approver, int offset, int limit)`, `long countPendingForApprover(User approver)`
     - Shared history query API used by the UI component (Step 4):
       - `List<Purchase> fetchPurchases(PurchaseHistoryMode mode, int offset, int limit, User currentUser)`
       - `long countPurchases(PurchaseHistoryMode mode, User currentUser)`
     - Login-time status-change query (Step 3):
       - `List<Purchase> findRecentlyDecidedPurchases(User requester, Instant since)`
     - Approval/rejection methods will be added in a later step.

8. **`PurchaseServiceImpl`**
   - Singleton with `getInstance()` similar to existing services.
  - Uses `PurchaseDao` and `HibernateUtil` helpers (DAO methods wrap `inSession` / `inTransaction` / `saveOrUpdate`).
   - **No** Vaadin dependencies in backend.
  - Dev/UX helper: can generate mock purchases on startup if the DB has none (controlled via system property `generate.data`, default enabled).

### 1.3 Acceptance Criteria

- Integration tests verify:
  - `createPendingPurchase` creates a `PENDING` purchase with address + price snapshots and derived totals.
  - Stock is **not** modified when creating a pending purchase.
  - Paging queries (`findMyPurchases`, `countMyPurchases`, `findAll`, `findPendingForApprover`) work.
- No UI changes yet; application runs unchanged.

---

## 2. Step 2 – Storefront Wizard Skeleton & Pending Requests

### 2.1 Scope

Introduce `StorefrontView` for `CUSTOMER` role with a **wizard** that:

- Builds an in-memory cart,
- Captures delivery address,
- Captures supervisor,
- Creates a **PENDING** `Purchase` via `PurchaseService`.

No approval UI, no stock changes yet.

### 2.2 Requirements

#### 2.2.1 StorefrontView layout

1. New `StorefrontView` (Vaadin view):
   - Visible for role `CUSTOMER` only.
   - Registered in navigation/menu similarly to `BooksView`.
   - Layout:
     - Root `CssLayout` with two child panels:
       - Left: `PurchaseWizard` component.
       - Right: purchase history panel.
   - Responsive behavior:
     - Wide screens: flex row (wizard left, history right).
     - Narrow screens: flex column (wizard above history).
     - Implemented via theme SCSS mixins (storefront + responsive rules).

#### 2.2.2 PurchaseWizard behavior

2. `PurchaseWizard` is a separate `Composite` with a simple stepper:

   Steps:

   1. **Select books & quantities**
      - Shows orderable products via backend `ProductDataService.getOrderableProducts()`.
      - UI is a multi-select grid:
        - Select one or more products.
        - Quantity input is shown for selected rows.
        - Footer shows total price and total quantity.
      - Validations:
        - Must select at least one product.
        - Only positive quantities are added to the cart.
      - Result: in-memory `Cart` owned by the wizard (not persisted).

   2. **Delivery address**
      - Shows a form with address fields:
        - Street, Postal code, City, Country – all required.
      - Behavior:
        - Starts empty (no user default address prefill currently).
        - User must fill all required fields.
      - Wizard *must not* advance from this step if validation fails.

   3. **Supervisor selection**
      - Shows a required `ComboBox<User>` (or similar) listing valid approvers:
        - Users with roles `USER` or `ADMIN`.
      - Pre-selection: none (user must pick one).
      - Validation:
        - Cannot proceed without selecting a supervisor.

   4. **Review & submit**
      - Shows read-only summary:
        - Line items with product name, quantity, current unit price, derived line totals.
        - Total amount.
        - Delivery address.
        - Selected supervisor.
      - Also shows an assistive notification containing the summary text.
      - “Submit” button:
        - Calls `PurchaseService.createPendingPurchase(cart, address, requester, supervisor)`.
        - On success:
          - Clears in-memory cart.
          - Resets wizard to first step.
          - Shows confirmation message indicating:
            - Purchase created with `PENDING` status,
            - Supervisor responsible for approval.

3. **Edge cases for Step 2**

   - **Products edited by admin while wizard is open**:
     - At this stage, we **do not** handle concurrency yet; submitting simply snapshots:
       - Current product price as `unitPrice`,
       - Current product metadata (name, etc.) as read from `Product` at submission time.
     - No stock changes yet, so no inventory conflicts.
   - **Empty cart**:
     - Wizard must **not** allow submission with an empty cart.
   - **Missing address or supervisor**:
     - Wizard must block “Next”/“Submit” until required data is filled in.

### 2.3 Acceptance Criteria

- `StorefrontView` visible for `CUSTOMER`.
- Wizard steps work end-to-end and create `PENDING` purchases via `PurchaseService`.
- No stock is modified when submitting.
- No changes to existing admin (`BooksView`) flows.
- Application navigates correctly; Storefront is functional.
- Storefront wizard flow is covered by UI unit tests in the same style as existing view tests.

---

## 3. Step 3 – Customer “My Purchases” History & Login-time Notifications

### 3.1 Scope

Extend backend and UI so that:

- Customers see a **paged history** of their own purchases and statuses.
- On login, customers are informed about **status changes** (completed/rejected/cancelled) since their last session.

### 3.2 Requirements

#### 3.2.1 Backend queries

1. Implement `PurchaseService.findMyPurchases` and `countMyPurchases`:
   - Query `Purchase` by `requester = currentUser`.
   - Support paging (offset/limit).
   - Sort order:
     - `createdAt` descending by default.

2. In `User` entity, add a field:

   - `lastStatusCheck` (type `Instant`, nullable):
     - Represents last time the user’s purchase statuses were shown in a “summary” dialog.
     - Initially `null`; treat as “no previous checks”.

#### 3.2.2 StorefrontView – history panel

3. `StorefrontView` shows purchase history using the shared `PurchaseHistoryGrid` component (Step 4) in `PurchaseHistoryMode.MY_PURCHASES`.

   - Must use **callback DataProvider** internally in the component.
   - Visible columns for `MY_PURCHASES` mode:
     - toggle/details control,
     - `createdAt`,
     - `status`,
     - `totalAmount` (derived).
   - Row details (toggle by click) show:
     - ID / reference,
     - `approver` (or “Pending” if null),
     - `decidedAt` (may be null),
     - `decisionReason` (if present),
     - line items.
   - Grid is **read-only** for the customer.

4. The history panel must be **performant**:
   - If there are many purchases, paging via callback provider improves performance.
   - Sorting (e.g. by date, status) is allowed but not required in this step.

#### 3.2.3 Login-time status-change summary

5. On user login (or first navigation into the app for the session):

   - Determine `since`:
     - If `lastStatusCheck != null`: `since = lastStatusCheck`.
     - Else: `since = now - 30 days`.
   - Query purchases for this user where:
     - `requester = currentUser`,
     - `status IN (COMPLETED, REJECTED, CANCELLED)`,
     - `decidedAt > since`.

6. If any such purchases exist:

   - Show a **notification** summarizing the changes:
     - At minimum, count per status:
       - e.g. “2 purchases completed, 1 rejected since your last session.”
   - No navigation action/button is currently provided; the message prompts the user to check the history grid.

7. After showing the summary:

   - Update `User.lastStatusCheck = now` and persist.

### 3.3 Edge Cases

- **User logs in but has no decided purchases**:
  - No dialog is shown.
- **User logs in frequently**:
  - `lastStatusCheck` ensures they only see new changes.
- **Status changes while user is online**:
  - Live notifications are not implemented yet; they see changes:
    - After manual refresh / navigation, or
    - On next login via summary.
  - Live updates may be implemented in a later step.

### 3.4 Acceptance Criteria

- Customer can see their own purchase history with correct statuses and amounts.
- On entering Storefront, customer gets a status update tray notification when there have been new `COMPLETED`/`REJECTED`/`CANCELLED` decisions since the last check.
- Summary updates `lastStatusCheck` correctly.
 - Storefront history and login-time summary behavior are covered by UI unit tests similar to current views.

---

## 4. Step 4 – Shared PurchaseHistoryGrid & PurchasesView Shell

### 4.1 Scope

Introduce reusable history component and a top-level `PurchasesView` (for USER/ADMIN) that will later host approvals and stats.

### 4.2 Requirements

#### 4.2.1 PurchaseHistoryGrid component

1. Create `PurchaseHistoryGrid` `Composite` with internal `Grid<Purchase>`.

2. It must support multiple **modes** via backend enum `PurchaseHistoryMode`:

   - `MY_PURCHASES` – for customer:
     - Filter: purchases where `requester = currentUser`.
   - `ALL` – for admin history:
     - No filter (or broader org-based filter if needed).
   - `PENDING_APPROVALS` – for approvals:
     - Filter: `status = PENDING` and `approver = currentUser`.

3. The filtering logic is performed in **backend**:

  - `PurchaseService.fetchPurchases(mode, offset, limit, currentUser)` and `countPurchases(mode, currentUser)`.
  - UI must not construct JPQL or SQL; it only passes mode and paging info.

4. `PurchaseHistoryGrid` must:

  - Configure shared columns (when visible): ID, requester, approver, createdAt, status, decidedAt, total amount, decisionReason.
  - Include a toggle/details control column and row details showing purchase line items.
   - For `PENDING_APPROVALS` mode:
     - Additional column(s) will be added in Step 5 for approve/reject actions.
   - Provide a method `refresh()` to re-run the data provider (for use after actions).

5. **Important Vaadin constraint**:

   - This component is a **reusable class**, but each view instantiates its own instance.
   - No static/singleton `Grid` or component instances.

#### 4.2.2 PurchasesView (container for USER/ADMIN)

6. New `PurchasesView` for roles `USER` and `ADMIN`:

   - Uses existing `TabNavigator` (as with other views).
   - Tabs:
     - `History` – shows all purchases (`PurchaseHistoryGrid` with `PurchaseHistoryMode.ALL`).
     - `Approvals` – initially placeholder (grid or label), will be implemented in Step 5.
     - `Stats` – placeholder for Step 6.

7. Wire navigation/menu:

  - `PurchasesView` accessible under `purchases` (Vaadin Navigator fragment path `#!/purchases`) for `USER`/`ADMIN`.
   - `StorefrontView` remains separate for `CUSTOMER`.

### 4.3 Acceptance Criteria

- `PurchaseHistoryGrid` is used:
  - In `StorefrontView` for `MY_PURCHASES`.
  - In `PurchasesView` → History tab for `ALL`.
- Each view has its own grid instance; no shared component instances.
- Tabs in `PurchasesView` show:
  - History (functional),
  - Approvals & Stats (stub content for now).
 - Purchase history views using `PurchaseHistoryGrid` are covered by UI unit tests similar to current views.

---

## 5. Step 5 – Approval Workflow & Stock Adjustment

### 5.1 Scope

Implement approval/rejection workflow:

- `USER`/`ADMIN` can approve or reject `PENDING` purchases.
- **Approval** performs the actual stock update and finalizes the purchase.
- Handle optimistic locking and stock edge cases.

### 5.2 Requirements

#### 5.2.1 Backend: approval/rejection

1. Extend `PurchaseService`:

   - `Purchase approve(long purchaseId, User currentUser, String decisionCommentOrNull)`
     - Behavior:
       - Load `Purchase` in a transaction.
       - Verify:
         - `status == PENDING`.
         - `currentUser` is allowed:
           - Has role `USER` or `ADMIN`.
           - Must match the assignee: `Purchase.approver == currentUser`.
       - For each `PurchaseLine`:
         - Reload associated `Product` with proper locking (optimistic via version).
         - Check stock:
           - If `product.stock < quantity` for any line:
             - **Do not** update stock.
             - Set:
               - `status = CANCELLED`,
               - `decidedAt = now`,
               - `decisionReason` to a human-readable summary of missing quantities/products.
             - Persist and return the updated purchase.
       - If all lines have sufficient stock:
         - Decrement stock for each `Product` by `quantity`.
         - Persist updated products.
         - Set:
           - `status = COMPLETED`,
           - `approver` remains as-is (assignee),
           - `decidedAt = now`,
           - `decisionReason`:
             - `decisionCommentOrNull` (nullable).
       - Commit transaction.
       - Concurrency conflicts:
         - Let `OptimisticLockException` bubble out (do not wrap into a domain exception), consistent with existing services.

   - `Purchase reject(long purchaseId, User currentUser, String reason)`
     - Behavior:
       - Load `Purchase` in transaction.
       - Verify `status == PENDING`.
       - Set:
         - `status = REJECTED`,
         - `approver` remains as-is (assignee),
         - `decidedAt = now`,
         - `decisionReason = reason` (required).
       - **No stock change**.
       - Persist.

2. **Status transitions**:

   - Allowed:
     - `PENDING → COMPLETED` via `approve`.
     - `PENDING → REJECTED` via `reject`.
     - `PENDING → CANCELLED` via `approve` when there is insufficient stock at approval time.
   - Disallowed (must throw an exception):
     - Approving or rejecting a non-PENDING purchase.

   For this epic, use `IllegalArgumentException` (or equivalent) for invalid transitions.

3. **Concurrency edge cases**

   - **Admin modifying Product while approval happens (direction A)**:
     - Approval may encounter optimistic lock conflict when updating stock.
     - In this case:
       - Approval must **fail** (surface `OptimisticLockException` to UI/presenter).
       - `Purchase` remains `PENDING`.
       - No partial stock changes.
   - **Purchase approval changing Product before admin saves (direction B)**:
     - Admin’s `BooksView` already uses optimistic locking.
     - Requirement:
       - Existing behavior must be preserved:
         - Admin save fails with conflict,
         - UI reloads current data and shows message.

#### 5.2.2 ApprovalsView UI

4. Implement `ApprovalsView`:

   - Uses `PurchaseHistoryGrid` in `PENDING_APPROVALS` mode.
   - Filter in backend:
     - `status = PENDING`,
     - `approver = currentUser`.
   - Add columns/controls for actions:
     - Use a component column (`grid.addComponentColumn(...)`) with “Approve” and “Reject” buttons per row.
       - Render buttons only when the state is `PENDING`.
       - Wrap the buttons in `HorizontalLayout`.
       - Use disable-on-click on both buttons to prevent double submits.
     - Decision comment modal:
       - Clicking either button opens a modal sub-window (placed in the `purchases` package).
       - The window contains:
         - a text field/area for decision comment,
         - `Cancel` button,
         - `Approve` or `Reject` confirmation button.
       - `Reject` requires a non-empty reason (trimmed); `Approve` comment is optional.
   - On approve:
     - Call `PurchaseService.approve(purchaseId, currentUser, decisionCommentOrNull)`.
     - If the result is `CANCELLED`:
       - Show a message detailing which products failed due to lack of stock (based on `decisionReason`).
     - Catch `OptimisticLockException` (optimistic lock conflict):
       - Retry by calling `PurchaseService.approve(purchaseId, currentUser, decisionCommentOrNull)` again.
         - Use the same `decisionCommentOrNull` value for the retry.
       - If the retry still fails with `OptimisticLockException`:
         - Show message that products changed during approval.
         - Prompt user to reload the grid and try again, or reject with reason.
     - On success:
       - Show success notification.
       - Call `PurchaseHistoryGrid.refresh()`.

   - On reject:
     - Ask for decision reason (simple dialog with text field).
    - Call `PurchaseService.reject(purchaseId, currentUser, reason)`.
     - Refresh grid.
     - Customer will see rejection reason in history (Step 3).

5. **Security & validation**

   - UI must pass `currentUser` down to service; service re-validates:
     - Only `USER`/`ADMIN` may approve/reject.
     - Only `PENDING` purchases can be modified.
   - UI must not rely solely on client-side validation.

### 5.3 Acceptance Criteria

- Supervisors/admins can:
  - See a list of pending approvals addressed to them.
  - Successfully approve and cause stock updates.
  - Reject with a reason, without stock changes.
- Insufficient stock and optimistic locking conflicts are surfaced clearly to the approver.
- `Purchase` statuses and `Product` stocks are consistent.
 - Approval workflow and edge cases are covered by UI unit tests in the same style as existing views.

---

## 6. Step 6 – Purchase Statistics View

### 6.1 Scope

Replace the placeholder “Stats” tab in `PurchasesView` with real purchase statistics charts, similar in spirit to existing product `StatsView` (Vaadin Charts + async loading).

### 6.2 Requirements

1. **Backend queries**

   Implement in `PurchaseService` (or dedicated `PurchaseStatsService`) the aggregations required by the charts below.

   Definitions:
   - Only **COMPLETED** purchases are included.
   - Use `Purchase.decidedAt` for time bucketing.
   - “Quantity” means sum of `PurchaseLine.quantity` across included purchases.
   - “Amount” means sum of `PurchaseLine.unitPrice * PurchaseLine.quantity` across included purchases.

   Required aggregation APIs (exact class/DTO names can vary, but returned data must support these):
   - **Top purchased products by quantity** (descending):
     - Return the **top 10** products by purchased quantity.
     - Each item must include at least: product id, product name, quantity.
   - **Least purchased products by quantity** (ascending):
     - Return the **bottom 10** products by purchased quantity.
     - To keep the chart meaningful, exclude products with **zero** purchased quantity (i.e. only consider products that appear in at least one completed purchase line).
     - Each item must include at least: product id, product name, quantity.
   - **Completed purchase totals per month (money)**:
     - Return monthly totals ordered by month ascending.
     - Include months with no purchases as **0** so the line chart is continuous.
     - Range: last **12 months** (including current month) by default.

2. **PurchasesStatsTab**

   Implement the “Stats” tab content (e.g. `PurchasesStatsTab`) using **Vaadin Charts**.

   Charts to display (read-only):
   - **Column chart:** “Top 10 most purchased products (by quantity)”
     - Data source: COMPLETED purchase lines aggregated by product, descending, limit 10.
     - Chart type: `ChartType.COLUMN`.
     - Stable component id: `purchases-top-products-chart`.
     - Y-axis name: Count, localized
     - X-axis categories: 1 to 10
   - **Column chart:** “Top 10 least purchased products (by quantity)”
     - Data source: COMPLETED purchase lines aggregated by product, ascending, limit 10, excluding zero-quantity products.
     - Chart type: `ChartType.COLUMN`.
     - Stable component id: `purchases-least-products-chart`.
     - Y-axis name: Count, localized
     - X-axis categories: 1 to 10
   - **Line chart:** “Completed purchases per month (total amount)”
     - Data source: monthly totals from COMPLETED purchases over last 12 months.
     - Chart type: `ChartType.LINE`.
     - X-axis: month (e.g. `YYYY-MM`).
     - Y-axis: total amount (money).
     - Y-axis: labels to have currency
     - Stable component id: `purchases-per-month-chart`.

   Loading & updates:
   - Reuse existing `StatsView` patterns:
     - Load data asynchronously using the existing executor.
     - Update the UI safely (`UI.access`/existing `Utils.access` helper).
     - Show a “loading/no data” message until data arrives.

   Styles:
   - Reuse StatsView styles from dashboard.scss as the layout is the same.

   Accessibility:
   - Follow the existing chart accessibility approach (aria labels / live region updates) as used in `StatsView`, and thus use CustomChart component from common
   package.

### 6.3 Acceptance Criteria

- Stats tab displays the three charts described above without blocking the UI.
- Charts are based only on `COMPLETED` purchases and match the underlying data used by history/approvals.
- UI unit tests verify that the chart components exist (by stable ids) and that the view can load data asynchronously (same testing style as existing view tests).

---

## 7. Step 7 – Live UI Notifications via UI EventBus

### 7.1 Scope

When both employee and supervisor are online:

- Employee’s Storefront history updates live when a supervisor approves/rejects a purchase.
- No backend EventBus refactor; events are published from UI presenters.

### 7.2 Requirements

1. Define UI-level event classes in backend module, e.g. `PurchaseStatusChangedEvent` and `PurchaseSavedEvent`. Like the other events, e.g. `BooksChangedEvent`
these are not data bearers. They should just have Purchase id.

   - The events must be lightweight because they are distributed in the cluster.
   - When handling an event, the UI presenter must fetch the latest `Purchase`
     from `PurchaseService` using the id carried by the event.
   - `PurchaseService` must provide `fetchPurchaseById(purchaseId)` (or equivalent)
     for looking up the latest `Purchase` instance by id.

2. In `ApprovalsPresenter`:

   - After a successful approve/reject (service call succeeds):
     - Publish `PurchaseStatusChangedEvent` (purchase id only) on existing UI EventBus.

3. In `StorefrontPresenter`:

   - After a customer submits a purchase for approval (service call succeeds):
     - Publish `PurchaseSavedEvent` (purchase id only) on existing UI EventBus.

4. In `PurchaseHistoryPresenter` (which is also used in `StorefrontView`):

   - Subscribe to `PurchaseStatusChangedEvent` and `PurchaseSavedEvent`.

   - On `PurchaseStatusChangedEvent`:
     - Fetch the `Purchase` using the id from the event.
     - If `purchase.requester == currentUser`:
       - Refresh the affected purchase item in the Grid.
     - Implement method to show notification in `PurchaseHistoryGrid` using Utils.access for asynchronous updates.
     - Show a short notification:
       - E.g. “Your purchase #123 was approved” or “rejected: <reason>”.

   - On `PurchaseSavedEvent`:
     - Fetch the `Purchase` using the id from the event.
     - If the current user has admin role OR the current user is the purchase approver OR the current user is requester:
       - Refresh the Grid using `DataProvider.refreshAll()` (items were added).

5. Ensure proper lifecycle:

   - Subscriptions are added in constructor and removed on detach.

### 7.3 Acceptance Criteria

- While both are online:
  - Employee sees history updated and notification shortly after supervisor action.
- Desired side effect
  - When customer submits a purchase the purchase history is automatically refreshed
  - Assert this in StorefrontTest
- When offline:
  - Behavior remains as in Step 3 (login-time summary).
 - Live notification behavior (when implemented) is covered by UI unit tests similar to current views.

---
