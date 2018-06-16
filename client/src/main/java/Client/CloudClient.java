package Client;

import MessageType.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;


import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class CloudClient {
    private Socket clientSocket;
    //если у нас сервак netty а клиент на io, то используем обязательно  ObjectDecoderInputStream и ObjectEncoderOutputStream
    private ObjectDecoderInputStream inputStream;
    private ObjectEncoderOutputStream outputStream;
    private boolean isConnected;
    private List<String> filesList;

    public CloudClient(String address, int port){
        try {
            clientSocket = new Socket(address, port); //подключились
            outputStream = new ObjectEncoderOutputStream(clientSocket.getOutputStream()); //открыли потоки
            inputStream = new ObjectDecoderInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnected = true;
    }

    public synchronized void sendMessage(AbstractMessage outMessage){
        try {
            outputStream.writeObject(outMessage);
        } catch (IOException e) {
            System.out.println("Error sending AbstractMessage to " + this.toString());
            e.printStackTrace();
        }
    }
//запускаем поток общения клиента с серваком. т.е если сервак по нашему запросу файл прислал, то мы его получаем и в отдельном
    //треде обрабатываем
    public void startReadingThread(Controller controller){ //передаем контроллер
        new Thread(() ->{
            try {
                while (isConnected){ //если подключен клиент
                    Object msg = inputStream.readObject();
                    if(msg != null){
                        System.out.println("One message received " + msg.toString());
                        if(msg instanceof AbstractMessage){
                            AbstractMessage incomingMsg = (AbstractMessage) msg;
                            if(incomingMsg instanceof CommandMessage){
                                CommandMessage cmdMsg = (CommandMessage) incomingMsg;
                                if(cmdMsg.getCommand() == CommandMessage.AUTH_OK){
                                    System.out.println("AUTH OK");
                                    controller.loginOk();
                                }
                            }
                            if(incomingMsg instanceof FileListMessage){
                                filesList = ((FileListMessage) incomingMsg).getFileList();
                            }
                            if(incomingMsg instanceof FileTransferMessage){
                                saveFileToStorage((FileTransferMessage) incomingMsg);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void saveFileToStorage(FileTransferMessage incomingMsg){
        try {
            Path newFilePath = Paths.get("C:\\Users\\olskr_000\\Desktop\\programming\\Java\\Storage\\isClientDisk" +
                    "\\" + incomingMsg.getFileName());
            if(Files.exists(newFilePath)){
                Files.write(newFilePath, incomingMsg.getData(), StandardOpenOption.TRUNCATE_EXISTING);
            }else {
                Files.write(newFilePath, incomingMsg.getData(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFilesList(){
        return filesList;
    }
}
