# Data Export Feature PRD (Admin Purchase History)

Terminology in this document follows the glossary in [Ubiquitous Language](UbiquitousLanguage.md). Where existing class names or technical field names differ, the glossary defines the preferred business term.

## 1. Overview

This document specifies the Purchase History Data Export feature for admin users.

The feature extends the Admin Purchase History toolbar by adding:

- a `From` date field,
- a `To` date field,
- an `Export` action button,
- existing `Purge` action retained.

The goal is to export purchase history records for a selected date range to CSV, including purchase line items (one purchase can contain one or more items).


## 2. Scope

### 2.1 In Scope

- Admin Purchase History tab toolbar enhancement.
- Date-range validation with max allowed span of three months.
- Async export execution using the UI managed executor.
- CSV generation with OpenCSV.
- Download UX via dialog + button extended with Vaadin `FileDownloader`.
- Export data model based on flattened DTO rows (purchase + line-item data).

### 2.2 Out of Scope

- Changes to Purge business rules.
- New user roles or role model changes.
- New backend storage entities for export jobs.
- Long-term persisted export files.


## 3. Users and Access

- Primary actor: `ADMIN`.
- The feature is available only in Admin Purchase History tab.
- Authorization must be validated server-side in presenter/service flow before export starts.


## 4. UI and Interaction Requirements

## 4.1 Toolbar Composition

In the Purchase History toolbar, controls appear in this order:

1. `From` date field
2. `To` date field
3. `Export` button
4. `Purge` button

The design follows the [DataExport reference image](DataExport.png).

## 4.2 Date Fields

- Both dates are required for export.
- `From` and `To` use date-only values (no time picker in UI).
- Required indicator must be visible on both date fields.
- Future dates must be disallowed in both fields by setting field range end to today.
- Date field lower bound is constrained to today minus 24 months.
- The implementation should use DateField range limit API (`setRangeEnd`) for this constraint.

## 4.2.1 To-Date Change Grid Positioning

When user changes `To` date to a valid value, the purchase history grid should move to the first row whose purchase date matches or follows the selected `To` boundary.

- Because the grid supports `scrollTo(index)` (not `scrollToItem`), index must be resolved before scrolling.
- Add a service-level method that resolves the first matching row index for the selected range boundary.
- If a matching index exists, call grid `scrollTo(index)`.
- If no matching row exists, do not scroll and keep current position.
- Scroll operation must not execute when `To` value is null or current date inputs are invalid.

## 4.3 Export Button Enablement Rules

`Export` must be disabled when any of the following is true:

- `From` date is missing.
- `To` date is missing.
- `To` is before `From`.
- Date range exceeds three months.

`Export` is enabled only when all checks pass.

## 4.4 Three-Month Validation Rule

Range is valid if and only if:

- $to \ge from$
- $to \le from + 3\text{ months}$

Implementation must use calendar-month semantics (`plusMonths(3)`), not fixed-day assumptions.

## 4.4.1 Validation Feedback on Date Components

When the date range is invalid, the UI must provide component-level feedback in addition to disabling `Export`.

- If range exceeds three months, set a component error on the `To` date field with localized message equivalent to "Range can be at most three months".
- If `To` is before `From`, set a component error on the `To` date field with localized message equivalent to "To date cannot be earlier than From date".
- When the range becomes valid, clear the `To` field component error.
- Missing-date states may keep `Export` disabled without component error until both values are provided.
- Validation feedback must use `setComponentError` on the `To` date field (no additional custom helper text component required).

## 4.5 Export Click Loading Behavior

When user clicks `Export` and validation passes:

- Button must use disable-on-click behavior.
- Button icon changes to loading indicator.
- Button remains disabled while export job is running.

When export flow ends (success, cancel, or failure):

- Button icon is restored to normal export icon.
- Button is re-enabled if the date inputs remain valid.

## 4.6 Completion UX

When background export generation finishes successfully:

- Open a dialog that contains a `Download` button.
- `Download` button is extended with Vaadin `FileDownloader` bound to generated CSV resource.
- Dialog content includes a localized export-ready status text (instead of a separate ready notification).
- Dialog contains only the download action button; closing uses standard dialog close behavior.
- Closing the dialog resets `Export` button from loading state to normal state.

## 4.7 Failure UX

If export generation fails:

- Show visible notification with localized error message.
- Ensure loading state is always cleared.
- No broken/stale download resource should be left attached.

## 4.8 Range Row Visual Emphasis

Rows that belong to the currently selected date range should use emphasized text styling in the grid.

- Implement row style assignment with grid row class name generator.
- Apply CSS style for selected-range rows with `font-weight: 600`.
- Styling must update when date range changes.
- Rows outside selected range must not receive the emphasized style.


## 5. Asynchronous Execution Requirements

## 5.1 Executor

