# Complete Multiplayer System — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the game's online multiplayer so a dedicated server hosts a shared, persistent, living world that multiple clients play together — animated named players with health bars, server-spawned mobs and harvestable objects, death/respawn/PvP, durable save/load, chat + roster, synced day/night, and smooth predicted local movement.

**Architecture:** Server-authoritative. Each feature follows: server simulates → snapshot payload carries new fields (backward-compatible append in `ProtocolPayloadCodec`, using the existing `try/catch(IOException)` optional-trailing-section idiom) → client reflects it. No protocol-version bump. No new external dependencies (pure JDK).

**Tech Stack:** Java 17, no external jars, hand-rolled binary `DataOutputStream`/`DataInputStream` codec, Swing rendering, headless probe harness (`resources.testing.MultiplayerTestRunner`).

---

## Conventions used throughout

- **Build:** `find resources -name "*.java" > /tmp/sources.txt && javac -d /tmp/mpbuild @/tmp/sources.txt` from project root. Expected: exit 0, no `error:`.
- **Probe run:** `java -cp /tmp/mpbuild resources.testing.MultiplayerTestRunner` from project root. Expected tail: `N probe(s), 0 failure(s)`.
- **Regression gate:** every task ends with a full build + full probe run, all green. Never let an existing probe regress.
- **Commit cadence:** commit at the end of each task. Message style: `feat(mp): …` / `fix(mp): …`. Co-author trailer:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
- **New probes:** each feature adds a probe class under `resources/testing/probes/` named `Mp<Feature>Probe.java`, registered in `MultiplayerTestRunner.main()` alongside the existing 15. Probes are headless, drive the server via `AuthoritativeLobbyRuntime`/`AuthoritativeGameHost` directly (loopback), assert via `System.out` + a boolean, and follow the existing probe pattern (read an existing probe like `MultiplayerWorldReplicationProbe` first for the exact harness API).
- **TDD note:** in this codebase "test" = a headless probe. Where a true unit test fits (pure codec round-trip, persistence round-trip) write a small `mainprobe`-style assertion first, watch it fail, then implement.

---

## Feature D — Durable dedicated-server persistence

**Why first:** foundation; everything restart-durable depends on it, and it's isolated from gameplay.

### Task D1: File-backed persistence store

**Files:**
- Create: `resources/net/multiplayer/server/persistence/JsonFilePersistenceStore.java`
- Reference (read first, match interface exactly): `resources/net/multiplayer/server/persistence/PersistenceStore.java`, `InMemoryPersistenceStore.java`

- [ ] **Step 1 — Write the failing probe.** Create `resources/testing/probes/MpPersistenceFileProbe.java` that:
  1. creates a temp dir under `/tmp` (use `Files.createTempDirectory`),
  2. opens a `JsonFilePersistenceStore(dir)`, calls `savePlayer(new PersistedPlayer("alice", 100, 200, 1, 2, 7))`, `saveWorldChunk(42L, new byte[]{1,2,3})`, `putMeta("k","v")`, `checkpoint()`, `close()`,
  3. opens a **second** `JsonFilePersistenceStore(dir)` over the same dir,
  4. asserts `loadPlayer("alice")` returns x=100,y=200,seq=7; `loadWorldChunk(42L)` returns `{1,2,3}`; `getMeta("k","x")`==`"v"`; `listWorldChunkKeys()` contains 42L.
  Print `PASS MpPersistenceFileProbe` / `FAIL …`. Register it in `MultiplayerTestRunner`.
