# VaadinCreate Wireframes

This file augments `docs/VaadinCreate.PRD.md` with framework-agnostic visual composition specs.
It is intentionally more detailed than flow diagrams, but lighter than pixel-perfect screenshots.

## 1. How To Read These Mocks

Notation:
- `[]` container, panel, or card
- `()` interactive control
- `<>` data view (grid, list, chart)
- `{}` transient UI (toast, dialog)

Sizing and spacing guidance (rough):
- Desktop canvas: 1280 to 1440 px wide.
- Main shell columns: nav rail ~240 px + content fills remaining width.
- Common spacing scale: 8, 12, 16, 24 px.
- Typical card/panel radius: 6 to 8 px.
- Form label to input spacing: ~8 px.
- Section to section spacing: ~16 to 24 px.

## 2. Global App Shell (All Authenticated Views)

### 2.1 Desktop Shell

```mermaid
flowchart LR
    subgraph APP["[App Shell | Desktop]"]
        direction LR

        subgraph NAV["[Left rail ~240px]"]
            direction TB
            BRAND["Brand / logo"]

            subgraph NAVLIST["[Primary navigation]"]
                direction TB
                ABOUT("About")
                INV("Inventory")
                PUR("Purchases")
                ST("Statistics")
                ADM("Admin")
                SF("Storefront")

                ABOUT --> INV
                INV --> PUR
                PUR --> ST
                ST --> ADM
                ADM --> SF
            end

            VIS["Role-based visibility"]
            OUT("Logout")

            BRAND --> NAVLIST
            NAVLIST --> VIS
            VIS --> OUT
        end

        subgraph CONTENT["[Main content area | fluid]"]
            direction TB
            ROUTE["Route-specific workspace"]
        end

        NAV --> CONTENT
    end
```

### 2.2 Narrow Window Shell

```mermaid
flowchart TB
    subgraph APPN["[App Shell | Narrow]"]
        direction TB
        subgraph TOP["[Top utility row ~56px]"]
            direction LR
            MENU("Menu toggle")
            TITLE["View title"]
            OUTN("Logout/menu action")
        end

        subgraph CN["[Content area]"]
            ROUTEN["Route-specific workspace"]
        end

        DRAWER["[Navigation drawer overlay]\nSame nav items as desktop left rail"]
    end

    MENU -. opens/closes .-> DRAWER
```

Callouts:
- Desktop uses a left vertical rail; the utility/action area is in that rail, not as a right-side top column.
- Top utility row is a narrow-window/mobile behavior.
- Only one nav item is active at a time.
- `Storefront` is customer/employee-facing; `Purchases` and `Admin` are supervisor/admin-facing.

## 3. Login View

### 3.1 Desktop Mock

```mermaid
flowchart LR
    subgraph L["[Login Page]"]
        direction LR
        subgraph C1["[Auth card ~420px]"]
            direction TB
            ULBL["Label: Username"]
            U("Username input")
            PLBL["Label: Password"]
            P("Password input")
            AR["[Action row]"]
            LOGIN("Primary: Log in")
            FORGOT("Secondary: Forgot password")
            LLBL["Label: Language"]
            LANG("Language selector")
            AR --> LOGIN
            AR --> FORGOT
        end

        subgraph C2["[Info card ~420px]"]
            direction TB
            INFOH["Login information"]
            INFOT["Credential hint and role behavior"]
        end
    end

    TOAST{"{Login success/failure toast}"}
    L --> TOAST
```

### 3.2 Mobile Mock

```mermaid
flowchart TB
    subgraph M["[Login Page | Mobile]"]
        direction TB
        subgraph MC["[Single auth card | full width]"]
            direction TB
            U2("Username input with placeholder")
            P2("Password input with placeholder")
            LOGIN2("Log in")
            FORGOT2("Forgot password")
            LANG2("Language selector")
        end
    end
```

Callouts:
- Desktop shows info card; mobile hides it.
- Mobile uses placeholders where labels are reduced.

## 4. About View

```mermaid
flowchart TB
    subgraph A["[About View]"]
        direction TB
        subgraph INFO["[Info card]"]
            direction TB
            TITLE["Vaadin Create '23"]
            VER["Version line"]
            EXT("External site link")
        end

        subgraph NOTE["[System message card]"]
            direction TB
            TS["Timestamp"]
            MSG["Rendered note/message body"]
            EDIT("Edit")
        end

        SHUT("Shutdown")

        INFO --> NOTE
        NOTE --> SHUT
    end

    T{"{Assistive/status toasts}"}
    A --> T
```

