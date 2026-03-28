# Vaadin Create 23 - Style Guide

## Scope
This document describes the visual conventions used by the application theme in `webapp/VAADIN/themes/vaadincreate` and how they appear across views.

The guide is based on:
- Theme SCSS implementation (`vaadincreate.scss` and mixins)
- View-specific style bindings in Java views
- Existing visual baseline screenshots (`reference-screenshots/*.png`)

## Navigation Coverage

### Primary Routes and Styled Areas
- `about`: About content card, admin note block
- `inventory`: Product grid, toolbar, side editor form
- `storefront`: Purchase wizard + history panel split layout
- `purchases`: Tabbed purchase history/approvals/stats content
- `stats`: Dashboard cards and charts
- `admin`: Category grid + user form views

## Design Tokens

### Core Theme Variables
Defined in `vaadincreate.scss`.

- Focus color: `rgb(33, 101, 161)` (`$v-focus-color`)
- Menu background: `rgb(59, 63, 66)` (`$valo-menu-background-color`)
- Error color: `#eb1937` (`$v-error-indicator-color`)
- Friendly/positive button color: `rgb(45, 166, 73)` (`$v-friendly-color`)
- Editor background: `#fafafa` (`$editor-background-color`)
- Light shadow: `0 2px 3px rgba(0, 0, 0, 0.05)`
- Editor shadow: `0 0 10px 10px rgba(0,0,0,.1)`

### Semantic Status Colors
Global CSS custom properties under `.vaadincreate`:

- `--color-available: #2dd085`
- `--color-coming: #ffc66e`
- `--color-discontinued: #f54993`

Used in availability badges/icons and product availability selectors.

## Sizing and Spacing Conventions

### Base Scale
- Base font size: `15px`
- Base unit: `32px` (`$v-unit-size`)
- Layout margin: `round(32 / 1.5)` = `21px`
- Vertical and horizontal spacing: `round(32 / 1.8)` = `18px`

### Recurring View Sizes
- Login info panel width: `300px`
- Book filter width: `9 * unit` = `288px`
- Book side editor width: `9 * unit` = `288px`
- Storefront minimum columns: wizard `400px`, history `450px`
- About content max width: `500px`

## Visual Style Conventions

### 1. Application Shell
- Navigation shell is built on Valo menu classes (`.valo-menu`, `.valo-menu-item`, `.valo-menu-toggle`) with a custom menu item padding of `4px 32px 4px 16px`.
- For selected menu items, the icon color is `lighten($v-focus-color, 25%)`, creating a brighter active-state icon than the base focus color (`rgb(33, 101, 161)`).
- Menu title hides text caption and emphasizes logo.
- Interactive elements consistently provide `:focus-visible` feedback.

### 2. Panels and Cards
- Card surfaces are mostly custom theme classes over `$editor-background-color` (`#fafafa`), not direct Valo utility classes.
- Elevation is primarily from `$light-shadow` (`0 2px 3px rgba(0, 0, 0, 0.05)`) applied in custom selectors (for example `.adminview-userform`, `.adminview-categorygrid`, `.dashboard-chart`).
- Admin cards are implemented with custom styling: `.adminview-userform` uses white background + `$v-grid-border`, and `.adminview-categorygrid` uses custom shadow treatment.
- Dashboard chart wrappers (`.dashboard-chart`, `.dashboard-chart-wide`) are custom card containers with fixed margins and shadow; Valo built-ins are used selectively elsewhere (for example `ValoTheme.LAYOUT_WELL` in Storefront history/wizard containers).

### 3. Buttons
- Primary actions: Valo primary style.
- Positive actions: Valo friendly style.
- Destructive actions: Valo danger style with forced white foreground.
- Warning/cancel actions: custom `.v-button-cancel` variant with amber tones.
- Tooltip-enabled actions may add a corner indicator via wrapper/slot classes (`.has-tooltip` / `.v-slot-slot-has-tooltip`) instead of replacing the button icon.

### 3.1 Tooltip Indicator Marker
- Tooltip discoverability is reinforced with a small corner marker rendered as a right-angle triangle in the top-right corner of the target container.
- Marker geometry: `8px x 8px`, black fill, clipped using `polygon(0% 0%, 100% 0%, 100% 100%)`.
- Marker anchor: absolute positioning at `top: 0; right: 0;` with a relatively positioned host.
- Intended usage includes Inventory filter/rows with responsive descriptions, About admin actions, and Purchases purge controls.

