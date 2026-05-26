package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.tile.Tile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.inventory.ItemManager;

import java.util.ArrayList;

import resources.presentation.ui.BaseComponent;

public class UIContainer {
    
    private ArrayList<BaseComponent> UIComponents = new ArrayList<>();


    public void addComponents(BaseComponent newComponent){
        UIComponents.add(newComponent);
    }

    

}
