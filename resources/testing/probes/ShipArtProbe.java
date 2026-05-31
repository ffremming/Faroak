package resources.testing.probes;

import java.awt.image.BufferedImage;
import java.io.File;

import resources.domain.ship.ShipKind;
import resources.domain.ship.ShipKindRegistry;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies every registered ship kind that points at a real sprite directory
 * actually has its 8 numbered frame files on disk (0.png..7.png). This guards
 * against a kind silently falling back to the procedural placeholder because a
 * spriteDir path is wrong or art is missing.
 */
public final class ShipArtProbe implements Probe {
    @Override public String name() { return "ship_art"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        StringBuilder missing = new StringBuilder();
        int checked = 0;
        for (ShipKind kind : ShipKindRegistry.ALL) {
            String dir = kind.spriteDir();
            if (dir == null) continue;
            // The new sets use numbered frames; the legacy sloop uses its own
            // filename map, so only assert numbered frames for the ships/ root.
            if (!dir.contains("/objects/ships/")) continue;
            checked++;
            for (int f = 0; f < 8; f++) {
                File file = new File(dir + f + ".png");
                if (!file.exists()) {
                    missing.append(kind.id()).append("/").append(f).append(".png ");
                    continue;
                }
                // Sanity: the file must decode to a non-empty image.
                try {
                    BufferedImage img = javax.imageio.ImageIO.read(file);
                    if (img == null || img.getWidth() <= 0)
                        missing.append(kind.id()).append("/").append(f).append(".png(unreadable) ");
                } catch (Exception e) {
                    missing.append(kind.id()).append("/").append(f).append(".png(err) ");
                }
            }
        }
        if (checked == 0)
            return ProbeResult.fail(name(), "no kinds use the ships/ art root");
        if (missing.length() > 0)
            return ProbeResult.fail(name(), "missing/bad frames: " + missing);
        return ProbeResult.pass(name(), checked + " art-backed kinds, all 8 frames present");
    }
}
