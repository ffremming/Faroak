package resources.testing.probes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.combat.WeaponSwingEffect;
import resources.domain.entity.BaseEntity;
import resources.domain.player.Playable;
import resources.geometry.Vector;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Regression guard for the melee/tool swing visual.
 *
 * The swing effect composes a rotated weapon + slash arc into its image list,
 * but the scene renderer draws {@code getImages()}. {@link WeaponSwingEffect}
 * must therefore expose that composed visual through {@code getImages()};
 * otherwise it inherits {@code BaseEntity.getImages()}, which resolves the
 * entity name "weapon_swing" through the tile loader and yields a flat green
 * placeholder swatch instead of the swing art.
 */
public final class WeaponSwingArtProbe implements Probe {

    /** Tile fallback colour returned for unknown tile names (TileImageLoader). */
    private static final int GREEN_SWATCH_RGB = 0xFFA0C080;

    @Override public String name() { return "weapon_swing_art"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        if (!(panel.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        BaseEntity owner = (BaseEntity) panel.player();

        WeaponSwingEffect swing = new WeaponSwingEffect(
            panel, owner, new Vector(1, 0), "sword", 8, 160.0, 54.0);

        ArrayList<BufferedImage> rendered = swing.getImages();
        if (rendered == null || rendered.isEmpty() || rendered.get(0) == null) {
            return ProbeResult.fail(name() + " getImages() returned no frame");
        }

        BufferedImage frame = rendered.get(0);
        if (isUniform(frame, GREEN_SWATCH_RGB)) {
            return ProbeResult.fail(name() + " swing rendered as flat green tile placeholder",
                "getImages() must expose the composed weapon+slash visual, not the tile fallback");
        }
        if (isUniform(frame, frame.getRGB(0, 0))) {
            return ProbeResult.fail(name() + " swing rendered as a flat solid swatch",
                String.format("uniform 0x%08X", frame.getRGB(0, 0)));
        }

        String detail = String.format("frame=%dx%d", frame.getWidth(), frame.getHeight());
        return ProbeResult.pass(name(), detail);
    }

    private static boolean isUniform(BufferedImage img, int rgb) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (img.getRGB(x, y) != rgb) return false;
            }
        }
        return true;
    }
}
