package resources.testing.probes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.domain.entity.component.LightSourceComponent;
import resources.presentation.lighting.LightField;
import resources.presentation.lighting.LightSource;
import resources.presentation.lighting.LightingPass;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the lighting system is live: the player carries a registered
 * {@link LightSource}, the source position tracks the player as it moves, and
 * a full {@link LightingPass#apply} runs without exceptions over a real
 * Graphics2D.
 *
 * Catches regressions where the component is added but never registers with
 * the field, or where the pass is wired but throws on empty/edge cases.
 */
public final class LightingProbe implements Probe {

    private static final Logger LOG = Logger.forClass(LightingProbe.class);

    @Override public String name() { return "lighting"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        LightField field = ctx.lighting();

        if (field.size() == 0) {
            return ProbeResult.fail(name() + " no light sources registered");
        }

        LightSourceComponent playerLight =
            ctx.player().components().get(LightSourceComponent.class);
        if (playerLight == null) {
            return ProbeResult.fail(name() + " player missing LightSourceComponent");
        }
        LightSource source = playerLight.source();
        if (source == null || source.host() != ctx.player()) {
            return ProbeResult.fail(name() + " player light source not bound to host");
        }

        int beforeX = source.worldCenterX();
        ctx.player().setWorldX(ctx.player().getWorldX() + 128);
        int afterX = source.worldCenterX();
        if (afterX - beforeX != 128) {
            return ProbeResult.fail(name() + " source did not follow player",
                String.format("dx-expected=128, dx-actual=%d", afterX - beforeX));
        }

        LightingPass pass = new LightingPass(field, ctx.clock());
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        try {
            pass.apply(g2, ctx.camera(), w, h);
        } catch (RuntimeException ex) {
            return ProbeResult.fail(name() + " apply threw", ex.toString());
        } finally {
            g2.dispose();
        }

        String detail = String.format("sources=%d, radius=%d, intensity=%.2f",
            field.size(), source.radius(), source.intensity());
        LOG.info(detail);
        return ProbeResult.pass(name(), detail);
    }
}
