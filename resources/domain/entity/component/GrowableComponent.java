package resources.domain.entity.component;

import resources.core.time.GameClock;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Tickable;

/**
 * Multi-stage growth driver for crops and other entities that change form
 * over time. Each tick reads the {@link GameClock} and recomputes the current
 * stage, so all instances stay in phase even if they were attached at
 * different real times.
 *
 * Modes:
 *   - tick-paced: stage advances every {@code ticksPerStage} ticks (simple,
 *     test-friendly, used by farming where growth is faster than a day).
 *   - day-paced:  stage advances on {@code daysPerStage} day boundaries
 *     (used by slow crops keyed to in-game days).
 *
 * Final stage is sticky — once reached, the plant is "mature" and stops
 * advancing.
 */
public final class GrowableComponent implements EntityComponent, Tickable {

    private final GameClock clock;
    private final long      startTick;
    private final long      cadenceTicks;
    private final int       stageCount;

    private int currentStage;
    private StageListener listener;

    private GrowableComponent(GameClock clock, int stageCount, long cadenceTicks) {
        if (stageCount < 2) throw new IllegalArgumentException("need >= 2 stages");
        if (cadenceTicks <= 0) throw new IllegalArgumentException("cadence must be > 0");
        this.clock        = clock;
        this.stageCount   = stageCount;
        this.cadenceTicks = cadenceTicks;
        this.startTick    = clock.ticks();
    }

    public static GrowableComponent perTicks(GameClock clock, int stages, long ticksPerStage) {
        return new GrowableComponent(clock, stages, ticksPerStage);
    }

    public static GrowableComponent perDays(GameClock clock, int stages, long daysPerStage) {
        return new GrowableComponent(clock, stages, clock.ticksPerDay() * daysPerStage);
    }

    @Override public void onAttach(BaseEntity owner) { /* no registration */ }
    @Override public void onDetach(BaseEntity owner) { /* no registration */ }

    @Override
    public void update() {
        long elapsed = clock.ticks() - startTick;
        int target = (int) Math.min(stageCount - 1, elapsed / cadenceTicks);
        if (target != currentStage) {
            currentStage = target;
            if (listener != null) listener.onStageChanged(currentStage);
        }
    }

    public int     currentStage() { return currentStage; }
    public int     stageCount()   { return stageCount; }
    public boolean isMature()     { return currentStage >= stageCount - 1; }

    /** Optional hook so the host can refresh its sprite when the stage flips. */
    public void onStageChanged(StageListener l) { this.listener = l; }

    @FunctionalInterface
    public interface StageListener {
        void onStageChanged(int newStage);
    }
}
