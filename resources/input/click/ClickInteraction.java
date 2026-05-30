package resources.input.click;

import java.awt.Point;

import resources.app.GamePanel;

/**
 * One handler in the left-click chain of responsibility. Each strategy inspects
 * the click (world point + equipped item via the panel) and either consumes it
 * (returns {@code true}) or passes it to the next strategy.
 *
 * The chain replaces the old hard-coded if-ladder in {@link resources.input.Mouse}
 * (board boat → open chest → open table → place/harvest). New click behaviours
 * plug in by adding a strategy to {@link ClickRouter}'s ordered list instead of
 * editing the dispatch site.
 */
@FunctionalInterface
public interface ClickInteraction {

    /**
     * Attempt to handle a left-click at {@code worldPoint}.
     * @return true if this strategy consumed the click (stop the chain).
     */
    boolean handle(GamePanel panel, Point worldPoint);
}
