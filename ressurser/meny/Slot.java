package ressurser.meny;

public abstract class Slot {
    public int indeks = 0;
    
    public Meny menu;
    
    public Slot(Meny menu){
        this.menu = menu;
    }
    public abstract void interact();

    public abstract void exit();

    public abstract String getName();

    public abstract void up();

    public abstract void down();
}
