# Layered Animated Water System — Design

**Date:** 2026-05-30
**Status:** Approved for planning

## Goal

Rework the ocean graphics into a layered, animated, depth-aware water system using
the new `OceanSprites` art. Water should read as one continuous moving surface:
shallow water reveals the seabed, deep water is opaque, shorelines get foam
overlays — without any change to gameplay (collision, boats, swimming).

## Source art

`resources/images/objects/_spritesheets/ocean_spritesheet_purple.png` — 16 cols ×
8 row-bands of ~100px cells, magenta-keyed. Already cleaned + sliced to 32×32 by
`tools/clean_ocean_spritesheet.py` into `resources/images/objects/ocean/`:

| Folder | Count | Role |
|---|---|---|
| `seabed/`  | 32 | static seabed (sand, pebbles, shells, algae, mud, rocks) |
| `shallow/` | 32 | shallow water animation frames |
| `medium/`  | 16 | medium-depth animation frames |
| `deep/`    | 32 | deep water animation frames |
| `details/` | 16 | foam edges/corners, bubbles, sparkles, ripple rings, drift |

**Constraint discovered:** shallow/medium frames are fully opaque in the source.
The cleaning script must bake per-tier alpha so the seabed shows through
(shallow ≈ 0.55 alpha, medium ≈ 0.78, deep stays opaque). Details are already
transparent.

## Architecture — reuse the existing image stack

The existing `TileBorderResolver.resolveInto(sink, frame)` already builds a
bottom-to-top `ArrayList<BufferedImage>` per animation frame: base + B-side
borders + C corners. Water tiles are already excluded from chunk baking and
drawn live each frame (`ChunkTileCache` skips `AnimationComponent` tiles), so
extra stack layers animate correctly with **no render-pass changes**.

We extend the stack for water tiles only:

```
[ seabed ]        NEW  — hash(worldX,worldY)-picked seabed sprite (shallow/medium only)
[ water frame ]   base — animated frame from the new tileset
[ foam border ]   existing B/C overlays at shorelines (foam art)
[ detail ]        NEW  — sparse hash-gated bubble/sparkle/ripple
```

### Depth tiers

| Tier | Tile name | Status | Seabed under? | Alpha |
|---|---|---|---|---|
| Shallow | `shallowWater` | exists | yes | ~0.55 |
| Medium  | `mediumWater`  | NEW    | yes | ~0.78 |
| Deep    | `ocean`        | exists | no  | opaque |

### Anti-tiling

In the resolver (where `tile.worldX/worldY` are available), a deterministic
`hash(worldX, worldY)` selects:
- which seabed variant draws under a given shallow/medium tile,
- a small per-tile animation **phase offset** so the field doesn't pulse in
  lockstep,
- sparse detail placement (only ~1 in N tiles gets a bubble/sparkle).

Hash, not RNG — stable across frames and sessions, no shimmer.

## What changes

1. **`tools/clean_ocean_spritesheet.py`** — bake per-tier alpha; emit the
   frame-named files the resolver expects (`ocean0..2`, `shallowWater0..2`,
   `mediumWater0..2`), seabed variants, foam border/corner sprites, detail sprites
   into `resources/images/tile/`.
2. **`Animations.java`** — add `MEDIUM_WATER_WAVES` clip; existing ocean/shallow
   clips now resolve to the new art (same frame names).
3. **`Tile.setupSpecialBehaviour()`** — attach the medium clip for `mediumWater`;
   wire `mediumWater` like the other water tiles (solid, lightSource).
4. **`TileTypeRegistry.java`** — register `MEDIUM_WATER` (water=true, animated=true,
   altitude between ocean and shallow).
5. **`TileBorderResolver.java`** — for water tiles, prepend the seabed layer and
   append the detail layer; route shoreline overlays to foam art.
6. **`BiomeRegistry.java`** — add `MEDIUM_WATER_LVL` threshold splitting the ocean
   band; classify the new band as `MEDIUM_WATER` placing `mediumWater` tiles.
7. **`TileRules.isWater()`** — add `"mediumWater"`.
8. **`TileImageLoader.registerCanonicalBiomes()`** — register `mediumWater` base.

## What does NOT change (safety)

- **Gameplay**: `TileRules.isWater()` is the single source of truth for boats,
  swimming, collision. `mediumWater` is added there, so it behaves exactly like
  other water. Depth is **purely visual**.
- **`ChunkTileCache`** baking logic — untouched. (Memory note: never bake animated
  water, or animation freezes. We keep water out of the bake, as it already is.)
- All land/beach tiles and their transitions.

## Shoreline foam (kept as overlays, never baked into water)

Foam stays in the B/C overlay layer driven by neighbour adjacency — the existing
mechanism. Water base frames stay borderless. New foam sprites (`oceanFoamB1`,
`oceanFoamC0`, etc.) are derived from the details row; the loader's existing
rotation logic generates the other sides/corners from one source side.

## Testing / verification

- Re-run the cleaning script; assert 0 opaque magenta (already passing) and that
  shallow/medium output has reduced alpha.
- Headless probe (pattern: `resources/testing/probes/BeachWaterTransitionProbe.java`)
  rendering a shallow→medium→deep→beach strip to a PNG; inspect that seabed shows
  under shallow, opacity ramps with depth, and foam appears only at shorelines.
- Run the game and screenshot a coastline.
- Confirm a boat still floats on `mediumWater` (gameplay unchanged).

## Out of scope

- River art rework (river reuses ocean; inherits the new look automatically).
- Shader-based water distortion (Java2D blit only).
- Drifting-foam motion as moving entities (details are static sparse overlays).
