# Host-Authoritative Multiplayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the HOST client's real game engine (`WorkingMemory`, `Boat`, `WorldInteraction`, `ClickRouter`, containers, farming) authoritative for multiplayer, serializing its live world into the existing snapshot protocol, so online gameplay behaves exactly like offline — instead of running a separate, divergent `AuthoritativeGameHost` simulation.

**Architecture:** Introduce a `HostAuthoritativeLobby` that *wraps the real engine running on the host* and produces the existing `ProtocolPayloads.Snapshot` each tick from `ctx.world().getEntities()/getTiles()`, applies remote players' inputs/clicks through the *real* `InputHandlingSystem` + `ClickRouter` + `WorldInteraction`, and feeds the existing transport/codec unchanged. The current `AuthoritativeLobbyRuntime`/`AuthoritativeGameHost`/`ActionResolver` stack is retained behind a config flag (`game.multiplayer.lobby=legacy|hostauth`, default flips to `hostauth` only in the final phase) so we can roll back instantly. The wire protocol, codec, transports (loopback + websocket), persistence, and client `ReplicatedWorldState` consumption are REUSED AS-IS — only the *source* of snapshot data changes.

**Tech Stack:** Java 17, Swing render loop, in-process + WebSocket transports, custom binary protocol codec, SQLite-backed persistence.

**Key insight from investigation:** The protocol already carries everything the real engine needs to express: entity id/type/pos/components, inventories, tile mutations, player pose/health/facing. The real engine already has stable enumeration (`EntityIndex`) and a `ClickRouter` chain that performs board/open/place/harvest. The gap is purely the *binding*: nothing today serializes the real world into snapshots or drives the real engine from remote inputs.

**Phasing strategy:** Phase 0 lands the three proven bugfixes so online is playable immediately and gives a regression baseline. Phases 1–6 build the host-authoritative path incrementally, each independently testable, behind a flag, with the flag flipped last.

---

## File Structure

New files (all under `resources/net/multiplayer/hostauth/`):
- `HostAuthoritativeLobby.java` — implements `LobbyRuntime`; owns the real `GamePanel`/engine on the host, applies inbound messages, ticks the engine, builds snapshots.
- `EngineSnapshotBuilder.java` — pure mapping: real `Entity`/`Tile`/`Inventory` → `ProtocolPayloads.Snapshot`. One responsibility: serialization.
- `RemoteInputApplier.java` — maps inbound `InputState`/`Command`/`Action` to engine mutations via `InputHandlingSystem` + `ClickRouter` + `WorldInteraction`, scoped per remote player.
- `StableEntityIds.java` — assigns/looks up stable `long` ids for real `BaseEntity` instances (the engine uses object identity; the protocol needs longs).

Modified files:
- `resources/net/multiplayer/server/AuthoritativeGameHost.java` — Phase 0 bugfix: clear rider on `removePlayer`; purge stale riders on load.
- `resources/net/multiplayer/MultiplayerRuntime.java` — Phase 0 bugfix: boarding resolves the clicked boat by id, not just nearest; debug accessors already added this session.
- `resources/net/multiplayer/MultiplayerConfig.java` — add `lobby()` selector (`legacy` | `hostauth`).
- `resources/net/multiplayer/LoopbackServerHub.java` and `EmbeddedWebSocketHost.java` — construct `HostAuthoritativeLobby` when `lobby()==hostauth`.

Reference (read-only, do not change): `ProtocolPayloads.java`, `ProtocolPayloadCodec.java`, `SnapshotCodec`, `ReplicatedWorldState.java`, `WorkingMemory.java`, `EntityIndex.java`, `Boat.java`, `ClickRouter.java`, `WorldInteraction.java`, `Inventory.java`, `FarmTile.java`.

---

## Phase 0 — Proven bugfixes (ship online playability now)

Lands the three concrete bugs confirmed by `resources/testing/BoatBoardProbe.java` this session. Independent of the rewrite; gives a working baseline and regression guard.

### Task 0.1: Clear rider component when a riding player disconnects

**Files:**
- Modify: `resources/net/multiplayer/server/AuthoritativeGameHost.java` (method `removePlayer`, line ~265)
- Test: `resources/testing/probes/GhostRiderProbe.java` (create)

- [ ] **Step 1: Write the failing probe**

Create `resources/testing/probes/GhostRiderProbe.java`:

