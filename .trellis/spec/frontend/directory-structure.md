# Directory Structure

> How frontend code is organized in this project.

---

## Overview

This is a **zero-build, zero-dependency static web app** (vanilla JS / HTML / CSS).
No bundler, no package.json, no npm scripts. Open `index.html` directly in a browser.
Every JS module is an **IIFE that assigns to a `window.*` global** — there is no
module system (`import`/`export`), no transpilation. Load order in `index.html`
is the dependency contract.

---

## Directory Layout

```
renovation/
├── index.html              # Single page; loads all scripts in order (the dependency contract)
├── server.js               # Optional static server for phone LAN access
├── phone-server.bat        # Launches server.js for mobile testing
├── open-app.bat            # Opens index.html directly
├── css/
│   └── style.css           # All styles (single stylesheet, mobile-first)
└── js/
    ├── data/               # Read-only knowledge bases (IIFE → window global)
    │   ├── knowledge.js    # window.DATA  — stages, styles, tips, glossary, modes, …
    │   └── prices.js       # window.PRICES — price rates, budget template, tier/grade
    ├── storage.js          # window.Store  — localStorage persistence + import/export
    ├── ui.js               # window.UI     — esc, money, modal, toast, form helpers
    ├── views/              # window.Views  — render functions, one per tab
    │   ├── home.js         #   首页仪表盘
    │   ├── stages.js       #   流程页 (14 阶段时间轴)
    │   ├── budget.js       #   预算页
    │   ├── guide.js        #   指南页 (tips/accept/styles/materials/wiki)
    │   └── more.js         #   我的页
    └── app.js              # window.App    — router, stage progress, first-run wizard
```

---

## Module Pattern (IIFE Global)

Every module follows the same shape — an IIFE that builds an object and assigns
it to a single `window.*` name:

```js
// js/data/knowledge.js
window.DATA = (function () {
  var stages = [ /* … */ ];
  function helper() { /* … */ }
  return { stages: stages, /* … */ };
})();
```

- **Exports**: exactly one `window.*` global per file (`DATA`, `PRICES`, `Store`, `UI`, `App`).
- **No `import`/`export`** — cross-module access is via the global name.
- **`window.Views`** is shared: each view file does `window.Views = window.Views || {}`
  then registers `Views.home = function (param) { … }`.

### Load Order (CRITICAL)

`index.html` loads scripts in dependency order. This order **is** the dependency
contract — there is no resolver:

```
knowledge.js → prices.js → storage.js → ui.js → views/* → app.js
```

- `prices.js` may lazily read `DATA.modes` at *call time* (not load time), so it
  must load after `knowledge.js` but the read is deferred — order is safe as-is.
- `views/*` read `DATA`, `PRICES`, `Store`, `UI` while rendering (call time), so
  they can load before `App` boots. `App.init()` runs last and triggers the first
  render.

---

## Data-View Contract (cross-layer)

The app has three runtime layers:

| Layer | Global | Mutability | Role |
|-------|--------|------------|------|
| Knowledge | `DATA`, `PRICES` | **read-only** | Static renovation knowledge + price reference |
| State | `Store.state` | **read-write** | User data in localStorage (profile, tasks, budget, checks) |
| Views | `Views.*` | pure render | Read `DATA`/`PRICES` + `Store.state`, emit HTML strings |

**Convention**: Views are **pure consumers** of `DATA`/`PRICES` — they never mutate
them. All user mutations go through `Store` (which persists to localStorage).

**Gotcha (caused a full app break once)**: A view reading `DATA.someField` that
isn't defined in `knowledge.js` throws a `ReferenceError` at **render time** (not
load time), because views are functions called later. `index.html` referencing a
missing `js/data/*.js` file breaks the app silently (subsequent scripts still
load, but any `DATA.*` access throws).

### Checklist: adding a new `DATA.*` consumer
- [ ] The field is defined in `js/data/knowledge.js` (or `prices.js` → `PRICES`).
- [ ] The field shape matches what the view reads (keys, types, array vs scalar).
- [ ] Internal ids referenced (e.g. `stage.acceptIds` → `checklists[].id`,
      `styleQuiz` option `scores` keys → `styles[].id`) resolve to real entries.
- [ ] `index.html` actually loads the data file before the view that uses it.

---

## Naming Conventions

- **Files**: lowercase, hyphen-free single words (`storage.js`, `budget.js`).
- **Globals**: PascalCase single word (`DATA`, `PRICES`, `Store`, `UI`, `App`);
  `Views` is the shared namespace object for view functions.
- **Data ids**: short prefixed slugs (`s-demolition`, `cl-water`, `modern`,
  `half`) — stable across releases (they are persisted in localStorage as
  `Store.state` keys, so renaming breaks existing user data).

---

## Examples

- **Adding a data file**: create `js/data/<name>.js` as `window.<NAME> = (function(){ … })();`,
  add a `<script>` tag in `index.html` **before** any view/app that reads it.
- **Adding a view**: create `js/views/<name>.js`, register `Views.<name> = function(param){…}`,
  add a `<script>` tag before `app.js`, wire a tab in `index.html` + a route in `app.js`.
