package MessageType;

public class CommandMessage extends AbstractMessage {
    private int command;
    public static final int LIST_FILES = 1;
    public static final int DOWNLOAD_FILE = 2;
    public static final int DELETE_FILE = 3;
    public static final int AUTH_OK = 4;
    public static final int CREATE_DIR = 5;


    private Object[] object;

    public CommandMessage(int command, Object ... objects) { //атачмент
        this.command = command;
        this.object = objects;
    }

    public int getCommand() {
        return command;
    }

    public Object[] getObject() {
        return object;
    }
}
