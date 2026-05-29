# Fantasy Crop Expansion — Design Plan + Spritesheet Prompt

_Drafted 2026-05-29. Extends the farming system documented in [FARM.md](FARM.md)._

8 new fantasy crops to grow on Farmland. Each entry gives: the theme, which biome it suits,
growth feel, and **what it's used for** — split into **uses that work with existing systems
today** (crafting via [RecipeRegistry.java](resources/domain/crafting/RecipeRegistry.java)) and
**proposed new mechanics** (flagged ⚙️ — these need new code: a consumable/effect system, which
does not exist yet).

All crops keep the engine's current shape: **4 growth stages** (`_stage0..3`), 600 ticks/stage
([Crop.java](resources/domain/farming/Crop.java)). Naming follows the existing pattern:
inventory item `seeds_<name>` → planted entity `crop_<name>` → harvested produce `<name>`.

---

## The 8 crops

### 1. Emberwheat 🔥
- **Theme:** Wheat whose grains glow like embers; stalks tipped with smoldering orange seed-heads.
- **Biome:** Savanna / desert edge (dry, warm).
- **Growth feel:** Sprouts dull green, ripens to a hot orange glow at stage 3.
- **Uses today:** A fire-themed `wheat` substitute. Recipe: `emberwheat ×1 + meat ×1 → torch`
  (replaces the current `torch_from_meat`), and `emberwheat ×2 → 2 torches` (fuel staple).
- **⚙️ Proposed:** smelting-fuel item; cook raw `meat` → `cooked_meat` at a future campfire.

### 2. Frostbloom ❄️
- **Theme:** Pale blue-white flowering crop, frost crystals on the petals.
- **Biome:** Icy plains / snowy taiga (only crop that thrives in cold — gives those biomes a point).
- **Growth feel:** Bare stem → blue bud → white frosted bloom.
- **Uses today:** Decorative/trade good; `frostbloom ×4 → barrel` analog for cold-storage flavor.
- **⚙️ Proposed:** brew into a "cooling" tonic that resists a future heat/thirst stat; preserves food.

