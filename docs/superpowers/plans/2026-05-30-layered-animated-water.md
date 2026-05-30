# Layered Animated Water System — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework ocean graphics into a depth-aware, animated, seabed-revealing water system using the new OceanSprites art, with foam shoreline overlays — gameplay unchanged.

**Architecture:** Reuse the existing per-frame image stack in `TileBorderResolver.resolveInto()`. For water tiles, prepend a hash-picked seabed sprite (shallow/medium only) and append sparse detail overlays. Add a `mediumWater` depth tier between `shallowWater` and `ocean`. Shallow/medium frames get baked alpha so the seabed shows through. Water stays out of the chunk bake (already true), so it animates live.

**Tech Stack:** Java (Java2D BufferedImage blitting), Python+Pillow (offline sprite prep), headless probe + worldgen-PNG exporter for verification.

**Build/run reference:**
- Compile: `javac -d out @sources.txt`
- Run game: `java -cp out resources.app.Main`
- Run probes: `java -cp out resources.testing.TestRunner`
- Render a world to PNG: `java -cp out resources.testing.WorldGenerationImageExporter <seed> <wTiles> <hTiles> <outPath>`

**Key facts (verified against code):**
- Tile size 64px; chunk 8×8; `TileImageLoader` loads `resources/images/tile/<name>.png` and scales border/overlay variants to 32px.
- Resolver looks up base frame `<name><frame>` (e.g. `ocean0`), falling back to `<name>`. Borders `<name>B<side>`, corners `<name>C<corner>`, with animated variants `<name><frame>B<side>`.
- `Tile.worldX/worldY` available in the resolver via `tile` field — use for hashing.
- Gameplay water check is solely `TileRules.isWater(tileName)`.

---

## File Structure

| File | Change | Responsibility |
|---|---|---|
| `tools/clean_ocean_spritesheet.py` | Modify | Bake per-tier alpha; emit frame-named tile PNGs into `resources/images/tile/` |
| `resources/images/tile/ocean{0,1,2}.png` etc. | Generate | New water/seabed/foam/detail sprites |
| `resources/presentation/animation/Animations.java` | Modify | Add `MEDIUM_WATER_WAVES` clip |
| `resources/domain/tile/Tile.java` | Modify | Wire `mediumWater` special behaviour |
| `resources/domain/tile/TileTypeRegistry.java` | Modify | Register `MEDIUM_WATER` type |
| `resources/world/placement/TileRules.java` | Modify | Add `mediumWater` to `isWater()` |
| `resources/presentation/image/TileImageLoader.java` | Modify | Register `mediumWater` canonical base |
| `resources/domain/tile/SeabedPicker.java` | Create | Deterministic hash → seabed/detail variant selection |
| `resources/domain/tile/TileBorderResolver.java` | Modify | Prepend seabed, append detail for water tiles |
| `resources/generation/biome/BiomeRegistry.java` | Modify | Add `MEDIUM_WATER_LVL` + `MEDIUM_WATER` biome + classify branch |
| `resources/testing/probes/WaterDepthProbe.java` | Create | Verify depth tiers, seabed, foam present |
| `resources/testing/TestRunner.java` | Modify | Register `WaterDepthProbe` |

---

## Task 1: Sprite prep — bake alpha and emit frame-named tile files

**Files:**
- Modify: `tools/clean_ocean_spritesheet.py`
- Generate into: `resources/images/tile/`

**Background:** The resolver loads water frames as `resources/images/tile/ocean0.png` etc. The current script writes to `resources/images/objects/ocean/`. We add an emit step that writes the engine-facing files with the right names AND bakes per-tier alpha (shallow/medium were fully opaque in the source).

- [ ] **Step 1: Add alpha constants and an emit-to-tile-dir function**

In `tools/clean_ocean_spritesheet.py`, after the `ROW_GROUPS` dict, add:

```python
TILE_OUT = os.path.join(ROOT, "resources/images/tile")

# Per-tier alpha so the seabed shows through. Deep stays opaque.
TIER_ALPHA = {"shallow": 0.55, "medium": 0.78, "deep": 1.0}

# How many animation frames the engine expects per water tier (resolver uses 0..2).
FRAMES_PER_TIER = 3
```

