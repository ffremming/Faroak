package resources.net.multiplayer.hostauth;

import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.Entity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.farming.Crop;
import resources.domain.farming.FarmTile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Stack;
import resources.domain.object.Barrel;
import resources.domain.object.Boat;
import resources.domain.object.Chest;
import resources.domain.object.CraftingTable;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Serializes the live (host-authoritative) game engine world into the multiplayer
 * {@link ProtocolPayloads.Snapshot} the existing codec, transport and client
 * {@code ReplicatedWorldState} already understand.
 *
 * <p>This is a pure mapping: it reads {@code ctx.world().getEntities()} and the
 * world clock and produces payloads. It performs no mutation and holds no state
 * beyond the {@link StableEntityIds} registry handed in.
 *
 * <p>Coordinate convention: the client reconstructs an entity with
 * {@code worldX = centerX - width/2} (see {@code ReplicatedWorldState.positionCentered}),
 * so this builder emits CENTER coordinates (corner + half-size).
 *
 * <p>Players, inventories, tile mutations and delta filtering are added in later
 * phases; this phase establishes entity + world-time serialization.
 */
public final class EngineSnapshotBuilder {

    private final GameContext ctx;
    private final StableEntityIds ids;
    // Resolves a boat's rider Playable to its owning playerId so the client sees the real
    // rider (host or a specific guest) instead of a placeholder. May be null in unit probes
    // that construct the builder without a lobby; the host player is then the only rider.
    private RemoteInputApplier remotes;
    private String hostPlayerId = "host";
    // Last-sent signature per entity/tile, for delta filtering. Updated only by the
    // delta path (buildDelta), so the per-frame delta is computed once and shared by all
    // recipients without one recipient's send hiding a change from another.
    private final java.util.Map<Long, String> lastEntitySig = new java.util.HashMap<>();
    private final java.util.Map<String, String> lastTileSig = new java.util.HashMap<>();
    // Last-sent signature per inventory id, for inventory delta filtering. An inventory's
    // full slot list rides every snapshot only when its contents (slot/item/quantity) change;
    // an unchanged inventory is omitted from deltas. The client keeps the last value per id
    // (it never clears inventoriesById on delta), so omission preserves the bag.
    private final java.util.Map<Long, String> lastInventorySig = new java.util.HashMap<>();

    public EngineSnapshotBuilder(GameContext ctx, StableEntityIds ids) {
        this.ctx = ctx;
        this.ids = ids;
    }

    /**
     * Supply the remote-player registry and the host's own playerId so boat riders are
     * serialized with their owning playerId. Without this, a boat rider is reported as the
     * host id, which makes guests unable to recognize their own (or peers') boarding state.
     */
    public void withRiderResolution(RemoteInputApplier remotes, String hostPlayerId) {
        this.remotes = remotes;
        if (hostPlayerId != null && !hostPlayerId.isBlank()) this.hostPlayerId = hostPlayerId;
    }

    public ProtocolPayloads.Snapshot buildBaseline(long ackSeq) {
        return build(true, ackSeq, null, null);
    }

    public ProtocolPayloads.Snapshot buildDelta(long ackSeq) {
        return build(false, ackSeq, null, null);
    }

    /** Baseline snapshot including the given remote players (host serializes peers). */
    public ProtocolPayloads.Snapshot buildBaseline(long ackSeq, List<ProtocolPayloads.PlayerState> players) {
        return build(true, ackSeq, players, null);
    }

    /** Delta snapshot including the given remote players. */
    public ProtocolPayloads.Snapshot buildDelta(long ackSeq, List<ProtocolPayloads.PlayerState> players) {
        return build(false, ackSeq, players, null);
    }

    /** Baseline snapshot with remote players AND externally-built player inventories. */
    public ProtocolPayloads.Snapshot buildBaseline(long ackSeq,
            List<ProtocolPayloads.PlayerState> players,
            List<ProtocolPayloads.InventoryStatePayload> playerInventories) {
        return build(true, ackSeq, players, playerInventories);
    }

