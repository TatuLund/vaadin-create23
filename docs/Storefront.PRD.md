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
     - `CANCELLED` - there were items missing in the invertory at the time of approval
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

7. **`PurchaseService` interface** (singleton service)
   - Responsibility: **aggregate boundary** for purchases.
   - Methods (signatures may be adapted to local style):
     - `Purchase createPendingPurchase(Cart cart, Address address, User requester, User defaultApproverOrNull)`
       - Creates a `Purchase` with:
         - `status = PENDING`,
         - `createdAt = now`,
         - `requester` set,
         - `approver` initial value:
           - Either explicitly passed in,
           - Or resolved from `UserSupervisor` (in a later step).
         - `deliveryAddress` snapshot from `address`.
         - `lines` built from `cart`:
           - `unitPrice` from current `Product` price,
           - `quantity` from cart,
           - Derived totals only in getters.
       - **No stock modification** here.
     - Simple query methods (paging):
       - `List<Purchase> findMyPurchases(User requester, int offset, int limit)`
       - `long countMyPurchases(User requester)`
       - `List<Purchase> findAll(int offset, int limit)`, `long countAll()`
       - `List<Purchase> findPendingForApprover(User approver, int offset, int limit)`, `long countPendingForApprover(User approver)`
     - Approval/rejection methods will be added in a later step.

8. **`PurchaseServiceImpl`**
   - Singleton with `getInstance()` similar to existing services.
   - Uses `PurchaseDao` and `HibernateUtil.doInTransaction(SessionFunction)` for all write operations.
   - **No** Vaadin dependencies in backend.

### 1.3 Acceptance Criteria

- Unit tests verify:
  - `Purchase` and `PurchaseLine` persist and load correctly with embedded `Address`.
  - Derived totals (`getLineTotal`, `getTotalAmount`) work as expected.
  - `createPendingPurchase`:
    - Creates `PENDING` purchase with lines, timestamps, and snapshots.
    - Does **not** modify product stock.
- No UI changes yet; application runs unchanged.

---

## 2. Step 2 – Storefront Wizard Skeleton & Pending Requests

### 2.1 Scope

Introduce `StorefrontView` for `CUSTOMER` role with a **wizard** that:

- Builds an in-memory cart,
- Captures delivery address,
- Captures (or auto-selects) supervisor,
- Creates a **PENDING** `Purchase` via `PurchaseService`.

No approval UI, no stock changes yet. Basic “My purchases” history can be a placeholder.

### 2.2 Requirements

#### 2.2.1 StorefrontView layout

1. New `StorefrontView` (Vaadin `Composite`, `View`):
   - Visible for role `CUSTOMER` only.
   - Registered in navigation/menu similarly to `BooksView`.
   - Layout:
     - Root `HorizontalLayout` (or equivalent, e.g. `CssLayout`) with two child panels:
       - Left: `PurchaseWizard` component.
       - Right: placeholder for purchase history (to be fully implemented in Step 3).
   - Responsive behavior:
     - On **wide** screens: wizard and history side-by-side (horizontal).
     - On **narrow** screens: stacked vertically.
     - Implementation can be via Vaadin responsive using CSS 
       - Desktop: both panels visible side-by-side.
       - Mobile: wizard above, history below.
       - Add responsive style rules to responsive.scss
   - Follow similar styling than the rest of the application
     - Create view specific storefront.scss for the other view specific styles

#### 2.2.2 PurchaseWizard behavior

2. `PurchaseWizard` is a separate `Composite` with a simple stepper:

   Steps:

   1. **Select books & quantities**
      - Shows available `Product`s that are *orderable*:
        - `Availability.AVAILABLE` (or similar field).
        - `stock > 0`.
      - Allows the user to:
        - Select one or more products,
        - Enter desired quantity per product.
      - Validations:
        - Quantity must be positive integer.
        - Optionally, soft-validate that requested quantity ≤ current stock (for UX; real stock check happens at approval time).
      - Result: in-memory `Cart` (simple model owned by wizard, not persisted).

   2. **Delivery address**
      - Loads **default address** for current user if such exists (Step 1 allowed for that, but if not implemented yet, default can be empty).
      - Shows a form with address fields:
        - Street, Postal code, City, Country – all required.
      - Behavior:
        - If user has saved default address, pre-fill the form.
        - User can edit before submitting.
      - Wizard *must not* advance from this step if validation fails.

   3. **Supervisor selection**
      - Shows a required `ComboBox<User>` (or similar) listing valid approvers:
        - Users with roles `USER` or `ADMIN`.
      - Pre-selection:
        - If `UserSupervisor` mapping exists for `(currentUser, supervisor)`, pre-select that supervisor.
        - Otherwise, user **must** pick one.
      - Validation:
        - Cannot proceed without selecting a supervisor.

   4. **Review & submit**
      - Shows read-only summary:
        - Line items with product name, quantity, **unit price snapshot**.
        - Derived line totals.
        - Total amount.
        - Delivery address snapshot.
        - Selected supervisor.
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
- On login, customers are informed about **status changes** (completed/rejected) since their last session.

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

