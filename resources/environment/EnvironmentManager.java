package resources.environment;

import java.awt.Point;

import resources.domain.entity.BaseEntity;
import resources.domain.spawn.MobSpawnService;
import resources.domain.spawn.SpawnRules;
import resources.app.GamePanel;

public class EnvironmentManager {
    GamePanel panel;
    int ms = 0;
    int second = 0;
    int minutes = 4;
    int days = 0;
    int lightLevel;
    int animationValue = 0;

    private final MobSpawnService mobSpawner = new MobSpawnService();

    //different types of envoriement.
    public int normalEnvironment = 0;
    public int caveEnvironment = 1;
    public int dungeonEnvoirment = 2;

    public int activeEnvironment = normalEnvironment;


    public EnvironmentManager(GamePanel panel){
        this.panel = panel;
    }

    public void updateTicks(){


        ms++;

        // Chunk streaming runs at ~10 Hz so newly entered chunks load
        // promptly, well before the player can outrun the render rectangle.
        int chunkTickInterval = Math.max(1, panel.camera.FPS / 10);
        if (ms % chunkTickInterval == 0){
            panel.world.update(panel.camera.getHitBox().getCenter());
        }

        if (ms%panel.camera.FPS == 0){
            second++;
            updatePanelDimensions();


            //ageAllObjects();
            updateAnimation();
            //panel.objM.updatePlants();
            // Mob population pulse: once every 10 in-game seconds. The spawn
            // service is rate-limited by its own per-rule density rolls.
            if (second % 10 == 0) tryPopulateMobs();
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

    private void tryPopulateMobs() {
        if (panel.player == null) return;
        Point center = new Point((int) panel.player.getWorldX(), (int) panel.player.getWorldY());
        mobSpawner.tryPopulateChunk(panel, center, SpawnRules.defaults(panel));
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
        if (activeEnvironment==normalEnvironment){

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
        } else if (activeEnvironment==caveEnvironment){
         //if cave envirmoent, implement normal light rules.
            return 255;
            
        }
        return lightLevel;
    }

    

    public void setCaveEnvoirment(){
        activeEnvironment = caveEnvironment;
    }

    public void setDungeonEnvoirment(){
        activeEnvironment = dungeonEnvoirment;
    }

    public void setNormalEnvoirment(){
        activeEnvironment = normalEnvironment;
    }

    private void updateAnimation() {
        animationValue ++;
        if (animationValue == 3){
            animationValue = 0;
        }
        
        panel.world.animate(animationValue);
    }

}
