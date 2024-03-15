package ressurser.chunkSystem;

import ressurser.main.GamePanel;

public class StructuralWorld extends WorkingMemory{

    /** this is supposed to be a smaller working memory interface tat does not generate chunks based on procedural generation */
    public StructuralWorld(GamePanel panel) {
        super(panel); 
        chunkSystem.generate = false;
    }
    
}
