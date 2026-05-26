package resources.domain.entity.component;

import java.util.Collections;
import java.util.Map;

import resources.domain.entity.BaseEntity;

/**
 * Per-tile speed multiplier table. Stays attached for the life of the
 * equipped item or persistent buff that owns it — when the buff drops off,
 * remove this component and movement reverts to whatever the next-attached
 * component (or none) reports.
 *
 * Multipliers default to {@code 1.0} for unmapped tile names so an attached
 * component never makes the player slower than no component at all.
 *
 * Why the constant default: a partial map shouldn't be a footgun. A boat
 * component listing only water+beach speeds shouldn't accidentally pin the
 * player to 0 on grass — the controller would just apply 1.0.
 */
public final class TerrainSpeedComponent implements EntityComponent {

    private final Map<String, Double> multipliers;

    public TerrainSpeedComponent(Map<String, Double> multipliers) {
        this.multipliers = Collections.unmodifiableMap(multipliers);
    }

    @Override public void onAttach(BaseEntity owner) { /* no registration */ }
    @Override public void onDetach(BaseEntity owner) { /* no registration */ }

    /** Multiplier for the named tile; 1.0 if the tile is not in the table. */
    public double multiplierFor(String tileName) {
        if (tileName == null) return 1.0;
        Double v = multipliers.get(tileName);
        return v == null ? 1.0 : v;
    }
}
