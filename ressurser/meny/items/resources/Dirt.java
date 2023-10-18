package ressurser.meny.items.resources;

import ressurser.meny.Meny;

public class Dirt extends Resource {

    public Dirt(Meny menu, String navn) {
        super(menu, navn);
        addQuantity(100);
        placeable = true;
        toolRequirement = "hoe";
        type = "d";
        tile = true;
        //TODO Auto-generated constructor stub
    }

    @Override
    protected String getInformationText() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInformationText'");
    }

    @Override
    public void interact() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'interact'");
    }
}
