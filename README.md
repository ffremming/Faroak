# FarOak

A 2D top-down survival game with a procedurally generated, chunk-streamed world. Explore forest, beach, and ocean biomes, gather resources, fight off pirates, and (soon) sail between islands.

![Gameplay](docs/screenshots/gameplay.png)

## About the project

- **Pure Java + Swing.** No game engine, no external rendering library — everything from the world generator to the sprite batcher is hand-written. Targets **JDK 17+**.
- **Procedural world.** Infinite chunked terrain with biome blending, altitude-driven beaches/cliffs, and on-the-fly object placement (trees, rocks, shrubs, driftwood, structures). Inlcudes procedural generation of tile sprites.
- **Online multiplayer — coming soon.** Networking scaffolding is in place (see [resources/net/](resources/net/)); the goal is co-op survival on a shared world.
- **Built-in debug overlay** with FPS, chunk counts, entity counts, draw time, and a hovered-tile inspector — useful when tuning generation or performance.

![Debug overlay](docs/screenshots/debug-overlay.png)
![Boats and beach](docs/screenshots/boats.png)

## Setup

Requirements: **JDK 17+**.

### Run from the command line

From the project root:

```bash
find resources -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out resources.app.Main
```

The working directory must be the project root so `resources/images/...` paths resolve.

### Run from an IDE (IntelliJ / VS Code)

1. Open the project folder (the one containing `resources/`).
2. Mark `resources` as a **sources root** (IntelliJ: right-click → *Mark Directory as → Sources Root*).
3. Set the run configuration's **working directory** to the project root.
4. Run `resources.app.Main` — entry point: [resources/app/Main.java](resources/app/Main.java).

## Online multiplayer

The game is server-authoritative. In the title screen, **Host Game** runs an
embedded server for a quick local session, and **Join Game** connects to a host
by address. For a persistent, shared world, run a **dedicated server**.

### Dedicated server

```bash
java -cp out \
  -Dgame.multiplayer.gatewayPort=7777 \
  -Dgame.multiplayer.dataDir=world-data \
  resources.net.multiplayer.server.ServerMain
```

It binds on all interfaces and prints `gateway listening on :7777`. The world is
seeded with harvestable objects on first run and saved to `world-data/`
(pure-JDK file store — no external database needed); it reloads on restart.

Players connect with **Join Game** in the title screen (host address + port), or
from the command line:

```bash
java -cp out \
  -Dgame.multiplayer.mode=client \
  -Dgame.multiplayer.backend=websocket \
  -Dgame.multiplayer.serverUrl=ws://<server-host>:7777/ws \
  resources.app.Main
```

### Server configuration flags

| Property | Default | Meaning |
|----------|---------|---------|
| `game.multiplayer.gatewayPort` | `8080` | TCP/WebSocket listen port |
| `game.multiplayer.dataDir` | `multiplayer-data` | World save directory (file store) |
| `game.multiplayer.persistence` | `file` | `file` \| `memory` \| `sqlite` |
| `game.multiplayer.maxPlayers` | `10` | Player cap |
| `game.multiplayer.pvp` | `true` | Allow player-vs-player damage |
| `game.multiplayer.respawnSeconds` | `5` | Auto-respawn delay after death |
| `game.multiplayer.mobCap` | `12` | Max simultaneous spawned mobs near players |
| `game.multiplayer.worldObjectCount` | `120` | Harvestable objects seeded on a fresh world |
| `game.world.seed` | `424242` | Shared world seed (client and server must match) |

### Multiplayer tests

```bash
java -cp out resources.testing.MultiplayerTestRunner          # 23 headless probes
java -cp out resources.testing.DedicatedServerAcceptance \    # two real clients vs a
  ws://127.0.0.1:7777/ws                                      # running ServerMain
```
