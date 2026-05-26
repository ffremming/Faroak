package ressurser.main;

import java.util.ArrayList;

import ressurser.main.GUIMenu.BaseComponent;

public class UIContainer {
    
    private ArrayList<BaseComponent> UIComponents = new ArrayList<>();


    public void addComponents(BaseComponent newComponent){
        UIComponents.add(newComponent);
    }

    

}