Callouts:
- In admin context, shutdown action is visible.
- Note panel is a single-message focal card (not a feed list).

## 5. Inventory View

### 5.1 Base Layout

```mermaid
flowchart TB
    subgraph IV["[Inventory View]"]
        direction TB
        subgraph TBAR["[Toolbar | horizontal]"]
            direction LR
            FILTER("Filter/search")
            NEW("New product")

            FILTER --> NEW
        end

        subgraph WORK["[Workspace | 2-pane when form open]"]
            direction LR
            GRID["<Products grid>\ncolumns: name | price | availability | in stock"]
            FORM["[Product form pane]\nname\nprice\navailability\nstock\ncategories\n(actions: delete | save | discard | cancel)"]
        end

        TBAR --> WORK
    end

    GRID -. row selected or New .-> FORM
```

### 5.2 Product Form Detail

```mermaid
flowchart TB
    subgraph PF["[Product Form]"]
        direction TB
        N["Label + input: Product name"]
        PR["Label + numeric: Price"]
        AV["Label + select: Availability"]
        STK["Label + numeric: In stock"]
        CAT["Label + multiselect: Categories"]

        subgraph ACT["[Actions row]"]
            direction LR
            DELETE("Delete")
            SAVE("Save")
            DISCARD("Discard")
            CANCEL("Cancel")

            DELETE --> SAVE
            SAVE --> DISCARD
            DISCARD --> CANCEL
        end

        N --> PR
        PR --> AV
        AV --> STK
        STK --> CAT
        CAT --> ACT
    end

    ERR{"{Inline validation + toast}"}
    AV --> ERR
    STK --> ERR
```

Callouts:
- Form can behave as a side pane/drawer relative to grid.
- Save/Discard stay disabled until valid and dirty.

## 6. Statistics View

```mermaid
flowchart TB
    subgraph SV["[Statistics View]"]
        direction TB
        subgraph R1["[Row 1]"]
            direction LR
            C1["<Availability chart card>"]
            C2["<Price-range chart card>"]
        end

        subgraph R2["[Row 2]"]
            direction LR
            C3["<Category chart card\nCount + In stock>"]
        end

        R1 --> R2
    end

    CTL["Chart utility controls\nprint/export menu, series toggle"]
    C1 --> CTL
    C2 --> CTL
    C3 --> CTL
```

Callouts:
- Three chart cards with clear spacing and equal visual weight in row 1.
- Category chart is denser and typically spans full row width.

## 7. Purchases View (Supervisor/Admin)

### 7.1 View Shell

```mermaid
flowchart TB
    subgraph PV["[Purchases View]"]
        direction TB
        TABS["(Tabs) Purchase History | Approvals | Statistics"]
        TABBODY["[Active tab workspace]"]
        TABS --> TABBODY
    end
```

### 7.2 Purchase History Tab

```mermaid
flowchart TB
    subgraph PH["[Tab: Purchase History]"]
        direction TB
        TOP["[Top action row]"]
        PURGE("Purge old purchases")
        TOP --> PURGE

        GRID["<purchase-history-grid>\n9 columns including toggle + metadata"]
        DETAIL["[Expandable row detail panel]\nPurchase ID\nApprover\nDecided At\nDecision Reason\nLine items"]

        TOP --> GRID
    end

    GRID -. toggle open/close .-> DETAIL
```

### 7.3 Approvals Tab

```mermaid
flowchart TB
    subgraph AP["[Tab: Approvals]"]
        direction TB
        AGRID["<purchase-approvals-grid>\npending purchases for current approver"]
        AACT["[Per-row action cell]"]
        APPROVE("Approve")
        REJECT("Reject")
        AGRID --> AACT
        AACT --> APPROVE
        AACT --> REJECT
    end

    DM{"{Decision modal}\ncomment field\nConfirm\nCancel"}
    APPROVE --> DM
    REJECT --> DM
```

### 7.4 Statistics Tab

```mermaid
flowchart TB
    subgraph PS["[Tab: Statistics]"]
        direction TB
        TP["<Top products chart>"]
        LP["<Least products chart>"]
        PM["<Purchases per month chart>"]

        TP --> LP
        LP --> PM
    end
```

Callouts:
- Tabs are peer workspaces, not wizard steps.
- History and approvals are grid-first, with actions and details attached to rows.

