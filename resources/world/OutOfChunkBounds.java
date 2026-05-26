package resources.world;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.domain.tile.CliffTile;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.generation.factory.EntityFactory;
import resources.domain.object.GameObject;
import resources.domain.player.Moveable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.presentation.camera.Camera;

public class OutOfChunkBounds extends Exception {
    public OutOfChunkBounds(String errorMessage){
        super(errorMessage);
    }
}
