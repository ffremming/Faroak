package ressurser.worldGeneration.dungeonGenerator;

public class Cell {
    
    int x;
    int y;
    Cell [][] list;
    Cell [] neightbor = new Cell [8] ;
    int amountNeighbors = 0;
    boolean air = false;
    boolean door = false;
    boolean corridor = false;
    boolean outerWall = false;
    boolean innerWall = false;
    Room thisRoom= null;


    public Cell(int x,int y,Cell [][]list){
        this.x = x;
        this.y = y;
        this.list = list;
       
    }

    public void setNeihgbors(){
       int teller = 0;
       if ((!(x<=1) && x< list[0].length-1 &&!(y<=1)&&y<list.length-1)){
        for (int ax = x-1;ax<x+2;ax++){
            for (int ay = y-1;ay<y+2;ay++){teller ++;
                if (list[ay][ax] != null){
                    if (!(ax == x && ay == y)){
                        neightbor[amountNeighbors] = list[ay][ax];
                        amountNeighbors++;
                    }
                }
            }
        }
                        
       }      
       
    }
        
        
    

    public Cell getCellAbove(){
        return list[y-1][x];
    }

    public Cell getCellBelove(){
        return list[y+1][x];
    }

    public Cell getCellRight(){
        return list[y][x+1];
    }

    public Cell getCellLeft(){
        return list[y][x-1];
    }
     public boolean  hasWallNeighbor(){
        for (Cell nb: neightbor){
            if (nb != null){
                if (nb.innerWall){
                    return true;
                }
            }
            
        }return false;
     }

     public void createWalls(){
        for (Cell nb: neightbor){
            if (nb != null){
                if (nb.air == false && nb.innerWall == false){
                    nb.outerWall = true;
                    //nb.wall = true;
                 }
           
                
        }
        }
     }
     public void createAirNeigbors(){
        for (Cell nb: neightbor){
            if (nb != null){
                if (nb.air == false && nb.door == false){
                   nb.air = true;
                }
            }
        }
     }

}