## 8. Admin View

### 8.1 View Shell

```mermaid
flowchart TB
    subgraph AV["[Admin View]"]
        direction TB
        T["(Tabs) Categories | Users"]
        B["[Active tab workspace]"]
        T --> B
    end
```

### 8.2 Categories Tab

```mermaid
flowchart TB
    subgraph CT["[Tab: Categories]"]
        direction TB
        ADD("Add new category")
        CL["<Editable category rows>"]
        ROW["Row anatomy: name input + delete icon/button"]
        ADD --> CL
        CL --> ROW
    end
```

### 8.3 Users Tab

```mermaid
flowchart TB
    subgraph UT["[Tab: Users]"]
        direction TB
        subgraph TOP["[Top row]"]
            direction LR
            NEW("New")
            PICK("User selector/search")
        end

        subgraph FORM["[User form]"]
            direction TB
            UN["Username"]
            PW["Password"]
            RPW["Repeat password"]
            ROLE["Role"]
            DEP["Deputy (conditional)"]
            ACT["Active checkbox"]

            UN --> PW
            PW --> RPW
            RPW --> ROLE
            ROLE --> DEP
            DEP --> ACT
        end

        subgraph ACTS["[Actions row]"]
            direction LR
            DEL("Delete")
            CAN("Cancel")
            SAVE("Save")

            DEL --> CAN
            CAN --> SAVE
        end

        TOP --> FORM
        FORM --> ACTS
    end
```

Callouts:
- Users form is a vertical form stack with a separate action bar.
- Actions row is at the bottom of the Users workspace.
- Deputy field is hidden by default and appears only when deputy assignment is required.

## 9. Storefront View (Customer)

### 9.1 Two-Pane Composition

```mermaid
flowchart LR
    subgraph SF["[Storefront View]"]
        direction LR
        subgraph LEFT["[Purchase wizard pane ~60%]"]
            direction TB
            WTITLE["Step title"]
            WBODY["Step content"]
            WNAV["Previous | Next | Submit"]

            WTITLE --> WBODY
            WBODY --> WNAV
        end

        subgraph RIGHT["[Purchase history pane ~40%]"]
            direction TB
            H["<purchase-history-grid>\ncolumns: toggle | created | status | total"]
            D["[Expanded detail block]\nmetadata + reason + line item math"]
            H -. toggle .-> D
        end
    end

    S{"{Status updates toast}"}
    SF --> S
```

### 9.2 Wizard Step Mocks

```mermaid
flowchart TB
    subgraph W1["[Step 1: Select Products]"]
        direction TB
        G["<purchase-grid>\nselect | name | stock | price | quantity"]
        SUM["Order summary row (count + total)"]
        N1["(Previous disabled) (Next)"]

        G --> SUM
        SUM --> N1
    end

    subgraph W2["[Step 2: Delivery Address]"]
        direction TB
        ADDR["Street\nPostal Code\nCity\nCountry"]
        N2["(Previous) (Next)"]

        ADDR --> N2
    end

    subgraph W3["[Step 3: Select Supervisor]"]
        direction TB
        SUP("Supervisor combobox")
        N3["(Previous) (Next)"]

        SUP --> N3
    end

    subgraph W4["[Step 4: Review and Submit]"]
        direction TB
        REV["Order summary\nItems list\nAddress summary\nSupervisor summary"]
        N4["(Previous) (Submit)"]

        REV --> N4
    end

    W1 --> W2
    W2 --> W3
    W3 --> W4

    WARN{"{Validation toasts}\nEmpty cart\nMissing address\nMissing supervisor"}
    W1 --> WARN
    W2 --> WARN
    W3 --> WARN
```

Callouts:
- Wizard remains in left pane; right history pane stays visible for context.
- Expanded history detail should read like a compact receipt.

## 10. Error/Fallback View

```mermaid
flowchart TB
    subgraph E["[Fallback view in shell]"]
        P1["Primary: The view could not be found"]
        P2["Secondary: route/path detail"]
    end
```

## 11. Implementation-Oriented Notes

- Keep toolbar/actions visually detached from data grids using a dedicated row.
- Keep row-detail content inside grid context; do not navigate away for details.
- Use consistent action order in form footers: destructive/secondary left, primary save right.
- Preserve persistent context panes where defined (Storefront wizard + history).
- Ensure tab workspaces maintain their internal state when switching tabs.
