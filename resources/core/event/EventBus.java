package resources.core.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple synchronous publish/subscribe by exact event class.
 *
 * Subscribers register a {@code Consumer<T>} for an event type; {@link #publish}
 * dispatches to every registered consumer for that exact class (no inheritance walk
 * — keeps fan-out predictable and avoids surprise subscribers).
 *
 * Synchronous: handlers run on the publishing thread, in registration order.
 * This matches the single-threaded game loop and avoids the ordering pitfalls of
 * async event buses. Heavy work in a handler should be queued elsewhere.
 *
 * Thread-safety: NOT thread-safe; intended for use from the main loop.
 */
public final class EventBus {

    private final Map<Class<? extends GameEvent>, List<Consumer<? extends GameEvent>>> subscribers = new HashMap<>();

    /** Register a handler for events of exactly {@code type}. */
    public <T extends GameEvent> void subscribe(Class<T> type, Consumer<T> handler) {
        subscribers.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    /** Remove a previously registered handler. */
    public <T extends GameEvent> void unsubscribe(Class<T> type, Consumer<T> handler) {
        List<Consumer<? extends GameEvent>> list = subscribers.get(type);
        if (list != null) list.remove(handler);
    }

    /** Dispatch an event to all handlers registered for its exact class. */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void publish(T event) {
        List<Consumer<? extends GameEvent>> list = subscribers.get(event.getClass());
        if (list == null) return;
        for (Consumer<? extends GameEvent> raw : list) {
            ((Consumer<T>) raw).accept(event);
        }
    }
}