- [ ] **Step 2: Add a function that applies alpha to an RGBA cell**

Add near `fit_into`:

```python
def apply_alpha(cell, factor):
    """Multiply the alpha channel by factor (keeps already-transparent pixels)."""
    if factor >= 0.999:
        return cell
    a = np.array(cell.convert("RGBA"))
    a[:, :, 3] = (a[:, :, 3].astype(float) * factor).round().astype("uint8")
    return Image.fromarray(a, "RGBA")
```

- [ ] **Step 3: Emit engine-facing tile PNGs in `main()`**

In `main()`, after the existing per-cell save loop completes (after `sheet.save(sheet_path)`), add a second pass that writes the named tile files. Append:

```python
    os.makedirs(TILE_OUT, exist_ok=True)

    def cleaned_cell(ri, ci, alpha=1.0, fit=False):
        (y0, y1) = ROWS[ri]; (x0, x1) = COLS[ci]
        c = clean_cell(src.crop((x0, y0, x1 + 1, y1 + 1)))
        c = fit_into(c, TILE) if fit else c.resize((TILE, TILE), Image.NEAREST)
        return apply_alpha(c, alpha)

    # Water animation frames: take the first 3 columns of each tier's primary row.
    # Deep -> ocean0..2 ; medium -> mediumWater0..2 ; shallow -> shallowWater0..2
    tier_rows = {"deep": 5, "medium": 4, "shallow": 2}
    tier_name = {"deep": "ocean", "medium": "mediumWater", "shallow": "shallowWater"}
    for tier, row in tier_rows.items():
        for f in range(FRAMES_PER_TIER):
            img = cleaned_cell(row, f, TIER_ALPHA[tier])
            img.save(os.path.join(TILE_OUT, f"{tier_name[tier]}{f}.png"))

    # Seabed variants: row 0 (light) gives us seabed0..7 from its first 8 columns.
    for i in range(8):
        cleaned_cell(0, i, 1.0).save(os.path.join(TILE_OUT, f"seabed{i}.png"))

    # Foam border (one source side) + corner (one source corner) from the detail row.
    # Detail band is ROWS[7]. Column 0 = a horizontal foam edge, column 4 = a corner.
    cleaned_cell(7, 0, 1.0, fit=True).save(os.path.join(TILE_OUT, "oceanFoamB1.png"))
    cleaned_cell(7, 4, 1.0, fit=True).save(os.path.join(TILE_OUT, "oceanFoamC0.png"))

    # Detail overlays: bubbles (col ~10), sparkle (col ~12), ripple ring (col ~14).
    cleaned_cell(7, 10, 1.0, fit=True).save(os.path.join(TILE_OUT, "oceanDetail0.png"))
    cleaned_cell(7, 12, 1.0, fit=True).save(os.path.join(TILE_OUT, "oceanDetail1.png"))
    cleaned_cell(7, 14, 1.0, fit=True).save(os.path.join(TILE_OUT, "oceanDetail2.png"))

    print("Emitted engine tile sprites into", TILE_OUT)
```

> Note: the exact detail/foam column indices (0,4,10,12,14) are best-guesses from the
> cleaned sheet. Step 5 verifies them visually; adjust indices if a sprite is wrong.

- [ ] **Step 4: Run the script**

Run: `python3 tools/clean_ocean_spritesheet.py`
Expected output ends with `OK: all purple removed.` and `Emitted engine tile sprites into .../resources/images/tile`.

- [ ] **Step 5: Visually verify the emitted sprites**

Run:
```bash
python3 -c "
from PIL import Image
names=['ocean0','mediumWater0','shallowWater0','seabed0','oceanFoamB1','oceanFoamC0','oceanDetail0','oceanDetail1','oceanDetail2']
import os
strip=Image.new('RGBA',(32*len(names),32),(80,80,80,255))
for i,n in enumerate(names):
    im=Image.open(f'resources/images/tile/{n}.png').convert('RGBA'); strip.alpha_composite(im,(i*32,0))
strip.resize((32*len(names)*3,96),Image.NEAREST).convert('RGB').save('/tmp/water_sprites.png'); print('ok')
"
```
Then Read `/tmp/water_sprites.png`. Expected: shallow visibly more transparent than ocean (gray shows through), seabed is sandy, foam pieces are white crescents, details are bubble/sparkle/ring shapes. If foam/detail cells are wrong, adjust the column indices in Step 3 and re-run.

