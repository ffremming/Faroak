# Dedicated Server Deployment (Single VM, Low Cost)

## Build
```bash
find resources -name '*.java' > /tmp/sources.txt
javac -encoding UTF-8 -d /tmp/gamebuild @/tmp/sources.txt
```

## Run server
```bash
java -cp "/tmp/gamebuild:/srv/game/lib/sqlite-jdbc.jar" \
  -Dgame.multiplayer.mode=host \
  -Dgame.multiplayer.maxPlayers=10 \
  -Dgame.multiplayer.serverTickRate=30 \
  -Dgame.multiplayer.snapshotRate=20 \
  -Dgame.multiplayer.gateway.enabled=true \
  -Dgame.multiplayer.gatewayPort=8080 \
  -Dgame.multiplayer.sqlitePath=/srv/game/multiplayer.db \
  resources.net.multiplayer.server.ServerMain
```

If `org.sqlite.JDBC` is missing, the server falls back to in-memory persistence.
Set `-Dgame.multiplayer.sqlite.required=true` to fail fast instead.

This starts the authoritative lobby loop and persistence.

## Client connection (current repo state)
```bash
-Dgame.multiplayer.mode=client
-Dgame.multiplayer.backend=websocket
-Dgame.multiplayer.serverUrl=ws://<server-host>:8080/ws
```

The client appends `playerId` automatically as a websocket query parameter.

## Multiplayer-only probe suite
```bash
java -cp /tmp/gamebuild resources.testing.MultiplayerTestRunner
```

## Nightly backup policy
- Daily retention: 7
- Weekly retention: 4

Example cron (02:30 daily):
```bash
30 2 * * * /srv/game/nightly-backup.sh /srv/game/multiplayer.db /srv/game/backups
```

Use `resources/net/multiplayer/server/nightly-backup.sh`.
