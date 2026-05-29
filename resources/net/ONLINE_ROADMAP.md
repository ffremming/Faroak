# Online Roadmap (10 Players)

## Current baseline
- Game loop, input, and world simulation are currently local-only.
- Networking abstractions existed (`NetworkChannel`, `ServerAuthority`) but had no implementations.
- World mutations (place/remove/harvest) were not authority-gated.

## Target model
- **Server-authoritative client/server**.
- Server simulates canonical world at fixed tick.
- Clients send input intents; server validates and applies.
- Server sends snapshot/delta state at a lower rate than simulation tick.
- Clients render remote entities with interpolation and reconcile local player state.

## Why this model here
- Existing world code is already centralized enough to run on one authority.
- Deterministic lockstep would be fragile here (floating-point movement, per-client timing).
- 10 players is a good fit for server authority with snapshots and interpolation.

## Phase plan
1. **Authority hooks (done)**: enforce authority checks on world mutations.
2. **Host runtime**: dedicated headless `GameServerRuntime` using the same world services.
3. **Protocol v1**:
   - `JoinRequest/JoinAccepted`
   - `InputCommand` (seq, movement bits, actions)
   - `WorldSnapshot` (players + relevant entities)
   - `Ack` (for client reconciliation window)
4. **Client networking loop**:
   - send buffered input commands
   - maintain interpolation buffer for remote actors
   - reconcile local player from authoritative snapshots
5. **Bandwidth/scope control**:
   - interest management by chunk radius
   - send deltas for changed entities only
6. **Stability**:
   - packet loss simulation in tests
   - connection timeout/reconnect flow (client reconnect + websocket idle timeout implemented; probe: `mp-reconnect`)
   - anti-cheat validation on server authority rules

## Initial tuning defaults
- Max players: `10`
- Server tick: `30 Hz`
- Snapshot rate: `20 Hz`
- Interpolation delay target: `100-150 ms` (tuned after live tests)
