# Automatic cave & house entrances/exits

## Goal

Make all dimension transitions (cave entrance, house entrance, and their
exits) trigger automatically when the player walks onto a portal, instead of
requiring a spacebar interaction. Additionally, guarantee a cave return portal
is placed right next to where the player arrives in the cave.

## Background

Today, portals are non-solid `GameObject`s (`solid=false`) that the player
walks over freely. Transition only happens when the player presses spacebar,
which calls `Playable.interact()`, finds the nearest interactable `GameObject`
whose hitbox overlaps the interaction reach box, and calls its `interact()`.
`Portal.interact()` publishes a `DimensionChangeEvent`; `DimensionService`
performs the world swap and player reposition.

Cave exits are generated procedurally and sparsely (`PORTAL_DENSITY = 0.00010`)
at random floor cells outside the spawn clearing, so the player can land in a
cave with no nearby way back.

Relevant files:
- `resources/domain/player/Playable.java` — `update()` (per-frame hook),
  `interact()` (spacebar path).
- `resources/domain/object/Portal.java` — `interact()` fires the event.
- `resources/world/DimensionService.java` — performs the swap.
- `resources/generation/cave/CaveGenerator.java` — emits cave exit portals.
- `resources/generation/interior/InteriorManager.java` — emits house exit
  portals (unchanged by this work).
- `resources/app/GenerationManager.java` — `seedWorldEntities()` places the
  overworld cave/house portals (unchanged by this work).

## Design

### 1. Walk-over auto-trigger in `Playable.update()`

After `super.update()` (movement applied for this frame), check whether the
player's body hitbox overlaps any `Portal`. If so, fire that portal's
transition via its existing `interact(this)` method — reusing the single
existing event path. No new event type.

Query: `panel.world().getEntitiesCollidedWith(getHitBox())`, filter to
`Portal` instances. If one is found and the step-off latch is armed, call
`portal.interact(this)`.

Because `interact()` can swap the world (replacing the entity list), the check
must tolerate that: stop after firing the first portal.

### 2. Step-off latch (re-trigger guard)

Add a `boolean steppedOffPortal` field to the player, initially `true`.

- When an auto-transition fires, set `steppedOffPortal = false`.
- Each frame, if the player currently overlaps **no** portal, set
  `steppedOffPortal = true` (re-arm).
- Only fire a transition when `steppedOffPortal` is `true`.

Because both sides of every portal pair place the arrival point one tile away
from the destination's exit portal, the player arrives *not* overlapping a
portal, so the latch re-arms immediately and a single deliberate step onto the
return portal sends them back. This prevents instant ping-ponging.

### 3. Guaranteed cave exit at arrival

In `CaveGenerator`, replace the sparse random exit portals with a single
deterministic exit portal at a fixed tile adjacent to the `(0,0)` arrival:
tile `(1, 0)` = world pixel `(64, 0)`. This cell is inside the always-floor
spawn clearing (`SPAWN_CLEARING_TILES = 4`), so it is guaranteed reachable and
visible the moment the player arrives.

- Remove `PORTAL_DENSITY` and the random roll in `isPortalCell`.
- `isPortalCell(x, y)` returns true only for the fixed exit tile.

The cave will then have exactly one obvious exit, right where the player lands.

### 4. Houses unchanged

Interior doors already emit a return portal at the door cell, and the overworld
house arrival is one tile north of the door tile. With the walk-over hook,
both directions become automatic with no interior-specific changes.

### Spacebar

Spacebar `interact()` stays as-is for chests, crafting tables, and barrels.
Portals remain technically interactable by spacebar (harmless) but the player
never needs it — they transition by walking on.

## Testing

Headless probes (matching existing test style):

1. **Walk-over enters:** place player overlapping a portal, run one
   `Playable.update()`, assert `DimensionService.currentDimension()` changed.
2. **Step-off latch blocks re-entry:** with player still overlapping a portal
   immediately after a transition, run another `update()`, assert no second
   transition.
3. **Latch re-arms:** move player off the portal, run `update()`, move back on,
   run `update()`, assert a transition fires.
4. **Cave exit at arrival:** query the cave generator/dimension for an entity
   at tile `(1, 0)` and assert it is a `Portal` back to the overworld.

## Out of scope

- Changing the overworld portal placement in `seedWorldEntities()`.
- Procedural cave layout other than the exit portal change.
- Any visual/animation transition effect.
