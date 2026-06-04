import javax.swing.JFrame;
import resources.app.GamePanel;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

public class TestOtherComponents {
    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());
        ProtocolPayloads.Snapshot snap = builder.buildBaseline(0L);
        
        System.out.println("Entity component summary:");
        int totalEntities = 0;
        int healthEntities = 0;
        int boatEntities = 0;
        int otherEntities = 0;
        int emptyComponents = 0;
        
        for (ProtocolPayloads.EntityStatePayload e : snap.entities) {
            totalEntities++;
            boolean hasHealth = false;
            boolean isBoat = e.entityType.contains("boat") || e.entityType.contains("dinghy") || e.entityType.contains("fisher");
            
            for (ProtocolPayloads.ComponentStatePayload c : e.components) {
                if (c.key.equals("health") || c.key.equals("max_health")) {
                    hasHealth = true;
                }
            }
            
            if (hasHealth) healthEntities++;
            else if (isBoat) boatEntities++;
            else otherEntities++;
            
            if (e.components.isEmpty()) emptyComponents++;
        }
        
        System.out.println("Total entities: " + totalEntities);
        System.out.println("  With health: " + healthEntities);
        System.out.println("  Boats/water: " + boatEntities);
        System.out.println("  Other: " + otherEntities);
        System.out.println("  With zero components: " + emptyComponents);
        
        panel.stopGameThread();
        System.exit(0);
    }
}
