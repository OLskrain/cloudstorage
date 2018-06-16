package Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;


public class CloudServer {
    private int port;
    private static final int MAX_OBJ_SIZE = 1024 * 1024 * 100; // 10 mb
    public CloudServer(int port) { //передаем порт
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup(); //пул потоков, который отвечает за подключение клиентов
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // пул потоков, который отвечает за тех клиентов, которые подключились
        try {
            ServerBootstrap b = new ServerBootstrap(); //класс помошник, который помогает настроить наш сервак
            b.group(mainGroup, workerGroup)            //мы говорим что используем 2 потока
                    .channel(NioServerSocketChannel.class) //в качестве каналов сы используем
                    .handler(new LoggingHandler(LogLevel.INFO)) //подключаем чтобы сыпались логи того, что происходит с серваком
                    .childHandler(new ChannelInitializer<SocketChannel>() { //мы для каналов настраиваем как наши данные будут проходить через фильтры
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //все тоже самое, кроме pipeline(). тут надо самому писать/менять хендлеры
                            socketChannel.pipeline().addLast(

                                    //ObjectDecoder стандартный. он возмет на себя преобразование байт буфера. тут указываеться какой максимально большой может
                                    // может прилететь объект(у нас 10 мб).
                                    new ObjectDecoder(MAX_OBJ_SIZE, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new CloudServerHandler()
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128) //настраиваем опции нашего сокета
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); //опция канала

            ChannelFuture future = b.bind(port).sync(); //запускаем сервак, который слушает порт
            future.channel().closeFuture().sync(); // как только работа закончится
        } finally { //закрывает пулы потоков
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        port = 8189;

        SQLConnect.connect();
       // DbConnector.connect(); // конектимся к базе?
        new CloudServer(port).run();
    }
}
