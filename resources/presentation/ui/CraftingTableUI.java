package resources.presentation.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

import resources.app.GamePanel;
import resources.domain.crafting.CraftingGrid;
import resources.domain.crafting.CraftingService;

/**
 * Composite UI for a crafting workbench:
 *   - 4x4 input grid (delegated to {@link ItemContainer} so it inherits the
 *     left-click swap / right-click split semantics of the player inventory)
 *   - one read-only output cell (custom {@link CraftingOutputSlot})
 *
 * Layout-wise this is a {@link Container} with two children — the grid
 * sub-container and the output slot — positioned side by side. We override
 * mouse dispatch only to ensure the output slot gets click events even though
 * it lives outside the input grid bounds.
 */
public final class CraftingTableUI extends Container {

    private static final int GRID_COLS = CraftingGrid.COLS;
    private static final int GRID_ROWS = CraftingGrid.ROWS;
    private static final int CELL      = 56;
    private static final int GUTTER    = 10;
    private static final int OUTPUT_GAP = 40;

    // Shared chest house style — keeps the crafting panel visually consistent
    // with the chest / barrel / player inventory.
    private static final Color BG = ItemContainer.PANEL_BG;
    private static final Color FG = ItemContainer.PANEL_FG;

    private final ItemContainer inputGrid;
    private final CraftingOutputSlot outputSlot;

    public CraftingTableUI(GamePanel panel, CraftingService service, int x, int y) {
        super(panel, x, y);
        setBackground(BG);
        setForeGround(FG);
        setPadding(12);

        int gridWidth  = GRID_COLS * CELL + (GRID_COLS + 1) * GUTTER;
        int gridHeight = GRID_ROWS * CELL + (GRID_ROWS + 1) * GUTTER;

        this.inputGrid = new ItemContainer(
            panel, GRID_ROWS, GRID_COLS,
            x + padding, y + padding,
            service.grid());
        this.inputGrid.width  = gridWidth;
        this.inputGrid.height = gridHeight;
        this.inputGrid.setPadding(GUTTER);
        add(inputGrid);

        int outX = x + padding + gridWidth + OUTPUT_GAP;
        int outY = y + padding + gridHeight / 2 - CELL / 2;
        this.outputSlot = new CraftingOutputSlot(panel, service, outX, outY, CELL);
        add(outputSlot);

        this.width  = padding * 2 + gridWidth + OUTPUT_GAP + CELL;
        this.height = padding * 2 + gridHeight;
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!visible) return;
        drawRect(g2);
        inputGrid.visible = true;
        inputGrid.draw(g2);
        outputSlot.visible = true;
        outputSlot.drawRect(g2);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = new Point(e.getX(), e.getY());
        if (outputSlot.contains(p)) {
            outputSlot.mousePressed(e);
            return;
        }
        // Delegate everything else to the input grid — it handles cell-level
        // hit-testing against its own slots.
        inputGrid.mousePressed(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        inputGrid.mouseMoved(e);
    }

    /**
     * Hover-tooltip hook: forward to the input grid so the crafting table
     * participates in the same item-name tooltip as the other inventory panels.
     * The output cell is intentionally excluded — its contents are a transient
     * recipe preview, not a real stored item.
     */
    public resources.domain.inventory.Stack stackUnderMouse(int mouseX, int mouseY) {
        return inputGrid.stackUnderMouse(mouseX, mouseY);
    }
}
