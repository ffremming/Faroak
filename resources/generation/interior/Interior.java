package resources.generation.interior;

/**
 * Predefined, hand-authored interior layout. Each Interior is a fixed-size
 * char grid; the door's tile-local coordinate is recorded so the world-level
 * portal can be placed regardless of where the interior is anchored.
 *
 * Char legend (see {@link Cell}):
 *   '#' wall (indoor stone wall)
 *   '.' wood floor
 *   ',' stone floor
 *   'D' door tile (floor + portal)
 *   'T' table prop (on wood floor)
 *   'C' chair prop (on wood floor)
 *   'B' crate prop (on wood floor)
 *   ' ' or '_' void (outside the room)
 *
 * Interiors are pure data — no procedural generation. Mutations to the layout
 * happen by editing {@link InteriorRegistry}.
 */
public final class Interior {

    public enum Cell { VOID, WALL, FLOOR_WOOD, FLOOR_STONE, DOOR, TABLE, CHAIR, CRATE }

    private final String name;
    private final int width;
    private final int height;
    private final int doorTx;
    private final int doorTy;
    private final Cell[][] cells; // [ty][tx]

    public Interior(String name, int doorTx, int doorTy, String... rows) {
        this.name = name;
        this.height = rows.length;
        this.width = rows.length == 0 ? 0 : rows[0].length();
        this.doorTx = doorTx;
        this.doorTy = doorTy;
        this.cells = new Cell[height][width];
        for (int y = 0; y < height; y++) {
            String row = rows[y];
            if (row.length() != width) {
                throw new IllegalArgumentException(
                    "Interior '" + name + "' row " + y + " has width " + row.length()
                    + ", expected " + width);
            }
            for (int x = 0; x < width; x++) {
                cells[y][x] = decode(row.charAt(x));
            }
        }
        if (cellAt(doorTx, doorTy) != Cell.DOOR) {
            throw new IllegalArgumentException(
                "Interior '" + name + "' door (" + doorTx + "," + doorTy + ") is not a 'D' cell");
        }
    }

    public String name() { return name; }
    public int width()   { return width; }
    public int height()  { return height; }
    public int doorTx()  { return doorTx; }
    public int doorTy()  { return doorTy; }

    /** Cell at local tile coords; {@link Cell#VOID} if out of bounds. */
    public Cell cellAt(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return Cell.VOID;
        return cells[ty][tx];
    }

    private static Cell decode(char c) {
        switch (c) {
            case '#': return Cell.WALL;
            case '.': return Cell.FLOOR_WOOD;
            case ',': return Cell.FLOOR_STONE;
            case 'D': return Cell.DOOR;
            case 'T': return Cell.TABLE;
            case 'C': return Cell.CHAIR;
            case 'B': return Cell.CRATE;
            case ' ':
            case '_': return Cell.VOID;
            default:
                throw new IllegalArgumentException("Unknown interior cell char: '" + c + "'");
        }
    }
}