- [ ] **Step 6: Commit**

```bash
git add tools/clean_ocean_spritesheet.py resources/images/tile/
git commit -m "feat: emit alpha-baked layered water sprites from ocean spritesheet"
```

---

## Task 2: Register the medium-water animation clip

**Files:**
- Modify: `resources/presentation/animation/Animations.java`

- [ ] **Step 1: Add the identifier**

In `Animations.java`, after the `SHALLOW_WATER_WAVES` field (line 16):

```java
    public static final Identifier MEDIUM_WATER_WAVES  = Identifier.of("tile/medium_water_waves");
```

- [ ] **Step 2: Register it in bootstrap**

In `bootstrap()`, after the `SHALLOW_WATER_WAVES` registration line:

```java
        if (!library.contains(MEDIUM_WATER_WAVES))  library.register(MEDIUM_WATER_WAVES,  mediumWaterWaves(images));
```

- [ ] **Step 3: Add the clip builder**

After the `shallowWaterWaves` method:

```java
    /** Medium-depth wave loop — same cadence, darker sprite set. */
    private static AnimationClip mediumWaterWaves(ImageContainer images) {
        return new AnimationClip(MEDIUM_WATER_WAVES, true,
            new AnimationFrame(images.getTileImage("mediumWater0"), 30),
            new AnimationFrame(images.getTileImage("mediumWater1"), 30),
            new AnimationFrame(images.getTileImage("mediumWater2"), 30));
    }
```

- [ ] **Step 4: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add resources/presentation/animation/Animations.java
git commit -m "feat: add medium-water wave animation clip"
```

---

## Task 3: Register the mediumWater tile type, gameplay, and base sprite

**Files:**
- Modify: `resources/domain/tile/TileTypeRegistry.java:29`
- Modify: `resources/world/placement/TileRules.java:31-36`
- Modify: `resources/presentation/image/TileImageLoader.java:54`
- Modify: `resources/domain/tile/Tile.java:60-65`

- [ ] **Step 1: Register the tile type**

In `TileTypeRegistry.java`, after the `SHALLOW_WATER` line (line 29), add:

```java
    public static final TileType MEDIUM_WATER    = define("mediumWater",     1,   true,  true,  true);
```
(altitudeBucket 1 sits between ocean=0 and shallowWater=3, so render ordering ramps deep→medium→shallow.)

- [ ] **Step 2: Add to gameplay water check**

In `TileRules.isWater()`, add the `mediumWater` clause:

```java
    public static boolean isWater(String tileName) {
        if (tileName == null) return false;
        return "ocean".equals(tileName)
            || "river".equals(tileName)
            || "mediumWater".equals(tileName)
            || "shallowWater".equals(tileName);
    }
```

- [ ] **Step 3: Register the canonical base sprite**

In `TileImageLoader.registerCanonicalBiomes()`, after the `shallowWater` line (line 54):

```java
        loadTile("mediumWater",     "mediumWater0.png");
