package resources.domain.object;

import java.util.HashMap;
import java.util.Map;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.EntityComponent;
import resources.domain.entity.component.TerrainSpeedComponent;

/**
 * Marker component attached to a player who has boarded a boat. Its job is to
 * carry the water-traversal speed table for the duration of the ride; when the
 * player dismounts, this component (and the {@link TerrainSpeedComponent} it
 * installs) should be removed together.
 *
 * Kept intentionally thin — mount/dismount lifecycle is not wired through
 * input yet, so this exists primarily as the attachment point that
 * {@link Boat#interact} would use once that flow lands.
 */
public final class BoatRideComponent implements EntityComponent {

    private final Boat boat;

    public BoatRideComponent(Boat boat) {
        this.boat = boat;
    }

    @Override
    public void onAttach(BaseEntity owner) {
        if (owner == null) return;
        if (owner.hasComponent(TerrainSpeedComponent.class)) return;
        owner.addComponent(new TerrainSpeedComponent(waterTable()));
    }

    @Override
    public void onDetach(BaseEntity owner) {
        if (owner == null) return;
        if (owner.hasComponent(TerrainSpeedComponent.class)) {
            owner.components().remove(TerrainSpeedComponent.class);
        }
    }

    public Boat boat() { return boat; }

    private static Map<String, Double> waterTable() {
        Map<String, Double> m = new HashMap<>();
        m.put("ocean", 1.0);
        m.put("river", 1.0);
        return m;
    }
}
