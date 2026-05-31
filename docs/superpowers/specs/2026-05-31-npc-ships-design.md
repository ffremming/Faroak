# NPC Ships — Design

**Date:** 2026-05-31
**Status:** Approved

## Goal

Add NPC ships that sail the water-based world with real purposes (fishing routes, long trade voyages, pirate hunts), an extensible architecture for many ship kinds, hostile ships that detect/chase/shoot the player using the existing broadside combat system, the ability for the player to attack any ship, and a large "galleon" ship the player can board to enter an interior scene.

## Existing systems we build on

- **`Boat`** (`resources/domain/object/Boat.java`) — `final`, `extends Moveable`. Water-only movement via `canEnter()` (all hitbox corners must be water tiles). While ridden, reads player input; while unmanned, runs an `AIComponent`. Hardcoded constants: 192×192 sprite, 144×144 hitbox, speed 4.0. 8-direction facing (`facingIndex`, `DIR_VECTORS`). Renders one directional sprite via `getImages()`.
- **`BoatCombatComponent`** — broadside firing (two cannonballs left+right of facing), HP via `HealthComponent`, cooldown, sink flow. Hardcoded 30 HP / 8 dmg / 24-tick cooldown / 15-tile range.
- **`BoatProjectile`** — straight-line cannonball, damages first non-shooter `Boat` it hits (so player↔NPC damage already works once NPC boats exist), expires off-water or past range. FX via `BoatCombatFx`.
- **`AIBehavior`** — `void tick(BaseEntity host, GameContext ctx)`. Strategy pattern, behaviors hold their own state. `BoatPatrolBehavior` is the existing water-drift example.
- **`AIComponent`** — hosts one `AIBehavior`, `setBehavior()` swaps at runtime.
- **`DimensionService` + `Portal` + `DimensionChangeEvent`** — scene transitions. Each dimension owns a `ChunkSystem`; `Portal.interact(player)` publishes a `DimensionChangeEvent(from, to, arrival)`. `InteriorBootstrap` registers an interior dimension backed by hand-authored `Interior` char-grid layouts in `InteriorRegistry`, placed into slots by `InteriorManager`.
- **`ClickRouter`** — ordered click-interaction chain; `boardBoat` is the first link (boards a clicked `Boat` in range).
- **`BoatSpawner`** — scatters boats on water near a point; called from `GenerationManager.seedWorldEntities()`.
- **Build/test:** `find resources -name '*.java' > /tmp/srcs.txt; javac -encoding UTF-8 -d /tmp/gamebuild @/tmp/srcs.txt`. Headless probes via `java -cp /tmp/gamebuild resources.testing.TestRunner`. Probes implement `Probe` and register in `TestRunner.main`.

## Architectural decisions

1. **Data-driven `ShipKind`** (not subclass-per-type). One `ShipKind` immutable definition object per type holds size, hitbox, speed, terrain table, sprite directory, max HP, weapon loadout, default goal factory, faction, and optional boardable interior target. A `ShipKindRegistry` holds all kinds. Adding a kind = one registry entry + sprites.

2. **Keep the class named `Boat`** rather than renaming to `Ship`. `BoatProjectile`, `BoatCombatComponent`, `ClickRouter`, `BoatSpawner` are all hard-typed to `Boat`; a rename is high-risk churn for no behavioral gain. Instead, make `Boat` **non-final** and give it an **optional `ShipKind`**. The current player boat becomes the default kind `PLAYER_SLOOP`. All existing constants move into that kind so current behavior is byte-for-byte preserved.

3. **Goal + hostile-override AI.** NPC ships run a single `ShipPilotBehavior` that internally runs a two-layer state machine: a `ShipGoal` (the purpose) underneath, and a hostile combat override on top. A shared `WaterNavigator` converts "reach point P" into one legal water step (head toward P; if blocked, fan headings ±45°/±90° to steer around coastline). No A* (deferred).

