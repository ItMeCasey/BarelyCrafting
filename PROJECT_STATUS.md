# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-13
Summary: Barely Crafting 2026.11.1 is built on Forge and NeoForge for 26.2 and 26.3 (tests, headless self test and real-client frames done); committed locally, not pushed, no GitHub repo, CurseForge project or wiki deploy yet.
Spend: none

## Foundation
- [x] Plan and decisions written (PLAN.md) (w:2)
- [x] Four Gradle modules (forge and neoforge for 26.2 and 26.3) with shared common/ and version-26.x folders (w:4)
- [x] Version 2026.11.1 in all four modules (w:1)
- [x] Local git repository with first commit (w:1)

## Crafting Station
- [x] Block, block entity and menu with a persistent 3x3 grid (saved per slot, drops on break) (w:4)
- [x] Result slot: click, shift-click craft-max, leftovers, full inventory, mismatched cursor (w:4)
- [x] Crafting from neighboring storages (six sides, set order, grid first then neighbors, refill) (w:5)
- [x] Connected panel: block-icon tab per storage, real slots, per-storage scrolling (scroll math unit-tested; the scrolling itself not seen on a storage over 54 slots) (w:4)
- [x] Button column: clear, rotate, mirror, balance, refill, craft all, tooltips on top, config switches (w:4)
- [x] BEI plus button fills the grid (compiled; the BEI transfer itself not run in a client) (w:2)

## Testing
- [x] Unit tests of the pure logic (25 per module, all four modules) (w:3)
- [x] Headless server self test, 42 checks, on all four layers (w:4)
- [x] Real-client frames on NeoForge 26.2 and Forge 26.3 (isolated launcher): block, screen, tooltip over buttons, rotate, craft from chest (w:4)
- [ ] BEI plus button run in a real client (w:2)
- [ ] Real client on Forge 26.2 and NeoForge 26.3 (w:2)

## Compatibility
- [~] BEI done; JEI, REI and EMI plus buttons not written; SMWTI and Jade lines not written (known gaps) (w:3)
- [ ] Craft events for other mods (ItemCraftedEvent) (w:2)

## Release and CI
- [x] build.yml, release.yml, changed-builds-only plan_matrix.py with tests, issue templates (w:3)
- [ ] GitHub repository, push, CI run (Project Manager) (w:2)
- [ ] CurseForge project and project id, secrets (Project Manager) (w:2)
- [ ] Release 2026.11.1 tag and upload (Project Manager) (w:2)

## Documentation and branding
- [x] README, CHANGELOG, LICENSE (MIT), PLAN, LISTING, CurseForge description (w:2)
- [x] Icon (3D station) and SMWTI-style banners (w:2)
- [x] Wiki pages written in the itmecasey.com site folder (not deployed) (w:3)
- [ ] Wiki deployed (Project Manager, ask Casey first) (w:1)
- [ ] Added to the Command Center projects.json (w:1)
