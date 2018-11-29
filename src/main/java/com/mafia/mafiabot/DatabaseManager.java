package com.mafia.mafiabot;

import java.sql.*;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

/**
*
* @author Charlie Hands
*/
public class DatabaseManager {
    private Connection conn;
    private final String url = "mysql://" + System.getenv("database-name")+":3306/";
    private void startConnection() throws ClassNotFoundException, SQLException{
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(url, System.getenv("database-user"), System.getenv("database-password"));
    }
    private void endConnection()throws SQLException{
        conn.close();
    }
    public void addPlayer(User user){
        try{
        startConnection();
        conn.createStatement().executeUpdate("INSERT INTO players VALUES ("+ user.getDiscriminatedName()+",0)");
        endConnection();
        }catch(SQLException|ClassNotFoundException e ){
            e.printStackTrace();
        }
    }
    public int getPlayerScore(User user){
        try{
        startConnection();
        ResultSet set = conn.createStatement().executeQuery("SELECT score FROM players WHERE displayName="+user.getDiscriminatedName()+";");
        endConnection();
        return set.getInt("score");
        }catch(SQLException|ClassNotFoundException e ){
            e.printStackTrace();
        }
        return 0;
    }
    public void createTable(){
    	try{
            startConnection();
            conn.createStatement().executeUpdate("DROP TABLE IF EXIST players;"
                    + "CREATE TABLE players ("
	    			+ "id INTEGER not NULL,"
	    			+ "displayName VARCHAR(255),"
	    			+ "score INTEGER not NULL,"
	    			+ "PRIMARY KEY (id));");
            endConnection();
    	}catch(SQLException | ClassNotFoundException e) {
    		e.printStackTrace();
    	}
    	
    }
    
}
