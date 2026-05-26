package resources.generation.biome;

/**
 * One spawn entry for a biome: which object asset to drop, how often, and how big.
 * Object name must match a folder under resources/images/objects/.
 */
public class VegetationRule {

    public final String objectName;
    public final double density;
    public final int width;
    public final int height;
    public final int hitBoxWidth;
    public final int hitBoxHeight;
    public final boolean solid;

    public VegetationRule(String objectName, double density,
                          int width, int height,
                          int hitBoxWidth, int hitBoxHeight,
                          boolean solid) {
        this.objectName   = objectName;
        this.density      = density;
        this.width        = width;
        this.height       = height;
        this.hitBoxWidth  = hitBoxWidth;
        this.hitBoxHeight = hitBoxHeight;
        this.solid        = solid;
    }
}