- [ ] **Step 2 — Run, verify it fails** (class doesn't exist / compile error). Build; expect failure.
- [ ] **Step 3 — Implement `JsonFilePersistenceStore`.** Implements `PersistenceStore`. Layout under the given root dir: `players/<playerId>.dat`, `chunks/<chunkKey>.bin`, `meta.properties`. Use simple JDK serialization: players as a small fixed text/`DataOutputStream` record (playerId, 4 doubles, long); chunks as raw bytes written to the `.bin` file; meta via `java.util.Properties.store/load`. Sanitize `playerId` for filenames (replace non `[A-Za-z0-9_.-]`). `loadPlayer` returns `Optional.empty()` if no file. `listWorldChunkKeys()` lists `chunks/*.bin` parsing the long. `checkpoint()` flushes (fsync optional — at minimum ensure files are written). `close()` = checkpoint. `appendSessionEvent` may append a line to `events.log` (cheap). Thread-safety: synchronize all methods (server calls under a single lock already, but be safe).
- [ ] **Step 4 — Run probe, verify PASS.** Build + run runner.
- [ ] **Step 5 — Commit.** `feat(mp): add pure-JDK file-backed persistence store`

### Task D2: Make file store the default; explicit backend selector

**Files:**
- Modify: `resources/net/multiplayer/server/persistence/PersistenceStoreFactory.java`

- [ ] **Step 1 — Update the failing expectation.** Extend `MpPersistenceFileProbe` (or add `MpPersistenceFactoryProbe`) asserting `PersistenceStoreFactory.createDefault(path)` returns a `JsonFilePersistenceStore` by default (not `InMemoryPersistenceStore`), and that `-Dgame.multiplayer.persistence=memory` returns in-memory.
- [ ] **Step 2 — Run, verify it fails.**
- [ ] **Step 3 — Implement.** Rewrite `createDefault` to read `game.multiplayer.persistence` (`file` default, `memory`, `sqlite`):
  - `memory` → `InMemoryPersistenceStore`.
  - `sqlite` → try `Class.forName("org.sqlite.JDBC")` then `SqlitePersistenceStore`; if absent and `game.multiplayer.sqlite.required=true` throw, else fall through to file.
  - `file` (default) → `JsonFilePersistenceStore` rooted at a data dir derived from `sqlitePath` (strip `.db`, use sibling dir, default `multiplayer-data/`). Read dir from `game.multiplayer.dataDir` if set.
  Keep the method signature `createDefault(String sqlitePath)`.
- [ ] **Step 4 — Run probes, verify PASS** (and all existing green, incl. `mp-persistence`).
- [ ] **Step 5 — Commit.** `feat(mp): default to durable file persistence with explicit backend selector`

### Task D3: ServerMain startup logging

**Files:**
- Modify: `resources/net/multiplayer/server/ServerMain.java`

- [ ] **Step 1 — Implement.** After a successful `gateway.start(gatewayPort())`, print
  `System.out.println("[ServerMain] gateway listening on :" + gatewayPort());`
  and after creating the store print `System.out.println("[ServerMain] persistence=" + store.getClass().getSimpleName());`. Also print `"[ServerMain] tickRate=" + cfg.serverTickRate() + " maxPlayers=" + cfg.maxPlayers()`.
- [ ] **Step 2 — Verify by launching.** From project root, background-run:
  `java -cp /tmp/mpbuild -Dgame.multiplayer.gatewayPort=7777 resources.net.multiplayer.server.ServerMain`
  Expected stdout includes `gateway listening on :7777` and `persistence=JsonFilePersistenceStore`. Kill it. Confirm a data dir was created.
- [ ] **Step 3 — Commit.** `feat(mp): log gateway bind + persistence backend on server start`

---

## Feature A — Remote player visuals

**Why:** isolated, highest "feels alive" impact, no gameplay dependency.

### Task A1: Add appearance fields to the player wire payload

**Files:**
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloads.java` (PlayerState ~line 175)
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloadCodec.java` (encodeSnapshot ~201, decodeSnapshot ~242)
- Modify: `resources/net/multiplayer/message/PlayerStateMessage.java`
- Modify: `resources/net/multiplayer/ProtocolMessageTranslator.java` (PlayerState ↔ PlayerStateMessage mapping)

- [ ] **Step 1 — Failing probe.** Add `MpAppearanceCodecProbe`: build a `Snapshot` with one `PlayerState` carrying facing=1, moving=true, spriteName="red", displayName="Alice"; `encodeSnapshot` then `decodeSnapshot`; assert the four new fields survive round-trip AND that an **old-format** payload (player row without the trailing appearance block) still decodes with defaults (facing=0, moving=false, spriteName="red", displayName=""). Register in runner.
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.**
  - `PlayerState`: add `public final int facing; public final boolean moving; public final String spriteName; public final String displayName;` Add a new constructor overload taking them; keep existing constructors delegating with defaults (facing=0, moving=false, spriteName="red", displayName="").
  - Codec encode: **after** the existing per-player `maxHealth` write, append a per-player appearance block: `out.writeInt(facing); out.writeBoolean(moving); BinaryEnvelopeCodec.writeString(out, spriteName); BinaryEnvelopeCodec.writeString(out, displayName);` — but to keep backward-compat with the player-row loop (which has no per-row optional marker), instead append a **single trailing appearance section** AFTER tile mutations: write player count again then for each player the 4 fields, wrapped so `decodeSnapshot` reads it inside a `try { … } catch (IOException ignored) {}` (same idiom as worldObjects). Decode: after reading tileMutations, attempt to read the appearance section; if present, zip it back onto the already-built `PlayerState` list by index (rebuild list with appearance applied); if absent, leave defaults.
  - `PlayerStateMessage`: add the four fields + getters + a constructor overload; keep existing constructors delegating with defaults.
  - `ProtocolMessageTranslator`: carry the four fields in both directions.
- [ ] **Step 4 — Run probe + full runner, verify PASS** (existing snapshot probes must stay green — proves back-compat).
- [ ] **Step 5 — Commit.** `feat(mp): carry remote player facing/moving/sprite/name in snapshots`

### Task A2: Server populates appearance

**Files:**
- Modify: `resources/net/multiplayer/server/Session.java`
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (where Session is created from join, and `toPayloadState` callsite / integration)
- Reference: `resources/net/multiplayer/protocol/ProtocolPayloads.java` JoinRequest (does it carry a name? if not, derive from playerId)

- [ ] **Step 1 — Failing probe.** Add `MpAppearanceServerProbe`: drive lobby, join a player who moves right (velocity +x), tick, capture the outbound snapshot for an observer, assert that player's row has `moving==true` and `facing==` the right-facing index, and `displayName` non-empty (derived from playerId base before `-`).
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** In `Session` add `String spriteName="red"; String displayName;` set `displayName` from the playerId base (strip the `-<suffix>` the LobbyScreen adds) at construction. Each integration tick, derive `facing` from `(vx,vy)` (0=down,1=left? — **match `Moveable.updateDirectionIndex` mapping exactly**; read that method first) and `moving = (vx*vx+vy*vy) > epsilon`. Update `toPayloadState()` to pass facing/moving/spriteName/displayName.
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): derive and replicate remote player facing/moving/name on server`

### Task A3: Client renders facing, animation, name tag, health bar

**Files:**
- Modify: `resources/net/multiplayer/RemotePlayerAvatar.java`
- Modify: `resources/net/multiplayer/RemotePlayerDirectory.java` (pass appearance into avatar; spawn with spriteName)
- Reference: `resources/domain/player/Moveable.java` (`updateDirectionIndex`, `advanceAnimation`, `directionIndex`, draw), and how Player draws its sprite + any existing health-bar/label drawing in the codebase to match style.

- [ ] **Step 1 — Implement (visual; verify in real run, not probe).**
  - `RemotePlayerAvatar`: store latest `facing`, `moving`, `health`, `maxHealth`, `displayName`. In `pushSnapshot` read them from the now-richer `PlayerStateMessage`. In `advanceInterpolation` (or `update`) set `directionIndex` from `facing` and call the existing walk-animation advance when `moving`. Construct with the real `spriteName` (fall back "red").
  - Add a `drawOverlay(Graphics2D, camera)` or hook into the existing entity draw path to render a name tag (small centered string above the sprite) and a health bar (red/green bar) using `health`/`maxHealth`. Match the offline player's health-bar style if one exists; otherwise a simple 2-color rect.
  - `RemotePlayerDirectory.spawn`: pass `state.spriteName()` to the avatar constructor.
- [ ] **Step 2 — Verify in a real 2-client run** (see Final Verification harness). Expect: remote player faces the way it moves, animates while walking, shows name + live health bar.
- [ ] **Step 3 — Commit.** `feat(mp): render remote players with facing, walk animation, name tag, health bar`

---

## Feature B — Server-side world population

**Why:** makes the shared world real; prerequisite for C (things to fight). Largest item.

### Task B1: Authoritative harvestable object seeding

**Files:**
- Create: `resources/net/multiplayer/server/world/ServerWorldPopulator.java`
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (call populator on fresh world; integrate with persistence restore so seeding only happens when nothing restored)
- Reference: `resources/net/multiplayer/server/ServerTerrainRules.java` (seed `424242L`, `biomeAt`, `isWaterAt`), how `applyPlaceType` creates an `EntityState` (object type strings), `WorldState` add/bump API.

- [ ] **Step 1 — Failing probe.** `MpWorldPopulationProbe`: build a fresh lobby/host with an empty store, run startup, assert `world.entities()` contains >0 objects of harvestable types (e.g. "tree","rock") placed only on valid land tiles (not water), and that the SAME seed produces the SAME object set across two fresh hosts (determinism).
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** `ServerWorldPopulator.populate(WorldState world, ServerTerrainRules terrain, long seed, int radiusTiles)`: deterministic `java.util.Random(seed)`, scatter trees/rocks/ore across the spawn-region tiles, skipping water/solid via `terrain`. Create `EntityState` with the object-type strings the client already renders and the server already treats as harvestable (verify by checking `applyDamageToEntity`/harvest paths — choose types that drop loot). Call from `AuthoritativeGameHost` constructor/init only when `restoreFromSnapshot` restored nothing (guard with a meta flag `world.populated` in persistence so a restarted server doesn't double-seed).
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): seed authoritative harvestable world objects on fresh server`

