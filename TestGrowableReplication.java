import javax.swing.JFrame;
import java.awt.Point;
import resources.app.GamePanel;
import resources.domain.farming.FarmTile;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.farming.Crop;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

public class TestGrowableReplication {
    public static void main(String[] a) throws Exception {
        GamePanel hostPanel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) hostPanel.update(1.0);

        // Till and plant on host
        double px = hostPanel.player().getWorldX();
        double py = hostPanel.player().getWorldY();
        FarmTile farm = null;
        outer:
        for (int r = 0; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    farm = hostPanel.world().tillTileAt(new Point((int) px + dx * 64, (int) py + dy * 64));
                    if (farm != null) break outer;
                }
            }
        }
        
        if (farm != null && farm.plant("wheat")) {
            hostPanel.world().update(new Point((int) farm.getWorldX(), (int) farm.getWorldY()));
            
            // Simulate time passing on host
            for (int i = 0; i < 100; i++) hostPanel.update(1.0);
            
            Crop hostCrop = farm.crop();
            GrowableComponent hostGrowable = hostCrop.getComponent(GrowableComponent.class);
            int hostStage = hostCrop.stage();
            System.out.println("Host crop stage after 100 updates: " + hostStage);
            System.out.println("Host GrowableComponent.currentStage(): " + hostGrowable.currentStage());
            
            // Build snapshot
            EngineSnapshotBuilder builder = new EngineSnapshotBuilder(hostPanel, new StableEntityIds());
            ProtocolPayloads.Snapshot snap = builder.buildBaseline(0L);
            
            // Check: are GrowableComponent stages in snapshot components?
            for (ProtocolPayloads.EntityStatePayload e : snap.entities) {
                if (e.entityType.contains("stage")) {
                    System.out.println("\nCrop entity in snapshot:");
                    System.out.println("  Type: " + e.entityType);
                    System.out.println("  Components: " + e.components.size());
                    for (ProtocolPayloads.ComponentStatePayload c : e.components) {
                        System.out.println("    " + c.key + " = " + c.value);
                    }
                    
                    // Is the stage visible from components?
                    boolean hasStageInfo = false;
                    for (ProtocolPayloads.ComponentStatePayload c : e.components) {
                        if (c.key.contains("growth") || c.key.contains("stage")) hasStageInfo = true;
                    }
                    if (!hasStageInfo) {
                        System.out.println("  MISSING: No stage/growth component!");
                    }
                }
            }
            
            // Check tile mutations as well
            System.out.println("\nTile mutations:");
            for (ProtocolPayloads.TileMutationPayload t : snap.tileMutations) {
                System.out.println("  Tile (" + t.tileX + "," + t.tileY + "): crop=" + t.cropType + " stage=" + t.cropStage);
            }
        }
        hostPanel.stopGameThread();
        System.exit(0);
    }
}