```

- [ ] **Step 4: Wire special behaviour on the tile**

In `Tile.setupSpecialBehaviour()`, add an `else if` after the `shallowWater` branch (before the closing brace at line 65):

```java
        } else if ("mediumWater".equals(getName())) {
            solid = true;
            lightSource = true;
            AnimationClip waves = panel.animations().require(Animations.MEDIUM_WATER_WAVES);
            addComponent(new AnimationComponent(waves, panel.clock()));
```

- [ ] **Step 5: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add resources/domain/tile/TileTypeRegistry.java resources/world/placement/TileRules.java resources/presentation/image/TileImageLoader.java resources/domain/tile/Tile.java
git commit -m "feat: register mediumWater tile type and wire its animation"
```

---

## Task 4: Deterministic seabed/detail picker

**Files:**
- Create: `resources/domain/tile/SeabedPicker.java`

- [ ] **Step 1: Write the picker**

```java
package resources.domain.tile;

/**
 * Deterministic per-cell selection for the layered water system. A stable hash
 * of world coordinates picks which seabed variant draws under a water tile and
 * whether a sparse detail overlay (bubble/sparkle/ripple) appears — so the water
 * field never visibly repeats and never shimmers between frames (hash, not RNG).
 */
public final class SeabedPicker {

    public static final int SEABED_VARIANTS = 8;
    public static final int DETAIL_VARIANTS = 3;
    /** Roughly 1-in-N water tiles carries a detail overlay. */
    private static final int DETAIL_SPARSITY = 11;

    private SeabedPicker() {}

    private static int hash(int x, int y) {
        int h = x * 73856093 ^ y * 19349663;
        h ^= (h >>> 13);
        return h & 0x7fffffff;
    }

    /** Seabed sprite name (seabed0..7) for the cell at the given world coords. */
    public static String seabedFor(int worldX, int worldY) {
        return "seabed" + (hash(worldX, worldY) % SEABED_VARIANTS);
    }

    /** Detail sprite name, or null if this cell has no detail overlay. */
    public static String detailFor(int worldX, int worldY) {
        int h = hash(worldX + 1, worldY - 1);
        if (h % DETAIL_SPARSITY != 0) return null;
        return "oceanDetail" + ((h / DETAIL_SPARSITY) % DETAIL_VARIANTS);
    }
}
```

- [ ] **Step 2: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add resources/domain/tile/SeabedPicker.java
git commit -m "feat: deterministic seabed/detail picker for water tiles"
```

---

## Task 5: Layer seabed under + details over water in the resolver

**Files:**
- Modify: `resources/domain/tile/TileBorderResolver.java:73-82`

**Background:** `resolveInto` builds the stack bottom→top: base, borders, corners. We insert a seabed layer *before* the base for shallow/medium water, and append a detail layer *after* corners for all water. Deep ocean gets no seabed (opaque) but can still get details.

- [ ] **Step 1: Add a helper to detect the water tier**

In `TileBorderResolver.java`, add near the top of the class (after the constructor):

```java
    private boolean isSeabedRevealing() {
        String n = tile.getName();
        return "shallowWater".equals(n) || "mediumWater".equals(n);
    }

    private boolean isWaterTile() {
        String n = tile.getName();
        return "ocean".equals(n) || "river".equals(n)
            || "mediumWater".equals(n) || "shallowWater".equals(n);
    }
```

- [ ] **Step 2: Insert seabed + detail layers in resolveInto**

Replace the body of `resolveInto` (lines 73-82) with:

```java
    public void resolveInto(ArrayList<BufferedImage> sink, int frame) {
        sink.clear();
        if (isSeabedRevealing()) {
            sink.add(images.getTileImage(
                SeabedPicker.seabedFor((int) tile.worldX, (int) tile.worldY)));
        }
        sink.add(baseFrame(frame));
        boolean[] borders = tile instanceof resources.domain.farming.FarmTile
                ? addFarmBorders(sink, frame)
                : addBorders(sink, frame);
        addCorners(sink, frame, borders);
        if (isWaterTile()) {
            String detail = SeabedPicker.detailFor((int) tile.worldX, (int) tile.worldY);
            if (detail != null) sink.add(images.getTileImage(detail));
        }
    }
```

> `tile.worldX/worldY` are `protected`/accessible fields on `BaseEntity` used elsewhere; if they are not visible from this package, add `public int tileWorldX()`/`tileWorldY()` accessors on `Tile` and call those instead. Verify at compile time (Step 3).

- [ ] **Step 3: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors. If `worldX/worldY` are inaccessible, add accessors on `Tile` (`public int tileWorldX() { return (int) worldX; }`) and switch the calls, then recompile.

- [ ] **Step 4: Commit**

```bash
git add resources/domain/tile/TileBorderResolver.java resources/domain/tile/Tile.java
git commit -m "feat: layer seabed under and details over animated water"
```

---

## Task 6: Add the medium-water generation band

**Files:**
- Modify: `resources/generation/biome/BiomeRegistry.java:22-23` (levels), `:94` (biome), `:155-156` (classify)

- [ ] **Step 1: Add the depth threshold**

In `BiomeRegistry.java`, after `OCEAN_LVL` / before `SHALLOW_WATER_LVL` (lines 22-23), insert:

```java
    public static final double MEDIUM_WATER_LVL   = -0.1375;
```
(midpoint between OCEAN_LVL −0.175 and SHALLOW_WATER_LVL −0.10, giving a medium band on the continental shelf.)

- [ ] **Step 2: Define the biome**

After the `SHALLOW_WATER` biome (line 94):

```java
    public static final Biome MEDIUM_WATER     = new Biome("mediumWater",     "mediumWater",     true,  Collections.emptyList());
```

- [ ] **Step 3: Add the classify branch**

In `classify()`, between the OCEAN and SHALLOW_WATER checks (after line 155):

```java
        if (height <= MEDIUM_WATER_LVL)  return MEDIUM_WATER;
```

- [ ] **Step 4: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors.

- [ ] **Step 5: Render a world to verify the depth bands**

Run: `java -cp out resources.testing.WorldGenerationImageExporter 12345 256 256 /tmp/water_world.png`
Then Read `/tmp/water_world.png`. Expected: coastlines show a deep→medium→shallow→sand gradient (three distinct blue bands before the beach), not a single flat ocean color. Seabed texture visible in the shallow band.

- [ ] **Step 6: Commit**

```bash
git add resources/generation/biome/BiomeRegistry.java
git commit -m "feat: generate a medium-water depth band on the continental shelf"
```

---

## Task 7: Foam shoreline overlays

**Files:**
- Modify: `resources/domain/tile/TileBorderResolver.java` (OVERLAY_FAMILY map)

**Background:** Beach/sand neighbours already draw `wetBeach`/`beach` overlays on the adjacent water tile via the B/C machinery + rotation. We route the water-side shore overlay to the new `oceanFoam` family so foam (not a flat beach edge) renders where water meets land. `oceanFoamB1`/`oceanFoamC0` exist; the loader's rotation logic derives the other sides/corners.

- [ ] **Step 1: Route shoreline overlays to foam**

In `buildHostOverlayFamilyMap()` (line 50), add entries so each water host draws foam against sandy neighbours:

```java
    private static Map<String, String> buildHostOverlayFamilyMap() {
        Map<String, String> m = new HashMap<>();
        m.put("tidalSand|wetBeach", "wetBeachDry");
        m.put("shallowWater|tidalSand", "oceanFoam");
        m.put("shallowWater|wetBeach", "oceanFoam");
        m.put("mediumWater|shallowWater", "oceanFoam");
        m.put("ocean|mediumWater", "oceanFoam");
        return m;
    }
```

- [ ] **Step 2: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors.

- [ ] **Step 3: Render and verify foam at shorelines**

Run: `java -cp out resources.testing.WorldGenerationImageExporter 12345 256 256 /tmp/water_foam.png`
Then Read `/tmp/water_foam.png`. Expected: white foam crescents trace the waterline where shallow water meets sand, and at the deep/medium/shallow internal boundaries. Water interiors stay borderless (no foam in open water).

- [ ] **Step 4: Commit**

```bash
git add resources/domain/tile/TileBorderResolver.java
git commit -m "feat: foam overlays at water depth and shore boundaries"
```

---

## Task 8: Verification probe

**Files:**
- Create: `resources/testing/probes/WaterDepthProbe.java`
- Modify: `resources/testing/TestRunner.java:69` (register)

- [ ] **Step 1: Write the probe**

Model it on `BeachWaterTransitionProbe`. It asserts: the three water base sprites differ, shallow/medium sprites have sub-255 alpha somewhere, and seabed/foam/detail sprites load (non-null, non-fallback).

```java
package resources.testing.probes;

import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the layered water assets are wired: the three depth-tier base frames
 * are distinct, shallow/medium have baked transparency (so seabed shows
 * through), and the seabed/foam/detail overlay sprites resolve.
 */
public final class WaterDepthProbe implements Probe {

    private static final Logger LOG = Logger.forClass(WaterDepthProbe.class);

    @Override public String name() { return "water-depth"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        BufferedImage deep    = ctx.images().getTileImage("ocean0");
        BufferedImage medium  = ctx.images().getTileImage("mediumWater0");
        BufferedImage shallow = ctx.images().getTileImage("shallowWater0");
        if (deep == null || medium == null || shallow == null)
            return ProbeResult.fail(name() + " missing a water base frame");

        if (sameImage(deep, medium) || sameImage(medium, shallow))
            return ProbeResult.fail(name() + " depth tiers are not visually distinct");

        if (!hasTransparency(shallow) || !hasTransparency(medium))
            return ProbeResult.fail(name() + " shallow/medium not alpha-baked (seabed can't show through)");

        for (String n : new String[]{"seabed0", "oceanFoamB1", "oceanDetail0"}) {
            if (ctx.images().getTileImage(n) == null)
                return ProbeResult.fail(name() + " missing overlay sprite " + n);
        }
        LOG.info("water depth tiers, alpha, and overlays all present");
        return ProbeResult.pass(name());
    }

    private static boolean hasTransparency(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if (((img.getRGB(x, y) >>> 24) & 0xff) < 250) return true;
        return false;
    }

    private static boolean sameImage(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
        return true;
    }
}
```

> If `ProbeResult.pass/fail/skip` or `GameContext.images()` signatures differ, match
> them to `BeachWaterTransitionProbe` (already in the tree) — it uses the same API.

- [ ] **Step 2: Register the probe**

In `TestRunner.java`, add the import next to the other probe imports and, after `probes.add(new BeachWaterTransitionProbe());` (line 69):

```java
        probes.add(new WaterDepthProbe());
```
(import: `import resources.testing.probes.WaterDepthProbe;`)

- [ ] **Step 3: Compile**

Run: `javac -d out @sources.txt`
Expected: no errors.

- [ ] **Step 4: Run probes**

Run: `java -cp out resources.testing.TestRunner`
Expected: output includes `water-depth` PASS, and existing probes (especially `beach-water-transition`, `boat`) still PASS.

- [ ] **Step 5: Commit**

```bash
git add resources/testing/probes/WaterDepthProbe.java resources/testing/TestRunner.java
git commit -m "test: probe asserting layered water tiers, alpha, and overlays"
```

---

## Task 9: Full integration check in the running game

- [ ] **Step 1: Run the game**

Run: `java -cp out resources.app.Main`
Walk to a coastline. Expected: animated water, visible deep→medium→shallow gradient, seabed texture under shallow water, foam at the shoreline, sparse bubbles/sparkles. No purple anywhere.

- [ ] **Step 2: Confirm gameplay unchanged**

In-game, place/ride a boat across medium water. Expected: boat floats on `mediumWater` exactly as on ocean (because `TileRules.isWater` includes it). Player can't walk into any water tier.

- [ ] **Step 3: Final commit (if any tweaks)**

```bash
git add -A
git commit -m "feat: layered animated water system — integration verified"
```

---

## Self-review notes

- **Spec coverage:** seabed underlayer (T1 alpha + T5), three tiers (T1/T3/T6), animation (T2/existing), anti-tiling hash (T4/T5), foam overlays kept separate (T7), gameplay unchanged (T3 step 2, T9 step 2), no bake changes (resolver-only), verification (T8 probe + worldgen PNG). All covered.
- **Type consistency:** `SeabedPicker.seabedFor/detailFor`, sprite names `ocean0..2`/`mediumWater0..2`/`shallowWater0..2`/`seabed0..7`/`oceanFoamB1`/`oceanFoamC0`/`oceanDetail0..2`, biome/type name `mediumWater`, clip `MEDIUM_WATER_WAVES` — consistent across tasks.
- **Known risk flagged inline:** worldX/worldY visibility (T5 step 2 note); foam/detail column indices in the spritesheet (T1 step 5 verifies visually).
```
