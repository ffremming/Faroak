# Completing the Multiplayer System — Design

Date: 2026-05-31
Status: Approved

## Goal

Make the game fully playable online with other players, deployed as a **dedicated server**
(`ServerMain`) that multiple game clients connect to. The networking foundation already exists
and works (transport, protocol, replication, authority, persistence framework, and a wired
Host/Join UI). This spec covers finishing the **gameplay-completeness gaps** and hardening the
**dedicated-server experience**.

## Current state (verified)

- Compiles cleanly (407 source files, Java 17, no external jars).
- All 15 backend probes pass, including a real WebSocket gateway and 2-client movement
  replication over actual sockets.
- `IntroScreen` → `LobbyScreen` provides a working Host Game / Join Game UI that sets system
  properties and launches the game.
- **Works today:** transport (loopback + real WebSocket), protocol codec, player position
  replication, world building (place/remove), farming (till/plant/water/harvest), containers
  (chest/barrel, shared multi-player access), crafting tables, inventory slot-clicks
  (move/split/craft — server-authoritative), boat riding, persistence *framework*.
- The dedicated server (`ServerMain`) binds a real socket on all interfaces (`0.0.0.0`),
  accepts clients, and shuts down cleanly.

## Confirmed gaps (the work)

Ranked by player-visible impact. Each is implemented with the same server-authoritative
pattern: **server simulates → snapshot carries the field → client reflects it.**

### Architecture principle

Stay server-authoritative. Extend wire payloads in a **backward-compatible** way: append new
fields to existing payload encoders/decoders so a decoder tolerates older/newer frames where
practical, avoiding a protocol-version bump. The proven transport/envelope layer is untouched.

Relevant wire types:
- `message/PlayerStateMessage.java` — per-player snapshot row (client-facing).
- `protocol/ProtocolPayloads.java` `PlayerState` — wire payload row.
- `protocol/ProtocolPayloadCodec.java` — binary encode/decode of snapshots/payloads.
- `server/Session.java` `toPayloadState()` — server → wire.
- `MultiplayerRuntime` / `RemotePlayerDirectory` / `RemotePlayerAvatar` — wire → client render.
- `core/time/GameClock.java` — `ticks()` / `advance(delta)`, the day/night source of truth.

---

### A. Remote player visuals

**Problem:** Remote players render as a hardcoded `"red"` sprite (`RemotePlayerAvatar.java:18`),
always facing down (directionIndex stays 2), with no walk animation, no name tag, and no health
bar — even though health already arrives in the payload and is dropped on remotes.

**Approach:**
- Add fields to `PlayerState` / `PlayerStateMessage`: `facing` (int 0–3), `moving` (bool),
  `spriteName` (String), `displayName` (String). Backward-compatible append in the codec.
- Server: `Session` tracks `spriteName`/`displayName` (from join request) and derives
  `facing`/`moving` from velocity each tick (or accepts client-reported facing in the input
  message — chosen: derive from velocity server-side for authority; cheaper and good enough).
- Client `RemotePlayerAvatar`: set `directionIndex` from `facing`; advance the existing
  `Moveable` walk animation when `moving`; render a name tag and a health bar above the sprite
  using the already-present `health`/`maxHealth`.
- Join carries the chosen player name (already in `LobbyScreen` as `playerId` base) and a
  sprite name (default `"red"`, but real now and animated).

**Done when:** in a 2-client run each player sees the other move with correct facing + walk
animation, a name tag, and a live health bar.

### B. Server-side world population (mobs / objects / animals)

**Problem:** The server's world starts empty. Trees/rocks/ore the client draws come from the
client's *local* generation (same seed) and are **not** authoritative — they can't be harvested
or fought on the shared server. No mobs/animals ever spawn server-side; the existing `tickMobs`
AI is dead code with nothing to drive.

**Approach:**
- New `server/world/ServerWorldPopulator`: at fresh world creation (when persistence restored
  nothing), deterministically seed authoritative **harvestable objects** (trees, rocks, ore)
  using the same seed the client uses (`424242L` / `-Dgame.world.seed`) so positions line up
  with what clients render — but now backed by real `EntityState` the server tracks.
- New `server/world/ServerMobSpawner`: on the server tick, maintain a capped mob/animal
  population near active players (uses existing mob stat tables and `tickMobs` AI). Honors a
  configurable cap and spawn radius; despawns when no players are nearby.
- **Scope:** port only object/mob kinds the server simulation already understands (trees,
  rocks, ore, `goblin`/`spider`/`deer`). NPC ships (ShipKind) are **out of scope** for this
  pass — boats remain rideable placed objects; AI-driven NPC ships stay singleplayer-only. (Cut
  to keep the item bounded; can be a follow-up.)
- Seeded objects/mobs persist through the normal world snapshot persistence (item D).

**Done when:** a client connecting to a fresh dedicated server sees authoritative trees/rocks it
can actually harvest, and mobs that wander, chase, and can be killed — visible to all clients.

### C. Death / respawn / PvP

**Problem:** At 0 HP a player freezes at 0 forever (`damageSession` floors at 0), is never
respawned or healed, and its body keeps moving. Players aren't in the damageable target set, so
PvP is impossible.

