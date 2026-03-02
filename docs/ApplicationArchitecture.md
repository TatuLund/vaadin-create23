# Application Architecture

This project is organized as a multi-module Maven build with three main modules:

- **vaadincreate-ui**: Vaadin UI layer (views + presenters) and UI-local infrastructure (notably the event bus implementation).
- **vaadincreate-backend**: domain model, DAOs, services, and infrastructure (e.g., Redis pub/sub) that emits domain events.
- **vaadincreate-components**: reusable Vaadin components/utilities shared across UI.

## Layering (typical call flow)

`View (Vaadin UI) → Presenter (MVP) → Service → DAO → Database`

Cross-cutting:

- Shared UI building blocks are provided by **vaadincreate-components**.
- UI styling/layout resources live in **vaadincreate-ui** under `src/main/webapp/VAADIN/themes/...`.
- UI-local **EventBus** dispatches events (and can bridge to backend Redis pub/sub).

## Mermaid diagram

```mermaid
flowchart LR
  %% =======================
  %% Modules (high level)
  %% =======================

  subgraph UI[vaadincreate-ui]
    UIROOT["Vaadin UI root<br/>VaadinCreateUI"]
    SHELL["App shell / navigation<br/>AppLayout"]
    VIEWS["Views (Vaadin)<br/>e.g. AboutView, BooksView, StatsView, AdminView, ..."]
    PRES["Presenters (MVP)<br/>e.g. AboutPresenter, BooksPresenter, StatsPresenter, ..."]
    COMMON["Shared UI patterns<br/>e.g. TabView, navigators, util"]
    UIBUS_API["EventBus API<br/>(interface)"]
    UIBUS_IMPL["UI-local EventBus implementation<br/>EventBusImpl"]
    UITHEME["Theme + web resources<br/>SCSS/HTML layouts"]
  end

  subgraph COMP[vaadincreate-components]
    SHARED_COMP["Reusable Vaadin components<br/>e.g. Shortcuts, extensions, dialogs, ..."]
  end

  subgraph BE[vaadincreate-backend]
    SVC[Service interfaces]
    SVCIMPL["Service implementations<br/>e.g. AppDataServiceImpl"]
    DAO["DAOs<br/>Hibernate sessions/transactions"]
    DOMAIN["Domain entities<br/>JPA model"]
    EVENTS["Domain events<br/>records like ShutdownEvent"]
    PUBSUB["Redis pub/sub infrastructure<br/>distributed messaging"]
    DB[(Database)]
  end

  %% =======================
  %% Primary call stack
  %% =======================

  UIROOT --> SHELL
  SHELL --> VIEWS
  VIEWS --> PRES
  PRES --> SVC
  SVC --> SVCIMPL
  SVCIMPL --> DAO
  DAO --> DOMAIN
  DAO --> DB

  %% =======================
  %% Cross-cutting: components + theme
  %% =======================

  VIEWS -. uses .-> SHARED_COMP
  SHELL -. uses .-> SHARED_COMP
  UIROOT -. loads .-> UITHEME
  VIEWS -. styled by .-> UITHEME
  SHELL -. styled by .-> UITHEME

  %% =======================
  %% Cross-cutting: event bus + distributed events
  %% =======================

  UIROOT -. registers listener .-> UIBUS_API
  PRES  -. registers listener .-> UIBUS_API
  UIBUS_API --> UIBUS_IMPL

  %% backend events are posted/received via bus
  PRES -. posts/handles .-> EVENTS
  UIROOT -. handles .-> EVENTS

  %% distributed propagation
  UIBUS_IMPL <--> PUBSUB
  PUBSUB --> EVENTS
```
