package com.mafia.mafiabot;

import java.sql.*;
import org.javacord.api.entity.user.User;

/**
*
* @author Charlie Hands
*/
public class DatabaseManager {
    private Connection conn;
    private final String url = "jdbc:mysql://" + System.getenv("database-user")+"/"+System.getenv("database-name");
    private void startConnection() throws ClassNotFoundException{
        Class.forname("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(url, System.getenv("database-user"), System.getenv("database-password"));
    }
    private void endConnection(){
        conn.close();
    }
    public void addPlayer(User user){
        startConnection();
        endConnection();
    }
}
