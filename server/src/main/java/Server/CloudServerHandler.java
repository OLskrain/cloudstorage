package Server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import MessageType.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {
    private String nickname;
    private String rootDirectory = "C:\\Users\\olskr_000\\Desktop\\programming\\Java\\Storage\\isServerDisk"; //рут директория
    private String fullFolderPath; //полный путь?
    private boolean isLogged; //залогиненн или нет
    private Logger logger;


//    public Server.CloudServerHandler(){
//        this.logger = LoggerFactory.getLogger();
//    }

    @Override
    // метод, что сделать когда активировался канал
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
        // Send greeting for a new connection.
         ctx.write("Welcome to Cloud"  + "!\r\n"); //приветствуем клиента
         ctx.write("It is " + new Date() + " now. \r\n"); //отсылаем ему время
         ctx.flush();  //ctx - это контекст , котрый позволяет реагировать на события
    }

    @Override
    // метод. как прочитать сообщение
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) //если ни чего нет(пустой объект пришол). то выходим
                return;

            if (msg instanceof AbstractMessage) { //если объект от MyMessage
                processingMsg((AbstractMessage) msg, ctx);
            } else { //если непонятное что то , то пишем что непонятное что то
                System.out.printf("Server received wrong object!");
            }
        } finally {
            ReferenceCountUtil.release(msg); //осободили наш буффер
        }
    }

    @Override
    //метод когда закончилось чтение из канала
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    //если исключение, то пичатаем то что произошло
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
        ctx.close(); // и закрываем контекст
    }

    //метод обработки сообщения
    private void processingMsg(AbstractMessage msg, ChannelHandlerContext ctx){

        if(msg instanceof AuthMessage){ //если сообщение автаризации
            System.out.println("Client " + ((AuthMessage) msg).getLogin() + " with password " +
                    ((AuthMessage) msg).getPassword() + " are trying to login!"); //печатаем что нам прислал такой то логик и пароль
            checkAuthorization((AuthMessage) msg, ctx); //метод автаризации пользователя
        }
        if(isLogged){ //если пользователь уже залогинился
            if(msg instanceof FileTransferMessage){ //команда, если прилетел файл
                saveFileToStorage((FileTransferMessage) msg); // то сохраняем файл в хранилище
            }else if (msg instanceof  CommandMessage){ //если пришла команда
                System.out.println("Server received a command " + ((CommandMessage) msg).getCommand());
                processingCommand((CommandMessage) msg, ctx); //обрабатываем команду
            }
        }
    }

    //метод обработки команд
    private void processingCommand(CommandMessage msg, ChannelHandlerContext ctx){
        switch (msg.getCommand()){
            case CommandMessage.LIST_FILES:
                sendData(new FileListMessage(getClientFilesList(msg.getObject()[0])), ctx);
                break;
            case CommandMessage.DOWNLOAD_FILE:
                try {
                    Path filePath = Paths.get(rootDirectory + nickname + "\\",  //ищем этот файл
                            (String) (msg.getObject()[0]));
                    sendData(new FileTransferMessage(filePath), ctx); // и отправляем его клиенту
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case CommandMessage.DELETE_FILE:
                deleteFileFromStorage((String)(msg.getObject()[0]));
                break;
            case CommandMessage.CREATE_DIR:
                System.out.println("Try to create a directory."); //поправить сообщение
                createDirectory(msg); //создаем
                break;
            default:
                System.out.println("Error");

        }
//        if(msg.getCommand() == CommandMessage.LIST_FILES){ //если запросили список файлов
//            sendData(new FileListMessage(getClientFilesList(msg.getObject()[0])), ctx); //формируем и отправляем
//        }else if(msg.getCommand() == CommandMessage.DOWNLOAD_FILE){ //если запросили скачать файл
//            try {
//                Path filePath = Paths.get(rootDir + nickname + "\\",  //ищем этот файл
//                        (String) (msg.getObject()[0]));
//                sendData(new FileTransferMessage(filePath), ctx); // и отправляем его клиенту
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }else if (msg.getCommand() == CommandMessage.DELETE_FILE){ //если запросили удалить файл
//            deleteFileFromStorage((String)(msg.getObject()[0])); //удаляем
//        }else if (msg.getCommand() == CommandMessage.CREATE_DIR){ //если запросили создать директорию
//            System.out.println(" Что ЧТО ТАм"); //поправить сообщение
//            createDirectory(msg); //создаем
//        }
    }
// метод создания котолока
    private void createDirectory (CommandMessage msg){
        Path rootPath = Paths.get(rootDirectory + nickname + "\\"); //при создании указываем диск (rootdir) + папку по нику
        Object inObj1 = msg.getObject()[0];
        Object inObj2 = msg.getObject()[1];
        if(inObj1 instanceof String && inObj2 instanceof String){
            Path tempPath1 = Paths.get((String) inObj1);
            Path folderRootPath = Paths.get((String) inObj2);
            System.out.println(tempPath1.toString());
            System.out.println(folderRootPath.toString());
            //создаем каталог
            Path newPath = Paths.get(rootPath.toString() + "\\" + tempPath1.subpath(1, tempPath1.getNameCount()).toString());
            try {
                Files.createDirectories(newPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("Invalid command!");
        }
    }
    //метод удаления файла
    private void deleteFileFromStorage(String filename){
        try {
            Files.delete(Paths.get(rootDirectory + nickname + "\\" + filename));//ищем и удаляем
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//метод сохранения файла
    private void saveFileToStorage(FileTransferMessage msg){
        try {
            Path relPath = Paths.get(msg.getPath()); // получаем путь где сохранить
            String tempPath = relPath.subpath(1, relPath.getNameCount()).toString();//разбираем
            Path newFilePath = Paths.get(rootDirectory + nickname + "\\" + tempPath);//создаем новый путь для данного клиента

            if(Files.exists(newFilePath)){ //если файл существует
            Files.write(newFilePath,
                    msg.getData(),
                    StandardOpenOption.TRUNCATE_EXISTING); // то мы его переписываем
            }else {                                   //если не сушествует
                Files.write(newFilePath,
                        msg.getData(),             //данные файла храняться в гед дата
                        StandardOpenOption.CREATE); // то мы его создаем и записываем
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//метод для сбора путей файлов в один лист
    private List<String> getClientFilesList(Object folderName){
        final List<String> fileList = new ArrayList<>();
        if(folderName != null){ //ищем ищем папку пользователя
            if(folderName.equals("..")){
                if(!fullFolderPath.equals(rootDirectory + nickname + "\\")){
                    fullFolderPath = Paths.get(fullFolderPath).getParent().toString() + "\\";
                    fileList.add("..");
                }
            }else {
                fileList.add("..");
                fullFolderPath += folderName + "\\";
            }
        }
        System.out.println(fullFolderPath);
        try {
            Files.newDirectoryStream(Paths.get(fullFolderPath)).forEach( //newDirectoryStream - поток элементов из директории
                    path -> fileList.add(path.getFileName().toString())); //запихиваем все файлы клиента? в лист
        } catch (IOException e) {
            e.printStackTrace();
        } return fileList; //возвращаем лист
    }

    //метод проверки автаризации!
    private void checkAuthorization(AuthMessage incomingMsg, ChannelHandlerContext ctx){
        if(incomingMsg != null){ //проверяем что сообшение есть
            nickname = getNickname(incomingMsg); //вытаскиваем никнейм из входятего сообщения

            if(nickname != null){
                System.out.println("Client Auth OK!");
                isLogged = true;
                fullFolderPath = rootDirectory + nickname + "\\"; //даем его рутфолдлер
                sendData(new CommandMessage(CommandMessage.AUTH_OK, null), ctx); //и отсылаем что авторизация прошла ок
            } else {
                System.out.println("Client not found!");
                isLogged = false;
            }
        }
    }

    //метод
    private String getNickname(AuthMessage msg){
       // return "kolo";
        return SQLConnect.checkAutorization(msg.getLogin(), msg.getPassword());
        //return DbConnector.getNickname(msg.getLogin(), msg.getPassword());
    }

    private void sendData(AbstractMessage msg, ChannelHandlerContext ctx){
        ChannelFuture channelFuture = ctx.writeAndFlush(msg); //берем месседж и отсылаем его в канал
        System.out.println("Message send.");
    }
}