### 4. Focus and Accessibility
- Keyboard focus ring is strongly standardized: `2px` outline in focus color with small offset/radius.
- Applied to grids, forms, charts, tabs, and menu interactions.
- Chart legends/menu items are made keyboard-focusable and styled on focus.

### 5. Data Grids and Editing
- Grid rows are pointer targets in interactive contexts.
- Locked rows reduce opacity and animate on state change.
- Edited row text uses focus color accent.
- Side editor slides in from the right using transform transition.
- Dirty form fields use dashed warning border.

### 6. Dashboard Charts
- Two-column card layout on larger screens (`50% - 20px` each).
- Full-width chart variant for broad datasets.
- Chart context menu and legend use focus/hover visual parity.

### 7. Storefront
- Two-panel layout with clear task split: wizard vs history.
- History container has soft gray background; embedded grid is white.
- Purchase history row states:
  - Hover: light gray
  - Expanded/details: light blue tint
  - Old purchase (`purchase-old`): warm yellow tint

### 8. Offline Experience
- Dedicated shell with fixed side menu (desktop) and topbar + slide-in menu (mobile).
- Warning banner and status chip pattern for connectivity state.
- Card-based informational content with compact typography.

## Utility Classes

Defined in `mixins/utilities.scss` and shared by views:

- `.whitespace-pre`: preserve line breaks and spacing
- `.no-stripes`: remove alternating grid stripes
- `.no-borders`: remove grid row borders
- `.no-cell-focus`: hide native grid cell focus marker
- `.row-focus`: replace cell focus with row highlight + row border
- `.v-select-optiongroup-scrollable`: scrollable boxed checkbox group
- `.v-button-cancel`: warning-toned cancel button skin
- `.has-tooltip`: generic host class that renders the top-right tooltip marker
- `.v-slot-slot-has-tooltip`: Vaadin slot host variant for tooltip marker rendering

Additional CSS utility behavior in `additional-styles.css`:
- Tab captions with error indicators are tinted amber via `:has(.v-errorindicator)`
- On very small screens (`max-width: 550px`), inventory filter and grid are hidden while side editor is visible

## Responsive Conventions

### Breakpoints
- `0-800px`: compact menu/login and tighter storefront spacing
- `0-1350px`: storefront switches to stacked layout
- `0-550px`: side editor takes full width; tighter paddings for inventory/about/admin/storefront
- `551-1000px`: dashboard cards collapse to full width

### Responsive Behavior Patterns
- Hide non-essential labels and preserve meaning with aria alternatives.
- Collapse multi-column workflows into vertical stacks on small viewports.
- Preserve keyboard and focus visibility while changing layout.

## Per-View Style Binding Summary

- Login: `login.scss`
  - `loginview`, `loginview-form`, `loginview-information`, `loginview-center`
- About: `about.scss`
  - `aboutview`, `aboutview-aboutcontent`, `aboutview-aboutlabel`, `aboutview-adminscontent`
- Inventory/Books: `books.scss`
  - `bookview*`, `bookform*`, `fakegrid*`
- Admin: `admin.scss`
  - `adminview*`
- Stats + Purchases stats charts: `dashboard.scss`
  - `dashboard*`
- Storefront: `storefront.scss`
  - `storefrontview*`, `purchase-old`
- Shared utilities: `utilities.scss`
- Responsive overrides: `responsive.scss`
- Offline page styles: `offline.scss`

## Visual Consistency Rules for New UI Work

- Reuse existing semantic colors (`available/coming/discontinued`) instead of introducing new status colors.
- Use focus ring conventions (`2px` focus color outline) for all custom interactive widgets.
- Prefer existing surface/shadow pattern (`white + light-shadow`) for cards and panels.
- Keep spacing aligned to the base unit scale (18px/21px rhythm derived from `32px` unit).
- For data-heavy screens, follow inventory/dashboard structure: toolbar/header + card/grid body.
- For mobile variants, stack columns and preserve full-width controls.
- When a control exposes a tooltip/description that affects task safety or context, prefer adding the tooltip marker classes so discoverability is consistent across views.

## Reference Baselines

Reference screenshots are stored in vaadincreate-ui/reference-screenshots and are generated per OS/browser (for example, *_windows_chrome_145.png and *_linux_chrome_145.png).

Baselines actively used by acceptance visual tests (logical names in test code):
- stats.png
- inventory.png
- category.png
- user.png

Some views intentionally do not have visual baselines because time-dependent or frequently changing content would make pixel comparisons unstable.
