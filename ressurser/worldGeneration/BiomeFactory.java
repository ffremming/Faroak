package ressurser.worldGeneration;

public class BiomeFactory {

    TerrainGenSimplex gen;

    public BiomeFactory(TerrainGenSimplex gen){
        this.gen = gen;
    }

    //landtype is defined by its height in the world.
    private String createBiome(String landType,double temperatur,double humidity,double height){

        if (landType.equals("ocean")){
            if (gen.getTemperatureZone(temperatur).equals("cold")){
                return "coldOcean";
            } else if (gen.getTemperatureZone(temperatur).equals("freezing")){
                return "freezingOcean";
            } else if (gen.getTemperatureZone(temperatur).equals("normal")){
                return "ocean";
            } else if (gen.getTemperatureZone(temperatur).equals("hot")){
                return "warmOcean";
            }

        }else if (landType.equals("beach")){
            if (gen.getTemperatureZone(temperatur).equals("freezing")){
                return "freezingBeach";}
            else {
                return "beach";
            }
        }
        
        else if (landType.equals("moantain")){
            return "mountain";

        }
        else if (landType.equals("land")){

            //if cold;
            if (gen.getTemperatureZone(temperatur).equals("cold")){

                return getLandBiomesFromHumidity(humidity,"snowyTaiga","snowyForest","coldForest","coldSteps");

            //if freezin:
            } else if (gen.getTemperatureZone(temperatur).equals("freezing")){
                return getLandBiomesFromHumidity(humidity,"icyPlains","icyPlains","coldForest","coldSteps");

            //if normal:
            } else if (gen.getTemperatureZone(temperatur).equals("normal")){
                return getLandBiomesFromHumidity(humidity,"swamp","darkForest","plains","plains");

            //if hot:
            } else if (gen.getTemperatureZone(temperatur).equals("hot")){
                return getLandBiomesFromHumidity(humidity,"rainForest","jungel","savanna","dessert");
            }

        }
        return null;
    }

    private String getLandBiomesFromHumidity(double humidity,String wet,String normal,String dry,String arrid){
        if (gen.getHumidityZone(humidity).equals("wet")){
            return wet;
        } else if (gen.getHumidityZone(humidity).equals("normal")){
            return normal;
        } else if (gen.getHumidityZone(humidity).equals("dry")){
            return dry;
        } else if(gen.getHumidityZone(humidity).equals("arrid")){
            return arrid;
        }
        return null;
    }
}
