package resources.geometry;

/**
 * Mutable 2D vector. Used for velocity, direction, and small offsets through
 * the simulation. {@link #x} and {@link #y} are public for hot-path reads /
 * writes (kept this way to avoid bytecode noise in the movement loop); use
 * setters where stylistic consistency matters.
 *
 * Operations split: arithmetic ({@link #add}, {@link #remove}, {@link #set})
 * mutate in place; geometric helpers ({@link #normalize}, {@link #transfer},
 * {@link #pacify}) return new vectors or report a boolean.
 */
public class Vector {

    public double x = 0;
    public double y = 0;

    public Vector() {}

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // ---- accessors ----

    public double getX() { return x; }
    public double getY() { return y; }

    public void set(double newX, double newY) { this.x = newX; this.y = newY; }
    public void set(Vector other)              { this.x = other.x; this.y = other.y; }

    // ---- arithmetic ----

    public void addX(double v) { x += v; }
    public void addY(double v) { y += v; }

    public void add(Vector other) {
        x += other.x;
        y += other.y;
    }

    public void remove(Vector other) {
        x -= other.x;
        y -= other.y;
    }

    public Vector copy() { return new Vector(x, y); }

    // ---- magnitude / geometry ----

    public boolean hasNoVelocity() { return x == 0 && y == 0; }

    /** Scale this vector to a magnitude of {@code transferValue}; returns a new vector. */
    public Vector normalize(double transferValue) {
        double mag = Math.sqrt(x * x + y * y);
        if (mag == 0) return new Vector();
        return new Vector(x * transferValue / mag, y * transferValue / mag);
    }

    /**
     * Decompose this vector into a step of size {@code transferValue} along its
     * direction; the step is removed from this vector and the remainder pacified
     * (snapped to zero if smaller than the step). Returns the step.
     */
    public Vector transfer(double transferValue) {
        Vector step = normalize(transferValue);
        remove(step);
        pacify(transferValue);
        return step;
    }

    /** Snap each component to zero if its absolute value is below {@code value}. */
    public boolean pacify(double value) {
        boolean changed = false;
        if (x < value && x > -value) { x = 0; changed = true; }
        if (y < value && y > -value) { y = 0; changed = true; }
        return changed;
    }

    @Override
    public String toString() { return "x: " + x + ", y: " + y; }
}
