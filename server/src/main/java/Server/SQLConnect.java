package Server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SQLConnect {
    /**
     * Класс SQLConnect
     * В классе реализаванн основной функционал работы с базой данных
     * При создании экземпляра класса будуд задействованы методы:
     * connect() - создание JDBS коннекта к базе данных
     * createBD() - если БД не существует то она будет созданна
     * createTestUser() - создание тестового пользователя
     *
     * Основной метод checkAutorisation он отвечает за проверку полученных авторизационных данных.
     * В случае успеха метод вернет TRUE
     */
    private static Connection connection;
    private static Statement statement;
    private static Logger logger = LoggerFactory.getLogger(SQLConnect.class);

//    public static void main(String[] args) {
//        try {
//            connect();
//            System.out.println("tetet");
//            createBD();
//            createTestUser();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }finally {
//            disconnect();
//        }
//    }
    SQLConnect(){
        try {
            connect();
            createBD();
            createTestUser();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            disconnect();
        }
    }

    private static void createBD() {
        try {
            statement.execute("CREATE TABLE IF NOT EXISTS Users (" + "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "User STRING , Pass STRING  );");
            logger.info("BD create successful");

        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Cannot create BD");
        }
    }
    private static void createTestUser() {
        try {
            statement.executeUpdate("INSERT INTO Users (User,Pass) VALUES ('kolo','lopo');");
            logger.info("Tests user create in BD");

        } catch (SQLException e) {
            logger.error("Tests user already exist in BD");
        }
    }
    public static String checkAutorization(String login, String pass) {
        /**
         * Метод checkAutorisation возвращает TRUE в случае наличия в базе данных
         * записи равной login и pass.
         */
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM Users WHERE User = ? AND Pass = ?;");
            ps.setString(1,login);
            ps.setString(2,pass);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logger.info("User: "+ login +" authorized");
                return login;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getSQLState());
        }
        logger.error("User: " + login + " cannot authorized");
        return null;
    }


    public static void connect() throws Exception{
        System.out.println("Connection to the database.");
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:Server.db");
        statement = connection.createStatement();
    }

    private static void disconnect(){
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
