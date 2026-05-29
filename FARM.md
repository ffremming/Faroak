# Farming System — State & How To Try It

_Last surveyed: 2026-05-29_

The farming system is **architecturally complete and end-to-end functional** (till → plant →
grow → harvest), but it currently can't be played by hand because of a **seed-naming mismatch**
and because **none of the farming art exists yet** (everything renders as a grey `?` placeholder).
Both are easy to fix. Details below.

---

## 1. The loop, as designed

```
[hoe] press SPACE on plains/forest/savanna tile      → spawns Farmland
[watering_can] interact with Farmland                → marks it watered (cosmetic only, see §4)
[seed] click / SPACE on unplanted Farmland           → spawns Crop (stage 0)
Crop grows 4 stages × 600 ticks (~2400 ticks total)  → stage 3 = mature
left-click mature Crop                               → drops 1–3 produce into inventory
```

| Concept | File |
|---|---|
| Crop entity (growth + harvest) | [Crop.java](resources/domain/farming/Crop.java) |
| Farmland tile (till target, water/plant state) | [Farmland.java](resources/domain/farming/Farmland.java) |
| Crop definitions (produce, required tool) | [CropRegistry.java](resources/domain/farming/CropRegistry.java) |
| Harvest-profile bootstrap | [FarmingRegistry.java](resources/domain/farming/FarmingRegistry.java) |
| Hoe / plant operations | [FarmingService.java](resources/domain/farming/FarmingService.java) |
| Growth driver (stage timer) | [GrowableComponent.java](resources/domain/entity/component/GrowableComponent.java) |
| Harvest data (tool, drops, durability) | [HarvestableComponent.java](resources/domain/entity/component/HarvestableComponent.java) |
| Harvest execution + drops | [HarvestService.java](resources/domain/inventory/HarvestService.java) |
| Item type defs (seeds, tools, produce) | [ItemTypeRegistry.java](resources/domain/inventory/ItemTypeRegistry.java) |
| Placement wiring (mouse path) | [PlacementRegistry.java](resources/world/placement/PlacementRegistry.java) |
| Tillable-terrain rule | [TileRules.java](resources/world/placement/TileRules.java) |

Crops supported today: **wheat** and **carrot** (`CropRegistry`, both harvestable with bare hands).
Wheat feeds ~9 crafting recipes ([RecipeRegistry.java](resources/domain/crafting/RecipeRegistry.java));
carrot has no consumer yet.

---

## 2. Player starts with the farming kit

