package com.mafia.mafiabot;

import java.sql.*;
import org.javacord.api.entity.user.User;

/**
*
* @author Charlie Hands
*/
public class DatabaseManager {
    private final String url = "mysql://playerdatabase:3306/";
    public DatabaseManager(){
        try{
           Class.forName("com.mysql.jdbc.Driver");
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }
    
    public void addPlayer(User user){
        try(Connection conn=DriverManager.getConnection(url,System.getenv("database-username"),System.getenv("database-password"))){
        conn.createStatement().executeUpdate("INSERT INTO players VALUES ("+ user.getDiscriminatedName()+",0)");
        }catch(SQLException e ){
            e.printStackTrace();
        }
    }
    public int getPlayerScore(User user){
        try(Connection conn=DriverManager.getConnection(url,System.getenv("database-username"),System.getenv("database-password"))){
        ResultSet set = conn.createStatement().executeQuery("SELECT score FROM players WHERE displayName="+user.getDiscriminatedName()+";");
        return set.getInt("score");
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0;
    }
    public void createTable(){
    	try(Connection conn=DriverManager.getConnection(url,System.getenv("database-username"),System.getenv("database-password"))){
            conn.createStatement().executeUpdate("DROP TABLE IF EXIST players;"
                    + "CREATE TABLE players ("
	    			+ "id INTEGER not NULL,"
	    			+ "displayName VARCHAR(255),"
	    			+ "score INTEGER not NULL,"
	    			+ "PRIMARY KEY (id));");
    	}catch(SQLException e) {
    		e.printStackTrace();
    	}
    	
    }
    
}
