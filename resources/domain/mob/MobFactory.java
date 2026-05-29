package resources.domain.mob;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.ai.CombatChaseBehavior;
import resources.domain.ai.WanderBehavior;
import resources.domain.entity.component.LootComponent;
import resources.domain.inventory.DropSpec;
import resources.domain.inventory.DropTable;

/**
 * Bundles the "what kind of mob is this" decision in one place: HP, behaviour,
 * drops, and combat profile. Callers don't have to remember which numbers
 * pair with which species — they just ask for a deer or a goblin.
 */
public final class MobFactory {

    private MobFactory() {}

    private static final DropTable DEER_DROPS = DropTable.of(
        new DropSpec("hide", 1, 2), new DropSpec("meat", 1, 3));
    private static final DropTable GOBLIN_DROPS = DropTable.of(
        new DropSpec("stone", 1, 2));
    private static final DropTable SPIDER_DROPS = DropTable.of(
        new DropSpec("stone", 1, 1));

    public static Mob peacefulDeer(GamePanel panel, int x, int y, GameContext ctx) {
        Mob m = new Mob(panel, "deer", x, y, 8,
            new WanderBehavior(System.nanoTime() ^ ((long) x << 16) ^ y, 1, 90));
        attachLoot(m, DEER_DROPS);
        return m;
    }

    public static Mob hostileGoblin(GamePanel panel, int x, int y, GameContext ctx) {
        Mob m = new Mob(panel, "goblin", x, y, 10,
            new CombatChaseBehavior(
                ctx.player(),
                0.9,
                44, 2, 20, "axe",
                0, 0, 0, 0.0, 0, null));
        attachLoot(m, GOBLIN_DROPS);
        return m;
    }

    public static Mob hostileSpider(GamePanel panel, int x, int y, GameContext ctx) {
        Mob m = new Mob(panel, "spider", x, y, 5,
            new CombatChaseBehavior(
                ctx.player(),
                1.2,
                34, 1, 28, "hammer",
                220, 2, 45, 10.0, 50, "block"));
        attachLoot(m, SPIDER_DROPS);
        return m;
    }

    private static void attachLoot(Mob mob, DropTable drops) {
        if (drops != null) mob.addComponent(new LootComponent(drops));
    }
}
