package resources.core.event;

/**
 * Marker interface for all game events. Concrete events are tiny immutable
 * records carrying the data subscribers need.
 *
 * Examples (defined alongside their producers):
 *   - {@code ChunkLoaded(chunkId)}
 *   - {@code EntityPlaced(entity)}
 *   - {@code TileChanged(tile)}
 *   - {@code PlayerMoved(oldPos, newPos)}
 *   - {@code TimeOfDayChanged(phase)}
 *   - {@code LightSourceMoved(source, pos)}
 *
 * Keeping the marker empty lets {@link EventBus} subscribe by exact class without
 * coupling unrelated events to a shared base.
 */
public interface GameEvent {
}
