package ressurser.worldGeneration.dungeonGenerator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.lang.Math;

import javax.imageio.ImageIO;

public class Dungeon {

    int height;
    int width;
    int amountRooms;
    int currentRooms = 0;
    Random rand = new Random();
    Cell [][] dungeon;
    Room [] roomList;
    public static int numberCounter = 0;
    public int  number;
   

    public Dungeon(int height,int width,int amountRooms){
        this.height = height;
        this.width = width;
        this.amountRooms = amountRooms;
        dungeon = new Cell [height][width];
        roomList = new Room[amountRooms] ;
        fillDungeon();
        setNeihgbors();
        generateRooms();
        generateCorridors();
        
        number = numberCounter;
        numberCounter ++;
        writeToFile();

        
        

    }

    private void setNeihgbors(){
        for (int x = 0;x<width;x++){
            for (int y = 0;y<height;y++){
               dungeon[y][x].setNeihgbors();
            }
        }
    }

    private void generateCorridors(){
        for (Room room :roomList ){
            if (room != null){
                room.attachCooridor();
            }
            
        }
        Room startRoom = roomList[0] ;

        if (amountRooms>2){
            roomList[0].Corridor(startRoom.middleX,startRoom.middleY,roomList[currentRooms-1].middleX,roomList[currentRooms-1].middleY,roomList[currentRooms-1]);
            //roomList[0].attachCooridor();
            roomList[0].Corridor(startRoom.middleX,startRoom.middleY,roomList[currentRooms-2].middleX,roomList[currentRooms-2].middleY,roomList[currentRooms-2]);
        }
        



        //doors
        for (int x = 0;x<width;x++){
            for (int y = 0;y<height;y++){
                if (dungeon[y][x].corridor){
                    
                    if (dungeon[y][x].outerWall){
                        
                       
                        dungeon[y][x].outerWall = false;
                        dungeon[y][x].air = true;
                        dungeon[y][x].corridor = true;
                        
                    }
                    if (dungeon[y][x].innerWall){
                        
                       
                        dungeon[y][x].innerWall = false;
                        dungeon[y][x].door = true;
                        dungeon[y][x].corridor = true;
                        dungeon[y][x].air = true;
                        
                    }
                }
            }
        }
        

        for (int x = 0;x<width;x++){
            for (int y = 0;y<height;y++){
                if(dungeon[y][x].air){
                   
                    dungeon[y][x].createWalls();
                    
                    
                }
            }
        }
    }

    private void fillDungeon(){
        for (int x = 0;x<width;x++){
            for (int y = 0;y<height;y++){
                dungeon[y][x] = new Cell(x,y,dungeon);
            }
        }
    }


    private void generateRooms(){
        CreateStartRoom();
        int teller = 1000;
        while (currentRooms<amountRooms && teller>0){
            if (createRoom()){
                currentRooms++;
            } else {
                teller--;
            }
        }
    }
    private void CreateStartRoom(){
        int roomWidth = 10;
        int roomHeight = 10;
        int x = 20;
        int y = 20;

        Room rom = new Room(x,y,roomWidth,roomHeight,dungeon,this);
        rom.placeRoom();
        roomList[currentRooms] = rom;
        currentRooms++;        
       
    }

    private boolean createRoom(){
        int x = rand.nextInt(5,width-25);
        int y = rand.nextInt(5,height-25);

        int width = rand.nextInt(4,24);
        int height = rand.nextInt(4,24);


        Room rom = new Room(x,y,width,height,dungeon,this);
        if (rom.isNotColliding()){
            rom.placeRoom();
            roomList[currentRooms] = rom;
            return true;
        } else {
            return false;
        }
    }

    public int getColor(int x,int y){
        if (dungeon[y][x].outerWall){
            return 0x232999;
        }
        else if (dungeon[y][x].innerWall){
            return 0x336960;

        }
         else if (dungeon[y][x].door){
            return 0x9999400;

        } else if (dungeon[y][x].air){
            if (dungeon[y][x].thisRoom != null){
                return 0x507483;
            }
            else {
                return 0x937483;
            }
            

        } else if (dungeon[y][x] == null){
            return 0x999999;

        
        } else {
            return 0x000000;
        }
    }

    public String getLetter(int x,int y){
        if (dungeon[y][x].outerWall){
            return "W";
        }
        else if (dungeon[y][x].innerWall){
            return "W2";

        } else if (dungeon[y][x].door){
            return "D";

        } else if (dungeon[y][x].thisRoom!= null){
            return "F";
        } else if (dungeon[y][x].air){
            return "F";
        

        
        } else {
            return "0";
        }
    }

    public String getObjLetter(int x,int y){
        if (x == 22 && y == 22){
            return "cave1,ladderBack,0,0,1,1";
        }
        if (dungeon[y][x].air){
            if (Math.random()<0.001){return "cave1,dungeonTeleport,0,0,1,1";}
        }
        return "0";

    }
    
    public void writeToFile(){
        PrintWriter writer;
    FileWriter writerobj;
    File file = new File("ressurser/Tiles/tileMaps/tm,1-"+number+".txt");
    File fileObj = new File("ressurser/objects/objectTxtDocuments/tm,1-"+number+".txt");
    
    try{
        writer = new PrintWriter(file);
        writerobj = new FileWriter(fileObj);

        int rgb;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++){

                    rgb = getColor(x,y);
                    String letter = getLetter(x,y);
                    writer.write (letter);
                    writer.write(" ");


                    writerobj.write(getObjLetter(x,y));
                    writerobj.write(" ");
                    
                    image.setRGB(x, y, rgb);

                    //needs an objectmaker()  !!!
            }
            if (y+1<height){
                writer.write("\n");
                writerobj.write("\n");
            }
            ;
        }
        writer.flush();
        writerobj.flush();
        
        ImageIO.write(image, "png", new File("ressurser/worldGeneration/dungeonGenerator/dungeon"+number+".png"));
        
    } catch (IOException e){
       
        e.printStackTrace();
    }

    }
    
    
}


