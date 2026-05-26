package resources.testing.probes;

import java.util.ArrayList;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.AnimationComponent;
import resources.domain.tile.Tile;
import resources.presentation.animation.AnimationClip;
import resources.presentation.animation.Animations;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies that ocean tiles' {@link AnimationComponent}s advance through every
 * frame of the clip, not just frame 0.
 *
 * Catches the regression where the component is attached but never ticked
 * (e.g. tile-tick pass removed by accident) — symptom would be a static
 * ocean tile despite the clip being registered.
 */
public final class AnimationFrameProbe implements Probe {

    private static final Logger LOG = Logger.forClass(AnimationFrameProbe.class);

    @Override public String name() { return "animation-frames"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        AnimationClip waves = ctx.animations().get(Animations.OCEAN_WAVES).orElse(null);
        if (waves == null) return ProbeResult.fail(name() + " ocean clip not registered");

        Tile oceanTile = findFirstOcean(ctx);
        if (oceanTile == null) return ProbeResult.skip(name() + " no ocean tiles visible");

        AnimationComponent anim = oceanTile.getComponent(AnimationComponent.class);
        if (anim == null) return ProbeResult.fail(name() + " ocean tile missing AnimationComponent");

        boolean[] seenFrame = new boolean[waves.frameCount()];
        for (int i = 0; i < waves.totalTicks() + 5; i++) {
            harness.tick(1);
            int idx = anim.currentFrameIndex();
            if (idx >= 0 && idx < seenFrame.length) seenFrame[idx] = true;
        }

        int missing = 0;
        for (boolean s : seenFrame) if (!s) missing++;
        String detail = String.format("frames-seen=%s", java.util.Arrays.toString(seenFrame));
        LOG.info(detail);

        return missing == 0
            ? ProbeResult.pass(name(), detail)
            : ProbeResult.fail(name() + " missing frame transitions", detail);
    }

    private Tile findFirstOcean(GameContext ctx) {
        ArrayList<BaseEntity> visible = ctx.world().getVisibleTiles(ctx.camera());
        for (BaseEntity be : visible) {
            if (be instanceof Tile && "ocean".equals(((Tile) be).getName())) return (Tile) be;
        }
        return null;
    }
}