### Task B2: Server mob spawner + wiring the existing mob AI

**Files:**
- Create: `resources/net/multiplayer/server/world/ServerMobSpawner.java`
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (call spawner each tick; ensure `tickMobs` runs on spawned mobs)
- Reference: `AuthoritativeGameHost.tickMobs` (~541), mob stat tables (~1456), `nearestSession`.

- [ ] **Step 1 — Failing probe.** `MpMobSpawnProbe`: lobby with one joined player at a location; run ~N ticks; assert at least one mob entity (type goblin/spider/deer) exists within spawn radius, that it moves over time (position changes), and that when no players are near, population trends to 0 (despawn). Also assert a player melee command against a mob reduces its health and eventually removes it (reuse combat path).
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** `ServerMobSpawner.tick(world, sessions, tick, rng)`: maintain mob count near active players up to a cap (`game.multiplayer.mobCap`, default e.g. 12), spawn on valid land within a ring around a random active player, despawn mobs with no player within a despawn radius. Spawn `EntityState` mobs with the component fields `tickMobs` expects (health, attack_cooldown, type). Wire `spawner.tick(...)` into the server tick before/after `tickMobs`.
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): spawn and maintain server-authoritative mobs near players`

---

## Feature C — Death / respawn / PvP

### Task C1: Player death + respawn state machine

**Files:**
- Modify: `resources/net/multiplayer/server/Session.java` (add `alive`, `respawnAtTick`)
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (`damageSession`, integration skip when dead, respawn logic)
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloads.java` + codec + `PlayerStateMessage` + translator (add `alive` to player row — append to the appearance trailing section from A1)
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloads.java` CommandRequest types / `MultiplayerAction` for a `RESPAWN` command (read existing command enum first)

- [ ] **Step 1 — Failing probe.** `MpDeathRespawnProbe`: damage a session to 0 → assert `alive==false`, input no longer integrates (position frozen), snapshot row `alive==false`; advance past respawn timer (or send RESPAWN command) → assert `alive==true`, `health==maxHealth`, relocated to a valid spawn.
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** `damageSession`: on reaching 0 set `alive=false`, `respawnAtTick = tick + respawnDelayTicks` (config `game.multiplayer.respawnSeconds`, default 5 → ticks via serverTickRate). Integration loop: skip movement/input for dead sessions. Each tick: if `!alive && (tick>=respawnAtTick || respawnRequested)` → respawn (full health, valid spawn point via existing `nearestLand`). Add `alive` to the player payload appearance section + message + translator. Add a `RESPAWN` action/command handled in the command dispatch that sets `respawnRequested`.
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): server player death state machine with timed/triggered respawn`

