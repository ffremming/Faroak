package resources.testing.probes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.AnimationComponent;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies that ocean tiles bordering beach render the animated
 * {@code beachB*}/{@code beach1B*}/{@code beach2B*} overlays — not the legacy
 * single-frame {@code wetBeachB*} family — and that the overlay image actually
 * cycles when the ocean animation frame changes.
 */
public final class BeachWaterTransitionProbe implements Probe {

    private static final Logger LOG = Logger.forClass(BeachWaterTransitionProbe.class);

    @Override public String name() { return "beach-water-transition"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        Tile ocean = findOceanWithBeachNeighbour(ctx);
        if (ocean == null) return ProbeResult.skip(name() + " no ocean/beach border in view");

        Set<BufferedImage> frame0 = new HashSet<>();
        Set<BufferedImage> frame1 = new HashSet<>();
        Set<BufferedImage> frame2 = new HashSet<>();
        for (int side = 0; side < 4; side++) {
            frame0.add(ctx.images().getTileImage("beachB" + side));
            frame1.add(ctx.images().getTileImage("beach1B" + side));
            frame2.add(ctx.images().getTileImage("beach2B" + side));
        }

        boolean sawF0 = false, sawF1 = false, sawF2 = false;
        for (int i = 0; i < 240; i++) {
            harness.tick(1);
            for (BufferedImage img : ocean.getImages()) {
                if (frame0.contains(img)) sawF0 = true;
                if (frame1.contains(img)) sawF1 = true;
                if (frame2.contains(img)) sawF2 = true;
            }
        }
        String detail = String.format("seen overlays: beachB*=%s, beach1B*=%s, beach2B*=%s",
            sawF0, sawF1, sawF2);
        LOG.info(detail);

        boolean ok = sawF0 && sawF1 && sawF2;
        return ok
            ? ProbeResult.pass(name(), detail)
            : ProbeResult.fail(name() + " missing animated beach overlay frames", detail);
    }

    private Tile findOceanWithBeachNeighbour(GameContext ctx) {
        ArrayList<BaseEntity> visible = ctx.world().getVisibleTiles(ctx.camera());
        for (BaseEntity be : visible) {
            if (!(be instanceof Tile)) continue;
            Tile t = (Tile) be;
            if (!"ocean".equals(t.getName())) continue;
            if (t.getComponent(AnimationComponent.class) == null) continue;
            for (Tile n : t.getNeighbors()) {
                if (n != null && "beach".equals(n.getName())) return t;
            }
        }
        return null;
    }
}
