package resources.domain.combat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.presentation.image.CombatSpriteSheet;

/**
 * Short-lived impact sparkle shown whenever an attack applies damage.
 */
public final class CombatHitEffect extends Entity implements TransientWorldEntity {

    private static final int FRAME_STEP_TICKS = 2;

    private final ArrayList<BufferedImage> frames;
    private int ticks;
    private int frameIndex;
    private boolean expired;

    public CombatHitEffect(GamePanel panel, double centerX, double centerY, int sizePx) {
        super(panel, "combat_hit_fx",
            (int) Math.round(centerX - sizePx / 2.0),
            (int) Math.round(centerY - sizePx / 2.0),
            sizePx, sizePx, sizePx, sizePx, 0, 0);
        this.solid = false;
        this.frames = loadFrames(sizePx);
        this.images = frames;
    }

    @Override
    public void update() {
        if (expired) return;
        ticks++;
        if (ticks % FRAME_STEP_TICKS != 0) return;
        frameIndex++;
        if (frameIndex >= frames.size()) {
            expire();
        }
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>(1);
        out.add(frames.get(Math.min(frameIndex, frames.size() - 1)));
        return out;
    }

    private void expire() {
        if (expired) return;
        expired = true;
        panel.world.addToRemovalQueue(this);
    }

    private static ArrayList<BufferedImage> loadFrames(int sizePx) {
        ArrayList<BufferedImage> fromSheet = CombatSpriteSheet.hitFrames(sizePx);
        if (!fromSheet.isEmpty()) return fromSheet;

        ArrayList<BufferedImage> out = new ArrayList<>(3);
        out.add(fallbackFrame(sizePx, 10, 0));
        out.add(fallbackFrame(sizePx, 18, 4));
        out.add(fallbackFrame(sizePx, 8, 8));
        return out;
    }

    private static BufferedImage fallbackFrame(int sizePx, int starRadius, int sparkSpread) {
        int size = Math.max(16, sizePx);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = size / 2;
        int cy = size / 2;

        g.setColor(new Color(255, 180, 20, 220));
        drawStar(g, cx, cy, starRadius + 4, 6);
        g.setColor(new Color(255, 245, 185, 240));
        drawStar(g, cx, cy, starRadius, 8);

        if (sparkSpread > 0) {
            g.setColor(new Color(255, 150, 0, 210));
            g.fillRect(cx - sparkSpread, cy - (starRadius + sparkSpread), 3, 3);
            g.fillRect(cx + sparkSpread - 2, cy + (starRadius + sparkSpread) - 2, 3, 3);
            g.fillRect(cx + (starRadius + sparkSpread) - 2, cy - sparkSpread, 3, 3);
            g.fillRect(cx - (starRadius + sparkSpread), cy + sparkSpread - 2, 3, 3);
        }
        g.dispose();
        return img;
    }

    private static void drawStar(Graphics2D g, int cx, int cy, int radius, int points) {
        int inner = Math.max(2, radius / 2);
        int n = points * 2;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2.0 + (Math.PI * i / points);
            int r = (i % 2 == 0) ? radius : inner;
            xs[i] = cx + (int) Math.round(Math.cos(a) * r);
            ys[i] = cy + (int) Math.round(Math.sin(a) * r);
        }
        g.fillPolygon(xs, ys, n);
    }
}
