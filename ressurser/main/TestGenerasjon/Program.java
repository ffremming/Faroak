package ressurser.main.TestGenerasjon;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import ressurser.Tiles.TileManager;
import ressurser.main.GamePanel;

import java.io.FileWriter;
import java.io.IOException;

public class Program {

    TileManager tileM;
    GamePanel panel;
    public int maxW;
    public int maxH;

    public Program(GamePanel panel){
        this.panel = panel;
        this.tileM = panel.tileM;
        try {
            main();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }

    public void main() throws IOException {
        // funskjon som tar inn et tekstdokument med bare string verdier. 
        //gjør om dette til en array.

        //funksjon som tar inn en array med bare string verdier
        //returnerer en array tilsvarende med int verdier for hvilken sprite de skal ha.

        //hjelpefunkjson som tar inn en enkel tile, dvs en x og y verdi. 
        //sjekker alle naboer, hvis de har samme string verdi som vårt objekt, påvirker det utfallet.

        //funkjson som tar inn en array med int tall og konverterer det til et tekstdokument.

        //dette skal gjøres med alle kart.
        for (int i = 1;i<3;i++){
            getMap("rawmap"+i+".txt");
        }
    }

    private void getMap(String mapName) throws IOException{
        int[] xy = countXY("ressurser/main/TestGenerasjon/"+mapName);
        String [][] mapL = txtConvertToArray("ressurser/main/TestGenerasjon/"+mapName,xy);
        int [][] mapN = getIntegerArray(xy,mapL);
        
        writeArrayToFile(mapN,xy,mapName);
    }

    private  int[] countXY(String fileName){
        int [] xy  = new int [2];
        int x= 0;
        int y = 0;
        
        File fil = new File(fileName);
        int counter = 0;
        try {
            Scanner scan = new Scanner(fil);

            while (scan.hasNext()){
                String []line = scan.nextLine().split(" ");
                x = line.length;
                counter ++;

            }

            scan.close();
        } catch (FileNotFoundException e){

        }
        y = counter;

        xy[0] = y;
        xy[1] = x;
        maxW = x;
        maxH = y;
        return  xy;
    }

    private String [][] txtConvertToArray(String fileName,int[]xy){

        //oprettter fil,array og counter
        String [][] array = new String [xy[0]][xy[1]];
        
        File fil = new File(fileName);
        int counter = 0;
        try {
            Scanner scan = new Scanner(fil);
            while (scan.hasNext()){
                String []line = scan.nextLine().split(" ");
                
                //loops through every letter in the line. add in array
                int antBokstav = 0;
                for (String bokstav:line){
                    array[counter][antBokstav] = bokstav;
                    antBokstav++;    
                }

                counter ++;
            }
            scan.close();

            
        } catch (FileNotFoundException e){
            System.out.println(e);
        }
        return array;
    }


    //funksjon som tar inn en array med bare string verdier
        //returnerer en array tilsvarende med int verdier for hvilken sprite de skal ha.
    private int[][] getIntegerArray(int[]xy,String[][]mapL){
        int [][] newArray = new int [xy[0]][xy[1]];

        for(int i =0;i <xy[0];i++){
            for(int j =0;j <xy[1];j++){
                newArray[i][j] = getIndexValue(i,j,xy,mapL);
                //System.out.print(mapL[i][j]);
            }
        }

        return newArray;
    }

    private int getIndexValue(int y,int x,int[]xy,String[][]mapL){
        String letter = mapL[y][x];
        int adder = 0;
        if (letter.equals("p")){
            adder = 13;
        }
        int maxWorldRow = xy[0];
        int maxWorldCol = xy[1];

       

        boolean venstreOppe = false;
        boolean venstreMidt = false;
        boolean venstreNede = false;
        boolean mitreOppe = false;
        boolean mitreMidt = false;
        boolean mitreNede= false;
        boolean hoyreOppe = false;
        boolean hoyreMidt= false;
        boolean hoyreNede = false;

        

        
        if (letter.equals("w")||letter.equals("p")){
        for (int i = -1;i<2;i++){
            for (int j = -1;j<= 2;j++){
                if ((y+i >= 0 && y+i < maxWorldRow)&&(x+j >= 0 && x+j < maxWorldCol) && !(i==0&&j==0)){
                   // System.out.println((y+i)+","+(x+j));
                   // System.out.println(mapL[y+i][x+j]);
                    if (mapL[y+i][x+j].equals(letter) ){
                        if (i ==-1){
                            //-1 == oppe (i)
                            if (j == -1){
                                venstreOppe = true;
                            } else if (j == 0){
                                mitreOppe = true;
                            } else if (j == 1){
                                hoyreOppe = true;
                            }

                        } else if (i == 0){
                            // 0 == midt (i)
                            if (j == -1){
                                venstreMidt = true;
                            } else if (j == 1){
                                hoyreMidt = true;
                            }

                        } else if (i ==1){
                            // 1 == nede (i)
                            if (j == 1){
                                hoyreNede = true;
                            } else if (j == 0){
                                mitreNede = true;
                            } else if (j == -1){
                                venstreNede = true;
                            }   
                        }
                    } else {}
                } 
            }
        }
        


        
        //ovre rad
        if (!venstreMidt && !mitreOppe && mitreNede && hoyreMidt && hoyreNede){return 1+adder;}
        if (venstreNede && venstreMidt && !mitreOppe && mitreNede && hoyreMidt && hoyreNede){return 2 + adder;}
        if (venstreNede && venstreMidt && !mitreOppe && mitreNede && !hoyreMidt&&venstreOppe){return 3 + adder;}

        //mitre rad
        if (mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede &&!venstreMidt){return 4+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 5+adder;}
        if (venstreOppe && mitreOppe && !hoyreMidt && mitreNede &&venstreMidt &&venstreNede){return 6+adder;}

        //nedre rad
        if (!venstreMidt && !mitreNede && hoyreMidt &&hoyreOppe&&mitreOppe){return 7+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && !mitreNede && venstreMidt){return 8+adder;}
        if (venstreOppe && mitreOppe && !hoyreMidt && !mitreNede && venstreMidt){return 9+adder;}

        //alene hjørner
        if (!venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 10+adder;}
        if (venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 11+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && !venstreNede){return 12+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && venstreNede){return 13+adder;}


        //new

        if ( mitreOppe && !hoyreMidt && !mitreNede && !venstreMidt){return 14+adder;}
        if ( !mitreOppe && !hoyreMidt && !mitreNede && venstreMidt){return 15+adder;}
        if ( !mitreOppe && !hoyreMidt && mitreNede && !venstreMidt){return 16+adder;}
        if ( !mitreOppe && hoyreMidt && !mitreNede && !venstreMidt){return 17+adder;}

        if ( !mitreOppe && !hoyreMidt && mitreNede && venstreMidt && !venstreNede){return 18+adder;}
        if ( mitreOppe && hoyreMidt && !mitreNede && !venstreMidt && !hoyreOppe){return 19+adder;}
        if ( !mitreOppe && hoyreMidt && mitreNede && !venstreMidt && !hoyreNede){return 20+adder;}
        if ( mitreOppe && !hoyreMidt && !mitreNede && venstreMidt && !venstreOppe){return 21+adder;}
        
        //mitre med hjorne
        if ( mitreOppe && hoyreMidt && !mitreNede && venstreMidt && !hoyreOppe && venstreOppe){return 22+adder;}
        if ( mitreOppe && !hoyreMidt && mitreNede && venstreMidt && !venstreOppe && venstreNede){return 23+adder;}
        if ( mitreOppe && hoyreMidt && mitreNede && !venstreMidt && !hoyreNede && hoyreOppe){return 24+adder;}
        if ( !mitreOppe && hoyreMidt && mitreNede && venstreMidt && !venstreNede && hoyreNede){return 25+adder;}

        if ( !mitreOppe && hoyreMidt && mitreNede && venstreMidt && !hoyreNede&& venstreNede){return 42+adder;}
        if ( mitreOppe && !hoyreMidt && mitreNede && venstreMidt && !venstreNede && venstreOppe){return 43+adder;}
        if ( mitreOppe && hoyreMidt && !mitreNede && venstreMidt && hoyreOppe &&!venstreOppe){return 44+adder;}
        if ( mitreOppe && hoyreMidt && mitreNede && !venstreMidt && !hoyreOppe && hoyreNede){return 45+adder;}


        if (!venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && !mitreNede && venstreMidt ){return 26+adder;}
        if (!venstreOppe && mitreOppe  && !hoyreMidt  && mitreNede && venstreMidt && !venstreNede){return 27+adder;}
        if (  !mitreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && !venstreNede){return 28+adder;}
        if ( mitreOppe && !hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && !venstreMidt){return 29+adder;}

        if (!venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 30+adder;}
        if (!venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && !venstreNede){return 31+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && !venstreNede){return 32+adder;}
        if (venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && venstreNede){return 33+adder;}

        if (!venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && !venstreNede){return 34+adder;}
        if (!venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && venstreNede){return 35+adder;}
        if (venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && !venstreNede){return 36+adder;}
        if (!venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && !venstreNede){return 37+adder;}

        if (!venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && !venstreNede){return 38+adder;}
        if (! mitreOppe&& !hoyreMidt  && !mitreNede && !venstreMidt ){return 39+adder;}

        if (mitreOppe&& !hoyreMidt  && mitreNede && !venstreMidt ){return 40+adder;}
        if (!mitreOppe && hoyreMidt && !mitreNede && venstreMidt ){return 41+adder;}
        



        if (!mitreOppe && venstreMidt && hoyreMidt){return 2 + adder;}

        //if (!venstreMidt && !mitreNede){return 7+adder;}
        //if (!hoyreMidt && !mitreNede){return 9+adder;}

        if (!hoyreMidt && !mitreOppe){return 3+adder;}
        if (!venstreMidt && !mitreOppe){return 1+adder;}
        
        if (venstreOppe && mitreOppe && hoyreOppe && !hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 5+adder;}
        

        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && !hoyreNede && mitreNede && venstreMidt && venstreNede){return 13+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && !venstreNede){return 12+adder;}
        if (venstreOppe && mitreOppe && !hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 11+adder;}
        if (!venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede && venstreMidt && venstreNede){return 10+adder;}

        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && mitreNede &&!venstreMidt &&!venstreNede){return 4+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && !hoyreMidt && !hoyreNede && mitreNede &&venstreMidt &&venstreNede){return 6+adder;}
        if (venstreOppe && mitreOppe && hoyreOppe && hoyreMidt && hoyreNede && !mitreNede && venstreMidt && venstreNede){return 8+adder;}

        //new

        //
        
        if (venstreOppe && mitreOppe && hoyreOppe){
            if (venstreMidt && hoyreMidt){
                if (venstreNede && mitreNede && hoyreMidt){
                    return 5+adder;
                } else{
                    if (!(venstreNede || mitreNede || hoyreNede)){
                        return 8+adder;
                    } else if (!(venstreNede)&&(hoyreNede && mitreNede)){
                        return 12+adder;
                    } else if (!(hoyreNede)&&(venstreNede && mitreNede)){
                        return 13+adder;
                    } else if (!(mitreNede || hoyreNede)){
                        return 8+adder;
                    }else if (!(venstreNede || hoyreNede)){
                        return 8+adder;
                    }else if (!(venstreNede || mitreNede)){
                        return 8+adder;
                    }
                }
            }
        } else if (venstreMidt && hoyreMidt){
            if (!venstreOppe && !mitreOppe && !hoyreOppe){
                return 2+adder;
            } else if (!(venstreOppe)&&(hoyreOppe&&mitreOppe)){
                return 10+adder;
            } else if (!(hoyreOppe)&&(venstreOppe&&mitreOppe)){
                return 11+adder;
            }

        } else if (!hoyreOppe && !hoyreMidt && !hoyreNede){
            return 6+adder;
        }else if (!venstreOppe && !venstreMidt && !venstreNede){
            return 4+adder;
        }else if (!venstreOppe && !venstreMidt && !venstreNede && !venstreOppe && !mitreOppe ){
            return 3+adder;
        } else if (!venstreOppe && !venstreMidt && !venstreNede && !hoyreOppe && !mitreOppe ){
            return 1+adder;
        } else if (!venstreOppe && !venstreMidt && !venstreNede && !hoyreNede && !mitreNede){
            return 7+adder;
        } else if (!hoyreOppe && !hoyreMidt && !hoyreNede && !venstreNede && !mitreNede){
        return 9+adder;
        } else if (!hoyreMidt && !hoyreNede){
            return 6+adder;
        } else if (!hoyreMidt && !hoyreOppe){
            return 6+adder;
        }
    
    } else if (!(letter.equals("g")||letter.equals("f")||letter.equals("S")||letter.equals("null")||letter.equals("sa"))) {

        
       
        return tileM.tileValues.get(getObjValue(x,y,letter,mapL));
    }