[Playable.java:51-61](resources/domain/player/Playable.java#L51-L61) gives every new player:
`hoe ×1`, `watering_can ×1`, `seeds_wheat ×16`, `seeds_carrot ×16` (plus general tools).

Input: there are **no dedicated farming keys**. Everything goes through the generic interact paths:
- **SPACE** → `Playable.interact()` → calls `FarmingService.tryHoeTile` then `tryPlantOnFarmland`.
- **Left mouse** → placement pipeline → for seeds, the `PLANT_SEED` action also calls
  `FarmingService.tryPlantOnFarmland`.

---

## 3. ✅ FIXED (2026-05-29) — seed→crop mapping

> This blocker is now resolved. `FarmingService.cropKeyFor()` maps `seeds_<name>` →
> `crop_<name>` (and accepts direct `crop_*`), validated against `CropRegistry`. Both the SPACE
> path and the mouse `PLANT_SEED` path use it, and `Farmland.interact()` delegates to it. Original
> bug description kept below for context.

### Original bug — seeds can't be planted as shipped

`FarmingService.tryPlantOnFarmland` only accepts an equipped item whose name **starts with
`crop_`** ([FarmingService.java:24,34](resources/domain/farming/FarmingService.java#L24)):

```java
private static final String SEED_PREFIX = "crop_";
...
if (itemName == null || !itemName.startsWith(SEED_PREFIX)) return false;
```

But the seeds in the player's inventory are named **`seeds_wheat`** / **`seeds_carrot`**
([Playable.java:60-61](resources/domain/player/Playable.java#L60-L61),
[ItemTypeRegistry.java](resources/domain/inventory/ItemTypeRegistry.java)). The planted-crop key
is `crop_wheat`. Nothing translates `seeds_wheat → crop_wheat` before the prefix check — so the
check fails and **no crop is ever planted**, on both the SPACE path and the mouse `PLANT_SEED`
path.

The farming test only passes because it side-steps this by equipping an item literally named
`crop_wheat` ([FarmingProbe.java:36-39](resources/testing/probes/FarmingProbe.java#L36-L39)).

**Pick one fix:**

- **(A) Map seed → crop in the service (recommended).** In `tryPlantOnFarmland`, translate the
  equipped name before planting, e.g. strip a `seeds_` prefix to `crop_`, or look the crop key up
  from `ItemTypeRegistry`'s "placed name" field (which is already `crop_wheat` / `crop_carrot`).
  Then call `fl.plant(cropKey)`. Apply the same mapping inside
  [Farmland.interact()](resources/domain/farming/Farmland.java#L73) (it has the identical
  `crop_` prefix assumption).
- **(B) Rename the inventory seeds to `crop_wheat` / `crop_carrot`** in `Playable` and
  `ItemTypeRegistry`. Quickest, but loses the nice "seeds_" vs "crop_" item/entity distinction.

---

## 4. What's stubbed / missing (non-blocking)

- **Watering does nothing.** `Farmland.watered` is set and swaps the sprite, but
  `GrowableComponent` never reads it — growth rate is identical wet or dry.
- **`farmland_watered` sprite is missing** → watered tiles show the `?` placeholder.
- **Carrot has no use** — harvestable but no recipe/consumer.
- **All crops hardcoded**: 4 stages, 600 ticks/stage, durability 1, drop 1–3. No biome/season
  modifiers, soil depletion, disease, or multi-tile plots.
- **No seed economy** — seeds only come from the starter inventory; harvesting yields produce, not
  more seeds.

---

## 5. Assets — what exists vs. what you need to generate

Image loading is fault-tolerant: a missing sprite resolves to a grey `?` tile
([ObjectImageLoader.java:80,151-169](resources/presentation/image/ObjectImageLoader.java#L151-L169)),
so the game **won't crash** — farming will just look like `?` markers. To actually see it, generate:

| Sprite name | Type | Path convention | Status |
|---|---|---|---|
| `farmland` | object | `resources/images/objects/.../soil/farmland/farmland.png` | ✅ exists |
| `farmland_watered` | object | a `farmland_watered/farmland_watered.png` folder under `objects/` | ❌ missing |
| `crop_wheat_stage0..3` | object | one folder each, e.g. `crop_wheat_stage0/crop_wheat_stage0.png` | ❌ missing (4 PNGs) |
| `crop_carrot_stage0..3` | object | same convention | ❌ missing (4 PNGs) |
| `hoe` | item | `resources/images/items/hoe.png` | ⚠️ falls back to `hammer` icon |
| `watering_can` | item | `resources/images/items/watering_can.png` | ❌ missing (`?`) |
| `seeds_wheat`, `seeds_carrot` | item | `items/<name>.png` | ❌ missing — but they alias to `crop_*_stage0` object art, so they'll show once crop sprites exist ([ItemManager.java:81-82](resources/domain/inventory/ItemManager.java#L81-L82)) |
| `wheat`, `carrot` | item | `items/<name>.png` | ❌ missing (`?`) |

**Asset conventions** (from [ObjectImageLoader.java](resources/presentation/image/ObjectImageLoader.java)):
- **Objects**: each name maps to a folder `<name>/` _anywhere_ under
  `resources/images/objects/` containing one `<name>.png`. Folder depth is irrelevant — the loader
  indexes the whole tree by folder name. Tile size is 64×64.
- **Items**: a flat `resources/images/items/<name>.png`.

So the minimum art to make farming look real: **9 object PNGs** (8 crop stages + 1 watered farmland)
plus, optionally, item icons for `watering_can`, `wheat`, `carrot`.

---

## 6. How to run it

```
# from gameDev2D/
javac -d out $(find resources -name "*.java")
java -cp out resources.app.Main
```

Entry point: [Main.java](resources/app/Main.java) → `IntroLauncher.launch()`.

**To try farming once §3 is fixed:** spawn in (you already have hoe + seeds), stand on a
plains/forest/savanna tile, press **SPACE** with the hoe equipped to till, equip seeds and **SPACE**
(or click the farmland) to plant, wait ~2400 ticks, then left-click the mature crop to harvest.
Until art is added, the crop appears as a `?` that changes shape per stage only if you've added the
stage sprites.

---

## 7. Suggested order of work

1. **Fix the seed→crop mapping (§3)** — this is the one thing that makes farming playable.
2. Generate the **9 object sprites** (`crop_wheat_stage0..3`, `crop_carrot_stage0..3`,
   `farmland_watered`) so growth is visible.
3. (Optional) item icons for `watering_can`, `wheat`, `carrot`.
4. (Optional) make watering matter — have `GrowableComponent` read `Farmland.isWatered()` to speed
   growth, and give carrot a recipe.
