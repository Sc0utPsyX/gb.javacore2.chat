package ru.gb.may_chat.server.model;

import java.sql.*;

public class DatabaseHandler {
    private static Connection connection;
    private static Statement statement;


    public static String readUserDatabase(String name, String password){
        try(ResultSet resultSet = statement.executeQuery("SELECT * FROM users")){
            if (resultSet == null){
                return null;
            }
            while (resultSet.next()){
                if (resultSet.getString("username").equals(name) && resultSet.getString("password").equals(password)){
                    return resultSet.getString("nickname");
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static void changeNick(String name, String newName){
        try {
            connect();
            statement.executeQuery(new String("UPDATE users SET nickname = '" + newName + "' WHERE nickname = '" + name + "'"));
            disconnect();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }


    public static void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:may-22-chat-master/chat-server/src/main/java/ru/gb/may_chat/server/model/Users.db");
        statement = connection.createStatement();
    }

    public static void disconnect(){
        if (statement != null){
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
