package resources.domain.object;

import resources.domain.combat.BoatCombatFx;
import resources.domain.combat.BoatProjectile;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Tickable;
import resources.domain.entity.component.EntityComponent;
import resources.domain.entity.component.HealthComponent;
import resources.domain.player.Playable;
import resources.input.InputHandlingSystem;

/**
 * Boat-vs-boat combat state and behavior (HP, broadside cooldown, sink flow).
 */
public final class BoatCombatComponent implements EntityComponent, Tickable {

    private static final int BOAT_MAX_HEALTH = 30;
    private static final int BROADSIDE_DAMAGE = 8;
    private static final int BROADSIDE_COOLDOWN_TICKS = 24;

    private static final double PROJECTILE_SPEED = 13.0; // px/tick
    private static final int PROJECTILE_RANGE_TILES = 15;

    private static final double SIDE_MUZZLE_OFFSET_SCALE = 0.46;
    private static final double SIDE_MUZZLE_MIN_OFFSET_PX = 24.0;
    private static final double SIDE_MUZZLE_MAX_OFFSET_PX = 42.0;
    private static final double BROADSIDE_FORWARD_LEAD_PX = 16.0;

    private Boat boat;
    private int cooldownTicks;
    private boolean destroyed;

    @Override
    public void onAttach(BaseEntity owner) {
        if (!(owner instanceof Boat)) {
            throw new IllegalStateException("BoatCombatComponent requires Boat host");
        }
        this.boat = (Boat) owner;
        if (!boat.hasComponent(HealthComponent.class)) {
            boat.addComponent(new HealthComponent(BOAT_MAX_HEALTH));
        }
    }

    @Override
    public void onDetach(BaseEntity owner) {
        this.boat = null;
    }

    @Override
    public void update() {
        if (destroyed || boat == null) return;
        if (cooldownTicks > 0) cooldownTicks--;
        HealthComponent hp = boat.getComponent(HealthComponent.class);
        if (hp != null && hp.isDead()) sink();
    }

    public boolean fireBroadside() {
        if (boat == null || destroyed || cooldownTicks > 0) return false;

        int[] facing = Boat.directionVectorForIndex(boat.facingIndex());
        double fx = facing[0];
        double fy = facing[1];
        double len = Math.hypot(fx, fy);
        if (len <= 0.0001) return false;
        fx /= len;
        fy /= len;

        double leftX = -fy;
        double leftY = fx;
        double rightX = -leftX;
        double rightY = -leftY;

        double cx = boat.getWorldX() + boat.getWidth() / 2.0;
        double cy = boat.getWorldY() + boat.getHeight() / 2.0;

        double lead = forwardLeadPx();
        double leadX = fx * lead;
        double leadY = fy * lead;

        double leftOffset = muzzleOffsetFor(leftX, leftY);
        double rightOffset = muzzleOffsetFor(rightX, rightY);

        spawnBroadsideShot(cx + leadX + leftX * leftOffset,
                           cy + leadY + leftY * leftOffset,
                           leftX, leftY);
        spawnBroadsideShot(cx + leadX + rightX * rightOffset,
                           cy + leadY + rightY * rightOffset,
                           rightX, rightY);

        cooldownTicks = BROADSIDE_COOLDOWN_TICKS;
        return true;
    }

    public void takeDamage(int amount, double hitX, double hitY) {
        if (boat == null || amount <= 0 || destroyed) return;
        HealthComponent hp = boat.getComponent(HealthComponent.class);
        if (hp == null || hp.isDead()) return;
        hp.takeDamage(amount);
        BoatCombatFx.spawnHitBurst(boat.panel, hitX, hitY);
        if (hp.isDead()) sink();
    }

    public boolean isDestroyed() { return destroyed; }

    private double muzzleOffsetFor(double sideX, double sideY) {
        double halfW = boat.getHitBox().width / 2.0;
        double halfH = boat.getHitBox().height / 2.0;
        double neededX = Math.abs(sideX) < 0.0001 ? Double.POSITIVE_INFINITY : halfW / Math.abs(sideX);
        double neededY = Math.abs(sideY) < 0.0001 ? Double.POSITIVE_INFINITY : halfH / Math.abs(sideY);
        double edgeDistance = Math.min(neededX, neededY);
        double tuned = edgeDistance * SIDE_MUZZLE_OFFSET_SCALE;
        return Math.max(SIDE_MUZZLE_MIN_OFFSET_PX, Math.min(SIDE_MUZZLE_MAX_OFFSET_PX, tuned));
    }

    private double forwardLeadPx() {
        Playable rider = boat.rider();
        if (rider == null) return 0.0;
        InputHandlingSystem in = boat.panel.input();
        if (in == null) return 0.0;
        int ix = (in.isRight() ? 1 : 0) - (in.isLeft() ? 1 : 0);
        int iy = (in.isDown()  ? 1 : 0) - (in.isUp()   ? 1 : 0);
        return (ix == 0 && iy == 0) ? 0.0 : BROADSIDE_FORWARD_LEAD_PX;
    }

    private void spawnBroadsideShot(double muzzleX, double muzzleY, double dirX, double dirY) {
        BoatCombatFx.spawnMuzzleFlash(boat.panel, muzzleX, muzzleY);
        double maxRange = PROJECTILE_RANGE_TILES * boat.panel.tileSize;
        BoatProjectile projectile = new BoatProjectile(
            boat.panel, boat, muzzleX, muzzleY, dirX, dirY,
            PROJECTILE_SPEED, maxRange, BROADSIDE_DAMAGE);
        boat.panel.world().placeEntityIgnoringTerrainCollision(projectile);
    }

    private void sink() {
        if (destroyed || boat == null) return;
        destroyed = true;

        double cx = boat.getWorldX() + boat.getWidth() / 2.0;
        double cy = boat.getWorldY() + boat.getHeight() / 2.0;
        BoatCombatFx.spawnSinkBurst(boat.panel, cx, cy);

        Playable rider = boat.rider();
        if (rider != null && rider.lifecycle() != null) {
            rider.lifecycle().damage(BOAT_MAX_HEALTH);
        }

        boat.clearRiderForSink();
        boat.panel.world().addToRemovalQueue(boat);
    }
}