```java
package resources.testing.probes;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.PersistenceStoreFactory;
import resources.net.multiplayer.server.persistence.PersistenceStore;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;

/**
 * Asserts a boat freed when its rider disconnects: rider component must go blank
 * so a later joiner can board. Reproduces the "boat already occupied" ghost-rider bug.
 * Run: java -cp out resources.testing.probes.GhostRiderProbe
 */
public final class GhostRiderProbe {
    public static void main(String[] args) {
        System.setProperty("game.multiplayer.serverActionRange", "768.0");
        MultiplayerConfig cfg = MultiplayerConfig.fromSystemProperties();
        PersistenceStore store = PersistenceStoreFactory.createDefault(":memory:");
        AuthoritativeLobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        // Drive a join → board → disconnect sequence, then assert the boat's rider is blank.
        // (Detailed envelope construction mirrors DedicatedServerAcceptance.java helpers.)
        boolean freed = GhostRiderHarness.boatFreedAfterRiderDisconnect(lobby, cfg);
        System.out.println("[GhostRider] boat freed after disconnect=" + freed);
        if (!freed) { System.err.println("FAIL: boat stayed occupied"); System.exit(1); }
        System.out.println("PASS");
    }
}
```

> NOTE for implementer: model the join/board/leave envelope sequence on the existing harness in `resources/testing/DedicatedServerAcceptance.java`. If a static `GhostRiderHarness` helper is heavier than warranted, inline the sequence in `main` using `lobby.receive(envelope)` + `lobby.tick()` and read back the boat state via a new package-private `lobby.debugBoatRider(entityId)` accessor.

- [ ] **Step 2: Run to verify it fails**

Run: `javac -d out @sources.txt && javac -d out -cp out resources/testing/probes/GhostRiderProbe.java && java -cp out resources.testing.probes.GhostRiderProbe`
Expected: `FAIL: boat stayed occupied` (exit 1).

- [ ] **Step 3: Implement the fix**

In `AuthoritativeGameHost.java`, replace `removePlayer`:

```java
void removePlayer(String playerId) {
    if (playerId != null) {
        // Free any boat this player was riding so others can board it. Without this,
        // a disconnect (or restored persisted state) leaves rider=playerId forever and
        // toggleBoatRide rejects everyone with "boat already occupied".
        for (EntityState entity : world.entities()) {
            if (entity == null || !"boat".equals(entity.entityType())) continue;
            if (playerId.equals(entity.component("rider"))) {
                entity.putComponent("rider", "", world.bumpRevision(), world.tick());
                dirty = true;
            }
        }
    }
    world.removePlayer(playerId);
}
```

> If `world.tick()` is not exposed, add a package-private getter on `WorldState` returning the last `setTick` value, or thread the current `tick` into `removePlayer` (caller `AuthoritativeLobbyRuntime.onDisconnect` has `tick` in scope — prefer passing it: change signature to `removePlayer(String playerId, long tick)` and update the one caller).

- [ ] **Step 4: Run to verify it passes**

Run: `java -cp out resources.testing.probes.GhostRiderProbe`
Expected: `PASS`.

- [ ] **Step 5: Commit**

```bash
git add resources/net/multiplayer/server/AuthoritativeGameHost.java resources/net/multiplayer/server/AuthoritativeLobbyRuntime.java resources/testing/probes/GhostRiderProbe.java
git commit -m "fix(mp): free boat rider on disconnect so others can board"
```

### Task 0.2: Purge stale riders from restored/persisted world on load

**Files:**
- Modify: `resources/net/multiplayer/server/AuthoritativeLobbyRuntime.java` (after `restoreWorldObjects()` in constructor, line ~96)

- [ ] **Step 1: Write the failing assertion into GhostRiderProbe**

Extend `GhostRiderProbe.main` to first restore a world whose boat already has `rider=p-stale`, construct the lobby, and assert no boat retains a rider for a player with no active session after the first `tick()`.

- [ ] **Step 2: Run to verify it fails**

Run: `java -cp out resources.testing.probes.GhostRiderProbe`
Expected: FAIL — restored boat keeps `rider=p-stale`.

- [ ] **Step 3: Implement**

In `AuthoritativeLobbyRuntime` constructor, after `restoreWorldObjects();`:

```java
// Persisted worlds can carry rider components for players who are not connected
// (server crashed mid-ride, or old saves). Clear them so boats are boardable.
gameHost.clearOrphanRiders(sessions.keySet());
```

Add to `AuthoritativeGameHost`:

