package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.domain.player.Playable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;

public class ActionModule {
    final int WOOD = 0;
    final int ROCK = 1;
    final int DIRT = 2;
    final int FARMLAND = 3;
    final int WATER = 4;
    final int FARM = 5;
    final int BUILD = 6;
    final int HURT = 7;

    private int type = -1;
    private int weight = 0;

    public ActionModule(int type, int weight){
        this.type = type;
        this.weight = weight;
    }

    public ActionModule(int type){
        this.type = type;
    }

    public ActionModule(){
    }

    public int getWOOD() {
        return WOOD;
    }



    public int getROCK() {
        return ROCK;
    }



    public int getDIRT() {
        return DIRT;
    }



    public int getFARMLAND() {
        return FARMLAND;
    }



    public int getWATER() {
        return WATER;
    }



    public int getFARM() {
        return FARM;
    }



    public int getBUILD() {
        return BUILD;
    }



    public int getHURT() {
        return HURT;
    }



    public int getWeight() {
        return weight;
    }



    public void setWeight(int weight) {
        this.weight = weight;
    }



    public int getType() {
        return type;
    }



    public void setType(int value) {
        this.type = value;
    }

    
}
