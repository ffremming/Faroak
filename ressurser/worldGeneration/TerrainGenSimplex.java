package ressurser.worldGeneration;
/*
 * OpenSimplex2S Noise sample class.
 */

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.lang.Math;
import java.time.LocalTime;
import java.util.HashMap;
import static java.time.temporal.ChronoUnit.MILLIS;


public class TerrainGenSimplex
{
	private  int WIDTH ;
	private  int HEIGHT;

	private  double [][] heightMap ;
	private  double [][] temperatureMap ;
	private  double [][] rainMap ;
	private  double [][] vegetationMap ;
	private  double [][] riverMap ;


	public String [][] biomeMap ;

	HashMap<String,String> biomeString = new HashMap<>();
	

	public TerrainGenSimplex(int height,int width,boolean generateNew){
		WIDTH = width;
		HEIGHT = height;
		if (generateNew){
			generateMap();
		}
	}





	public void generateMap(){
		System.out.println("started map generation");
		LocalTime lt1 = LocalTime.now();
		

		setup();

		try {
			getHeightMap();
			getRainMap();
			getTemperatureMap();
			getVegetationMap();
			getRiverMap();

			paintMap();
			//paintCaveMap();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		LocalTime lt2 = LocalTime.now();
        System.out.println("completed map generation: time used: "+MILLIS.between(lt1,lt2)+" ms");
	}


	private void setupBiomeHashMap(){

		biomeString.put("snowyTaiga","snow");
		biomeString.put("snowyForest","snow");
		

		biomeString.put("icyPlains","ice");
		biomeString.put("coldForest","Tundra");
		biomeString.put("coldSteps","Tundra");

		biomeString.put("swamp","snow");
		biomeString.put("darkForest","dG");
		biomeString.put("plains","g");
		biomeString.put("plains","Tundra");


		biomeString.put("rainForest","dG");
		biomeString.put("jungel","dG");
		biomeString.put("savanna","Sa");
		biomeString.put("dessert","s");
	}

	private void  setup(){
		biomeMap = new String [HEIGHT][WIDTH];

		heightMap = new double [HEIGHT][WIDTH];
		temperatureMap = new double [HEIGHT][WIDTH];
		rainMap = new double [HEIGHT][WIDTH];
		
		vegetationMap = new double [HEIGHT][WIDTH];
		riverMap = new double [HEIGHT][WIDTH];
		

	}
	


	private  final long SEED = 800;
	
	private    double FREQUENCY = 0.1 / 24.0;

	private double XFREQ = 0.001 / 24.0;


	private int green = 0xA0C080;
	private int darkGreen = 0x408000;
	private int sand = 0xffe7c3;
	private int moss = 0x4E4E35;
	private int water = 0x0000ff;;
	private int mountain =0x808080 ;
	private int ice = 0xBFEFFF;
	private int savanna = 0xCDBE70;
	private int sForest = 0x629632;
	private int rainFr = 0x78AB46;
	private int black =  0x000000;
	


	

	double oceanLvl = -0.6;
	double beachLvl = -0.6;
	double moantaintLvl = 0.9;
	


	//temperatur zones:



	// snowy tundra
	double sTundraTmpMax = -0.5;
	double sTundraTmpMin = -1;
	double sTundraRMax = 0;
	double sTundraRMin = -1;


	//plains
	double plainsTmpMax = 0.3;
	double plainsTmpMin = -0.5;
	double plainsRMax = 0;
	double plainsRMin = -1;


	//desert
	double desertTmpMax = 1;
	double desertTmpMin = 0.3;
	double desertRMax = -0.5;
	double desertRMin = -1;

	//snowy Taiga
	double sTaigaTmpMax = -0.5;
	double sTaigaTmpMin = -1;
	double sTaigaRMax = 1;
	double sTaigaRMin = 0;

	//seasonal forest
	double sForestTmpMax = 0;
	double sForestTmpMin = -0.5;
	double sForestRMax = 1;
	double sForestRMin = 0;

	//savanna
	double savannaTmpMax = 1;
	double savannaTmpMin = 0;
	double savannaRMax = 0;
	double savannaRMin = -0.5;

	//forest
	double forestTmpMax = 1;
	double forestTmpMin = 0;
	double forestRMax = 0.5;
	double forestRMin = 0;

	//swamp
	double swampTmpMax = 0.5;
	double swampTmpMin = 0;
	double swampRMax = 1;
	double swampRMin = 0.5;

	//rain forest
	double rainFTmpMax = 1;
	double rainFTmpMin = 0.3;
	double rainFRMax = 1;
	double rainFRMin = 0.5;

	

	HashMap<String,HashMap<String,String>> biomes = new HashMap<>();
	

	//if landType = ocean:
		//if tempZone = freeqing:
			//biome = free<ing ocean
	
		// cold ocean
		
		//normal ocean

		//warm ocean:

	
	//if lyndtype = beach:
		
		
		

	//return hot, normal cold, freezing
	public String getTemperatureZone(double temp){
		if (temp<-0.7){
			return "freezing";
		} else if (temp < -0.3){
			return "cold";
		} else if (temp < 0.5){
			return "normal";
		} else {
			return "hot";
		}
	}

	//return wet, normal dry, arid
	public String getHumidityZone(double humidity){
		if (humidity<-0.7){
			return "arid";
		} else if (humidity < -0.3){
			return "dry";
		} else if (humidity < 0.5){
			return "normal";
		} else {
			return "hot";
		}
	}


	public void getHeightMap(){
		long seed = SEED;
		double frequency = FREQUENCY;


				for (int y = 0; y < HEIGHT; y++)
				{
					for (int x = 0; x < WIDTH; x++)
					{	
						double value = getNoiseValue(SEED,frequency,x,y)* (Math.pow(getHeightFactor(x,y,100,XFREQ)+1,3) );
						heightMap[y][x] = value;
			}
		}
	}  

	public void getHeightMap2(){
		long seed = SEED;
		double frequency = FREQUENCY;


				for (int y = -HEIGHT/2; y < HEIGHT/2; y++)
				{
					for (int x = -WIDTH/2; x < WIDTH/2; x++)
					{	
						double value = getNoiseValue(SEED,frequency,x,y)* (Math.pow(getHeightFactor(x,y,100,XFREQ)+1,3) );
						heightMap[y+HEIGHT/2][x+WIDTH/2] = value;
			}
		}
	}  
	
	private  double getHeight(int x,int y,int seed,double frequency){
		return getNoiseValue(seed,frequency,x,y);
	}
	private double getmoist(int x,int y,int seed,double frequency){
		return getNoiseValue(seed,frequency,x,y);
	}
	private double getTemperature(int x,int y,int seed,float frequency){
		return getNoiseValue(seed,frequency,x,y);
	}
	private double getVegetation(int x,int y,int seed,double frequency){
		return getNoiseValue(seed,frequency,x,y);
	}

	private double getHeightFactor(int x,int y,int seed,double frequency){
		return getNoiseValue(seed,frequency,x,y);
	}

	
	private double getNoice(int x,int y,int seed,double frequency){
		return getNoiseValue(seed,frequency,x,y);
	}




	private double[][] getNoiseArray(long seed,double frequency,int width,int height){
		double [][] newNoiseArray = new double [height][width];

		for (int y = 0; y < height; y++){
			for (int x = 0; x < width; x++){	

				double value = sumOcatave(seed,6,x,y,0,frequency);
				//double value = (getNoiseValueAbs(seed,frequency,x,y));
				//double value = getNoiseValue(seed,frequency,x,y);
				newNoiseArray[y][x] = value;
			}
		}
		return newNoiseArray;
	}    
	
	private double[][] getNoiseArrayAbs2(long seed,double frequency,int height,int width,int num_iterations){
		double [][] newNoiseArray = new double [height][width];

		for (int y = 0; y < height; y++){
			for (int x = 0; x < width; x++){	

				//double value = Math.abs(sumOcatave(seed,6,x,y,0,frequency));
				//double value = getNoiseValue(seed,frequency,x,y);
				
				double value = Math.abs(sumOcatave(seed,num_iterations,x,y,0,frequency));
				newNoiseArray[y][x] = value;
			}
		}
		return newNoiseArray;
	}  

	private double[][] getNoiseArrayAbs(long seed,double frequency,int height,int width,int num_iterations){
		double [][] newNoiseArray = new double [height][width];

		for (int y = 0; y < height; y++){
			for (int x = 0; x < width; x++){	

				
				double value = Math.abs(sumOcatave(seed,num_iterations,x-width/2,y-height/2,0,frequency));
				newNoiseArray[y][x] = value;
			}
		}
		return newNoiseArray;
	}  
	

	private double getNoiseValue(long seed,double frequency,int x,int y){
		double value = OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY, y * FREQUENCY);
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*2, y * FREQUENCY*2)*0.5;
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*4, y * FREQUENCY*4)*0.25;
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*8, y * FREQUENCY*8)*0.125;
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*16, y * FREQUENCY*16)*0.06;
		value = (value /(1 + 0.5 + 0.25+0.125+0.06));
		return value;
	}

	private double getNoiseValueAbs(long seed,double frequency,int x,int y){
		double value = OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY, y * FREQUENCY);
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*2, y * FREQUENCY*2)*0.5;
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*4, y * FREQUENCY*4)*0.25;
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*8, y * FREQUENCY*8)*0.125;
		value += OpenSimplex2S.noise2_ImproveX(seed, x * FREQUENCY*16, y * FREQUENCY*16)*0.06;
		value = (value /(1 + 0.5 + 0.25+0.125+0.06));
		return Math.abs(value);
	}



		public void getTemperatureMap(){
			int biomeSeed = 30;
			double freq = 0.1/24;
			temperatureMap = getNoiseArray(biomeSeed,freq,WIDTH,HEIGHT);

		}    

		public void getRainMap(){
			int biomeSeed = 50;
			double freq = 0.1/24;
			rainMap = getNoiseArray(biomeSeed,freq,WIDTH,HEIGHT);
		}    

		public void getVegetationMap(){
			int biomeSeed = 100;
			double freq = 5/24;
			vegetationMap = getNoiseArray(biomeSeed,freq,WIDTH,HEIGHT);
		}


		public void paintMap()
		

		throws IOException {
			FileWriter writer;
			FileWriter writerobj;
			File file = new File("ressurser/Tiles/tileMaps/tm,0-0.txt");
			File fileObj = new File("ressurser/objects/objectTxtDocuments/tm,0-0.txt");
			try{
				writer = new FileWriter(file);
				writerobj = new FileWriter(fileObj);
            	String space = "";

				int rgb;
				BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
				for (int y = 0; y < HEIGHT; y++)
				{
					for (int x = 0; x < WIDTH; x++){
							writer.write(getLetter(x,y));
							writer.write(" ");
							rgb = getColor(x,y);
							image.setRGB(x, y, rgb);

							biomeMap[y][x] = getLetter(y,x);


							//disabled object generation
							//writerobj.write(getObj(x,y));
					}

					if (y+1<HEIGHT){
						writer.write("\n");
						writerobj.write("\n");
					}
					
					
				}
				writer.flush();
				writerobj.flush();
				ImageIO.write(image, "png", new File("ressurser/worldGeneration/noise.png"));
				
			} catch (IOException e){
				e.printStackTrace();
			}
			
		}     

		private String getObj(int x,int y){
			if (!(getLetter(x,y).equals("s"))&&!(getLetter(x,y).equals("w"))){

			
				
					
				

				
					if (Math.random()<0.2){
						if (Math.random()<0.5){
							return "tree,woodYield,0,1,2,1 ";
						}else if (Math.random()<0.1){
							return "cave1,dungeonTeleport,0,0,1,1 ";
						} else if (Math.random()<0.5){
							return "highTree,woodYield,1,3,1,1 ";
						}else if (Math.random()<0.5){
							return "highbush,woodYield,0,1,1,1 ";
						} else if (Math.random()<0.3){
							return "fDecorGrassBush,nonCI,0,0,1,1 ";
						}
					
					
						else if (Math.random()<0.3){
							return "fDecorGrass,nonCI,0,0,1,1 ";
						}
					
					
						else if (Math.random()<0.2){
							return "fDecorMushroom,nonCI,0,0,1,1 ";
						}
					
	
					
						else if (Math.random()<0.5){
							return "wildgrassP3,nonCI,0,0,1,1 ";
						}
						
						else {return "wildgrassP3,nonCI,0,0,1,1 ";}
						
					
					
				} else if (Math.random()<0.05){
					return "skrentNormal,cliff,0,0,1,1 ";
				}
			
			}
			 else if (getLetter(x,y).equals("s")){
				if (Math.random()<0.5){
				
					if (Math.random()<0.1){
						return "decorDriftwood2,woodYield,0,0,1,1 ";

				}
			
			
			
				if (vegetationMap[y][x]>0.5){
					if (Math.random()<0.5){
						return "StoneM1,stone,0,0,1,1 ";
					}
				}
				if (vegetationMap[y][x]<-0.5){
					if (Math.random()<0.2){
						return "StonePile,stone,0,0,1,1 ";
					}
				}
				if (vegetationMap[y][x]>0.5){
					if (Math.random()<0.3){
						return "smallStonePile,stone,0,0,1,1 ";
					}
				}
			
			} 
		}
			

			

		
		return "0 ";
	}

		public void main(String[] args) throws IOException {
			TerrainGenSimplex gen = new TerrainGenSimplex(2000,2000,true);

		
			
		}
		

		private double sumOcatave(long seed,int num_iterations, int x, int y, double persistence, double freq){
		double maxAmp = 0;
		double amp = 1;
		double noise = 0;
		
		//add successively smaller, higher-frequency terms
		for(int i = 0; i < num_iterations; ++i){
			 noise += OpenSimplex2S.noise2_ImproveX(seed,x*freq,y*freq)*amp;
			
			//noise += simplex_noise(x * freq, y * freq) * amp;
			maxAmp+=amp;
			amp/=2;
			freq *=2;
		}
		
		//#take the average value of the iterations
		noise /= maxAmp;
		
	
		//#normalize the result
		//noise = noise * (high - low) / 2 + (high + low) / 2;
		
		return noise;
		}
		
	
