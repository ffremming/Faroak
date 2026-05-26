package resources.testing.perf;

import java.awt.image.BufferedImage;

/** Small timing helper for probe-style performance cases. */
public final class PerfSampler {

    private static volatile int pixelGuard;

    private PerfSampler() {}

    public interface Operation {
        void run();
    }

    public static long[] sampleMicros(int samples, Operation operation) {
        long[] timings = new long[samples];
        for (int i = 0; i < samples; i++) {
            timings[i] = timeMicros(operation);
        }
        return timings;
    }

    public static long timeMicros(Operation operation) {
        long start = System.nanoTime();
        operation.run();
        return (System.nanoTime() - start) / 1_000L;
    }

    public static long[] sampleDrawingMicros(int samples, BufferedImage canvas, Operation operation) {
        long[] timings = new long[samples];
        for (int i = 0; i < samples; i++) {
            timings[i] = timeDrawingMicros(canvas, i, operation);
        }
        return timings;
    }

    public static long timeDrawingMicros(BufferedImage canvas, int sampleIndex, Operation operation) {
        int w = Math.max(1, canvas.getWidth());
        int h = Math.max(1, canvas.getHeight());
        long start = System.nanoTime();
        operation.run();
        pixelGuard ^= canvas.getRGB((sampleIndex * 37) % w, (sampleIndex * 53) % h);
        return (System.nanoTime() - start) / 1_000L;
    }
}
