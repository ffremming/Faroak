# Multiplayer Architecture

This package uses ports and adapters so the server backend can be swapped with
minimal game-code change.

## Core client pattern
- Port: `MultiplayerServerAdapter`
- Adapter registry: `MultiplayerAdapterRegistry`
- Runtime orchestrator: `MultiplayerRuntime`
- Message contracts: `resources.net.multiplayer.message.*`

`GamePanel` depends only on `MultiplayerRuntime`, which depends only on the
adapter port.

## Core server pattern
- `LobbyRuntime` (authoritative single-lobby domain loop)
- `RealtimeTransport` (server transport port)
- `AuthorityService` (sequence/speed/range validation)
- `PersistenceStore` (players, chunks, metadata, event log)
- `SnapshotCodec` (snapshot payload serialization)

Default implementation set:
- `AuthoritativeLobbyRuntime` + `GameServerRuntime`
- `InProcessRealtimeTransport`
- `DefaultAuthorityService`
- `SqlitePersistenceStore` (with in-memory fallback when SQLite driver is absent)
- backup helper: `resources/net/multiplayer/server/nightly-backup.sh`
- websocket gateway: `resources.net.multiplayer.server.gateway.WebSocketGatewayServer`

## Protocol v1
Every wire message is a `ProtocolEnvelope`:
- `protocolVersion`
- `playerId`
- `sequence`
- `ackSequence`
- `serverTick`
- `messageType`
- `payload`

Types:
- C2S: `JOIN`, `INPUT_STATE`, `ACTION`, `PING`, `LEAVE`
- S2C: `WELCOME`, `REJECT`, `BASELINE_SNAPSHOT`, `DELTA_SNAPSHOT`, `ACK`, `PLAYER_JOIN_LEAVE`

Snapshot payload (baseline + delta) carries:
- player states (`playerId`, position, velocity, processed sequence)
- authoritative world object states (`objectId`, type, position, removed flag, revision)

`ACTION` payload includes an optional `argument` string; for `PLACE` this is
the requested object/item type.

## Runtime config flags
```bash
-Dgame.multiplayer.mode=client|host|offline
-Dgame.multiplayer.backend=loopback|<custom-id>
-Dgame.multiplayer.playerId=<id>
-Dgame.multiplayer.maxPlayers=10
-Dgame.multiplayer.serverTickRate=30
-Dgame.multiplayer.snapshotRate=20
-Dgame.multiplayer.protocolVersion=1
-Dgame.multiplayer.interpolationDelayMs=120
-Dgame.multiplayer.serverMoveSpeedPerTick=2.0
-Dgame.multiplayer.serverActionRange=128
-Dgame.multiplayer.serverInterestRadius=2048
-Dgame.multiplayer.sqlitePath=multiplayer.db
-Dgame.multiplayer.gateway.enabled=true
-Dgame.multiplayer.gatewayPort=8080
-Dgame.multiplayer.reconnect.enabled=true
-Dgame.multiplayer.reconnectDelayMs=1000
-Dgame.multiplayer.connectionTimeoutMs=15000
```

## Switching server backend
1. Implement `MultiplayerServerAdapter`.
2. Register it:

```java
MultiplayerAdapterRegistry.register("my-backend", cfg -> new MyBackendAdapter(cfg));
```

3. Launch with:

```bash
-Dgame.multiplayer.backend=my-backend
```

No gameplay module changes are required.

## Online Play (WebSocket) Quick Start

Option A (dedicated server process):
1. Start server:
```bash
java -cp /tmp/gamebuild \
  -Dgame.multiplayer.mode=host \
  -Dgame.multiplayer.gatewayPort=8080 \
  resources.net.multiplayer.server.gateway.WebSocketGatewayMain
```
2. Start each client with a unique player id:
```bash
java -cp /tmp/gamebuild \
  -Dgame.multiplayer.mode=client \
  -Dgame.multiplayer.backend=websocket \
  -Dgame.multiplayer.playerId=p1 \
  -Dgame.multiplayer.serverUrl=ws://<server-ip>:8080/ws \
  resources.app.Main
```

Option B (single-process host from game UI):
- `Host Game` now starts an embedded websocket host for that session.
- `Join Game` connects to the host websocket URL configured by host/port fields.
