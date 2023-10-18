package ressurser.meny.items.materials;

import ressurser.meny.Meny;
import ressurser.meny.items.Item;

public class BFence extends Material {

    

    public BFence(Meny menu, String navn) {
        super(menu, navn);
        //TODO Auto-generated constructor stub
        addQuantity(3000);
        type = "materialbrownFence";
        //navn = "brownFence";
        
        toolRequirement = "hammer";
    }


    

    


}
