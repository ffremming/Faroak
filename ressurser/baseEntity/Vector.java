package ressurser.baseEntity;

public class Vector {
    
    double x = 0;
    double y = 0;

    public Vector(){

    }

    public Vector(double x,double y){
        this.x = x;
        this.y = y;
    }

    public void addX(double xVal){
        x+= xVal;
    }

    public void addY(double yVal){
        y+= yVal;
    }

    public void add(Vector other) {
        this.x += other.x;
        this.y += other.y;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double transferX(double value){
        //if x is less than 0
        if (0>x){
            x+= value;
            if (x>0){x = 0;}
            return -value;
            
            
        } else if (x>0){
            x-= value;
            if (x<0){x = 0;}
            return value;
        }
        return 0;
    }

    public double transferY(double value){
        //if x is less than 0
        if (0>y){
            y+= value;
            if (y>0){y = 0;}
            return -value;
            
            
        } else if (y>0){
            y-= value;
            if (y<0){y = 0;}
            return value;
        }
        return 0;
    }

    


    @Override
    public String toString(){
        return "x:"+x+" ,y: "+y;
        
    }

    public void normalize() {
        double hybothenus = Math.sqrt(Math.pow(x,2)+Math.pow(x,2));
        x/=hybothenus;
        y/=hybothenus;
    }

    public void set(double newX,double newY){
        x = newX;
        y = newY;
    }

    public void set(Vector newVector) {
        this.x += newVector.x;
        this.y += newVector.y;
    }
    
    /**changes value to 1/-1 if it is more/less */
    public static double normalize(double value){
        if (value>1){return 1;}
        if (value<-1){return -1;}
        return value;
    }
}
