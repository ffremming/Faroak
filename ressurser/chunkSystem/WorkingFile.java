package ressurser.chunkSystem;

import java.util.ArrayList;

public class WorkingFile {
    String fileName;

    ArrayList<Chunk> fileContent;

    public WorkingFile(String fileName){
        this.fileName = fileName;
    }

    public void addChunk(Chunk c){
        if (!fileContent.contains(c)){
            fileContent.add(c);
        }
    }

    public void writeFile(){

    }

    public void readFromFile(){
        //TODO
        //
    }


}
