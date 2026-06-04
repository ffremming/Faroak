import javax.swing.JFrame;
import java.awt.Point;
import resources.app.GamePanel;
import resources.domain.farming.FarmTile;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.farming.Crop;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

public class CheckCropComponents {
    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        // Till a tile near the player and plant wheat.
        double px = panel.player().getWorldX();
        double py = panel.player().getWorldY();
        FarmTile farm = null;
        outer:
        for (int r = 0; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    farm = panel.world().tillTileAt(new Point((int) px + dx * 64, (int) py + dy * 64));
                    if (farm != null) break outer;
                }
            }
        }
        if (farm != null && farm.plant("wheat")) {
            panel.world().update(new Point((int) farm.getWorldX(), (int) farm.getWorldY()));
            
            Crop c = farm.crop();
            GrowableComponent g = c.getComponent(GrowableComponent.class);
            System.out.println("Crop has GrowableComponent: " + (g != null));
            System.out.println("Crop stage: " + c.stage());
            
            EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());
            ProtocolPayloads.Snapshot snap = builder.buildBaseline(0L);
            
            // Look for Crop entity in the entities list
            int foundCount = 0;
            for (ProtocolPayloads.EntityStatePayload e : snap.entities) {
                if (e.entityType.contains("stage")) {
                    foundCount++;
                    System.out.println("Found crop entity: " + e.entityType);
                    System.out.println("  Total components: " + e.components.size());
                    boolean hasGrowable = false;
                    for (ProtocolPayloads.ComponentStatePayload comp : e.components) {
                        System.out.println("    " + comp.key + " = " + comp.value);
                        if (comp.key.contains("growth") || comp.key.contains("stage") || comp.key.contains("grow")) {
                            hasGrowable = true;
                        }
                    }
                    if (!hasGrowable) {
                        System.out.println("  WARNING: No growth/stage component found!");
                    }
                }
            }
            System.out.println("Total crop entities found: " + foundCount);
        }
        panel.stopGameThread();
        System.exit(0);
    }
}
