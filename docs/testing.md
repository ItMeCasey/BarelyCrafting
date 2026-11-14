# Testing

Dated 2026-11-14, version 2026.11.3.

## What was checked

| Layer | Compiles | Unit tests | Headless self test | With a real ModJS jar and script | Seen in a client (tour) |
|---|---|---|---|---|---|
| NeoForge 26.2 | yes | yes | 49 checks | 56 checks | yes |
| Forge 26.2 | yes | yes | 49 checks | 56 checks | yes |
| NeoForge 26.3 | yes | yes | 49 checks | 56 checks | yes |
| Forge 26.3 | yes | yes | 49 checks | 56 checks | yes |

The BEI plus button ran in a real NeoForge 26.2 client: BEI's own transfer host (not a click on its popup) asked the station handler, which answered with an enabled button and filled the grid with a stick recipe (frame gui_bei_plus). The loader's item-crafted event for a station craft is checked in the self test on all four layers.

## How

- Unit tests: `gradlew test` in each layer (shared tests in common/src/test, including the script bridge through Rhino and the KubeJS helper).
- Headless self test: `python scripts/server_check.py all` starts each dedicated server and runs the checks.
- ModJS: set `BARELYCRAFTING_MODJS_JAR_<LAYER>` (for example `BARELYCRAFTING_MODJS_JAR_NEOFORGE_26_2`) to a ModJS jar; the check writes a server script that uses rules, listeners and the button event, and the self test confirms each acts on real crafts.
- Client tour: `python scripts/client_tour.py <layer>` through the isolated launcher; `BARELYCRAFTING_BEI_JAR` adds BEI and the plus-button step. Frames are in docs/frames.

Not tested in a client: the JEI and REI plus buttons, the Jade and SMWTI lines, KubeJS inside KubeJS (no 26.2 or 26.3 build). See known-gaps.md.
