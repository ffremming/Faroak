package resources.geometry;

import resources.domain.entity.BaseEntity;

public class Vector {
    
    public double x = 0;
    public double y = 0;

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

    public void remove(Vector other){
        this.x -= other.x;
        this.y -= other.y;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public Vector transfer(double transferValue){
        
        
        Vector vector =  normalize(transferValue);
        remove(vector);
        pacify(transferValue);
        return vector;

    }

    public double transferX(double value){

        double magnitude  = Math.sqrt(x * x + y * y);
        double val = value*x/magnitude;
        //if x is less than 0
        if (0>x){
            if (val<1 && val>-1){x = 0;val = 0;}
            x+= val;
            if (x>0){x = 0;}
            return -val;
            
            
        } else if (x>0){
            if (val<1 && val>-1){x = 0;val = 0;}
            x-= val;
            if (x<0){x = 0;}
            return val;
        }
        return 0;
    }

    public double transferY(double value){

        double magnitude = Math.sqrt(x * x + y * y);

        double val = value*y/magnitude;

        //if x is less than 0
        if (0>y){
            if (val<1 && val>-1){y = 0;val = 0;}
            y+= val;
            
            if (y>0){y = 0;}
            return -val;
            
            
        } else if (y>0){
            if (val<1 && val>-1){y = 0;val = 0;}
            y-= val;

            if (y<0){y = 0;}
            return val;
        }
        return 0;
    }

    /**transfers x and y in an array given [x,y] */
    public double[] transferValues(double value){

        double magnitude = Math.sqrt(x * x + y * y);

        double yValue = 0;
        double xValue = 0;

        //if x is less than 0
        if (0>y){
            
            y+= (value)+1;
            
            if (y>0){y = 0;}
            yValue = -(value)/magnitude;
            
            
        } else if (y>0){
            y-= (value)+1;
            if (y<0){y = 0;}
            yValue = (value)/magnitude;
        }
        


        if (0>x){
            
            x+= (value);
            if (x>0){x = 0;}
            xValue = -(value)/magnitude;
            
            
        } else if (x>0){
            x-= (value);
            if (x<0){x = 0;}
            xValue =  (value)/magnitude;
        }

        return new double[]{xValue,yValue};
        
    }

    /**transfers x and y in an array given [x,y] */
    public double[] transferPathValues(double value){

        double magnitude = Math.sqrt(x * x + y * y);

        double yValue = 0;
        double xValue = 0;

        //if x is less than 0
        if (0>y){
            y+= (x*value)/magnitude;
            
            if (y>0){y = 0;}
            yValue = -(y*value)/magnitude;
            
            
        } else if (y>0){
            y-= (x*value)/magnitude;
            if (y<0){y = 0;}
            yValue = (y*value)/magnitude;
        }
        


        if (0>x){
            
            x+= (x*value)/magnitude;
            if (x>0){x = 0;}
            xValue = -(x*value)/magnitude;
            
            
        } else if (x>0){
            x-= (x*value)/magnitude;
            if (x<0){x = 0;}
            xValue =  (x*value)/magnitude;
        }

        return new double[]{xValue,yValue};
        
    }


    


    @Override
    public String toString(){
        return "x:"+x+" ,y: "+y;
        
    }

    public Vector normalize(double transferValue) {
        double magnitude = Math.sqrt(x * x + y * y);
        if (magnitude != 0) {
            //x-=xValue*5 / magnitude;y-= yValue*5 / magnitude;
            return new Vector(x*transferValue / magnitude, y*transferValue / magnitude);
        } else {
            return new Vector(); // Return a zero vector if magnitude is zero to avoid division by zero
        }
    }

    public void set(double newX,double newY){
        x = newX;
        y = newY;
    }

    public void set(Vector newVector) {
        this.x =newVector.x;
        this.y = newVector.y;
    }
    
    /**changes value to 1/-1 if it is more/less */
    public static double normalizeValue(double value){
        if (value>1){return 1;}
        if (value<-1){return -1;}
        return value;
    }

    public boolean hasNoVelocity() {
        return (x ==0 && y == 0);
    }

    public Vector normalize(double xValue, double yValue) {

        double magnitude = Math.sqrt(xValue * xValue + yValue * yValue);

       
        if (magnitude != 0) {
            //if (pacify(5)){ new Vector();}
            
            return new Vector(xValue / magnitude, yValue / magnitude);
        } else {
            return new Vector(); // Return a zero vector if magnitude is zero to avoid division by zero
        }
    }

    /**if the values are too low, values are turned down to 0. */
    public boolean pacify(double value){
        if (x<value && x>-value){x = 0; if (y<value && y>-value){y = 0;} return true;}
        if (y<value && y>-value){y = 0; return true;}
        return false;
       
    }

    public Vector copy() {
        return new Vector(x,y);
    }
}