Export job must run asynchronously using the managed executor provided by `VaadinCreateUI.getExecutor()`.

- Do not create ad-hoc raw threads in view/presenter.
- UI updates from background completion must be marshalled to UI thread using existing safe access mechanism.

## 5.2 UI Thread Safety

All UI mutations after async completion (open dialog, notifications, button state restore) must be executed via the existing UI access helper pattern.

## 5.3 Single-Export Per View Instance

While export is running, the originating view instance must prevent duplicate starts from same button.

## 5.4 Detach and Cancellation Policy

- Export runs as an async `Future` on the UI managed executor.
- On full UI detach/logout/session close, no additional export-specific cancellation handling is required; existing UI lifecycle shuts down the executor.
- If only the originating view is detached while the same UI remains active (for example navigation to another view), cancel the running future on best effort.
- If cancellation does not preempt completion in time, completion handlers must be fault-tolerant and must not assume originating components still exist.
- No attempt is required to reopen export dialog on a different view after the originating view has detached.


## 6. Architecture and Responsibilities

## 6.1 View

Responsibilities:

- Render controls (`From`, `To`, `Export`, `Purge`).
- Perform immediate input-state checks and toggle button enabled state.
- Trigger presenter action on click.
- Display loading icon/state.
- Open download dialog when presenter signals success.

View must not:

- Fetch export DTOs directly.
- Generate CSV directly.

## 6.2 Presenter

Responsibilities:

- Re-validate authorization and date-range invariants.
- Assert admin authorization before export starts.
- Orchestrate async execution using managed executor.
- Coordinate success/failure callbacks for view.

Presenter must not:

- Contain CSV serialization logic.

## 6.3 Export Data Service (DTO Fetch)

Responsibilities:

- Query purchase records for selected range.
- Flatten purchase + line items into export DTO rows.
- Return rows ordered deterministically.
- Resolve grid row index for first purchase matching the selected `To` boundary (for view scroll behavior).

## 6.4 CSV Exporter Class

Create a dedicated class in purchases package, for example:

- `PurchaseHistoryCsvExporter`

Responsibilities:

- Convert export DTO rows to CSV bytes/stream using OpenCSV.
- Encapsulate headers, field mapping, and formatting.
- Build export filename convention.

This class is a focused exporter service component, not a generic static utility bag.


## 7. Data Contract for Export

## 7.1 Row Model Strategy

Use one CSV row per purchased item (purchase line), not one row per purchase.

Reason:

- preserves one-to-many relationship without lossy concatenation,
- keeps analytics and spreadsheet usage simple,
- allows direct filtering/pivot by item attributes.

## 7.2 Flattened DTO Fields

Each row must include purchase-level fields and line-level fields.

Recommended fields:

- `purchase_id`
- `purchase_created_at`
- `purchase_status`
- `requester_name`
- `approver_name`
- `purchase_decided_at`
- `decision_reason`
- `purchase_total_amount`
- `line_index`
- `product_id`
- `product_name`
- `unit_price`
- `quantity`
- `line_total`

Purchase-level fields repeat for each line row belonging to same purchase.

## 7.3 Date Filtering Semantics

Date fields are local dates in UI with day resolution.

Filter conversion:

- `fromInstant`: start of `From` date (inclusive)
- `toInstantExclusive`: start of day after `To` date (exclusive)

Query semantics:

- `createdAt >= fromInstant`
- `createdAt < toInstantExclusive`

This avoids boundary ambiguity and includes full `To` day.

No separate user-selected timezone is part of this feature.

## 7.4 Ordering

CSV rows must be deterministic:

1. purchase created timestamp descending
2. purchase id
3. line index ascending


## 8. CSV Composition Rules

## 8.1 Library

Use OpenCSV for generation (dependency added to `vaadincreate-ui` module).

## 8.2 Formatting

- Use UTF-8 encoding.
- Include header row.
- Timestamps serialized as ISO-8601.
- Monetary `BigDecimal` values are serialized using locale decimal separator.
- CSV field separator is locale-driven: locales with comma decimal separator use semicolon (`;`), and locales with dot decimal separator use comma (`,`).
- Nullable purchase fields (for example pending `decided_at`, `decision_reason`, nullable approver fields if applicable) must be serialized as empty string.

## 8.3 Escaping

Escaping/quoting handled by OpenCSV.

Required behavior:

- values containing delimiter, quote, or newline are correctly quoted,
- quote characters are escaped according to CSV standard.

## 8.4 File Name Convention

Use localized prefix and explicit date range in file name:

- `export-MMDDYY-MMDDYY.csv`

Where the two date tokens represent `From` and `To` values in month-day-year format.


## 9. Download Resource and Dialog

## 9.1 Resource Lifecycle

- Create resource only after CSV is generated.
- Use `StreamResource` as the download resource type.
- Bind `StreamResource` to dialog `Download` button via `FileDownloader`.
- Resource must be associated with current export result only.

