package resources.input;

import resources.core.id.Identifier;

/**
 * A semantic input ("move up", "interact", "open inventory") rather than a
 * specific key. Subsystems test {@code input.isDown(InputAction.MOVE_UP)};
 * the binding from key code → action lives in {@link InputBindings}, so users
 * can rebind without touching gameplay code.
 *
 * Adding new actions is two lines: a constant here, and a binding registered
 * at startup.
 */
public final class InputAction {

    public static final InputAction MOVE_UP        = of("move_up");
    public static final InputAction MOVE_DOWN      = of("move_down");
    public static final InputAction MOVE_LEFT      = of("move_left");
    public static final InputAction MOVE_RIGHT     = of("move_right");
    public static final InputAction INTERACT       = of("interact");
    public static final InputAction OPEN_INVENTORY = of("open_inventory");
    public static final InputAction ATTACK         = of("attack");
    public static final InputAction ATTACK_LIGHT   = of("attack_light");
    public static final InputAction ATTACK_HEAVY   = of("attack_heavy");
    public static final InputAction ATTACK_RANGED  = of("attack_ranged");
    public static final InputAction PLACE          = of("place");
    public static final InputAction TOGGLE_DEBUG   = of("toggle_debug");
    public static final InputAction NEW_SEED       = of("new_seed");

    private final Identifier id;

    private InputAction(Identifier id) { this.id = id; }

    public static InputAction of(String name) { return new InputAction(Identifier.of(name)); }

    public Identifier id() { return id; }

    @Override public String toString() { return id.toString(); }

    @Override public int hashCode() { return id.hashCode(); }

    @Override public boolean equals(Object o) {
        return (o instanceof InputAction) && ((InputAction) o).id.equals(id);
    }
}
