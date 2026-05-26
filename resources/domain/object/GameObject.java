package resources.domain.object;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.domain.player.Playable;
import resources.geometry.HitBox;

public class GameObject extends Entity {

    private static final String PREVIEW_SUFFIX = ",preview";

    public GameObject(GamePanel panel, String name, int worldX, int worldY, int width, int height,
                      int hitBoxWidth, int hitBoxHeight, int relativeXPlus, int relativeYPlus, boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPlus, relativeYPlus);
        this.solid = solid;
        getImage();
    }

    public GameObject(GamePanel panel, String name, double worldX, double worldY, int width, int height,
                      HitBox hitBox, boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBox);
        this.solid = solid;
        getImage();
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>(1);
        if (!images.isEmpty()) out.add(images.get(animationIndex));
        return out;
    }

    @Override
    public BufferedImage getImage() {
        try {
            images = panel.imageContainer.getObjectImages(name);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        return images.isEmpty() ? null : images.get(0);
    }

    public void interact(Playable playable) {}

    /** Lightweight clone used when placing an item into the world. */
    public GameObject placementCandidate(GamePanel targetPanel) {
        return cloneWithName(targetPanel, name);
    }

    /** Lightweight clone with preview sprites. */
    public GameObject getPreviewObject(GamePanel targetPanel) {
        return cloneWithName(targetPanel, name + PREVIEW_SUFFIX);
    }

    private GameObject cloneWithName(GamePanel targetPanel, String objectName) {
        int hitW = getHitBox().width;
        int hitH = getHitBox().height;
        int relX = getHitBox().getWorldX() - (int) getWorldX();
        int relY = getHitBox().getWorldY() - (int) getWorldY();
        return new GameObject(targetPanel, objectName, (int) getWorldX(), (int) getWorldY(),
            getWidth(), getHeight(), hitW, hitH, relX, relY, solid);
    }
}
