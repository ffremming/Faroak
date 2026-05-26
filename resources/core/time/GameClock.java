package resources.core.time;

/**
 * Authoritative game-time source. Counts logical ticks (one per simulation step)
 * and exposes derived quantities: in-game day length, time-of-day phase,
 * day count.
 *
 * Subsystems that need to fire on a cadence (farming growth, AI re-plan, day/night
 * transitions, lighting cycle) should read from this rather than tracking their
 * own counters — keeps the timeline coherent.
 *
 * One in-game day defaults to {@value #DEFAULT_TICKS_PER_DAY} ticks (~10 real
 * minutes at 60 FPS). Configurable by passing a different value to the constructor.
 */
public final class GameClock {

    public static final long DEFAULT_TICKS_PER_DAY = 720_000L;
    public static final long NOON_TICK_OF_DAY      = DEFAULT_TICKS_PER_DAY / 4L;

    private final long ticksPerDay;
    private long ticks;

    public GameClock() {
        this(DEFAULT_TICKS_PER_DAY, 0L);
    }

    public GameClock(long ticksPerDay) {
        this(ticksPerDay, 0L);
    }

    public GameClock(long ticksPerDay, long initialTicks) {
        if (ticksPerDay <= 0) throw new IllegalArgumentException("ticksPerDay must be > 0");
        this.ticksPerDay = ticksPerDay;
        this.ticks = Math.max(0L, initialTicks);
    }

    /** Advance one logical tick. Called once per simulation step. */
    public void tick() { ticks++; }

    public long ticks()       { return ticks; }
    public long day()         { return ticks / ticksPerDay; }
    public long tickOfDay()   { return ticks % ticksPerDay; }
    public long ticksPerDay() { return ticksPerDay; }

    /** 0.0 at sunrise, 0.5 at sunset, wraps. */
    public double dayPhase() {
        return (double) tickOfDay() / ticksPerDay;
    }

    /** Coarse phase enum suitable for lighting / spawn rules. */
    public Phase phase() {
        double p = dayPhase();
        if (p < 0.20) return Phase.DAWN;
        if (p < 0.45) return Phase.DAY;
        if (p < 0.55) return Phase.DUSK;
        return Phase.NIGHT;
    }

    public enum Phase { DAWN, DAY, DUSK, NIGHT }
}
