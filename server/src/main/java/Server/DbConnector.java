package Server;

import java.sql.*;

public class DbConnector {
    private static final String URL = ""; //посмотреть
    private static final String USER = "root";
    private static final String PASSWD = "zcofcjex";
    private static String pstmGetNicknameQuery = "SELECT nickname FROM users WHERE username = ? AND " + "password = ? ";
    private static String pstmCreateUserQuery = "INSERT INTO users " + "(fio, username, password, nickname)" + "VALUES(?, ?, ?, ?);";

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet resultSet;
    private static PreparedStatement pstmGetNickname, pstmRegisterUser;

    static void connect() throws SQLException{
        connection = DriverManager.getConnection(URL, USER, PASSWD);
        pstmGetNickname = connection.prepareStatement(pstmGetNicknameQuery);
        pstmRegisterUser = connection.prepareStatement(pstmCreateUserQuery);
    }

    static String getNickname(String username,String password){
        String nickname = null;

        try {
            pstmGetNickname.setString(1, username);
            pstmGetNickname.setString(2, password);
            resultSet = pstmGetNickname.executeQuery();
            System.out.println(resultSet);
            while (resultSet.next()){
                nickname = resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nickname;
    }
//метод регистрации
    static String registerUser(String fio, String username, String password, String nick){
        String nickname = null;

        try {
            pstmRegisterUser.setString(1, fio);
            pstmRegisterUser.setString(2, username);
            pstmRegisterUser.setString(3, password);
            pstmRegisterUser.setString(4, nick);

            nickname = getNickname(username, password);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nickname;
    }
}