    /** Delta snapshot with remote players AND externally-built player inventories. */
    public ProtocolPayloads.Snapshot buildDelta(long ackSeq,
            List<ProtocolPayloads.PlayerState> players,
            List<ProtocolPayloads.InventoryStatePayload> playerInventories) {
        return build(false, ackSeq, players, playerInventories);
    }

    private ProtocolPayloads.Snapshot build(boolean baseline, long ackSeq,
            List<ProtocolPayloads.PlayerState> playerStates,
            List<ProtocolPayloads.InventoryStatePayload> playerInventories) {
        ArrayList<ProtocolPayloads.PlayerState> players =
            playerStates == null ? new ArrayList<>() : new ArrayList<>(playerStates);
        ArrayList<ProtocolPayloads.WorldObjectState> compat = new ArrayList<>();       // legacy layer, unused here
        ArrayList<ProtocolPayloads.EntityStatePayload> entities = new ArrayList<>();
        // Candidate inventories (player bags/cursors passed in, plus container inventories
        // discovered below). These are delta-filtered as one batch just before assembly so an
        // unchanged inventory is omitted from deltas. Baselines send all of them.
        ArrayList<ProtocolPayloads.InventoryStatePayload> candidateInventories =
            playerInventories == null ? new ArrayList<>() : new ArrayList<>(playerInventories);
        ArrayList<ProtocolPayloads.TileMutationPayload> tiles = new ArrayList<>();          // Phase 5

        if (ctx.world() != null) {
            // Baseline rebuilds the cache from scratch; the client's removeMissing(seen)
            // handles deletions for baselines. Delta must explicitly emit removals for any
            // id that was in the previous snapshot but is no longer in the world.
            if (baseline) lastEntitySig.clear();
            java.util.Set<Long> presentIds = new java.util.HashSet<>();
            for (Entity e : ctx.world().getEntities()) {
                if (e == null || e == ctx.player()) continue;
                // Container contents (chest/barrel) ride alongside the entity, owned by it.
                Inventory container = containerInventory(e);
                if (container != null) {
                    long ownerId = ids.idFor(e);
                    String invType = (e instanceof CraftingTable) ? "crafting" : "generic";
                    candidateInventories.add(inventoryPayload(container, ids.idFor(container), ownerId, invType));
                }
                ProtocolPayloads.EntityStatePayload p = toPayload(e);
                presentIds.add(p.entityId);
                // Baseline: send everything and seed the cache. Delta: send only changed.
                String sig = entitySignature(p);
                if (baseline) {
                    lastEntitySig.put(p.entityId, sig);
                    entities.add(p);
                } else if (!sig.equals(lastEntitySig.get(p.entityId))) {
                    lastEntitySig.put(p.entityId, sig);
                    entities.add(p);
                }
            }
            // Delta: emit removals for entities that disappeared since the last snapshot,
            // drop their cache entries so the map does not grow without bound, and forget
            // their stable-id mapping so StableEntityIds does not keep strong references to
            // dead engine instances (a real heap leak) and can reuse the id space.
            if (!baseline) {
                java.util.Iterator<Long> it = lastEntitySig.keySet().iterator();
                while (it.hasNext()) {
                    Long id = it.next();
                    if (!presentIds.contains(id)) {
                        entities.add(removalPayload(id.longValue()));
                        Object gone = ids.entityFor(id.longValue());
                        if (gone != null) ids.forget(gone);
                        it.remove();
                    }
                }
            }
            java.util.Set<String> presentTileKeys = new java.util.HashSet<>();
            for (Tile t : ctx.world().getTiles()) {
                if (!(t instanceof FarmTile)) continue;
                ProtocolPayloads.TileMutationPayload tp = toTilePayload((FarmTile) t);
                String key = tp.dimensionId + ":" + tp.tileX + ":" + tp.tileY;
                presentTileKeys.add(key);
                String sig = tileSignature(tp);
                if (baseline) {
                    lastTileSig.put(key, sig);
                    tiles.add(tp);
                } else if (!sig.equals(lastTileSig.get(key))) {
                    lastTileSig.put(key, sig);
                    tiles.add(tp);
                }
            }
            // Delta: prune cache entries for FarmTiles that reverted to plain tiles, so
            // lastTileSig stays bounded over a long session of plant/harvest cycles.
            if (!baseline) {
                lastTileSig.keySet().removeIf(k -> !presentTileKeys.contains(k));
            }
        }

        ArrayList<ProtocolPayloads.InventoryStatePayload> inventories =
            filterInventories(baseline, candidateInventories);

        long worldTime = ctx.clock() == null ? 0L : ctx.clock().ticks();
        return new ProtocolPayloads.Snapshot(baseline, ackSeq, players, compat, entities, inventories, tiles)
            .withWorldTime(worldTime);
    }