### Task C2: PvP — players damageable

**Files:**
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (melee/projectile target scan to include sessions, gated by pvp flag)

- [ ] **Step 1 — Failing probe.** `MpPvpProbe`: two joined players adjacent, pvp on; player A issues a melee command toward B; assert B's health drops; with `pvp=false` assert no drop.
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** In the melee/projectile resolution, when `game.multiplayer.pvp` (default `true`) is on, also scan `sessions` for targets in arc/range (excluding the attacker and dead sessions) and route hits through `damageSession`. Respect the existing action range/authority checks.
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): enable player-vs-player melee/projectile damage behind pvp flag`

### Task C3: Client death overlay + respawn + remote dead rendering

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerRuntime.java` (detect local `alive==false`, show overlay, send RESPAWN on input)
- Modify: `resources/net/multiplayer/RemotePlayerAvatar.java` (render dead remotes as downed/hidden)
- Reference: existing overlay/UI patterns (e.g. `EscapeMenu`) for a simple centered overlay.

- [ ] **Step 1 — Implement (verify in real run).** Local: when the local player's snapshot row is `alive==false`, freeze local input, draw a "You died — press R to respawn" overlay, and on R send the RESPAWN command. Remote: when an avatar's last state is dead, render it downed or hide it + skip health bar.
- [ ] **Step 2 — Verify in 2-client run:** kill one player (mob or PvP), see death overlay, respawn with R, other client sees them die and reappear.
- [ ] **Step 3 — Commit.** `feat(mp): client death overlay, respawn input, dead remote rendering`