//private int getColor2(int y,int x){
	//if (getTemperatureZone()){

	//}

	//if (getHumidityZone()){

	//}
//}



private int getColor(int y,int x){
	int  color = 0;

	//if (heightMap[x][y]>0.5){
	//temperatureMap[x][y]-= Math.pow((heightMap[x][y]),5)*10;}
	//rainMap[x][y]+= Math.pow((heightMap[x][y]),5)*10;}

	//rainMap[x][y]-= waterMap[x][y];
	//temperatureMap[x][y] = temperatureMap[x][y] /2;
	//rainMap[x][y] = rainMap[x][y] /2;

	if (heightMap[x][y] <= oceanLvl) {
		//ocean
		if (temperatureMap[x][y]<-0.7){
			if (vegetationMap[x][y]<0.7){
				color = ice;
			} else {
				color = water;
			}
			
		} else {
			color = water;
		}
		
	} else if (heightMap[x][y] <= beachLvl) {
		// Beach
		color = sand;
		
		// Beach
	} else if (heightMap[x][y] >= moantaintLvl) {
		// Beach
		color = mountain;
		
		// Beach
	} else if (temperatureMap[x][y] <= sTundraTmpMax  && rainMap[x][y] <= sTundraRMax && rainMap[x][y] >= sTundraRMin) {
		// Snowy Tundra
		color = ice;
	} else if (temperatureMap[x][y] <= plainsTmpMax && temperatureMap[x][y] >= plainsTmpMin && rainMap[x][y] <= plainsRMax && rainMap[x][y] >= plainsRMin) {
		// Plains
		color = green;
	} else if (temperatureMap[x][y] <= desertTmpMax && temperatureMap[x][y] >= desertTmpMin && rainMap[x][y] <= desertRMax && rainMap[x][y] >= desertRMin) {
		// Desert
		color = sand;
	} else if (temperatureMap[x][y] <= sTaigaTmpMax  && rainMap[x][y] <= sTaigaRMax && rainMap[x][y] >= sTaigaRMin) {
		// Snowy Taiga
		color = ice;
	} else if (temperatureMap[x][y] <= sForestTmpMax && temperatureMap[x][y] >= sForestTmpMin && rainMap[x][y] <= sForestRMax && rainMap[x][y] >= sForestRMin) {
		// Seasonal Forest
		color = sForest;
	} else if (temperatureMap[x][y] <= savannaTmpMax && temperatureMap[x][y] >= savannaTmpMin && rainMap[x][y] <= savannaRMax && rainMap[x][y] >= savannaRMin) {
		// Savanna
		color = savanna;
	} else if (temperatureMap[x][y] <= forestTmpMax && temperatureMap[x][y] >= forestTmpMin && rainMap[x][y] <= forestRMax && rainMap[x][y] >= forestRMin) {
		// Forest
		color = darkGreen;
	} else if (temperatureMap[x][y] <= swampTmpMax && temperatureMap[x][y] >= swampTmpMin && rainMap[x][y] <= swampRMax && rainMap[x][y] >= swampRMin) {
		// Swamp
		color = moss;
	} else if (temperatureMap[x][y] <= rainFTmpMax && temperatureMap[x][y] >= rainFTmpMin && rainMap[x][y] <= rainFRMax && rainMap[x][y] >= rainFRMin) {
		// Rain Forest
		color = rainFr;
	} else {
		
		// Default biome
	}

	if (riverMap[x][y]< 0.08){
		
		color = water;
	}
	else if (riverMap[x][y]< 0.11){
		color = sand;
	}

	return color;
	}
	
	private void getRiverMap(){
		riverMap = getNoiseArrayAbs(SEED +10,0.002,HEIGHT,WIDTH,6);
	}


	private void getBiome(int x,int y){
		if (heightMap[x][y] <= oceanLvl) {
			//ocean
		}
	}


	public  String getLetterFromSpecificCoord(int x,int y){
		String  letter = "";

		double FREQUENCY = 0.1/24;

		double height = getHeight(x,y,1,FREQUENCY);
		double vegetation = getHeight(x,y,2,FREQUENCY*100);
		double temperatur = getHeight(x,y,3,FREQUENCY/10);
		double riverV = getHeight(x,y,4,FREQUENCY);
		double moist = getHeight(x,y,5,FREQUENCY);



		

	if (height <= oceanLvl) {
		//ocean
		if (temperatur<-0.7){
			if (vegetation<0.7){
				letter = "ice";
			} else {
				letter = "water";
			}
			
		} else {
			letter = "water";
		}
		
	} else if (heightMap[x][y] <= beachLvl) {
		// Beach
		letter = "sand";
		
		// Beach
	} else if (heightMap[x][y] >= moantaintLvl) {
		// Beach
		letter = "mountain";
		
		// Beach
	} else if (temperatur <= sTundraTmpMax  && moist <= sTundraRMax && moist >= sTundraRMin) {
		// Snowy Tundra
		letter = "ice";
	} else if (temperatur <= plainsTmpMax && temperatur >= plainsTmpMin && moist <= plainsRMax && moist >= plainsRMin) {
		// Plains
		letter = "green";
	} else if (temperatur <= desertTmpMax && temperatur >= desertTmpMin && moist <= desertRMax && moist >= desertRMin) {
		// Desert
		letter = "sand";
	} else if (temperatur <= sTaigaTmpMax  && moist <= sTaigaRMax && moist >= sTaigaRMin) {
		// Snowy Taiga
		letter = "ice";
	} else if (temperatur <= sForestTmpMax && temperatur >= sForestTmpMin && moist <= sForestRMax && moist >= sForestRMin) {
		// Seasonal Forest
		letter = "sForest";
	} else if (temperatur <= savannaTmpMax && temperatur>= savannaTmpMin && moist <= savannaRMax && moist >= savannaRMin) {
		// Savanna
		letter = "savanna";
	} else if (temperatur <= forestTmpMax && temperatur >= forestTmpMin && moist <= forestRMax && moist >= forestRMin) {
		// Forest
		letter = "darkGreen";
	} else if (temperatur <= swampTmpMax && temperatur >= swampTmpMin && moist <= swampRMax && moist >= swampRMin) {
		// Swamp
		letter = "moss";
	} else if (temperatur <= rainFTmpMax && temperatur >= rainFTmpMin && moist <= rainFRMax && moist >= rainFRMin) {
		// Rain Forest
		letter = "rainFr";
	} else {
		
		// Default biome
	}

	if (riverMap[x][y]< 0.08){
		
		letter = "water";
	}
	else if (riverMap[x][y]< 0.11){
		letter = "sand";
	}

	return letter;
	}


	public String getLetter(int y,int x){
		String  biome = "null";
		//if (heightMap[x][y]>0.5){
		//temperatureMap[x][y]-= Math.pow((heightMap[x][y]),5)*10;}
		//rainMap[x][y]-= waterMap[x][y];
		//temperatureMap[x][y] = temperatureMap[x][y] /2;
		//rainMap[x][y] = rainMap[x][y] /2;
		if (heightMap[x][y] <= oceanLvl) {
			//ocean
			biome = "w";
		} else if (heightMap[x][y] <= beachLvl) {
			// Beach
			biome = "s";
			
			// Beach
		} else if (heightMap[x][y] >= moantaintLvl) {
			// Beach
			biome = "m";
			
			// Beach
		} else if (temperatureMap[x][y] <= sTundraTmpMax && temperatureMap[x][y] >= sTundraTmpMin && rainMap[x][y] <= sTundraRMax && rainMap[x][y] >= sTundraRMin) {
			// Snowy Tundra
			biome = "s";
		} else if (temperatureMap[x][y] <= plainsTmpMax && temperatureMap[x][y] >= plainsTmpMin && rainMap[x][y] <= plainsRMax && rainMap[x][y] >= plainsRMin) {
			// Plains
			biome = "g";
		} else if (temperatureMap[x][y] <= desertTmpMax && temperatureMap[x][y] >= desertTmpMin && rainMap[x][y] <= desertRMax && rainMap[x][y] >= desertRMin) {
			// Desert
			biome = "s";
		} else if (temperatureMap[x][y] <= sTaigaTmpMax && rainMap[x][y] <= sTaigaRMax && rainMap[x][y] >= sTaigaRMin) {
			// Snowy Taiga
			biome = "s";
		} else if (temperatureMap[x][y] <= sForestTmpMax && temperatureMap[x][y] >= sForestTmpMin && rainMap[x][y] <= sForestRMax && rainMap[x][y] >= sForestRMin) {
			// Seasonal Forest
			biome = "dG";
		} else if (temperatureMap[x][y] <= savannaTmpMax && temperatureMap[x][y] >= savannaTmpMin && rainMap[x][y] <= savannaRMax && rainMap[x][y] >= savannaRMin) {
			// Savanna
			
			biome = "Sa";
		} else if (temperatureMap[x][y] <= forestTmpMax && temperatureMap[x][y] >= forestTmpMin && rainMap[x][y] <= forestRMax && rainMap[x][y] >= forestRMin) {
			// Forest
			biome = "g";
		} else if (temperatureMap[x][y] <= swampTmpMax && temperatureMap[x][y] >= swampTmpMin && rainMap[x][y] <= swampRMax && rainMap[x][y] >= swampRMin) {
			// Swamp
			biome = "Mo";
		} else if (temperatureMap[x][y] <= rainFTmpMax && temperatureMap[x][y] >= rainFTmpMin && rainMap[x][y] <= rainFRMax && rainMap[x][y] >= rainFRMin) {
			// Rain Forest
			biome = "dG";
		} else {
			
			biome = "g";
		}

		if (riverMap[x][y]< 0.08){
		
			biome = "w";
		}else if (riverMap[x][y]< 0.11){
			biome = "s";
		}
		return biome;
		}



		public void paintCaveMap() throws IOException {

			int height = 240;
			int width = 240;
 
			double[][] caves = getNoiseArrayAbs(200,0.015,height,width,4);
			double[][] caves2 = getNoiseArrayAbs(100,0.005,height,width,8);

			double[][] rooms = getNoiseArray(200,0.01,height,width);

			FileWriter writer;
			FileWriter writerobj;
			File file = new File("ressurser/main/TestGenerasjon/labyrinth1.txt");
			//File fileObj = new File("ressurser/objects/objectTxtDocuments/objectmap0.txt");
			try{
				writer = new FileWriter(file);
				//writerobj = new FileWriter(fileObj);
            	String space = "";

				int rgb;
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				for (int y = 0; y < height; y++)
				{
					for (int x = 0; x < width; x++){

							
							rgb = getColorCave(caves,x,y,0.05);
							if (rgb == black){
								rgb = getColorCave(caves2,x,y,0.05);
							}

							if (rgb == black){
								rgb = getColorCave(rooms,x,y,-0.30);
							}


							writer.write(getLetter(x,y));
							writer.write(" ");
							
							image.setRGB(x, y, rgb);

							//biomeMap[y][x] = getLetter(y,x);



							//writerobj.write(getObj(x,y));
					}
					if (y+2<height){
						writer.write("\n");
					}
					
					//writerobj.write("\n");
				}
				writer.flush();
				ImageIO.write(image, "png", new File("ressurser/worldGeneration/cavenoise.png"));
				
			} catch (IOException e){
				e.printStackTrace();
			}
			
		}    

		public int getColorCave(double [][] caveMap,int x,int y,double lessthan){

			if (caveMap[y][x]<lessthan){
				return green;
			} else{
				return black;
			}

		}
	
}