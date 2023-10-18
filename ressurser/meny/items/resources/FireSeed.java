package ressurser.meny.items.resources;

import ressurser.meny.Meny;

public class FireSeed extends Resource{

    public FireSeed(Meny menu, String navn) {
        super(menu, navn);

        addQuantity(100);
        placeable = true;
        toolRequirement = "none";
        type = "farmable";
        objectName = "firePlant";
        tileRequirment = "d";
        object = true;

        
    }

}
