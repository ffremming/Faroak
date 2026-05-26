package resources.domain.entity;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Anything the camera can paint. The camera/renderer only needs the image stack
 * and dimensions; concrete entity shape, animation state, and lifecycle stay
 * private behind the implementation.
 */
public interface Drawable {
    ArrayList<BufferedImage> getImages();
    double getWorldX();
    double getWorldY();
    int getWidth();
    int getHeight();
}
