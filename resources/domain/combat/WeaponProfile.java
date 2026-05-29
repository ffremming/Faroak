package resources.domain.combat;

import java.util.HashMap;
import java.util.Map;

/**
 * Tunable combat numbers + prototype visual assets for one weapon family.
 */
public final class WeaponProfile {

    private static final Map<String, WeaponProfile> BY_ITEM = new HashMap<>();

    static {
        register("sword", new WeaponProfile(
            4, 96, 102.0, 8,
            8, 112, 126.0, 20,
            3, 15.0, 38, 18,
            "sword", "combat_bolt", 8, 160.0, 54.0));

        register("axe", new WeaponProfile(
            3, 90, 92.0, 10,
            9, 106, 118.0, 24,
            2, 13.0, 34, 22,
            "axe", "combat_bolt", 9, 150.0, 56.0));

        register("hammer", new WeaponProfile(
            4, 86, 88.0, 11,
            10, 100, 116.0, 26,
            4, 12.0, 30, 26,
            "hammer", "combat_bolt", 10, 148.0, 52.0));

        register("pickaxe", new WeaponProfile(
            3, 82, 84.0, 11,
            7, 98, 108.0, 24,
            2, 12.0, 30, 22,
            "pickaxe", "combat_bolt", 9, 144.0, 50.0));

        register("shovel", new WeaponProfile(
            2, 80, 88.0, 9,
            6, 94, 108.0, 20,
            2, 12.0, 28, 20,
            "shovel", "combat_bolt", 8, 140.0, 48.0));

        register("hoe", new WeaponProfile(
            2, 78, 90.0, 8,
            5, 90, 104.0, 18,
            1, 11.0, 24, 18,
            "hoe", "combat_bolt", 8, 138.0, 46.0));

        register("block", new WeaponProfile(
            2, 72, 86.0, 9,
            4, 88, 102.0, 18,
            2, 11.0, 24, 18,
            "block", "combat_bolt", 8, 132.0, 44.0));
    }

    public final int lightDamage;
    public final int lightRangePx;
    public final double lightArcDegrees;
    public final int lightCooldownTicks;

    public final int heavyDamage;
    public final int heavyRangePx;
    public final double heavyArcDegrees;
    public final int heavyCooldownTicks;

    public final int rangedDamage;
    public final double projectileSpeedPxPerTick;
    public final int projectileLifeTicks;
    public final int rangedCooldownTicks;

    public final String swingSpriteName;
    public final String projectileSpriteName;
    public final int swingDurationTicks;
    public final double swingArcDegrees;
    public final double swingRadiusPx;

    private WeaponProfile(
            int lightDamage,
            int lightRangePx,
            double lightArcDegrees,
            int lightCooldownTicks,
            int heavyDamage,
            int heavyRangePx,
            double heavyArcDegrees,
            int heavyCooldownTicks,
            int rangedDamage,
            double projectileSpeedPxPerTick,
            int projectileLifeTicks,
            int rangedCooldownTicks,
            String swingSpriteName,
            String projectileSpriteName,
            int swingDurationTicks,
            double swingArcDegrees,
            double swingRadiusPx) {
        this.lightDamage = lightDamage;
        this.lightRangePx = lightRangePx;
        this.lightArcDegrees = lightArcDegrees;
        this.lightCooldownTicks = lightCooldownTicks;
        this.heavyDamage = heavyDamage;
        this.heavyRangePx = heavyRangePx;
        this.heavyArcDegrees = heavyArcDegrees;
        this.heavyCooldownTicks = heavyCooldownTicks;
        this.rangedDamage = rangedDamage;
        this.projectileSpeedPxPerTick = projectileSpeedPxPerTick;
        this.projectileLifeTicks = projectileLifeTicks;
        this.rangedCooldownTicks = rangedCooldownTicks;
        this.swingSpriteName = swingSpriteName;
        this.projectileSpriteName = projectileSpriteName;
        this.swingDurationTicks = swingDurationTicks;
        this.swingArcDegrees = swingArcDegrees;
        this.swingRadiusPx = swingRadiusPx;
    }

    public static WeaponProfile forItem(String itemName) {
        if (itemName == null) return defaultProfile();
        WeaponProfile profile = BY_ITEM.get(itemName);
        if (profile != null) return profile;
        return defaultProfile();
    }

    private static WeaponProfile defaultProfile() {
        WeaponProfile sword = BY_ITEM.get("sword");
        return sword != null ? sword : BY_ITEM.values().iterator().next();
    }

    public int comboDamage(int comboStep) {
        return lightDamage + Math.max(0, comboStep - 1);
    }

    public int comboRange(int comboStep) {
        return lightRangePx + Math.max(0, comboStep - 1) * 6;
    }

    public double comboArc(int comboStep) {
        return lightArcDegrees + Math.max(0, comboStep - 1) * 8.0;
    }

    private static void register(String name, WeaponProfile profile) {
        BY_ITEM.put(name, profile);
    }
}