```java
/** Blank the rider on any boat whose rider id is not in {@code activePlayerIds}. */
void clearOrphanRiders(java.util.Set<String> activePlayerIds) {
    for (EntityState entity : world.entities()) {
        if (entity == null || !"boat".equals(entity.entityType())) continue;
        String rider = entity.component("rider");
        if (rider == null || rider.isBlank()) continue;
        if (!activePlayerIds.contains(rider)) {
            entity.putComponent("rider", "", world.bumpRevision(), tick);
            dirty = true;
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `java -cp out resources.testing.probes.GhostRiderProbe`
Expected: `PASS`.

- [ ] **Step 5: Commit**

```bash
git add resources/net/multiplayer/server/AuthoritativeLobbyRuntime.java resources/net/multiplayer/server/AuthoritativeGameHost.java resources/testing/probes/GhostRiderProbe.java
git commit -m "fix(mp): purge orphan boat riders from restored world on load"
```

### Task 0.3: Boarding resolves the clicked boat by id, not nearest-overlap

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerRuntime.java` (`submitWorldClick`, line ~182; `entityAt`, line ~564)
- Test: `resources/testing/BoatBoardProbe.java` (already created this session — extend assertion)

- [ ] **Step 1: Strengthen the probe to assert riding engages**

In `BoatBoardProbe.main`, after the board click + step, add:

```java
if (!panel.multiplayer().localPlayerRiding()) {
    System.err.println("FAIL: not riding; reason=" + panel.multiplayer().debugLastCommandReason());
    System.exit(1);
}
System.out.println("PASS");
```

- [ ] **Step 2: Run to verify it fails**

Run: `javac -d out @sources.txt && javac -d out -cp out resources/testing/BoatBoardProbe.java && java -cp out resources.testing.BoatBoardProbe`
Expected: FAIL (after Phase 0.1/0.2 the *fresh* world should pass; if it still fails it is the wrong-boat resolution — proceed to Step 3).

- [ ] **Step 3: Implement — prefer the boat actually under the cursor and skip occupied boats**

In `MultiplayerRuntime.entityAt` (line ~564), when multiple boats overlap the click point, prefer the one whose hitbox center is closest to the click AND whose replicated `rider` is blank or the local player. Add a helper using existing `replicatedWorld`:

```java
private BaseEntity entityAt(double worldX, double worldY) {
    if (ctx.world() == null) return null;
    java.awt.Point p = new java.awt.Point((int) Math.round(worldX), (int) Math.round(worldY));
    BaseEntity bestBoat = null; double bestBoatD = Double.MAX_VALUE;
    BaseEntity container = null, fallback = null;
    for (BaseEntity entity : ctx.world().getEntitiesCollidedWith(p)) {
        if (entity == null || entity == ctx.player()) continue;
        if (entity instanceof Boat) {
            long id = replicatedWorld.entityIdFor(entity);
            String rider = replicatedWorld.riderOf(id);            // new accessor, see below
            boolean free = rider == null || rider.isBlank() || rider.equals(config.playerId());
            double d = Math.hypot(entity.getHitBox().getCenterX() - worldX,
                                  entity.getHitBox().getCenterY() - worldY);
            if (free && d < bestBoatD) { bestBoatD = d; bestBoat = entity; }
        } else if (isContainer(entity)) {
            container = entity;
        } else {
            fallback = entity;
        }
    }
    if (bestBoat != null) return bestBoat;
    if (container != null) return container;
    return fallback;
}
```

Add to `ReplicatedWorldState` (package-private) + a public passthrough on `MultiplayerRuntime` only if a test needs it:

