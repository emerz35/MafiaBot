package com.mafia.mafiabot;

/**
 *
 * @author Charlie hands
 */
public class GameDoesntExistException extends Exception{
    public GameDoesntExistException(){
        super("There is no game on the current server. Please start a game before using commands.");
    }
    public GameDoesntExistException(String message){
        super(message);
    }
}
