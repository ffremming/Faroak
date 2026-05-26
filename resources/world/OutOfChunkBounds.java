package ressurser.chunkSystem;

public class OutOfChunkBounds extends Exception {
    public OutOfChunkBounds(String errorMessage){
        super(errorMessage);
    }
}