3. Replace the placeholder history panel in `StorefrontView` with a real `Grid<Purchase>`:

   - Must use **callback DataProvider**:
     - `fetch` uses `PurchaseService.findMyPurchases(currentUser, offset, limit)`.
     - `count` uses `PurchaseService.countMyPurchases(currentUser)`.
   - Columns:
     - `createdAt`.
     - `status`.
     - `totalAmount` (derived).
    - Since the horizontal space is limited use Grid details to show additional information when clicking the row.
     - ID / reference.
     - `approver` (may be null for PENDING).
     - `decidedAt` (may be null).
     - `decisionReason`.
   - Grid is **read-only** for the customer.

4. The history panel must be **performant**:
   - If there are many purchases, paging via callback provider improves performance.
   - Sorting (e.g. by date, status) is allowed but not required in this step.

#### 3.2.3 Login-time status-change summary

5. On user login (or first navigation into the app for the session):

   - Determine `since`:
     - If `lastStatusCheck != null`: `since = lastStatusCheck`.
     - Else: `since = now - configurable time window` (or simply treat as “no previous checks” and skip).
   - Query purchases for this user where:
     - `requester = currentUser`,
     - `status IN (COMPLETED, REJECTED, CANCELLED)`,
     - `decidedAt > since`.

6. If any such purchases exist:

   - Show a **dialog or notification** summarizing the changes:
     - At minimum, count per status:
       - e.g. “2 purchases completed, 1 rejected since your last session.”
     - Optional: list references (ID, total, status).
   - Provide a button/action to open “My purchases” tab.

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
- On login, customer gets a summary dialog when there have been new `COMPLETED`/`REJECTED` statuses since last check.
- Summary updates `lastStatusCheck` correctly.
 - Storefront history and login-time summary behavior are covered by UI unit tests similar to current views.

---

## 4. Step 4 – Shared PurchaseHistoryGrid & PurchasesView Shell

### 4.1 Scope

Introduce reusable history component and a top-level `PurchasesView` (for USER/ADMIN) that will later host approvals and stats.

### 4.2 Requirements

#### 4.2.1 PurchaseHistoryGrid component

1. Create `PurchaseHistoryGrid` `Composite` with internal `Grid<Purchase>`.

2. It must support multiple **modes** via enum, e.g.:

   - `MY_PURCHASES` – for customer:
     - Filter: purchases where `requester = currentUser`.
   - `ALL` – for admin history:
     - No filter (or broader org-based filter if needed).
   - `PENDING_APPROVALS` – for approvals:
     - Filter: `status = PENDING` and `approver = currentUser` (or `approver IS NULL` but assigned to them by other logic; see next step).

3. The filtering logic is performed in **backend**:

   - `PurchaseService.fetchPurchases(mode, query, currentUser)` and `countPurchases(mode, query, currentUser)` (exact signatures up to implementation).
   - UI must not construct JPQL or SQL; it only passes mode and paging/sorting info.

4. `PurchaseHistoryGrid` must:

   - Configure shared columns: ID, requester, approver, status, createdAt, decidedAt, total amount, decisionReason.
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
     - `History` – shows all purchases (`PurchaseHistoryGrid` with `Mode.ALL`).
     - `Approvals` – initially placeholder (grid or label), will be implemented in Step 5.
     - `Stats` – placeholder for Step 6.

