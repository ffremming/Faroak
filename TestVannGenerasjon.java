import javax.imageio.ImageIO;

public class TestVannGenerasjon {

    public static void main(String[] args) {
        test(2,2);
    }
    public static  void test(int var,int variabel) {

        int [][] array = new int [4][4];
        ImageIO bilde;
        int maxWorldRow = 4;
        int maxWorldCol = 4;

        boolean venstreOppe;
        boolean venstreMidt;
        boolean venstreNede;
        boolean mitreOppe;
        boolean mitreMidt;
        boolean mitreNede;
        boolean hoyreOppe;
        boolean hoyreMidt;
        boolean hoyreNede;


        int verdi = 5;
        array[2][2]= verdi;
        int x = 2;int y = 2;

        for (int i = -1;i<2;i++){
            for (int j = -1;j<= 2;j++){
                if ((x+i >= 0 && x+i <= maxWorldRow)&&(y+i >= 0 && y+i <= maxWorldCol) && !(i==0&&j==0)){
                    if (array[x+i][y+j] == 0){
                        if (i ==-1){
                            if (j == -1){
                                venstreOppe = true;
                            } else if (j == 0){
                                mitreOppe = true;
                            } else {
                                hoyreOppe = true;
                            }

                        } else if (i == 0){
                            if (j == -1){
                                venstreMidt = true;
                            } else {
                                hoyreMidt = true;
                            }

                        } else {if (j == -1){
                            venstreNede = true;
                            } else if (j == 0){
                            mitreNede = true;
                            } else {
                            hoyreNede = true;
                            }   
                        }
                    }
                } 
            }
        }
        
        if (venstreOppe&&venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreMidt&&hoyreNede){  //rent vann
            bilde = ImageIO.read(getClass().getResourceAsStream("newWater1.png"));


        } else if (venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreMidt&&hoyreNede){ //venstre top mangler
            bilde = ImageIO.read(getClass().getResourceAsStream("waterCornerLU.png"));
        } else if (venstreOppe&&venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&mitreNede&hoyreMidt&&hoyreNede){ //hoyre mangler
            bilde = ImageIO.read(getClass().getResourceAsStream("waterCornerRU.png"));
        } else if (venstreOppe&&venstreMidt&&mitreOppe&&mitreMidt&&mitreNede&hoyreMidt&&hoyreNede&&hoyreOppe){ //venstre bunn mangler
            bilde = ImageIO.read(getClass().getResourceAsStream("waterCornerLL.png"));
        } else if (venstreOppe&&venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreMidt){   //hoyrebunn mangler
            bilde = ImageIO.read(getClass().getResourceAsStream("waterCornerRL.png"));



        }else if (venstreOppe&&venstreNede&&venstreMidt&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreMidt&&hoyreNede){  //midten oppe
            bilde = ImageIO.read(getClass().getResourceAsStream("waterUpper.png"));
        }else if (venstreOppe&&venstreNede&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreMidt&&hoyreNede&&mitreOppe){  //venstre midten
            bilde = ImageIO.read(getClass().getResourceAsStream("waterLeftSide.png"));
        } else if (venstreOppe&&venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreNede){  //hoyre midt
            bilde = ImageIO.read(getClass().getResourceAsStream("waterLeftMiddle.png"));
        } else if (venstreOppe&&venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&hoyreOppe&&hoyreMidt&&hoyreNede){  //nede midten
            bilde = ImageIO.read(getClass().getResourceAsStream("waterLowerMiddle.png"));
        
        } else if(venstreOppe&&venstreNede&&venstreMidt&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreNede){  // hjorne hoyre oppe - to gress
            bilde = ImageIO.read(getClass().getResourceAsStream("waterUpperRight.png"));
        } else if(venstreOppe&&venstreNede&&mitreMidt&&mitreNede&&hoyreOppe&&hoyreMidt&&hoyreNede){ // hjorne venstre oppe - to gress
            bilde = ImageIO.read(getClass().getResourceAsStream("waterUpperLeft1.png"));
        } else if(venstreOppe&&venstreNede&&mitreOppe&&mitreMidt&&hoyreOppe&&hoyreMidt&&hoyreNede){  // hjorne venstre nede - to gress
            bilde = ImageIO.read(getClass().getResourceAsStream("waterLowerLeft.png"));
        } else if(venstreOppe&&venstreNede&&venstreMidt&&mitreOppe&&mitreMidt&&hoyreOppe&&hoyreNede){  // hjorne hoyre nedde - to gress
            bilde = ImageIO.read(getClass().getResourceAsStream("waterLowerRight.png"));
        
        } else {
            bilde = ImageIO.read(getClass().getResourceAsStream("newWater2.png"));
        }
        return bilde;
        


    }
}
