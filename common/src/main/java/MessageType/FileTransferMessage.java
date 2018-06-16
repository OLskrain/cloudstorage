package MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTransferMessage extends AbstractMessage {
    private String fileName;
    private String path; //путь к файлу
    private byte [] data;
   // private long size;

    public FileTransferMessage(Path filePaths) throws IOException {
        this.path = filePaths.toString();
        this.fileName = filePaths.getFileName().toString();
        this.data = Files.readAllBytes(filePaths);
       // this.size = data.length;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data;
    }
}
