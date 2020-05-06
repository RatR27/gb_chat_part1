package com.rr27.server;

import java.sql.*;

public class UsersRepository {

    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement getNicknameStatement;
    private static PreparedStatement changeNickname;

    public static boolean connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:gbChat.db");
            statement = connection.createStatement();
            getNicknameStatement = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?;");
            changeNickname = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void disconnect(){
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNicknameByLoginAndPassword(String login, String pass) {
        String nick = null;
        try {
            getNicknameStatement.setString(1, login);
            getNicknameStatement.setString(2, pass);
            ResultSet rs = getNicknameStatement.executeQuery();
            if (rs.next()){
                nick= rs.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nick;
    }

    public static boolean updateNickname(String oldNickname, String newNickname) {
        try {
            changeNickname.setString(1, newNickname);
            changeNickname.setString(2, oldNickname);
            changeNickname.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