    private ProtocolPayloads.EntityStatePayload toPayload(Entity e) {
        long id = ids.idFor(e);
        double centerX = e.getWorldX() + e.getWidth() / 2.0;
        double centerY = e.getWorldY() + e.getHeight() / 2.0;

        ArrayList<ProtocolPayloads.ComponentStatePayload> comps = new ArrayList<>();
        HealthComponent health = e.getComponent(HealthComponent.class);
        if (health != null) {
            comps.add(component("health", health.current() + "/" + health.max()));
            comps.add(component("max_health", Integer.toString(health.max())));
        }
        if (e instanceof Boat) {
            Boat boat = (Boat) e;
            comps.add(component("movement", "water_only"));
            comps.add(component("rider", boat.rider() == null ? "" : riderPlayerId(boat.rider())));
        }

        return new ProtocolPayloads.EntityStatePayload(
            id, e.getName(), "core:overworld", centerX, centerY, false, 1L, comps);
    }

    /**
     * The playerId that owns a boat's rider. A guest's headless actor maps to that guest's
     * id via the remote registry; anything else (including the host's own local player, which
     * is never a registered guest actor) is the host. This lets clients recognize which
     * player is aboard a boat instead of always seeing the host id.
     */
    private String riderPlayerId(Playable rider) {
        if (remotes != null) {
            String guestId = remotes.playerIdForActor(rider);
            if (guestId != null) return guestId;
        }
        return hostPlayerId;
    }

    /** Tombstone payload telling the client to drop a no-longer-present entity. */
    private static ProtocolPayloads.EntityStatePayload removalPayload(long entityId) {
        return new ProtocolPayloads.EntityStatePayload(
            entityId, "", "core:overworld", 0.0, 0.0, true, 1L, new ArrayList<>());
    }

    private static final int TILE_SIZE = 64;

    private ProtocolPayloads.TileMutationPayload toTilePayload(FarmTile t) {
        int tileX = (int) Math.floor(t.getWorldX() / TILE_SIZE);
        int tileY = (int) Math.floor(t.getWorldY() / TILE_SIZE);
        String tileType = t.isWatered() ? "farmland_watered" : "farmland";
        Crop crop = t.crop();
        String cropType = crop == null ? "" : crop.baseName();
        int cropStage = crop == null ? 0 : crop.stage();
        return new ProtocolPayloads.TileMutationPayload(
            "core:overworld", tileX, tileY, tileType, t.isWatered(), cropType, cropStage, 1L);
    }

