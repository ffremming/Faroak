package ressurser.enviroment;

import ressurser.baseEntity.BaseEntity;
import ressurser.main.GamePanel;

public class EnviromentManager {
    GamePanel panel;
    int ms = 0;
    int second = 0;
    int minutes = 4;
    int days = 0;
    int lightLevel;
    int animationValue = 0;

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
        if (ms%panel.camera.FPS == 0){
            
            //based on camera, but should be something that is followed.
            panel.world.update(panel.camera.getHitBox().getCenter());
            second++;
            updatePanelDimensions();
            

            //ageAllObjects();
            updateAnimation();
            //panel.objM.updatePlants();
            if (second%60 == 0){
                minutes ++;
                ageAll();
                if (minutes%16 == 0){
                    //panel.objM.updatePlants();
                    days ++;
                    second = 0;
                    minutes = 0;
                    ms = 0;
                }
            } 
        }
    }

    private void ageAll() {
       for (BaseEntity ent:panel.world.getEntities()){
           ent.age();
       }
    }

    public void updatePanelDimensions(){
        panel.width = (int)panel.getSize().getWidth();
        panel.height = (int)panel.getSize().getHeight();
        panel.camera.setWidth((panel.width));
        panel.camera.setHeight((panel.height));
        panel.camera.getHitBox().width = panel.width;
        panel.camera.getHitBox().height = panel.height;
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

    

    public void setCaveEnvoirment(){
        activeEnviroment = caveEnviroment;
    }

    public void setDungeonEnvoirment(){
        activeEnviroment = dungeonEnvoirment;
    }

    public void setNormalEnvoirment(){
        activeEnviroment = normalEnviroment;
    }

    private void updateAnimation() {
        animationValue ++;
        if (animationValue == 3){
            animationValue = 0;
        }
        
        panel.world.animate(animationValue);
    }

}
