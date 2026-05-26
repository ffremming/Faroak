package resources.testing.perf;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/** Heap and GC counters captured at one instant. */
public final class GcSnapshot {

    private final long usedHeapKb;
    private final long maxHeapKb;
    private final long collections;
    private final long collectionTimeMs;

    private GcSnapshot(long usedHeapKb, long maxHeapKb, long collections, long collectionTimeMs) {
        this.usedHeapKb = usedHeapKb;
        this.maxHeapKb = maxHeapKb;
        this.collections = collections;
        this.collectionTimeMs = collectionTimeMs;
    }

    public static GcSnapshot now() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024L;
        long max = rt.maxMemory() / 1024L;
        long count = 0;
        long time = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() > 0) count += bean.getCollectionCount();
            if (bean.getCollectionTime() > 0) time += bean.getCollectionTime();
        }
        return new GcSnapshot(used, max, count, time);
    }

    public long usedHeapKb() { return usedHeapKb; }
    public long maxHeapKb() { return maxHeapKb; }
    public long collections() { return collections; }
    public long collectionTimeMs() { return collectionTimeMs; }

    public long usedHeapDeltaKb(GcSnapshot before) { return usedHeapKb - before.usedHeapKb; }
    public long collectionDelta(GcSnapshot before) { return collections - before.collections; }
    public long collectionTimeDeltaMs(GcSnapshot before) {
        return collectionTimeMs - before.collectionTimeMs;
    }
}