    /** Signature for delta filtering: changes when position (rounded) or any component does. */
    private static String entitySignature(ProtocolPayloads.EntityStatePayload p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.entityType).append('|')
          .append(Math.round(p.worldX)).append(',').append(Math.round(p.worldY))
          .append('|').append(p.removed).append('|');
        for (ProtocolPayloads.ComponentStatePayload c : p.components) {
            sb.append(c.key).append('=').append(c.value).append(';');
        }
        return sb.toString();
    }

    private static String tileSignature(ProtocolPayloads.TileMutationPayload t) {
        return t.tileType + "|" + t.watered + "|" + t.cropType + "|" + t.cropStage;
    }

    /**
     * Delta-filter inventories the same way entities/tiles are filtered. Baselines clear the
     * cache and emit every inventory (seeding signatures). Deltas emit only inventories whose
     * contents changed since the last snapshot, keyed by inventory id. The cache is advanced
     * here, once per built snapshot, mirroring the shared-delta discipline for entities so a
     * single per-frame delta is computed and shared by all recipients.
     */
    private ArrayList<ProtocolPayloads.InventoryStatePayload> filterInventories(
            boolean baseline, ArrayList<ProtocolPayloads.InventoryStatePayload> candidates) {
        if (baseline) lastInventorySig.clear();
        ArrayList<ProtocolPayloads.InventoryStatePayload> out = new ArrayList<>();
        for (ProtocolPayloads.InventoryStatePayload inv : candidates) {
            if (inv == null) continue;
            String sig = inventorySignature(inv);
            if (baseline) {
                lastInventorySig.put(inv.inventoryId, sig);
                out.add(inv);
            } else if (!sig.equals(lastInventorySig.get(inv.inventoryId))) {
                lastInventorySig.put(inv.inventoryId, sig);
                out.add(inv);
            }
        }
        return out;
    }

    /** Signature for inventory delta filtering: changes when type or any (slot,item,qty) does. */
    private static String inventorySignature(ProtocolPayloads.InventoryStatePayload inv) {
        StringBuilder sb = new StringBuilder();
        sb.append(inv.inventoryType).append('|').append(inv.ownerEntityId).append('|');
        int i = 0;
        for (ProtocolPayloads.ItemStackPayload s : inv.slots) {
            sb.append(i++).append(':').append(s.itemType).append('x').append(s.amount).append(';');
        }
        return sb.toString();
    }

    private static Inventory containerInventory(Entity e) {
        if (e instanceof Chest) return ((Chest) e).getChestInventory();
        if (e instanceof Barrel) return ((Barrel) e).getBarrelInventory();
        // CraftingTable's 4x4 input grid is an Inventory; replicate it so guests see the
        // host's actual grid contents instead of a default/empty grid.
        if (e instanceof CraftingTable) return ((CraftingTable) e).getService().grid();
        return null;
    }

    /** Serialize an engine Inventory into an InventoryStatePayload. Public so the lobby
     *  can build player/cursor inventories with the right type string. */
    public ProtocolPayloads.InventoryStatePayload inventoryPayload(
            Inventory inv, long inventoryId, long ownerEntityId, String inventoryType) {
        ArrayList<ProtocolPayloads.ItemStackPayload> slots = new ArrayList<>();
        if (inv != null) {
            int size = inv.getSize();
            for (int i = 0; i < size; i++) {
                Stack s = inv.getStack(i);
                if (s == null || s.isEmpty()) {
                    slots.add(new ProtocolPayloads.ItemStackPayload("empty", 0));
                } else {
                    slots.add(new ProtocolPayloads.ItemStackPayload(s.getName(), s.getAmount()));
                }
            }
        }
        return new ProtocolPayloads.InventoryStatePayload(inventoryId, ownerEntityId, inventoryType, 1L, slots);
    }

    /** Stable id for an engine Inventory instance (for player/cursor inventory ids). */
    public long inventoryId(Object inventory) {
        return ids.idFor(inventory);
    }

    /**
     * Serialize a player's cursor (the single item held on the mouse, {@code tempInHand})
     * into a one-slot {@link ProtocolPayloads.InventoryStatePayload}. An empty cursor emits a
     * single "empty" slot, which the client maps back to a null cursor. The {@code type} must
     * be {@code "cursor:<playerId>"} so the client's {@code cursorForPlayer} can find it.
     */
    public ProtocolPayloads.InventoryStatePayload cursorPayload(Stack held, long inventoryId, String type) {
        ArrayList<ProtocolPayloads.ItemStackPayload> slots = new ArrayList<>(1);
        if (held == null || held.isEmpty()) {
            slots.add(new ProtocolPayloads.ItemStackPayload("empty", 0));
        } else {
            slots.add(new ProtocolPayloads.ItemStackPayload(held.getName(), held.getAmount()));
        }
        return new ProtocolPayloads.InventoryStatePayload(inventoryId, 0L, type, 1L, slots);
    }

    private static ProtocolPayloads.ComponentStatePayload component(String key, String value) {
        return new ProtocolPayloads.ComponentStatePayload(key, value);
    }
}
