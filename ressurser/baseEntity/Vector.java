package ressurser.baseEntity;

public class Vector {
    
    int x = 0;
    int y = 0;

    public Vector(){

    }

    public void addX(int xVal){
        x+= xVal;
    }

    public void addY(int yVal){
        y+= yVal;
    }

    public void add(Vector other) {
        this.x += other.x;
        this.y += other.y;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public int transferX(int value){
        if (value>x){
            int movement = value-(value-x);
            x = 0;
            return movement;
        } else{
            x-= value;
            return value;
        }
    }

    public int transferY(int value){
        if (value>y){
            int movement = value-(value-y);
            y = 0;
            return movement;
        } else{
            y-= value;
            return value;
        }
    }
    
}
