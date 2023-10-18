package ressurser.meny;

public abstract class SlotSubCategories extends Slot {

    public SlotSubCategories(Meny menu) {
        super(menu);

    }

    public abstract void interact();
    
    public abstract void exit();

    public abstract String getName();
    
    public abstract void up();

    public abstract void down();

    
}