## 9.2 Dialog Behavior

- Dialog opens only when resource is ready.
- Dialog close event must restore export button normal state.
- The dialog implementation should extend `AbstractDialog` from the shared components module.


## 10. Internationalization

Add i18n keys for:

- export toolbar labels (`from`, `to`, `export`)
- validation messages (`missing dates`, `invalid range`, `max three months`)
- async states (`export started`, `export failed`, `export ready`)
- dialog labels (`download`)

All supported locales must receive translations.


## 11. Observability and Logging

- Log export start with user and range.
- Log export completion with row count and duration.
- Log failures with exception details.

No sensitive data should be logged in full payload form.


## 12. Performance and Limits

- Async generation must keep UI responsive.
- Export should support expected admin data volume for three-month window.
- If row count is zero, still generate valid CSV with header and show informative notification/dialog text.
- Single-threaded UI executor queuing is accepted by design; export must not start parallel jobs from the same view instance.


## 13. Acceptance Criteria

## 13.1 Functional

1. Toolbar contains From, To, Export, and Purge controls; tests should verify presence and behavior without coupling to exact child-index positions.
2. Export disabled when either date missing.
3. Required indicator is visible on both `From` and `To` fields.
4. Export disabled when range exceeds three months.
5. Export disabled when `To < From`.
6. When range exceeds three months, `To` field shows component error with localized hint.
7. When `To < From`, `To` field shows component error with localized hint.
8. Export enabled when range is valid, including exactly three months.
9. Clicking Export starts async job, button enters loading state.
10. On success, dialog opens with working download button.
11. Downloaded CSV includes purchase line items (one row per line).
12. Dialog close restores export button normal icon/state.
13. On failure, user sees notification and button state is restored.
14. If view detaches during export, future cancellation is attempted and no stale UI update errors occur.
15. Date fields disallow selecting future dates.
16. Date fields enforce lower bound of current date minus 24 months.
17. Changing `To` to a valid value scrolls grid to first matching row index when available.
18. Rows inside currently selected range are visually emphasized with bold text weight.
19. On view enter, `From` date field receives initial focus.
20. After successful export completion, download dialog shows export-ready text and download action.

## 13.2 Technical

21. Async execution uses `VaadinCreateUI.getExecutor()`.
22. Presenter contains orchestration only; CSV generation resides in dedicated exporter class.
23. CSV generation uses OpenCSV in UI module.
24. Download resource type is `StreamResource` bound through `FileDownloader`.
25. Presenter asserts admin authorization before starting export.
26. UI updates from async completion are done via UI-safe access mechanism.
27. Date fields use DateField range limit configuration for both end (today) and start (today minus 24 months).
28. Service exposes a method to resolve first matching grid row index for `To`-boundary scrolling.
29. CSV exporter applies locale-based decimal formatting and locale-matching field separators.


## 14. Test Requirements

## 14.1 UI Tests

- Export button enablement matrix by date values.
- Required indicator visibility for both date fields.
- Future date values are not selectable in `From` and `To` fields.
- Date field lower bound is enforced at current date minus 24 months.
- `To` date field component-error visibility/clearing for invalid range states.
- `To` value change triggers grid scroll to expected index when match exists.
- No scroll is triggered when `To` is null, invalid, or no match exists.
- Row style class is applied only for rows inside selected range and removed when out of range.
- Loading icon/state transitions on click/success/failure/dialog-close.
- Dialog opens only after async completion.
- Download button exists and is enabled in dialog.
- `From` field is initially focused when entering Purchase History tab.
- Export-ready feedback is shown inside dialog content (not as a separate notification).
- Detach/navigation-away during export does not produce stale-component failures.

## 14.2 Service/Presenter Tests

- Date-range validation logic, especially boundary at exactly three months.
- First matching index resolution for `To` date boundary (match/no-match/boundary cases).
- Async completion callback routes (success/failure).
- Authorization enforcement for admin-only export.

## 14.2.1 Async Test Synchronization

- Tests that verify async export behavior must use AbstractUIUnitTest#waitWhile to wait for background completion and resulting UI updates.
- Avoid fixed sleep-based waiting in async tests.

## 14.3 CSV Exporter Tests

- Header correctness and column ordering.
- One-row-per-line flattening for purchases with multiple items.
- Proper escaping for commas/quotes/newlines.
- Locale-specific separator and decimal formatting for CSV numeric values.
- Stable ordering.


## 15. Implementation Notes

- Keep this feature additive; do not alter purge behavior.
- Avoid static global mutable state for export jobs/resources.
- Recreate download button/resource per export operation to avoid stale references.


---

This PRD defines the implementation contract for Admin Purchase History CSV export with async execution, strict date validation, and robust UI state handling.