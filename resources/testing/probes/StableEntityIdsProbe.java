package resources.testing.probes;

import resources.net.multiplayer.hostauth.StableEntityIds;

/**
 * Verifies StableEntityIds: same instance -> same id, distinct instances -> distinct
 * ids, and a forgotten instance gets a fresh id on re-registration.
 * Run: java -cp out resources.testing.probes.StableEntityIdsProbe
 */
public final class StableEntityIdsProbe {
    public static void main(String[] a) {
        StableEntityIds ids = new StableEntityIds();
        Object e1 = new Object(), e2 = new Object();
        long id1 = ids.idFor(e1), id1again = ids.idFor(e1), id2 = ids.idFor(e2);
        boolean ok = id1 > 0 && id1 == id1again && id2 != id1;
        ok = ok && ids.entityFor(id1) == e1;
        ids.forget(e1);
        long id1New = ids.idFor(e1);
        ok = ok && id1New != id1;          // forgotten -> fresh id
        ok = ok && ids.idFor(null) == 0L;  // null -> 0
        System.out.println(ok ? "PASS" : "FAIL id1=" + id1 + " again=" + id1again
            + " id2=" + id2 + " new=" + id1New);
        if (!ok) System.exit(1);
    }
}