```java
String riderOf(long entityId) {
    ProtocolPayloads.EntityStatePayload s = entityStateById.get(entityId);
    if (s == null || s.components == null) return null;
    for (ProtocolPayloads.ComponentStatePayload c : s.components) {
        if (c != null && "rider".equals(c.key)) return c.value;
    }
    return null;
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `java -cp out resources.testing.BoatBoardProbe`
Expected: `PASS` (riding engages).

- [ ] **Step 5: Commit**

```bash
git add resources/net/multiplayer/MultiplayerRuntime.java resources/net/multiplayer/ReplicatedWorldState.java resources/testing/BoatBoardProbe.java
git commit -m "fix(mp): board the free boat under the cursor, not an occupied overlap"
```

### Task 0.4: Run the existing MP regression suite as a gate

- [ ] **Step 1:** Run: `java -cp out resources.testing.MultiplayerTestRunner`
- [ ] **Step 2:** Expected: all 23 headless probes pass. If any regress from Phase 0, STOP and fix before continuing.
- [ ] **Step 3: Commit** (only if suite changes are needed): `git commit -am "test(mp): keep regression suite green after rider fixes"`

---

## Phase 1 — Stable entity ids for real engine objects

The real engine identifies entities by object identity; the protocol needs stable `long` ids. This phase adds an id registry, no behavior change yet.

### Task 1.1: StableEntityIds registry

**Files:**
- Create: `resources/net/multiplayer/hostauth/StableEntityIds.java`
- Test: `resources/testing/probes/StableEntityIdsProbe.java`

- [ ] **Step 1: Write the failing test**

```java
// StableEntityIdsProbe.java
package resources.testing.probes;
import resources.net.multiplayer.hostauth.StableEntityIds;
public final class StableEntityIdsProbe {
    public static void main(String[] a) {
        StableEntityIds ids = new StableEntityIds();
        Object e1 = new Object(), e2 = new Object();
        long id1 = ids.idFor(e1), id1again = ids.idFor(e1), id2 = ids.idFor(e2);
        boolean ok = id1 > 0 && id1 == id1again && id2 != id1;
        ids.forget(e1);
        long id1New = ids.idFor(e1);
        ok = ok && id1New != id1;          // forgotten → fresh id
        System.out.println(ok ? "PASS" : "FAIL id1=" + id1 + " again=" + id1again + " id2=" + id2 + " new=" + id1New);
        if (!ok) System.exit(1);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `javac -d out -cp out resources/testing/probes/StableEntityIdsProbe.java`
Expected: FAIL to compile — `StableEntityIds` does not exist.

- [ ] **Step 3: Implement**

```java
// resources/net/multiplayer/hostauth/StableEntityIds.java
package resources.net.multiplayer.hostauth;

import java.util.IdentityHashMap;
import java.util.Map;

/** Assigns stable long ids to engine entity instances (object identity → id). */
public final class StableEntityIds {
    private final Map<Object, Long> byEntity = new IdentityHashMap<>();
    private final Map<Long, Object> byId = new java.util.HashMap<>();
    private long next = 1L;

    public synchronized long idFor(Object entity) {
        if (entity == null) return 0L;
        Long existing = byEntity.get(entity);
        if (existing != null) return existing;
        long id = next++;
        byEntity.put(entity, id);
        byId.put(id, entity);
        return id;
    }

    public synchronized Object entityFor(long id) { return byId.get(id); }

    public synchronized void forget(Object entity) {
        if (entity == null) return;
        Long id = byEntity.remove(entity);
        if (id != null) byId.remove(id);
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `javac -d out -cp out resources/net/multiplayer/hostauth/StableEntityIds.java resources/testing/probes/StableEntityIdsProbe.java && java -cp out resources.testing.probes.StableEntityIdsProbe`
Expected: `PASS`.

- [ ] **Step 5: Commit**

```bash
git add resources/net/multiplayer/hostauth/StableEntityIds.java resources/testing/probes/StableEntityIdsProbe.java
git commit -m "feat(mp): stable long ids for engine entities (host-auth groundwork)"
```

---

## Phase 2 — EngineSnapshotBuilder (real world → Snapshot payload)

Pure mapping from the live engine to `ProtocolPayloads.Snapshot`. No transport yet — fully unit-testable against a booted offline `GamePanel`.

### Task 2.1: Serialize entities + players into a Snapshot

**Files:**
- Create: `resources/net/multiplayer/hostauth/EngineSnapshotBuilder.java`
- Test: `resources/testing/probes/EngineSnapshotProbe.java`

- [ ] **Step 1: Write the failing test**

Boot an offline `GamePanel`, place a known entity, build a baseline snapshot, assert it contains an `EntityStatePayload` whose `entityType` matches the placed entity's name and whose `worldX/worldY` are centered correctly.

```java
// EngineSnapshotProbe.java (sketch — implementer fills coordinate assertions)
package resources.testing.probes;
import javax.swing.JFrame;
import resources.app.GamePanel;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;
public final class EngineSnapshotProbe {
    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i=0;i<30;i++) panel.update(1.0);
        EngineSnapshotBuilder b = new EngineSnapshotBuilder(panel, new StableEntityIds());
        ProtocolPayloads.Snapshot snap = b.buildBaseline(1L);
        boolean hasEntities = snap.entities != null && !snap.entities.isEmpty();
        System.out.println("[Snap] entities=" + (snap.entities==null?0:snap.entities.size())
            + " tiles=" + (snap.tileMutations==null?0:snap.tileMutations.size()));
        if (!hasEntities) { System.err.println("FAIL: no entities serialized"); System.exit(1); }
        System.out.println("PASS");
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Expected: compile failure — `EngineSnapshotBuilder` missing.

- [ ] **Step 3: Implement the builder**

Map using verified accessors: `ctx.world().getEntities()` (non-tiles), `e.getName()`, `e.getWorldX/Y()`, `e.getWidth/Height()` (to convert corner→center to match `positionCentered` on the client), `e.getComponent(HealthComponent.class)`, `Boat.rider()`. Use `StableEntityIds.idFor(e)` for `entityId`. Emit components `health`, `max_health`, and for boats `rider` and `movement=water_only`. Build `PlayerState` for the host player from `ctx.player()`.

```java
// resources/net/multiplayer/hostauth/EngineSnapshotBuilder.java (core shape)
package resources.net.multiplayer.hostauth;

import java.util.ArrayList;
import resources.app.GameContext;
import resources.domain.entity.Entity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.object.Boat;
import resources.net.multiplayer.protocol.ProtocolPayloads;

public final class EngineSnapshotBuilder {
    private final GameContext ctx;
    private final StableEntityIds ids;
    public EngineSnapshotBuilder(GameContext ctx, StableEntityIds ids) { this.ctx = ctx; this.ids = ids; }

    public ProtocolPayloads.Snapshot buildBaseline(long ackSeq) { return build(true, ackSeq); }
    public ProtocolPayloads.Snapshot buildDelta(long ackSeq) { return build(false, ackSeq); }

    private ProtocolPayloads.Snapshot build(boolean baseline, long ackSeq) {
        ArrayList<ProtocolPayloads.PlayerState> players = new ArrayList<>(); // host + remotes (Phase 4)
        ArrayList<ProtocolPayloads.WorldObjectState> compat = new ArrayList<>();
        ArrayList<ProtocolPayloads.EntityStatePayload> entities = new ArrayList<>();
        ArrayList<ProtocolPayloads.InventoryStatePayload> inventories = new ArrayList<>(); // Phase 5
        ArrayList<ProtocolPayloads.TileMutationPayload> tiles = new ArrayList<>();          // Phase 5

        for (Entity e : ctx.world().getEntities()) {
            if (e == null || e == ctx.player()) continue;
            long id = ids.idFor(e);
            double cx = e.getWorldX() + e.getWidth() / 2.0;
            double cy = e.getWorldY() + e.getHeight() / 2.0;
            ArrayList<ProtocolPayloads.ComponentStatePayload> comps = new ArrayList<>();
            HealthComponent h = e.getComponent(HealthComponent.class);
            if (h != null) {
                comps.add(new ProtocolPayloads.ComponentStatePayload("health", h.current() + "/" + h.max()));
                comps.add(new ProtocolPayloads.ComponentStatePayload("max_health", Integer.toString(h.max())));
            }
            if (e instanceof Boat) {
                Boat boat = (Boat) e;
                comps.add(new ProtocolPayloads.ComponentStatePayload("movement", "water_only"));
                comps.add(new ProtocolPayloads.ComponentStatePayload("rider",
                    boat.rider() == null ? "" : riderId(boat.rider())));
            }
            entities.add(new ProtocolPayloads.EntityStatePayload(
                id, e.getName(), "core:overworld", cx, cy, false, 1L, comps));
        }
        return new ProtocolPayloads.Snapshot(baseline, ackSeq, players, compat, entities, inventories, tiles)
            .withWorldTime(ctx.clock() == null ? 0L : ctx.clock().ticks());
    }

    private String riderId(Object rider) { return "host"; } // Phase 4 maps real player → playerId
}
```

> Coordinate convention: the client's `ReplicatedWorldState.positionCentered` sets `worldX = centerX - width/2`. Therefore the builder MUST emit CENTER coordinates (corner + half-size), as above. Verify against `EngineSnapshotProbe` round-trip in Task 2.2.

- [ ] **Step 4: Run to verify it passes**

Run: `javac -d out -cp out resources/net/multiplayer/hostauth/EngineSnapshotBuilder.java resources/testing/probes/EngineSnapshotProbe.java && java -cp out resources.testing.probes.EngineSnapshotProbe`
Expected: `PASS`, entities > 0.

- [ ] **Step 5: Commit**

```bash
git add resources/net/multiplayer/hostauth/EngineSnapshotBuilder.java resources/testing/probes/EngineSnapshotProbe.java
git commit -m "feat(mp): serialize the real engine world into Snapshot payloads"
```

### Task 2.2: Codec round-trip (builder output survives encode→decode)

**Files:**
- Test: `resources/testing/probes/EngineSnapshotProbe.java` (extend)

- [ ] **Step 1:** Extend the probe to `new DefaultSnapshotCodec().encode(snap)` then `.decode(bytes)` and assert entity count + first entity's type/center coords are preserved within 0.5px.
- [ ] **Step 2: Run** — expect FAIL if any field is dropped by the codec path.
- [ ] **Step 3:** Fix any mismatch by aligning emitted fields with `ProtocolPayloadCodec`'s entity section order (id, type, dimension, x, y, removed, revision, components).
- [ ] **Step 4: Run** — expect `PASS`.
- [ ] **Step 5: Commit:** `git commit -am "test(mp): snapshot builder survives codec round-trip"`

---

## Phase 3 — HostAuthoritativeLobby skeleton (engine on host, snapshots out)

Wire a `LobbyRuntime` that owns the engine and publishes builder snapshots, selected by a flag. Single-player-over-loopback (one host, zero guests) must look identical to offline.

### Task 3.1: Config selector

**Files:**
- Modify: `resources/net/multiplayer/MultiplayerConfig.java`

- [ ] **Step 1:** Add `public String lobby()` reading `game.multiplayer.lobby` (default `"legacy"`), with a test in an existing config probe (or inline assertion) that `lobby()` returns `"hostauth"` when the property is set.
- [ ] **Step 2: Run** — fails (method missing).
- [ ] **Step 3:** Implement the getter mirroring existing `backend()` parsing in the same file.
- [ ] **Step 4: Run** — passes.
- [ ] **Step 5: Commit:** `git commit -am "feat(mp): add lobby selector (legacy|hostauth)"`

### Task 3.2: HostAuthoritativeLobby implements LobbyRuntime

**Files:**
- Create: `resources/net/multiplayer/hostauth/HostAuthoritativeLobby.java`
- Read: `resources/net/multiplayer/server/LobbyRuntime.java` (interface), `GameServerRuntime.java`, `LoopbackServerHub.java`

- [ ] **Step 1: Write a probe** that constructs a `HostAuthoritativeLobby` around a booted offline `GamePanel`, sends a `JOIN` envelope for a guest id, calls `tick()` a few times, and asserts `drainFor(guestId)` yields a `BASELINE_SNAPSHOT` envelope whose decoded snapshot has entities > 0.
- [ ] **Step 2: Run** — fails (class missing).
- [ ] **Step 3: Implement** `HostAuthoritativeLobby`:
  - Hold the host `GamePanel ctx`, a `StableEntityIds`, an `EngineSnapshotBuilder`, an outbound queue map, a `Set<String>` of joined player ids, a `baselineSent` set, and a `snapshotEveryTicks` counter (reuse `config.serverTickRate()/config.snapshotRate()`).
  - `receive(envelope)`: enqueue inbound.
  - `tick()`: drain inbound (handle JOIN → welcome + mark needs-baseline; LEAVE → drop; INPUT/COMMAND/ACTION → Phase 4); the host engine itself is ticked by the host's own frame loop (`GamePanel.update`) NOT here — so `tick()` only builds/sends snapshots on the cadence and emits welcomes/acks. Document this clearly.
  - `drainFor(playerId)`: return queued envelopes.
  - On each snapshot tick: for each joined player, build baseline (first time) or delta, encode, wrap `BASELINE_SNAPSHOT`/`DELTA_SNAPSHOT`, enqueue.

> CRITICAL design note for the implementer: in HOST mode the real engine runs on the host's own render thread (`GamePanel.update` → `world.simulate()`). `HostAuthoritativeLobby.tick()` runs on the server thread (`GameServerRuntime`). These two threads share the engine. To stay safe, EITHER (a) drive `HostAuthoritativeLobby` from the host frame thread instead of `GameServerRuntime` (preferred — add a host-frame hook in `MultiplayerRuntime.update` when `lobby==hostauth`), OR (b) guard all engine reads with the same lock the engine uses. Choose (a): it removes the data race entirely and matches "host frame IS the tick." Update Phase 3.3 wiring accordingly.

- [ ] **Step 4: Run** — passes (guest receives a populated baseline).
- [ ] **Step 5: Commit:** `git commit -m "feat(mp): HostAuthoritativeLobby publishes engine snapshots"`

### Task 3.3: Select the lobby in loopback/host wiring (flag-gated)

**Files:**
- Modify: `resources/net/multiplayer/LoopbackServerHub.java`, `EmbeddedWebSocketHost.java`

- [ ] **Step 1: Probe** — with `-Dgame.multiplayer.lobby=hostauth -Dgame.multiplayer.mode=host -Dgame.multiplayer.backend=loopback`, boot a host `GamePanel`, step 120 frames, assert `panel.multiplayer().isJoined()` and that the client still renders its own world (no regression). With the flag absent, the legacy path must still be selected.
- [ ] **Step 2: Run** — fails (wiring not added).
- [ ] **Step 3: Implement** — in the hub/host factory, branch on `config.lobby()`: `"hostauth"` constructs `HostAuthoritativeLobby` (needs the host `GamePanel` reference — thread it through the same path `EmbeddedWebSocketHost.ensureStarted` uses), else the existing `AuthoritativeLobbyRuntime`. Drive the lobby from the host frame per the Task 3.2 note.
- [ ] **Step 4: Run** — passes both flag states.
- [ ] **Step 5: Commit:** `git commit -m "feat(mp): wire HostAuthoritativeLobby behind game.multiplayer.lobby flag"`

---

## Phase 4 — Remote players: input application + presence + pose

Guests' inputs drive the real engine; remote players render on every client.

### Task 4.1: RemoteInputApplier drives movement through the real engine

**Files:**
- Create: `resources/net/multiplayer/hostauth/RemoteInputApplier.java`

- [ ] **Step 1: Probe** — feed an `InputState{right=true}` for a guest id into the applier against a host engine; after N host frames, assert the guest's server-side avatar entity moved right and never entered a water tile (uses the real `WorldInteraction.solidCollision`).
- [ ] **Step 2: Run** — fails (class missing).
- [ ] **Step 3: Implement** — maintain a per-remote-player avatar (a real engine entity or a lightweight `Playable`-like mover) and apply movement using the SAME path the local player uses: set velocity from input flags and let `world.simulate()`/collision move it, OR adopt client-reported `posX/posY` clamped by `CLIENT_MOVE_CLAMP` (256px) exactly like the legacy server (`AuthoritativeGameHost.setInput`). Reuse the real collision so remotes obey the same rules as the host.
- [ ] **Step 4: Run** — passes (moves, respects collision).
- [ ] **Step 5: Commit:** `git commit -m "feat(mp): apply remote player input through the real engine"`

### Task 4.2: Remote players appear in snapshots + presence

**Files:**
- Modify: `EngineSnapshotBuilder.java` (emit a `PlayerState` per joined remote), `HostAuthoritativeLobby.java` (presence broadcast on join/leave)

- [ ] **Step 1: Probe** — two guests join; assert each one's snapshot lists the other as a `PlayerState` with correct pose, and a `PLAYER_JOIN_LEAVE` presence is delivered.
- [ ] **Step 2: Run** — fails.
- [ ] **Step 3: Implement** — builder iterates remote avatars → `PlayerState` (pos, vel, health, facing from velocity per `Session.facing()` logic, `alive`, `displayName`). Lobby emits presence envelopes mirroring `AuthoritativeLobbyRuntime.presence`.
- [ ] **Step 4: Run** — passes.
- [ ] **Step 5: Commit:** `git commit -m "feat(mp): replicate remote players (pose + presence) from host engine"`

---

## Phase 5 — Interactions, inventories, tiles through the real ClickRouter

This is the payoff: clicks run the real `ClickRouter` chain server-side, so board/open/place/harvest behave identically to offline.

### Task 5.1: USE_EQUIPPED_AT / INTERACT routed through ClickRouter + WorldInteraction

**Files:**
- Modify: `RemoteInputApplier.java`, `HostAuthoritativeLobby.java`

- [ ] **Step 1: Probe** — a guest sends `interactEntity` on a boat id; assert the real `Boat.tryBoardFromClick`/`interact` path runs on the host engine and the boat's `rider()` becomes that guest, surfaced in the next snapshot's `rider` component.
- [ ] **Step 2: Run** — fails.
- [ ] **Step 3: Implement** — translate inbound `CommandRequest`/`ActionRequest` to a world `Point` and invoke the real chain: for `INTERACT_ENTITY`/`INTERACT_AT` and `USE_EQUIPPED_AT`, resolve the target entity via `StableEntityIds.entityFor(targetEntityId)` (or `ctx.world().getEntitiesCollidedWith(point)`), then run `ClickRouter.route(panel, point)` *in the context of the acting remote player*. Because `ClickRouter` reads `panel.player()`, introduce a scoped "acting player" swap (set the engine's current actor to the remote avatar for the duration of the call, then restore) — add a minimal `WorldInteraction.withActingPlayer(Playable, Runnable)` or equivalent. Keep range checks (reuse `maxActionRange`).
- [ ] **Step 4: Run** — passes (guest boards; rider replicates).
- [ ] **Step 5: Commit:** `git commit -m "feat(mp): route remote interactions through the real ClickRouter"`

### Task 5.2: Inventories + tile mutations in snapshots

**Files:**
- Modify: `EngineSnapshotBuilder.java`

- [ ] **Step 1: Probe** — open a chest as a guest, assert an `InventoryStatePayload` with `inventoryType` and slots appears; till+plant a tile, assert a `TileMutationPayload` with `cropType/cropStage` appears.
- [ ] **Step 2: Run** — fails.
- [ ] **Step 3: Implement** — builder emits: player inventory (`player:<id>`), cursor (`cursor:<id>`), and container inventories (`ownerEntityId` = the chest/barrel/table's stable id) from the real `Inventory`/`ChestInventory`. Emit farm tiles from `ctx.world().getTiles()` filtered to `FarmTile` → `TileMutationPayload` (`tileType`, `watered`, `crop().name()`, `crop().stage()`).
- [ ] **Step 4: Run** — passes.
- [ ] **Step 5: Commit:** `git commit -m "feat(mp): replicate inventories and farm tiles from host engine"`

### Task 5.3: Delta filtering (don't resend unchanged state)

**Files:**
- Modify: `EngineSnapshotBuilder.java`, `HostAuthoritativeLobby.java`

- [ ] **Step 1: Probe** — after a quiet tick (no change), assert delta snapshot entity list is empty; after moving one entity, assert exactly that entity is in the delta.
- [ ] **Step 2: Run** — fails (builder currently emits everything every tick).
- [ ] **Step 3: Implement** — track last-sent revision/pose per stable id; emit only changed entities/inventories/tiles in deltas. Bump a per-entity revision when its serialized fields change (hash of pos+components is sufficient).
- [ ] **Step 4: Run** — passes.
- [ ] **Step 5: Commit:** `git commit -m "perf(mp): delta-filter host-authoritative snapshots"`

---

## Phase 6 — Acceptance, flag flip, cleanup

### Task 6.1: Two-client acceptance under hostauth

**Files:**
- Modify: `resources/testing/DedicatedServerAcceptance.java` (parameterize lobby) or add `resources/testing/HostAuthAcceptance.java`

- [ ] **Step 1:** Run the existing two-real-clients acceptance with `-Dgame.multiplayer.lobby=hostauth`. Assert: both move with collision, one boards a boat and the other cannot board the same boat, one places a fence the other sees, one harvests a tree the other sees gone.
- [ ] **Step 2: Run** — capture failures.
- [ ] **Step 3:** Fix gaps surfaced (each fix its own commit with a probe).
- [ ] **Step 4: Run** — passes.
- [ ] **Step 5: Commit:** `git commit -m "test(mp): two-client acceptance passes under host-authoritative lobby"`

### Task 6.2: Flip default + run full suite

- [ ] **Step 1:** Change `MultiplayerConfig.lobby()` default to `"hostauth"`.
- [ ] **Step 2: Run** `java -cp out resources.testing.MultiplayerTestRunner` and `resources.testing.OnlineCollisionDiagnostic` and `resources.testing.BoatBoardProbe`.
- [ ] **Step 3:** Expected: all pass; boat boarding works; no water-walking; interactions replicate. If any regress, keep default `legacy` and STOP for review.
- [ ] **Step 4: Commit:** `git commit -m "feat(mp): default to host-authoritative lobby"`

### Task 6.3: Remove temporary debug scaffolding (optional)

- [ ] **Step 1:** Decide whether to keep `debugEntityId`/`debugLastCommandReason`/`debugEntityState` (useful) or guard behind a flag. Keep `BoatBoardProbe`/`GhostRiderProbe` as regression tests.
- [ ] **Step 2: Commit** if changed.

---

## Self-Review Notes

- **Spec coverage:** ship-on-land (Phase 0.1/0.2 + Phase 4.1 real collision), interactions broken (Phase 0.3 + Phase 5.1 real ClickRouter), containers (5.1/5.2), harvest (5.1/5.2), remote players (4.2). Covered.
- **Rollback:** every phase is flag-gated; `legacy` stays default until 6.2.
- **Thread-safety:** Phase 3.2 note resolves the host-frame vs server-thread race by driving the lobby from the host frame. Implementers MUST follow option (a).
- **Risk:** Phase 5.1's "acting player" swap is the sharpest edge — `ClickRouter`/`WorldInteraction` assume `panel.player()` is the actor. If a clean scoped-actor swap proves infeasible, fall back to calling the specific interaction methods directly (`Boat.tryBoardFromClick(avatar)`, `chest.interact(avatar)`, `harvestService.attackEntity(avatar, ctx, target)`, `worldInteraction.placeEntity(entity)`) rather than the generic chain — same behavior, explicit actor.
- **Open question for implementer:** remote-player avatars — reuse `Playable` or a lighter mover? Decide in Phase 4.1; the rest of the plan only needs "a real entity with position + collision + inventory + interaction-hitbox."