**Approach:**
- Server death state machine on `Session`: state `ALIVE` / `DEAD`. On reaching 0 HP → `DEAD`:
  stop integrating input, broadcast death (new presence/snapshot flag), start a respawn timer.
  On timer expiry **or** a client `RESPAWN` command → relocate to a valid spawn point, restore
  full health, state `ALIVE`.
- Make players damageable: include `PlayerReplicaState` sessions in the melee/projectile target
  scan, gated by a `pvp` flag (`-Dgame.multiplayer.pvp`, default **on** for dedicated server).
- Carry `alive`/`dead` in the player snapshot row so clients can render a death state.
- Client: death overlay + "Respawn" prompt; sends `RESPAWN` command. Remote dead players render
  as downed/hidden rather than walking.

**Done when:** a player reduced to 0 HP dies, can respawn, and (with pvp on) players can damage
each other; all clients see consistent death/respawn.

### D. Durable dedicated-server persistence

**Problem:** `SqlitePersistenceStore` is correct but dead — there's no SQLite JDBC jar, so
`PersistenceStoreFactory` silently falls back to `InMemoryPersistenceStore`. The dedicated
server loses **all** state (players, placed objects, world) on every restart, with no warning.

**Approach:**
- New `server/persistence/JsonFilePersistenceStore` — pure-JDK, file-backed durable store
  (writes player rows + world-chunk snapshots to a directory of files; no external deps).
- Make it the **default** durable store in `PersistenceStoreFactory` (replacing the silent
  in-memory fallback). SQLite remains selectable if a driver is present;
  `-Dgame.multiplayer.persistence=memory|file|sqlite` chooses explicitly.
- `ServerMain`: print a clear `gateway listening on :PORT` line on successful bind, and log the
  chosen persistence backend + data path.

**Done when:** a dedicated server saves on exit and reloads players + placed/seeded world objects
on restart, verified by a probe that stops and restarts the store.

### E. Chat + player list

**Problem:** No chat; `ServerPlayerPresenceMessage` exists but the client ignores it
(`MultiplayerRuntime.java:320`). No roster, no join/leave notices.

**Approach:**
- New `ClientChatMessage` (client → server text) and `ServerChatMessage` (server → all,
  with sender name + system flag). Server relays and emits system lines on join/leave/death.
- Client: a minimal chat box (Enter opens input, Enter sends, Esc cancels) and a recent-messages
  overlay; a player-list overlay (Tab) built from the now-consumed presence roster.

**Done when:** players can type messages that all others see, see join/leave/death system lines,
and view a live roster.

### F. Synced day/night & world time

**Problem:** Each client runs its own `GameClock`, so time-of-day/lighting differs between
players; time-gated logic is unsynced.

**Approach:**
- Server owns the clock (its tick advances `GameClock`); add `worldTimeTicks` to the snapshot
  (header-level field alongside the existing server tick).
- Client applies `worldTimeTicks` to its `GameClock` (set/advance toward server) instead of
  free-running, so day/night lighting matches across clients.

**Done when:** all clients show the same time-of-day/lighting, driven by the server clock.

### G. Client-side movement prediction & replay

**Problem:** The client never integrates its own input; it lerps toward server samples and warps
at 96px (`applyLocalAuthoritativePose`), causing rubber-banding under latency. `pendingInputs` is
tracked but never replayed.

**Approach:**
- Predict locally: integrate the local player from input immediately each frame using the same
  movement model as the server (`serverMoveSpeedPerTick`).
- On each authoritative snapshot: snap the local player to the server pose **at the acked
  sequence**, then **replay** the still-unacked buffered inputs to recompute the present pose.
- Keep a small smoothing/threshold so tiny corrections don't jitter; retain the warp as a
  last-resort for large divergence. This is the most tuning-sensitive item and is sequenced last.

**Done when:** local movement feels immediate (no full-RTT delay) and reconciles without visible
rubber-banding under simulated latency.

---

## Sequencing

Dependency order; each step is proven before the next:

1. **D — Persistence** (foundation for restart-durability).
2. **A — Remote visuals** (isolated, high impact, low risk).
3. **B — World population** (makes the world real; needed by C).
4. **C — Death / respawn / PvP** (needs things to fight from B).
5. **F — Day/night sync** and **E — Chat + player list** (additive).
6. **G — Movement prediction** (most tuning, least breaking-if-deferred).

## Testing & verification

- Add a headless probe per feature to `resources/testing/MultiplayerTestRunner.java` where the
  feature is server-observable (persistence round-trip, world population present, death→respawn,
  player-vs-player damage, world-time in snapshot, chat relay).
- Extend the two-client WebSocket E2E probe to assert remote facing/animation/name/health,
  shared mobs, and synced time.
- Keep all 15 existing probes green throughout (regression gate).
- **Final acceptance:** launch a real dedicated `ServerMain` + two game clients and verify
  together: both see each other animated with names/health, a shared populated world with
  killable mobs, death/respawn, chat + roster, synced day/night, and smooth local movement;
  then restart the server and confirm the world persisted.

## Non-goals (this pass)

- NPC ships (ShipKind) on the server — boats stay rideable placed objects only.
- True mouse-drag inventory UX and drop-item-to-world / ground pickups.
- TLS/encryption, accounts/auth, anti-cheat beyond existing authority checks.
- Lag compensation / server-side rewind beyond client predict-replay.