### 3. Glowcap 🍄
- **Theme:** Bioluminescent mushroom cluster, teal-glowing caps. (Pairs with existing `mushroom`.)
- **Biome:** Forest / rain forest (shade-loving) and caves.
- **Growth feel:** Mycelium mat → small caps → tall glowing caps.
- **Uses today:** `glowcap ×2 → torch` (a light source that doesn't need fire); stacks with the
  existing `mushroom` economy.
- **⚙️ Proposed:** place as a soft ambient light entity (like `torch` but cold light).

### 4. Manaberry 💠
- **Theme:** Crystalline berry bush; fruit looks like the existing `crystal` ore but soft/translucent.
- **Biome:** Forest / plains.
- **Growth feel:** Green bush → faint glow → clustered glowing berries.
- **Uses today:** A renewable `crystal` alternative for crafting: `manaberry ×3 → crystal`
  (lets players farm crystal instead of only mining it).
- **⚙️ Proposed:** primary reagent for a future potion/enchant system.

### 5. Ironvine ⛓️
- **Theme:** Metallic-leafed climbing vine bearing hard, ore-like seed pods.
- **Biome:** Savanna / plains; slow grower (the "industrial" crop).
- **Growth feel:** Thin vine → thick metallic vine → heavy grey pods.
- **Uses today:** Renewable metal feedstock: `ironvine ×3 → iron_ore`, enabling tool crafting
  (`sword_from_iron`, etc.) without mining. Make it the **slowest** crop so it doesn't trivialize ore.
- **⚙️ Proposed:** harvested raw, must be "smelted" to refine.

### 6. Sungourd ☀️
- **Theme:** Big golden pumpkin/gourd with a sun-pattern rind. The "hearty food" crop.
- **Biome:** Plains (classic farm staple).
- **Growth feel:** Vine → small green gourd → large golden gourd. Reads clearly even at 32px.
- **Uses today:** High-value trade produce; `sungourd ×1 → 4 wheat` flavor as a bulk-food converter,
  or simply a sellable good.
- **⚙️ Proposed:** restores hunger/health when an `eat` action exists.

### 7. Bloodroot 🩸
- **Theme:** Dark crimson root vegetable, jagged leaves above a deep-red taproot.
- **Biome:** Swamp (only crop tuned for wet/mud — gives swamps farming value).
- **Growth feel:** Spiky red sprout → bushy → dark leaves with a hint of the root.
- **Uses today:** Dye/alchemy material; `bloodroot ×2 + hide ×1 → barrel` (red-stained) flavor, or
  a trade reagent.
- **⚙️ Proposed:** healing salve / red dye for a future dye system.

### 8. Stardrop Vine 🌟
- **Theme:** The rare "endgame" crop — a vine bearing a single star-shaped luminous fruit.
- **Biome:** Any tilled tile, but **slowest + lowest yield** (the prestige crop).
- **Growth feel:** Tiny sprout → curling vine with bud → single glowing star fruit at stage 3.
- **Uses today:** Top-tier trade good; `stardrop ×1 + manaberry ×3 → crystal ×4` (premium converter).
- **⚙️ Proposed:** crafting key for the best gear / a win-condition collectible.

---

## Crop tuning summary

| Crop | seeds item | crop entity | produce | biome niche | suggested growth | drop |
|---|---|---|---|---|---|---|
| Emberwheat | `seeds_emberwheat` | `crop_emberwheat` | `emberwheat` | savanna/desert | normal (600/stage) | 1–3 |
| Frostbloom | `seeds_frostbloom` | `crop_frostbloom` | `frostbloom` | icy/snowy | normal | 1–2 |
| Glowcap | `seeds_glowcap` | `crop_glowcap` | `glowcap` | forest/cave | fast (450/stage) | 2–4 |
| Manaberry | `seeds_manaberry` | `crop_manaberry` | `manaberry` | forest/plains | normal | 1–3 |
| Ironvine | `seeds_ironvine` | `crop_ironvine` | `ironvine` | savanna/plains | **slow (900/stage)** | 1–2 |
| Sungourd | `seeds_sungourd` | `crop_sungourd` | `sungourd` | plains | normal | 1 (big) |
| Bloodroot | `seeds_bloodroot` | `crop_bloodroot` | `bloodroot` | swamp | normal | 1–3 |
| Stardrop Vine | `seeds_stardrop` | `crop_stardrop` | `stardrop` | any | **slowest (1200/stage)** | 1 |

> Note: biome-restricted planting and per-crop growth speed are **not implemented yet** — the
> engine currently hardcodes 4 stages × 600 ticks and tills only plains/forest/savanna
> ([TileRules.java](resources/world/placement/TileRules.java)). Treat the biome/speed columns as
> the design target; wiring them up is a follow-up code task.

---

## What to wire up in code (per crop)

For each crop, mirror wheat/carrot:
1. `CropRegistry.register("crop_<name>", new Entry("<name>", null))` — [CropRegistry.java](resources/domain/farming/CropRegistry.java)
2. `FarmingRegistry` mature-stage harvest profile — [FarmingRegistry.java](resources/domain/farming/FarmingRegistry.java)
3. Three `ItemType` defines in [ItemTypeRegistry.java](resources/domain/inventory/ItemTypeRegistry.java):
   `seeds_<name>` (placed → `crop_<name>`), `<name>` produce, and ensure the seed→crop planting
   fix from [FARM.md §3](FARM.md) is in.
4. `PlacementRegistry` `PLANT_SEED` spec for `seeds_<name>` — [PlacementRegistry.java](resources/world/placement/PlacementRegistry.java)
5. `ItemManager` physical rep mapping `seeds_<name>` → `crop_<name>_stage0` preview.
6. Recipes in [RecipeRegistry.java](resources/domain/crafting/RecipeRegistry.java) per "Uses today" above.
7. The 4 stage sprites + produce/seed icons (see spritesheet below).

---

## Sprites required

Per crop: **4 stage sprites** (`crop_<name>_stage0..3`), plus optional **seed icon**
(`seeds_<name>`) and **produce icon** (`<name>`). Plus a refreshed **farmland tile set**.

- 8 crops × 4 stages = **32 crop sprites**
- 1 farmland base + edge/corner border variants (matching the `B1`/`C0` tile convention)
- Optional: 8 seed icons + 8 produce icons (the seed icon auto-aliases to `_stage0` if omitted)

Engine art facts to honor:
- **Tiles & crop sprites are 32×32 px** (confirmed: `plains.png`, `farmland.png` are 32×32).
- Top-down view. Transparent background for crops (they draw on top of farmland).
- Border tiles in this game use suffixes: base, `B1` (edge), `C0` (corner) — same as `plainsB1`/`plainsC0`.

---

## Spritesheet generation prompt

> Copy the block below into your image generator. It targets a single sheet on a 32×32 grid.

```
A single top-down pixel-art sprite sheet for a 2D survival/farming game. VERY LOW
RESOLUTION pixel art — each sprite is exactly 32x32 pixels, chunky readable pixels,
NO anti-aliasing, hard pixel edges, limited palette per sprite, drawn in a clean
top-down (bird's-eye) perspective. Crisp, retro, SNES-farming-game look. Every sprite
sits on its own 32x32 cell aligned to a 32x32 grid. Crop sprites have a fully
TRANSPARENT background (they are drawn on top of soil); tile sprites fill their whole
cell edge-to-edge with no transparency.

Layout: rows of 4 columns. Each crop occupies one row showing 4 growth stages left to
right — stage 0 (tiny sprout / disturbed soil), stage 1 (young growth), stage 2
(near-mature, larger), stage 3 (fully mature, vivid and harvest-ready). Growth should
read clearly even at 32px: each stage noticeably taller/fuller/more colorful than the last.

TOP ROW — FARMLAND TILES (soil, 32x32, top-down, fills the full cell, tileable):
  - tilled dark-brown soil with visible plow furrows (the base "farmland" tile)
  - the same soil but WATERED: darker, wet, slightly glossy ("farmland_watered")
  - soil EDGE border variant (soil meeting grass on one side, like the game's "B1" tiles)
  - soil CORNER border variant (soil meeting grass on two sides, like the game's "C0" tiles)

THEN ONE ROW PER CROP, 4 stages each, transparent background, plant centered in cell:

  EMBERWHEAT — fantasy wheat with glowing ember-orange grain heads, dry savanna stalks;
    stage3 grains glow hot orange like embers.
  FROSTBLOOM — pale blue-white flowering crop, frost crystals on petals; cold/icy feel;
    stage3 is a white frosted bloom with a faint blue glow.
  GLOWCAP — cluster of bioluminescent teal-glowing mushrooms; forest/cave feel;
    stage3 has tall glowing teal caps.
  MANABERRY — bush bearing crystalline translucent glowing berries (soft cyan/violet);
    stage3 has clustered glowing crystal berries.
  IRONVINE — metallic grey-leafed vine with hard ore-like seed pods; industrial feel;
    stage3 has heavy dark-grey metallic pods.
  SUNGOURD — large golden gourd/pumpkin with a radiant sun pattern on the rind;
    stage3 is a big bright golden gourd.
  BLOODROOT — dark crimson root vegetable with jagged dark-red leaves; swampy feel;
    stage3 shows bushy dark leaves and a hint of deep-red root.
  STARDROP VINE — rare curling vine bearing a single luminous star-shaped fruit;
    stage3 is one glowing yellow-white star fruit.

Consistent lighting from above, consistent pixel scale across all sprites, cohesive
earthy fantasy palette. No text, no labels, no grid lines drawn on the sprites, no
drop shadows outside the 32x32 cell. Pure 32x32 pixel sprites on a transparent sheet
background (except the soil tiles which are opaque).
```

### Tips for slicing the result
- Slice on a strict 32×32 grid. Each crop row → `crop_<name>_stage0.png` … `stage3.png`.
- Crop PNG goes in its own folder under `resources/images/objects/...`, e.g.
  `objects/nature/crops/crop_emberwheat_stage0/crop_emberwheat_stage0.png` (the loader indexes by
  folder name, so depth is free — see [ObjectImageLoader.java](resources/presentation/image/ObjectImageLoader.java)).
- Farmland tiles go in `resources/images/tile/` (e.g. `farmland.png`, `farmland_watered.png`) or
  the existing `objects/.../soil/farmland/` folder, matching current usage.
- If the generator can't do true 32px, render larger then **downscale with nearest-neighbor** (no
  smoothing) to 32×32 to preserve the chunky pixels.
```
