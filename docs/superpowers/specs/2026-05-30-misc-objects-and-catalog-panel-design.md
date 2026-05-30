# Misc Objects + Item Catalog Panel — Design

Date: 2026-05-30
Status: Approved (pre-implementation)

## Goal

Two deliverables driven by the new sprite sheets in
`resources/images/objects/_spritesheets/`:

1. **Implement every object from the misc sprite sheet as a placeable world object.**
   The `mega_spritesheet.png` (1536×1024) holds ~80 labeled objects across 7
   categories. Most are not yet individual sprites. Slice them all out and wire
   each as a placeable `GameObject` + inventory `ItemType`.
2. **A scrollable catalog panel triggered by key `I`** that lists *every* item in
   the game and gives the clicked item to the player (creative/dev tool).

Scope decisions (confirmed with user):
- Slice scope: **all ~80** objects from the mega sheet (full slice).
- Object form: **placeable world objects**.
- Collision: **smart per-category** (solid for big structures, walk-through for flat decor).
- Panel: **catalog + give-on-click** (creative tool, not survival inventory).
- Purpose: **dev/creative tool** — need not be balanced.

## Context (how the existing systems work)

- **Objects**: `GameObject` (`resources/domain/object/GameObject.java`) is the base.
  Concrete placeables (`Torch`, `Barrel`, `CraftingTable`) just delegate to the
  generic `GameObject(panel, name, w, h, hbW, hbH, relX, relY, solid)` constructor.
  Sprites are discovered by `ObjectImageLoader` walking
  `resources/images/objects/<category…>/<name>/<name>.png`; folders starting with
  `_` are skipped. Missing sprites fall back to a PLACEHOLDER (no crash).
- **Items**: `ItemTypeRegistry` (`resources/domain/inventory/ItemTypeRegistry.java`)
  is a static `Registry<ItemType>`, **insertion-ordered**. Each `ItemType` has
  `{id, spriteName (→ resources/images/items/<name>.png), maxStack, placedEntityName}`.
  `Registry.values()` enumerates every item deterministically.
- **Placement**: `PlacementRegistry.registerDefaults(panel)` (run once via
  `ItemManager.setupPR`) maps `itemName → PlacementSpec(factory, surfaceRule, snap,
  action, ghost)`. Adding a placeable = one `register()` call. Left-click →
  `ClickRouter` chain → `PlacementRegistry` → factory → `WorldInteraction.placeEntity`.
- **UI**: panels extend `Container` (← `Component` ← `BaseComponent` ← `Rectangle`),
  draw via `draw(Graphics2D)`, hit-test via `Rectangle.contains`. Modal overlays
  register through `UserInterface.openOverlay(overlay, closer)`; `Mouse` routes
  clicks/wheel to UI when `isModalUIOpen()`; Escape calls `closeTopOverlay()`.
  `Mouse.mouseWheelMoved` already delegates to the UI when a modal is open.
- **Input**: keybinds are hard-coded in `Keys.keyPressed` by `KeyEvent.VK_*`.
  `VK_E` toggles the inventory / closes top overlay — the template for `VK_I`.
- **Testing**: headless **probes** under `resources/testing/probes/` (the project's
  test style). Memory note: a placed object only becomes queryable after
  `world.update(point)`, not `placeEntity` alone.

### The real gap
- ~45 items already registered; per-item icons in `resources/images/items/` already
  extracted; a few object folders already exist (`door`, `cave_portal`, `crafting_table`,
  `barrel`, `torch`, trees/`oak_M`).
- Missing: (a) most `mega_spritesheet.png` objects are still one baked image — not
  sliced, not registered; (b) extracted-but-unwired sprites (`door`, `cave_portal`,
  `oak_M`); (c) no catalog panel.

## Architecture

Three independent pieces.

### A. Offline sprite slicer (build-time tool)

`resources/testing/tools/SpriteSheetSlicer.java` — a `public static void main`.
Run once to produce PNGs; **not** part of the game loop.

1. Load `mega_spritesheet.png`, treat bright magenta (~`(250,2,251)`, tolerance on
   the R≫G, B≫G chroma signature) as background → transparent.
2. Connected-component labeling over non-background pixels → one bounding box per
   blob. (Robust to the sheet's grid jitter; the grid is ~14 cols × 5 object rows
   but cells touch/merge in places — blob detection sidesteps that.)
3. Drop the bottom **caption strip** (y ≳ 715) and tiny noise blobs (area below a
   threshold). Merge blobs whose boxes overlap/adjoin (multi-part sprites).
4. Sort blobs **row-major** (band by y, then by x within a band).
5. Zip the ordered blobs against the hand-authored manifest (§ObjectCatalog). On a
   count mismatch, **fail loudly** with both counts and per-index boxes so the
   misalignment is caught before wiring — never silently truncate.
6. Write each crop to `resources/images/objects/<category>/<name>/<name>.png`.
   Idempotent (overwrites).

A second pass extracts the already-needed item icons only if any are missing
(most already exist in `resources/images/items/`).

### B. Object manifest + registration (data-driven, no 80 subclasses)

