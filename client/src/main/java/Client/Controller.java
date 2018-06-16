package Client;

import MessageType.*;
import io.netty.channel.ChannelHandlerContext;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable{

    @FXML
    public TextField usernameField;

    @FXML
    public PasswordField passwordField;

    @FXML
    public ListView<String> localList;

    @FXML
    public ListView<String> cloudyList;

    @FXML
    public HBox authPanel;

    @FXML
    public HBox actionPanel1;

    @FXML
    public HBox actionPanel2;

    private CloudClient client;
    private String rootDirectory;
    private String command;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = new CloudClient("localhost", 8189);
        rootDirectory = "C:\\Users\\olskr_000\\Desktop\\programming\\Java\\Storage\\isClientDisk";

        localList.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                if(mouseEvent.getClickCount() == 2){
                    System.out.println("Double clicked localList");
                    transitions((ListView) mouseEvent.getSource());
                }
            }
        });
        cloudyList.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                if(mouseEvent.getClickCount() == 2){
                    System.out.println("Double clicked cluodList");
                    transitions((ListView) mouseEvent.getSource());
                }
            }
        });
    }

    public void logIn(ActionEvent actionEvent){
        System.out.println("Client try connect");
        client.startReadingThread(this);
        //client.sendMessage(new AuthMessage(usernameField.getText().trim(), passwordField.getText().trim()));
        client.sendMessage(new AuthMessage("kolo", "lopo"));
    }

    public void getCloudFilesList(){
        client.sendMessage(new CommandMessage(CommandMessage.LIST_FILES, command)); //отправляем команду на запрос списка файлов

        Platform.runLater(() -> {        //олучаем его
            cloudyList.getItems().clear();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<String> list = client.getFilesList(); //здесь меняем файлы
            if(list.size() > 0){
                for(int i = 0; i < list.size(); i++){
                    cloudyList.getItems().add(list.get(i));
                }
            }else {
                cloudyList.getItems().add(" ЧТо то там"); //нужно посмотреть
            }
        });
    }
//загрузить файл или папку
    public void uploadFileOrFolder(ActionEvent event){
        String itemName = localList.getItems().get(localList.getFocusModel().getFocusedIndex()).toString();
        Path path = Paths.get(rootDirectory, itemName);
        if(Files.isDirectory(path)){
            try {
                sendFolder(path);
            } catch (IOException e) {
                e.printStackTrace();
               // System.out.println(" Что то там " + itemName);
            }
        }else {
            uploadFile(path);
        }
    }
//метод добавления папки. здесь мы рекурсивно обходим дерево файловой системы
    private void sendFolder(Path folderPath) throws IOException{
        Files.walkFileTree(folderPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
              //  System.out.println("Что то там" + dir.toString());
                //при получения команды мы подготавливаем директорию (1)
                client.sendMessage(new CommandMessage(CommandMessage.CREATE_DIR, dir.toString(), dir.getParent().toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               // System.out.println("что то там" + file);
                uploadFile(file); // (1) и потом добавляем сюда файл
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
//загрузка файла
    private void uploadFile(Path filePath){
        System.out.println("Send file - " + filePath.getFileName().toString());
        try {
            client.sendMessage(new FileTransferMessage (filePath)); //посылаем команду на скачивание
        } catch (IOException e) {
            e.printStackTrace();
        }
        getCloudFilesList();
    }

//скачивание файла
    public void downloadFile(ActionEvent event){
        String filename = cloudyList.getItems().get(cloudyList.getFocusModel().getFocusedIndex()).toString();
        System.out.println(filename);
        client.sendMessage(new CommandMessage(CommandMessage.DOWNLOAD_FILE, filename));
        updateLocalFilesList();
    }
//удаление файла
    public void deleteFileInStorage(ActionEvent event){
        String filename = cloudyList.getItems().get(cloudyList.getFocusModel().getFocusedIndex()).toString();
        System.out.println(filename);
        client.sendMessage(new CommandMessage(CommandMessage.DELETE_FILE, filename));
        getCloudFilesList();
    }
//удаление локального(на стороне клиента так понимать) файла
    public void deleteLocalFile(){
        String filename = localList.getItems().get(localList.getFocusModel().getFocusedIndex()).toString();
        Path newFilePath = Paths.get(rootDirectory, filename);
        //System.out.println(filename);
        try {
            Files.delete(newFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            //сдесь можно поставить "АЙЛЕР" , который покажет окошко что не удалось удалить или что то еще
        }
        updateLocalFilesList();
    }
    public void refreshFile(ActionEvent actionEvent){
        updateLocalFilesList();
    }

//вкл, откл панелей
    public void loginOk(){
      authPanel.setVisible(false);
      authPanel.setManaged(false);

      actionPanel1.setVisible(true);
      actionPanel2.setVisible(true);

      actionPanel1.setManaged(true);
      actionPanel2.setManaged(true);

      updateLocalFilesList();
      getCloudFilesList();
    }

    //обновление локальных файлов
    public void updateLocalFilesList(){
        localList.getItems().clear();
        localList.getItems().add(0, "...");
        try {
            Files.newDirectoryStream(Paths.get(rootDirectory)).forEach(
                    path -> localList.getItems().add(path.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//метод переходов вверх вниз и запросы листов
    private void transitions(ListView listView){
        String itemName = listView.getItems().get(listView.getFocusModel().getFocusedIndex()).toString();

        if(itemName.equals("...")){
            if(listView.equals(localList)){
                rootDirectory = Paths.get(rootDirectory).getParent().toString();
                updateLocalFilesList();
            }else if (listView.equals(cloudyList)){
                command = "...";
                getCloudFilesList();
            }
        }else {
            if(listView.equals(localList)){
                Path path = Paths.get(rootDirectory, itemName);
                if(Files.isDirectory(path)){
                    rootDirectory += itemName + "\\";
                    updateLocalFilesList();
                }
            }else if(listView.equals(cloudyList)){
                command = itemName;
                getCloudFilesList();
            }
        }
    }
}
