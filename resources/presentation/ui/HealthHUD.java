package resources.presentation.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import resources.app.GamePanel;
import resources.domain.entity.component.HealthComponent;
import resources.domain.player.Playable;

/**
 * Top-left HP bar overlay. Reads the player's {@link HealthComponent} each
 * frame and paints a red-fill / dark-frame bar plus a numeric "current/max"
 * label. Pure presentation — no state of its own.
 *
 * When the player is dead, swaps to a centered "YOU DIED — press ESC" banner
 * so the player can find their way back to the respawn button.
 */
public final class HealthHUD {

    private static final Font HUD_FONT   = new Font("Arial", Font.BOLD, 14);
    private static final Font DEATH_FONT = new Font("Arial", Font.BOLD, 28);

    private static final int BAR_X      = 16;
    private static final int BAR_Y      = 16;
    private static final int BAR_W      = 200;
    private static final int BAR_H      = 18;

    private static final Color FILL_BG   = new Color(40, 0, 0, 200);
    private static final Color FILL_FG   = new Color(210, 40, 40, 230);
    private static final Color FRAME     = new Color(0, 0, 0, 220);
    private static final Color LABEL     = Color.WHITE;
    private static final Color DEAD_TINT = new Color(40, 0, 0, 160);
    private static final Color DEAD_TEXT = new Color(255, 220, 220);

    private final GamePanel panel;

    public HealthHUD(GamePanel panel) {
        this.panel = panel;
    }

    public void draw(Graphics2D g2) {
        Playable player = panel.player();
        if (player == null) return;
        HealthComponent hc = player.getComponent(HealthComponent.class);
        if (hc == null) return;

        drawBar(g2, hc);
        if (hc.isDead()) drawDeathBanner(g2);
    }

    private void drawBar(Graphics2D g2, HealthComponent hc) {
        double fraction = hc.max() == 0 ? 0 : (double) hc.current() / hc.max();
        int fillW = (int) Math.round(BAR_W * Math.max(0.0, Math.min(1.0, fraction)));

        g2.setColor(FILL_BG);
        g2.fillRoundRect(BAR_X, BAR_Y, BAR_W, BAR_H, 6, 6);
        g2.setColor(FILL_FG);
        g2.fillRoundRect(BAR_X, BAR_Y, fillW, BAR_H, 6, 6);
        g2.setColor(FRAME);
        g2.drawRoundRect(BAR_X, BAR_Y, BAR_W, BAR_H, 6, 6);

        g2.setFont(HUD_FONT);
        g2.setColor(LABEL);
        String label = "HP " + hc.current() + " / " + hc.max();
        g2.drawString(label, BAR_X + 8, BAR_Y + BAR_H - 4);
    }

    private void drawDeathBanner(Graphics2D g2) {
        int w = panel.width;
        int h = panel.height;
        g2.setColor(DEAD_TINT);
        g2.fillRect(0, 0, w, h);
        g2.setFont(DEATH_FONT);
        g2.setColor(DEAD_TEXT);
        String line1 = "YOU DIED";
        String line2 = "Press ESC and choose Respawn";
        g2.drawString(line1, w / 2 - g2.getFontMetrics().stringWidth(line1) / 2, h / 2 - 20);
        g2.setFont(HUD_FONT);
        g2.drawString(line2, w / 2 - g2.getFontMetrics().stringWidth(line2) / 2, h / 2 + 16);
    }
}
