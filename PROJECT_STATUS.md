# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-14
Summary: Barely Crafting 2026.11.4 is built and checked (unit tests, headless self test, real ModJS script test and a real-client tour on all four Forge and NeoForge 26.2 and 26.3 builds); committed locally, release by the Project Manager.
Spend: none

## Foundation
- [x] Plan and decisions written (PLAN.md) (w:2)
- [x] Four Gradle modules (forge and neoforge for 26.2 and 26.3) with shared common/ and version-26.x folders (w:4)
- [x] Version 2026.11.4 in all four modules (w:1)
- [x] Local git repository with first commit (w:1)

## Crafting Station
- [x] Block, block entity and menu with a persistent 3x3 grid (saved per slot, drops on break) (w:4)
- [x] Result slot: click, shift-click craft-max, leftovers, full inventory, mismatched cursor (w:4)
- [x] Crafting from neighboring storages (six sides, set order, grid first then neighbors, refill) (w:5)
- [x] Connected panel: block-icon tab per storage, real slots, per-storage wheel and bar scrolling, windowed sync, no cap on storages or slots; seen scrolled next to a dev-only 216-slot storage on NeoForge 26.2 and Forge 26.3 (w:4)
- [x] Button column: clear, rotate, mirror, balance, refill, craft all, tooltips on top, config switches (w:4)
- [x] BEI plus button fills the grid (w:2)

- [x] Themes: minecraft (default) and modern, picked by ui.theme; seen on NeoForge 26.2 (minecraft) and Forge 26.3 (modern) (w:3)

- [x] Pedestal block model made in Blockbench (stone foot, column and cap, wooden table slab with the 3x3 grid, rim and metal corner posts), source in branding/models/station.bbmodel, matching selection shape, item display transforms; frames from top, two sides, hand and inventory on NeoForge 26.2 and Forge 26.3 (w:4)

## Testing
- [x] Unit tests of the pure logic and the script bridge (all four modules) (w:3)
- [x] Headless server self test, 49 checks (56 with ModJS), on all four layers (w:4)
- [x] Real-client frames on NeoForge 26.2 and Forge 26.3 (isolated launcher): block, screen, tooltip over buttons, rotate, craft from chest (w:4)
- [x] BEI plus button run in a real NeoForge 26.2 client through BEI's transfer host (frame gui_bei_plus) (w:2)
- [x] Real-client tours on Forge 26.2 and NeoForge 26.3 (w:2)

## Config
- [x] Loader config files (ModConfigSpec and ForgeConfigSpec) built from one key list, pushed into the common settings on load and reload, tested headless on all four layers (w:3)
- [x] One-time import of config/barelycrafting.properties (the file of every released version), checked and moved to .back, with fixtures from each release tag and invalid, clamped, empty, corrupt, partial, existing-backup and second-launch cases (w:3)
- [x] In-game config screen on NeoForge (compiled; not opened in a client) (w:1)
- [ ] In-game config screen on Forge: Forge 26.2 and 26.3 have no config screen class; add when it exists (w:1)

## Compatibility
- [x] ModJS scripting: BarelyCrafting global (craft and button events, station snapshot, recipe rules), unit tests with Rhino, live-server checks, docs/scripting.md and wiki page (w:3)
- [x] BEI, JEI and REI plus buttons (one shared fill routine); JEI and REI on NeoForge 26.2 and 26.3 compiled only (w:3)
- [ ] EMI plus button and JEI, REI and Jade on Forge: no 26.2 or 26.3 build exists yet; add when they release (w:2)
- [x] SMWTI lines (all four builds) and Jade lines (NeoForge): connected storages, last craft (w:2)
- [x] Craft event for other mods: the loader's item-crafted event is posted on every station craft, checked in the headless self test on all four layers (w:2)
- [x] ModJS typings (docs/barelycrafting.d.ts) and a self test with the real ModJS jar and a real server script on all four layers (w:2)
- [x] KubeJS startup-script bridge over the same API, unit tested through Rhino (w:2)
- [ ] KubeJS bridge run inside KubeJS: no 26.2 or 26.3 build exists yet (w:1)

## Release and CI
- [x] build.yml, release.yml, changed-builds-only plan_matrix.py with tests, issue templates (w:3)
- [x] GitHub repository, push, CI run (Project Manager; repo created and CI green 2026-11-13) (w:2)
- [x] CurseForge project and project id, secrets (Project Manager; project 1803251, 2026-11-13) (w:2)
- [x] Release 2026.11.1 tag and upload (Project Manager; 2026.11.2 released too, 2026-11-13) (w:2)

## Documentation and branding
- [x] README, CHANGELOG, LICENSE (MIT), PLAN, LISTING, CurseForge description (w:2)
- [x] Icon (3D station) and SMWTI-style banners (w:2)
- [x] Wiki pages written in the itmecasey.com site folder (not deployed) (w:3)
- [x] Wiki deployed (Project Manager, 2026-11-13) (w:1)
- [x] Added to the Command Center projects.json (w:1)
