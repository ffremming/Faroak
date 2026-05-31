package resources.testing.probes;

import resources.domain.ship.ShipKind;
import resources.domain.ship.ShipKindRegistry;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Verifies PLAYER_SLOOP exists with the legacy boat's footprint and stats,
 *  and (after Task 9) the concrete NPC kinds. */
public final class ShipKindRegistryProbe implements Probe {
    @Override public String name() { return "ship_kind_registry"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        ShipKind sloop = ShipKindRegistry.PLAYER_SLOOP;
        if (sloop == null) return ProbeResult.fail(name(), "PLAYER_SLOOP missing");
        if (sloop.width() != 192 || sloop.height() != 192)
            return ProbeResult.fail(name(), "sloop footprint should be 192x192, got "
                + sloop.width() + "x" + sloop.height());
        if (sloop.hitboxWidth() != 144 || sloop.hitboxHeight() != 144)
            return ProbeResult.fail(name(), "sloop hitbox should be 144x144");
        if (Math.abs(sloop.speed() - 4.0) > 0.001)
            return ProbeResult.fail(name(), "sloop speed should be 4.0");
        if (sloop.maxHealth() != 30)
            return ProbeResult.fail(name(), "sloop hp should be 30");
        if (sloop.boardable())
            return ProbeResult.fail(name(), "sloop should not be boardable");
        if (ShipKindRegistry.byId("player_sloop") != sloop)
            return ProbeResult.fail(name(), "byId lookup failed");

        if (ShipKindRegistry.PIRATE_BRIG == null || !ShipKindRegistry.PIRATE_BRIG.loadout().armed())
            return ProbeResult.fail(name(), "PIRATE_BRIG must exist and be armed");
        if (ShipKindRegistry.PIRATE_BRIG.faction() != resources.domain.ship.Faction.PIRATE)
            return ProbeResult.fail(name(), "PIRATE_BRIG must be PIRATE faction");
        if (ShipKindRegistry.FISHER == null || ShipKindRegistry.FISHER.loadout().armed())
            return ProbeResult.fail(name(), "FISHER must exist and be unarmed");
        if (ShipKindRegistry.GALLEON == null
                || ShipKindRegistry.GALLEON.maxHealth() <= ShipKindRegistry.PIRATE_BRIG.maxHealth())
            return ProbeResult.fail(name(), "GALLEON must exist and be tankier than a brig");

        return ProbeResult.pass(name(), "PLAYER_SLOOP + PIRATE_BRIG + FISHER + GALLEON present");
    }
}
