package ressurser.baseEntity.gameObjects;

public class Item {
    String name;
    int stackLimit = 64;
    int amount;

    public Item(String name){
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }
    public int getStackLimit() {
        return stackLimit;
    }

    public boolean increaseAmount(){
        if (!isFull()){
            amount ++;
            return true;
        }
        return false;
        
    }

    public boolean decreaseAmount(){
        if (!isEmpty()){
            amount --;
            return true;
        }
        return false;
        
    }


    public boolean isFull(){
        return amount >= stackLimit;
    }

    private boolean isEmpty(){
        return amount <= 0;
    }

    private void correctAmount(){
        if (amount <0){
            amount = 0;
        } else if (amount >stackLimit){
            amount = stackLimit;
        }
    }

    /**
     * sets the current amount of the object.
     */
    public void setAmount(int newAmount) {
        amount = newAmount;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Item)){
            return false;
        }
        Item itm = (Item) o;

       return (itm.name.equals(name));

        
    }
}
