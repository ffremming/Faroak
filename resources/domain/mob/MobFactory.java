package resources.domain.mob;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.ai.ChaseBehavior;
import resources.domain.ai.WanderBehavior;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.DropSpec;
import resources.domain.inventory.DropTable;
import resources.domain.inventory.HarvestRegistry;

/**
 * Bundles the "what kind of mob is this" decision in one place: HP, behaviour,
 * drops, and harvest profile. Callers don't have to remember which numbers
 * pair with which species — they just ask for a deer or a goblin.
 *
 * Drops + harvest are wired through the existing {@link HarvestRegistry} so
 * combat reuses the player's swing-tool pipeline: a "sword"-tagged hit damages
 * the mob, and the durability counter doubles as its remaining hit points.
 */
public final class MobFactory {

    private MobFactory() {}

    static {
        // Register harvest profiles once; safe to call repeatedly because the
        // registry is a Map<String, Profile>.
        HarvestRegistry.register("deer", new HarvestRegistry.Profile(
            "sword", 8,
            DropTable.of(new DropSpec("hide", 1, 2), new DropSpec("meat", 1, 3))));
        HarvestRegistry.register("goblin", new HarvestRegistry.Profile(
            "sword", 10,
            DropTable.of(new DropSpec("stone", 1, 2))));
        HarvestRegistry.register("spider", new HarvestRegistry.Profile(
            "sword", 5,
            DropTable.of(new DropSpec("stone", 1, 1))));
    }

    public static Mob peacefulDeer(GamePanel panel, int x, int y, GameContext ctx) {
        Mob m = new Mob(panel, "deer", x, y, 8,
            new WanderBehavior(System.nanoTime() ^ ((long) x << 16) ^ y, 1, 90));
        attachHarvest(m, "deer");
        return m;
    }

    public static Mob hostileGoblin(GamePanel panel, int x, int y, GameContext ctx) {
        Mob m = new Mob(panel, "goblin", x, y, 10,
            new ChaseBehavior(ctx.player(), 1, 32));
        attachHarvest(m, "goblin");
        return m;
    }

    public static Mob hostileSpider(GamePanel panel, int x, int y, GameContext ctx) {
        Mob m = new Mob(panel, "spider", x, y, 5,
            new ChaseBehavior(ctx.player(), 2, 32));
        attachHarvest(m, "spider");
        return m;
    }

    private static void attachHarvest(Mob mob, String registryKey) {
        HarvestableComponent harvest = HarvestRegistry.componentFor(registryKey);
        if (harvest != null) mob.addComponent(harvest);
    }
}