---

## Feature F — Synced day/night & world time

### Task F1: World time in snapshot header

**Files:**
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloads.java` (Snapshot: add `worldTimeTicks`)
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloadCodec.java` (encode/decode — append in a trailing try/catch section after appearance, default 0)
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (own a `GameClock`, advance per tick, include `clock.ticks()` in snapshots)
- Modify: `resources/net/multiplayer/ProtocolMessageTranslator.java` + `ServerSnapshotMessage` (carry worldTimeTicks)
- Reference: `resources/core/time/GameClock.java` (`ticks()`, `advance(delta)`, `ticksPerDay`)

- [ ] **Step 1 — Failing probe.** `MpWorldTimeProbe`: tick the server M times; assert outbound snapshot `worldTimeTicks` advances and equals server clock; assert old-format snapshot decodes to `worldTimeTicks==0`.
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** Server holds a `GameClock`; advance one tick per server tick; put `clock.ticks()` into the snapshot (new field, encoded in a trailing optional section). Translate through to `ServerSnapshotMessage`.
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): server owns and replicates world time in snapshots`

### Task F2: Client applies server clock

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerRuntime.java` (on snapshot, drive the client `GameClock` toward `worldTimeTicks`)
- Reference: how `GamePanel`/lighting obtains its `GameClock`; set/advance it when online.

- [ ] **Step 1 — Implement (verify in real run).** When online and a snapshot carries `worldTimeTicks>0`, set the client clock to the server value (or smoothly advance toward it to avoid lighting jumps). When offline, unchanged.
- [ ] **Step 2 — Verify in 2-client run:** both clients show the same time-of-day/lighting; advancing server time changes both.
- [ ] **Step 3 — Commit.** `feat(mp): client day/night driven by server world time`

---

## Feature E — Chat + player list

### Task E1: Chat protocol + server relay

**Files:**
- Create: `resources/net/multiplayer/message/ClientChatMessage.java`, `resources/net/multiplayer/message/ServerChatMessage.java`
- Modify: `resources/net/multiplayer/protocol/ProtocolMessageType.java` (add CHAT message types)
- Modify: `resources/net/multiplayer/protocol/ProtocolPayloads.java` + `ProtocolPayloadCodec.java` (chat payload encode/decode)
- Modify: `resources/net/multiplayer/server/AuthoritativeLobbyRuntime.java` (receive client chat → broadcast server chat to all; emit system chat on join/leave/death)
- Modify: `resources/net/multiplayer/ProtocolMessageTranslator.java`

- [ ] **Step 1 — Failing probe.** `MpChatProbe`: two joined players; A sends chat "hi"; assert B's outbound queue contains a ServerChat with sender=A.display, text="hi"; assert join produces a system chat line.
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement** the message types, codec, type enum entries, and lobby relay (broadcast to all sessions; system lines for join/leave; reuse death hook from C1).
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): chat protocol with server relay and system join/leave lines`

### Task E2: Client chat box + roster overlay

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerRuntime.java` (consume `ServerPlayerPresenceMessage` into a roster — replace the ignore at ~line 320; consume ServerChat into a message log; send ClientChat)
- Create: `resources/presentation/ui/ChatOverlay.java`, `resources/presentation/ui/PlayerListOverlay.java`
- Reference: input routing (how keys reach the runtime), existing overlay draw + the overlay-registration pattern from MEMORY (container UIs register as overlays; Escape handling).