7. Wire navigation/menu:

   - `PurchasesView` accessible under a logical path (e.g. `/purchases`) for `USER`/`ADMIN`.
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

   - `Purchase approve(long purchaseId, User approver)`
     - Behavior:
       - Load `Purchase` in a transaction.
       - Verify:
         - `status == PENDING`.
         - The given `approver` is allowed:
           - Has role `USER` or `ADMIN`.
           - If business rules require, matches or is allowed to replace existing `Purchase.approver`.
       - For each `PurchaseLine`:
         - Reload associated `Product` with proper locking (optimistic via version).
         - Check stock:
           - If `product.stock < quantity` for any line:
             - **Do not** update stock.
             - Throw a **domain exception** (e.g. `InsufficientStockException`) listing offending products.
       - If all lines have sufficient stock:
         - Decrement stock for each `Product` by `quantity`.
         - Persist updated products.
         - Set:
           - `status = COMPLETED`,
           - `approver = approver` (snapshot),
           - `decidedAt = now`,
           - `decisionReason = null` (or leave as-is).
       - Commit transaction.
       - Handle `OptimisticLockException` / `ObjectOptimisticLockingFailureException`:
         - Wrap as domain exception (e.g. `ConcurrentModificationException` with details).

   - `Purchase reject(long purchaseId, User approver, String reason)`
     - Behavior:
       - Load `Purchase` in transaction.
       - Verify `status == PENDING`.
       - Set:
         - `status = REJECTED`,
         - `approver = approver`,
         - `decidedAt = now`,
         - `decisionReason = reason` (required).
       - **No stock change**.
       - Persist.

2. **Status transitions**:

   - Allowed:
     - `PENDING → COMPLETED` via `approve`.
     - `PENDING → REJECTED` via `reject`.
   - Disallowed (must throw domain exception):
     - Approving or rejecting a non-PENDING purchase.

3. **Concurrency edge cases**

   - **Admin modifying Product while approval happens (direction A)**:
     - Approval may encounter optimistic lock conflict when updating stock.
     - In this case:
       - Approval must **fail** with a clear domain exception.
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
     - `approver = currentUser` **or**
     - (If `approver` is null and logic assigns default approver on the fly; define in service.)
   - Add columns/controls for actions:
     - Either:
       - A component column with “Approve” and “Reject” buttons per row, **or**
       - A checkbox column for approve + a separate “Approve selected” button.
   - On approve:
     - Call `PurchaseService.approve(purchaseId, currentUser)`.
     - Catch:
       - `InsufficientStockException`:
         - Show message detailing which products failed due to lack of stock.
         - Purchase remains `PENDING`.
       - `ConcurrentModificationException` (optimistic lock conflict):
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

Add a simple statistics tab for purchases, similar in spirit to existing `StatsView` for products.

### 6.2 Requirements

1. **Backend queries**

   - Implement in `PurchaseService` (or dedicated `PurchaseStatsService`):
     - Aggregations such as:
       - Total purchases per requester.
       - Total amount per product category.
       - Totals per month.

2. **PurchaseStatsView**

   - Added as the “Stats” tab in `PurchasesView`.
   - Reuse existing patterns from `StatsView`:
     - Asynchronous loading (if existing infra is used).
     - Simple charts or grids; scope can be modest (a few key tables/charts).
   - Must be read-only and support at least:
     - Filter by time range (e.g. last 30 days vs all time) OR
     - Filter by requester.

### 6.3 Acceptance Criteria

- Stats tab displays aggregate purchase information without blocking the UI.
- Stats are consistent with data shown in histories and approvals.
 - Purchase statistics UI is covered by UI unit tests similar to existing stats views.

---

## 7. Step 7 – (Optional, Later) Live UI Notifications via UI EventBus

> This step is **optional** and can be implemented after everything else is stable.

### 7.1 Scope

When both employee and supervisor are online:

- Employee’s Storefront history updates live when a supervisor approves/rejects a purchase.
- No backend EventBus refactor; events are published from UI presenters.

### 7.2 Requirements

1. Define a UI-level event class in UI module, e.g. `PurchaseStatusChangedEvent`.

2. In `ApprovalsPresenter`:

   - After a successful approve/reject (service call succeeds):
     - Publish `PurchaseStatusChangedEvent` with updated `Purchase` on existing UI EventBus.

3. In `StorefrontView` (or a `PurchasesModel` used by it):

   - Subscribe to `PurchaseStatusChangedEvent`.
   - If `event.purchase.requester == currentUser`:
     - Refresh “My purchases” grid.
     - Show a short notification:
       - E.g. “Your purchase #123 was approved” or “rejected: <reason>”.

4. Ensure proper lifecycle:

   - Subscriptions are added on attach and removed on detach.
   - No memory leaks or cross-session notifications.

### 7.3 Acceptance Criteria

- While both are online:
  - Employee sees history updated and notification shortly after supervisor action.
- When offline:
  - Behavior remains as in Step 3 (login-time summary).
 - Live notification behavior (when implemented) is covered by UI unit tests similar to current views.

---
