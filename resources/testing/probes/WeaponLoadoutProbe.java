package resources.testing.probes;

import resources.domain.ship.WeaponLoadout;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Verifies WeaponLoadout presets expose sane stats and NONE is unarmed. */
public final class WeaponLoadoutProbe implements Probe {
    @Override public String name() { return "weapon_loadout"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        if (WeaponLoadout.NONE.armed())
            return ProbeResult.fail(name(), "NONE must be unarmed");
        if (!WeaponLoadout.BROADSIDE.armed())
            return ProbeResult.fail(name(), "BROADSIDE must be armed");
        if (WeaponLoadout.BROADSIDE.damage() <= 0 || WeaponLoadout.BROADSIDE.cooldownTicks() <= 0)
            return ProbeResult.fail(name(), "BROADSIDE stats must be positive");
        if (WeaponLoadout.HEAVY.damage() <= WeaponLoadout.BROADSIDE.damage())
            return ProbeResult.fail(name(), "HEAVY should hit harder than BROADSIDE");
        return ProbeResult.pass(name(), "loadout presets valid");
    }
}