- [ ] **Step 1 — Implement (verify in real run).** Chat: Enter opens an input line, typing buffers, Enter sends a ClientChat + closes, Esc cancels; recent messages render bottom-left for a few seconds. Roster: hold/Toggle Tab to show the connected-player list from the presence roster. Register overlays per the existing overlay convention so input routes correctly.
- [ ] **Step 2 — Verify in 2-client run:** type in one client, see it in the other; Tab shows both players; join/leave/death system lines appear.
- [ ] **Step 3 — Commit.** `feat(mp): in-game chat box and player-list overlay`

---

## Feature G — Client-side movement prediction & replay

**Why last:** most tuning-sensitive, least breaking if deferred.

### Task G1: Predict-and-replay local movement

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerRuntime.java` (`publishMovement`, `pendingInputs`, `applyLocalAuthoritativePose`, snapshot apply)
- Reference: server movement model (`serverMoveSpeedPerTick`, how `integratePlayers` applies input) so client integration matches the server exactly.

- [ ] **Step 1 — Failing probe (logic-level).** `MpPredictionProbe`: simulate the client reconciliation in isolation — given a server pose at acked seq S and a buffer of unacked inputs S+1..S+k, assert the replay reproduces the same final pose the server would compute for those inputs (within epsilon), and that a matching server pose produces zero correction (no warp/jitter).
- [ ] **Step 2 — Run, verify fail.**
- [ ] **Step 3 — Implement.** Publish an input record per frame (or per input change with timestamps) into `pendingInputs` with its sequence. Each frame, integrate the local player locally from current input using the server movement model (immediate response). On snapshot: drop acked inputs (seq ≤ processedSequence), snap local player to the server pose at the ack, then **replay** the remaining unacked inputs to recompute the present pose. Keep a small dead-zone so sub-pixel corrections don't jitter; retain the large-divergence warp as a fallback only.
- [ ] **Step 4 — Run probe + runner, verify PASS.**
- [ ] **Step 5 — Commit.** `feat(mp): client-side movement prediction with input replay reconciliation`

### Task G2: Tune & verify feel under latency

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerRuntime.java` (constants/dead-zone if needed)

- [ ] **Step 1 — Verify in real run** with the loopback/local path and, if feasible, an artificial latency (sleep in the loopback transport or run client against a remote-ish server). Confirm: own movement is immediate, no rubber-banding on straight runs, corrections are smooth on divergence.
- [ ] **Step 2 — Commit** any tuning. `fix(mp): tune local prediction dead-zone and reconciliation smoothing`

---

## Final Verification (Task Z)

**Files:** none (acceptance run). Reference: `resources/testing/MultiplayerGuiE2ERunner.java` for the two-process harness; extend it if cheap, otherwise do a manual scripted run.

- [ ] **Step 1 — Full regression.** Build; run `MultiplayerTestRunner`; expect all probes (original 15 + new ones) `0 failure(s)`.
- [ ] **Step 2 — Dedicated server + two clients.** Launch `ServerMain` on a port; launch two clients in CLIENT mode pointed at it (via the JOIN LobbyScreen or system properties). Verify together: both see each other animated with names + health bars; shared world has harvestable objects and killable mobs; death + respawn works (and PvP if on); chat + roster work; day/night matches; local movement is smooth.
- [ ] **Step 3 — Persistence restart.** Have a player place an object / move; stop the server; restart `ServerMain`; reconnect; confirm placed objects, player position, and seeded world persisted.
- [ ] **Step 4 — Commit** any final fixes. Update `README.md` with the dedicated-server run command and the persistence/pvp/mob config flags.

---

## Self-review notes

- **Spec coverage:** A→Task A1-3, B→B1-2, C→C1-3, D→D1-3, E→E1-2, F→F1-2, G→G1-2; final acceptance → Task Z. All seven approved features plus dedicated-server hardening covered.
- **Back-compat:** every wire change uses the existing trailing-optional-section + `try/catch(IOException)` idiom proven in `decodeSnapshot`; existing snapshot probes are the regression guard.
- **Type consistency:** new payload fields (`facing`,`moving`,`spriteName`,`displayName`,`alive`,`worldTimeTicks`) are introduced in their codec task and consumed by name in later tasks. `JsonFilePersistenceStore` implements the exact `PersistenceStore` interface methods.
- **Known unknowns flagged in-task** (read-before-implement): exact `Moveable.updateDirectionIndex` facing mapping (A2/A3), the object-type strings that are harvestable + drop loot (B1), the existing command/action enum for adding RESPAWN/CHAT (C1/E1), and how the client obtains its `GameClock` (F2). Each task says to read the referenced file first.