    return 0;
    }
       
       

    //funkjson som tar inn en array med int tall og konverterer det til et tekstdokument.
    private static void writeArrayToFile(int [][] array,int[]xy,String mapName) throws IOException{
            String nystreng = mapName.replace("raw","");
        
            File file = new File("ressurser/Tiles/tileMaps/"+nystreng);
            FileWriter writer = new FileWriter(file);
            String space = "";
              
              for (int i = 0; i < xy[0]; i++) {
                for (int j = 0; j<xy[1];j++){
                    if (array[i][j] <10){
                        space = "  ";
                    } else {
                        space = " ";
                    }
                    writer.write(array[i][j] +space);
                }
                writer.write("\n");
                 
              }
              writer.close();
    }
    
    
        private String getObjValue(int x,int y,String type,String[][]mapL){
    
            //return the objValue of an objekt. can be used to updating.
            
            boolean oppe = false;
            boolean hoyre = false;
            boolean nede = false;
            boolean venstre = false;
    
            //if (isObject("opp")){
            
                if (y-1>=0 &&y-1 <499){
                    if (mapL[y-1][x].equals(type)){ oppe = true;}
                }
                if (y+1>=0 &&y+1 <499){
                    if (mapL[y+1][x].equals(type)){ nede = true;}
                }
                if (x-1>=0 &&x-1 <499){
                    if (mapL[y][x-1].equals(type)){ venstre = true;}
                }
                if (x+1>=0 &&x+1 <499){
                    if (mapL[y][x+1].equals(type)){ hoyre = true;}
                }

            
                
                
               
            
           
            
            
            //}
            
             //11 alternativer
            if (!oppe && hoyre && nede && !venstre){return type +1;}
    
            if (!oppe && hoyre && nede && venstre){return type +2;}
    
            if (!oppe && !hoyre && nede && venstre){return type +3;}
    
            if (oppe && hoyre && nede && !venstre){return type +4;}
    
            if (oppe && hoyre && nede && venstre){return type +5;}
    
            if (oppe && !hoyre && nede && venstre){return type +6;}
    
            if (oppe && hoyre && !nede && !venstre){return type +7;}
    
            if (oppe && hoyre && !nede && venstre){return type +8;}
    
            if (oppe && !hoyre && !nede && venstre){return type +9;}
    
            if (oppe && !hoyre && nede && !venstre){return type +":";}
            if (oppe && !hoyre && !nede && !venstre){return type +";";}
            if (!oppe && !hoyre && nede && !venstre){return type +"_";}
    
            if (!oppe && !hoyre && !nede && !venstre){return type +"*";}
            if (!oppe && hoyre && !nede && !venstre){return type +",";}
            if (!oppe && !hoyre && !nede && venstre){return type +"-";}
            if (!oppe && hoyre && !nede && venstre){return type + ".";}
    
            //returnerer null hvis ingen tilfeller stemmer
            return null;
    
    
    
    
    
            
    
                
            
        }
    }
