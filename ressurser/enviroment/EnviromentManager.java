package ressurser.enviroment;

import ressurser.main.GamePanel;
import ressurser.objects.Ageable;
import ressurser.objects.Farmable;
import ressurser.objects.ObjectManager;
import ressurser.objects.SuperObject;

public class EnviromentManager {
    GamePanel panel;
    int ms = 0;
    int tick = 0;
    int minutes = 4;
    int days = 0;
    int lightLevel;

    //different types of envoriement.
    public int normalEnviroment = 0;
    public int caveEnviroment = 1;
    public int dungeonEnvoirment = 2;

    public int activeEnviroment = normalEnviroment;


    public EnviromentManager(GamePanel panel){
        this.panel = panel;
    }

    public void updateTicks(){
       
        
        ms++;
        if (ms%panel.camera.frameRate == 0){
            //based on camera, but should be something that is followed.
            panel.chunkSystem.workingMemory.update(panel.camera.getHitBox().getCenter());
            tick++;
            ageAllObjects();
            
            //panel.objM.updatePlants();
            if (tick%60 == 0){
                minutes ++;
                if (minutes%16 == 0){
                    //panel.objM.updatePlants();
                    days ++;
                    tick = 0;
                    minutes = 0;
                    ms = 0;
                }
            } 
        }
    }

    public int lightLevel(){
        //if noamrl envirmoent, implement normal light rules.
        if (activeEnviroment==normalEnviroment){

            if (minutes>4 && minutes<12){
                lightLevel = 0;
                return 0;

            } else {
                if (minutes >12){
                    if (lightLevel<235){
                        if  (ms%40 == 0){
                            lightLevel ++;
                        }
                    }
                }
                
                else if (minutes< 4){
                    if  (ms%40 == 0){
                        if (lightLevel>0){
                            lightLevel --;
                        }
                       
                    }
                } 
            }
        } else if (activeEnviroment==caveEnviroment){
         //if cave envirmoent, implement normal light rules.
            return 255;
            
        }
        return lightLevel;
    }

    protected void ageAllObjects(){
        ObjectManager objM = panel.objM;
       
        for (SuperObject [] o:objM.map[panel.mapH.activeMapType][panel.mapH.activeMapNumber]){
            for (SuperObject ob:o){
                if (ob!= null){
                  
                    if (ob instanceof Ageable){
                        ((Ageable)ob).age();
                       
                    }
                }
            }
        }
    }

    public void setCaveEnvoirment(){
        activeEnviroment = caveEnviroment;
    }

    public void setDungeonEnvoirment(){
        activeEnviroment = dungeonEnvoirment;
    }

    public void setNormalEnvoirment(){
        activeEnviroment = normalEnviroment;
    }

    

}
