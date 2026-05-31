package resources.testing.probes;

import resources.domain.ship.Faction;
import resources.domain.ship.FactionRelations;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Verifies faction hostility table: pirates hostile to player & merchants/fishers,
 *  fishers/merchants neutral by default. */
public final class FactionProbe implements Probe {
    @Override public String name() { return "faction"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        if (!FactionRelations.isHostileToPlayer(Faction.PIRATE))
            return ProbeResult.fail(name(), "pirate should be hostile to player");
        if (FactionRelations.isHostileToPlayer(Faction.FISHER))
            return ProbeResult.fail(name(), "fisher should not be hostile to player by default");
        if (!FactionRelations.isHostile(Faction.PIRATE, Faction.MERCHANT))
            return ProbeResult.fail(name(), "pirate should be hostile to merchant");
        if (FactionRelations.isHostile(Faction.FISHER, Faction.FISHER))
            return ProbeResult.fail(name(), "fisher should not be hostile to own faction");
        return ProbeResult.pass(name(), "relations table correct");
    }
}
