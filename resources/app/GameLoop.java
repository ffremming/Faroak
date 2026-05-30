package resources.app;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

/**
 * Fixed-step game loop. Runs {@link GamePanel#update(double)} and
 * {@link GamePanel#repaint()} on the EDT at the target FPS.
 *
 * Pulled out of GamePanel so the loop policy (variable timestep, accumulator,
 * pause, replay, profiler) can be swapped without touching everything else
 * that lives on the panel.
 */
public final class GameLoop implements Runnable {

    private static final int  TARGET_FPS   = 60;
    private static final long FRAME_NANOS  = 1_000_000_000L / TARGET_FPS;

    private final GamePanel panel;
    private volatile boolean running = true;

    public GameLoop(GamePanel panel) {
        this.panel = panel;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        long lastLoopTime = System.nanoTime();
        long fpsAccumulator = 0;
        int  fpsCounter = 0;

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastLoopTime;
            lastLoopTime = now;
            double delta = elapsed / (double) FRAME_NANOS;

            tickUpdate(delta);
            tickRender();

            fpsAccumulator += elapsed;
            fpsCounter++;
            if (fpsAccumulator >= 1_000_000_000L) {
                panel.camera().setObservedFPS(fpsCounter);
                fpsAccumulator = 0;
                fpsCounter = 0;
            }

            sleepUntilNextFrame(lastLoopTime);
        }
    }

    private void tickUpdate(double delta) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                long before = System.nanoTime();
                panel.update(delta);
                long after = System.nanoTime();
                panel.camera().addbackendPrintData("update time " + ((after - before) / 1000) + " us");
            });
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void tickRender() {
        long before = System.nanoTime();
        panel.repaint();
        long after = System.nanoTime();
        panel.camera().addbackendPrintData("repaint time " + ((after - before) / 1000) + " us");
    }

    private void sleepUntilNextFrame(long lastLoopTime) {
        long sleepMs = (lastLoopTime - System.nanoTime() + FRAME_NANOS) / 1_000_000L;
        if (sleepMs <= 0) return;
        try { Thread.sleep(sleepMs); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