4. **Faction-driven hostility.** `Faction` enum (PIRATE, MERCHANT, FISHER, NAVY, NEUTRAL). A `FactionRelations` table decides who is hostile to whom and to the player. Neutral ships attacked by the player switch to a flee/retaliate reaction.

5. **Reuse `BoatCombatComponent.fireBroadside()`** for NPC fire — same projectiles, FX, damage — triggered by AI when a target is in range and roughly abeam.

6. **Boardable galleon → interior dimension.** Reuse `Interior` + `DimensionService`. A boardable `ShipKind` references an interior dimension id; clicking the ship (when in range) publishes a `DimensionChangeEvent` into that dimension. A door/portal inside returns the player to the overworld beside the ship.

## Components & data flow

```
ShipKind (data) ──► Boat (reads kind for size/speed/hp/sprites/loadout)
                      ├─ TerrainSpeedComponent (from kind table)
                      ├─ BoatCombatComponent (parameterized by kind loadout)
                      └─ AIComponent → ShipPilotBehavior (NPC only)
                                          ├─ ShipGoal (Sail/Fishing/Patrol/PirateHunt)
                                          │     └─ emits target waypoint
                                          ├─ WaterNavigator (waypoint → legal water step)
                                          └─ Hostile override:
                                               detect target (faction) → Pursue → Engage(fireBroadside) → resume goal
```

- **Targeting:** scan `world.getEntities()` for the player and other `Boat`s; filter by `FactionRelations.isHostile(myFaction, theirFaction)` and detection radius.
- **Attacking ships:** already works — `BoatProjectile` damages any non-shooter `Boat`. Player cannonballs sink NPCs; NPC cannonballs damage the player's boat (and its rider on sink, via existing `BoatCombatComponent.sink()`).
- **Boarding:** `ClickRouter` gets a new link (or `boardBoat` extends) — if the clicked `Boat`'s kind is boardable and the player is in range, publish the dimension change instead of riding.

## Error handling & edge cases

- Ship penned in by land on all sides: `WaterNavigator` returns no legal step; ship holds position (mirrors `BoatPatrolBehavior`).
- Target goes out of detection range or dies: hostile override releases, goal resumes from saved state.
- Boardable ship destroyed while player inside its interior: interior dimension persists (chunks survive); player exits via the interior door to the saved overworld arrival point. (The ship entity being gone is acceptable for v1 — exit returns to where the ship was.)
- Missing ship sprites: per-direction procedural placeholder (existing `BoatSprites` fallback generalized per kind).
- `ShipKind` with `WeaponLoadout.NONE` (fisher): `fireBroadside()` is never called by its goal; hostile override only engages for armed kinds.

## Testing strategy

Headless `Probe`s under `resources/testing/probes/`, registered in `TestRunner`:
- **`ShipKindProbe`** — construct a `Boat` for each registered kind, place on water, tick, assert no throw and kind stats applied (hp/size).
- **`ShipAIProbe`** — place a `SailRouteGoal` ship and a `FishingGoal` ship on water, tick N, assert they move toward waypoints / reach fishing spot.
- **`HostileShipProbe`** — place a pirate near the player's boat, tick, assert it closes distance and (when armed/in range) fires at least one projectile.
- **`ShipBoardingProbe`** — place a boardable galleon adjacent to the player, invoke the board interaction, assert a `DimensionChangeEvent` to the ship interior fires and `currentDimension()` changes.

Regression: existing `BoatProbe` must still pass unchanged (player boat = `PLAYER_SLOOP` kind).

## Scope (this implementation)

Full vision: `ShipKind` + faction system + goal-driven AI + hostile override + spawning + three concrete kinds end-to-end — **PIRATE_BRIG** (hostile hunt/shoot/attackable), **FISHER** (fishing route, flees if attacked), **GALLEON** (boardable interior), plus **PLAYER_SLOOP** (the migrated existing boat). Boardable galleon interior fully implemented.

Deferred: A* navigation, ship persistence/save, ship-vs-ship faction wars beyond player interactions, multiple interior layouts per galleon.
