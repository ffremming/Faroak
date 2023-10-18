package ressurser.worldGeneration.dungeonGenerator;

import java.util.ArrayList;
import java.util.Random;

public class Room {

    int startX;
    int startY;
    int width;
    int height;
    Cell [][] roomList;
    Cell [][] list;
    int middleX;
    int middleY;
    Dungeon dungeon;
    ArrayList<Room>  attached = new ArrayList<>();

    public Room(int startX,int startY, int width,int height,Cell [][]list,Dungeon dungeon){
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.list = list;
        this.dungeon = dungeon;
        middleX = startX +Math.round(width/2);
        middleY = startY +Math.round(height/2);
    }

    public boolean isNotColliding(){
        for (int x = startX-2;x<startX+width+2;x++){
            for (int y = startY-2;y<startY+height+2;y++){
                if (!(y< list.length&&y>0  && x< list[0].length && x>0)){
                    return false;
                } else {
                    if(list[y][x].air ||list[y][x].innerWall){
                        return false;
                    }
                }

                
            }
        }
        return true;
    }

    public void placeRoom(){
        for (int x = startX;x<startX+width;x++){
            for (int y = startY;y<startY+height;y++){
                if (y == startY ||x == startX||x == startX+width-1||y == startY + height-1){
                    list[y][x].innerWall = true;
                } else {
                    list[y][x].air = true;
                }
                
                list[y][x].thisRoom = this;
            }
        }
    }

    

    public boolean attachCooridor(){
        Room closest = null;
        int distance = 1000;
        for (Room room:dungeon.roomList){
            if (closest == null){
                closest = room;
            } 
            if (room!= this && room != null &&!room.attached.contains(this)){

                if ((Math.abs(room.middleX - middleX) +(Math.abs(room.middleY - middleY))<distance)){
                    closest = room;
                    distance = (Math.abs(room.middleX - middleX) +(Math.abs(room.middleY - middleY)));
                }
            }
            
        }
        Corridor(middleX,middleY,closest.middleX,closest.middleY,closest);
        attached.add(closest);
        return false;
    }

    void Corridor(int startX,int startY,int sluttX,int sluttY,Room room){
        System.out.println(startX+","+startY+","+sluttX+","+sluttY);
        int x = startX;
        int y = startY;
        boolean thick = true;
        
        boolean xForst = false;
        boolean yForst = false;
       
        if (Math.abs(y-sluttY)< Math.abs(x-sluttX)){
            xForst = true;
        } else {
            yForst = true;
        }

        int teller =0;

        while ( x!= sluttX ||y != sluttY){

            if (xForst){
                while (xForst){

                    if (x<sluttX){
                        x++;
                    }else {
                        x--;
                    }
                    if (x== sluttX||teller == Math.round(startX-sluttX/2)||teller == Math.round(width/2)+2||list[y][x].hasWallNeighbor()){
                        xForst = false;
                    }
                    teller ++;
                    list[y][x].air = true;
                    list[y][x].corridor = true;
                    list[y][x].createAirNeigbors();
                }

                while (y != sluttY){
                    if (y<sluttY){
                        y++;
                    }else {
                        y--;
                    }
                    list[y][x].air = true;
                    list[y][x].corridor = true;
                    list[y][x].createAirNeigbors();
                }
                while (x!= sluttX){
                    if (x<sluttX){
                        x++;
                    }else {
                        x--;
                    }
                    list[y][x].air = true;
                    list[y][x].corridor = true;
                    list[y][x].createAirNeigbors();
                }
            }
            

            if (yForst){
                while (yForst){
                    if (y<sluttY){
                        y++;
                    }else {
                        y--;
                    }
                    if (y== sluttY||teller == Math.round(startY-sluttY/2)||teller == Math.round(height/2)+2||list[y][x].hasWallNeighbor()){
                        yForst = false;
                    }
                    teller ++;
                    list[y][x].air = true;
                    list[y][x].corridor = true;
                    list[y][x].createAirNeigbors();
                }

                while (x != sluttX){
                    if (x<sluttX){
                        x++;
                    }else {
                        x--;
                    }
                    list[y][x].air = true;
                    list[y][x].corridor = true;
                    list[y][x].createAirNeigbors();
                }
                while (y!= sluttY){
                    if (y<sluttY){
                        y++;
                    }else {
                        y--;
                    }
                    list[y][x].air = true;
                    list[y][x].corridor = true;
                    list[y][x].createAirNeigbors();
                }
            }
            }
            



            
            /* 
                for (Cell nb: list[y][x].neightbor){
                    if (nb!= null){
                        if (nb.air = false){
                            nb.air = true;
                            nb.door = true;
                        } else {
                            nb.air = true;
                        }
                        
                    }
                }
                */
            
            
        }
}
