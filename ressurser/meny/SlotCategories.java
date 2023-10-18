package ressurser.meny;



public abstract class SlotCategories extends Slot {
    
    protected Meny menu;
    protected int antSlot;
   
    
    public SlotCategories(Meny menu) {
        super(menu);
       this.menu = menu;
    }

    public abstract void interact();

    public abstract void open();

    public  abstract void exit();
        
    public abstract String getName();
       
    public abstract void up();
       
    public abstract void down();

    public abstract void use();

    public  abstract int getSize();
}