`resources/domain/object/ObjectCatalog.java` — the **single source of truth**:
an ordered `List<Entry>` where `Entry = {name, category, solid, footprintTilesW,
footprintTilesH, maxStack}`. Authored by hand from the labeled sheet. Used twice:
the slicer's blob→name mapping reads it, and boot registration loops it.

Registration runs alongside `PlacementRegistry.registerDefaults` (so it executes
once at panel construction via `ItemManager.setupPR`). For each entry:
- If `ItemTypeRegistry` has no entry for `name`, `define(name, name, maxStack, name)`.
- `PlacementRegistry.register(new PlacementSpec(name, p -> new GameObject(p, name,
  …footprint+solid from entry), SurfaceRule.NOT_WATER, SnapPolicy.TILE,
  PlacementAction.PLACE_ENTITY, null, /*ghost*/ true))`.

Collision policy (smart per-category):
- **Solid**: structures/buildings (well, furnace, anvil, forge, statue, fountain,
  market stall, tent, windmill, door, large furniture, trees, large rocks).
- **Walk-through**: flat decor / small ground items (sign, lantern-on-ground,
  small plants, mushrooms, flowers, food/crop pickups).

The collision flag and footprint live in the manifest per object, so the policy is
data, not branching code. Already-extracted `door`, `cave_portal`, `oak_M` get
manifest entries too and wire through the same loop.

### C. Catalog panel (key `I`)

`resources/presentation/ui/ItemCatalogUI.java extends Container`.

- **Content**: every `ItemType` from `ItemTypeRegistry.instance().values()`
  (insertion-ordered → stable layout), rendered as a grid of icon cells
  (reuse `ItemContainer`'s cell metrics/colors for visual consistency).
- **Scrolling**: an integer `scrollOffsetPx`, clamped to
  `[0, max(0, contentHeight − viewportHeight)]`. `mouseWheelMoved` adjusts it.
  `draw` renders only rows intersecting the viewport (clip rect), offset by scroll.
- **Give-on-click**: `mousePressed` maps cursor → visible cell → item name →
  `player.addItem(new Item(panel, name), giveAmount)` where `giveAmount =
  min(itemType.maxStack(), DEFAULT_GIVE)` (e.g. 1 for tools, a stack for resources).
- **Modal**: opened via `UserInterface.openOverlay(this, closer)`; Escape closes it
  through the existing `closeTopOverlay()` path.
- **Toggle**: in `Keys.keyPressed`, add a `VK_I` branch mirroring `VK_E`
  (open if closed / close top overlay if open). A small `ItemCatalogUIBridge`
  (matching `ChestUIBridge`) handles open/close + overlay registration.

## Data flow

```
Boot:  ItemManager.setupPR
         → PlacementRegistry.registerDefaults(panel)         (existing)
         → ObjectCatalog.registerAll(panel)                  (new: items + placement)

Place: left-click → ClickRouter.useEquippedItem
         → PlacementRegistry.get(name) → new GameObject(name,…) → placeEntity
         → world.update(point) makes it queryable             (memory caveat)

Catalog: press I → ItemCatalogUIBridge.toggle
         → UserInterface.openOverlay(catalog, closer)
         → wheel scrolls; click → player.addItem(new Item(name), amount)
         → Escape → closeTopOverlay
```

## Error handling
- Missing sprite/icon → existing PLACEHOLDER path; panel & placement stay alive.
- Slicer blob/manifest count mismatch → loud failure with diagnostics; nothing
  written past the mismatch.
- Catalog click outside any cell → no-op.
- Scroll clamped so the list can't be scrolled past its bounds.

## Testing
Headless probes (project convention), plus manual `/run`:
- `CatalogProbe`: every `ItemType` resolves a **non-placeholder** icon; simulated
  click on a cell increases the player's inventory count for that item.
- `PlaceAllProbe`: each manifest object places into the world and becomes queryable
  **after `world.update(point)`**; solid objects report collision, walk-through don't.
- `SlicerProbe` (optional): blob count == manifest count for `mega_spritesheet.png`.
- Manual: launch the game, press `I`, scroll the full list, click a few items to
  confirm they land in inventory, then place each new object and eyeball the sprite
  + collision.

## Out of scope
- Crafting recipes / survival balance for the new objects.
- Interactive behaviour beyond placement + collision (e.g. functional furnace,
  working windmill). They render and block/allow movement; deeper behaviour is later.
- Networking-authoritative placement rules beyond what the existing pipeline gives.

## Files
New:
- `resources/testing/tools/SpriteSheetSlicer.java`
- `resources/domain/object/ObjectCatalog.java`
- `resources/presentation/ui/ItemCatalogUI.java`
- `resources/presentation/ui/ItemCatalogUIBridge.java`
- `resources/testing/probes/CatalogProbe.java`
- `resources/testing/probes/PlaceAllProbe.java`
- sliced sprites under `resources/images/objects/<category>/<name>/<name>.png`

Modified:
- `resources/input/Keys.java` (add `VK_I` toggle)
- `resources/domain/inventory/ItemManager.java` (call `ObjectCatalog.registerAll`)
- `sources.txt` (regenerated; new files compiled)
