package ressurser.objects;

public class GameObjectFactory {
    
    public static Object createGameObject(String objectString) {
        if (objectString.startsWith("bg")) {
            // return a Background object
            return new Background();
        } else if (objectString.startsWith("ch")) {
            // return a Character object
            return new Character();
        } else {
            // handle error if the objectString doesn't match any known object type
            throw new IllegalArgumentException("Invalid objectString: " + objectString);
        }
    }
}
