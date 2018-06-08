package com.mafia.mafiabot;

/**
 *
 * @author Charlie Hands
 */
public class NotInGameException extends Exception{
    public NotInGameException(){
        super("this player is not in the game.");
    }
    public NotInGameException(String message){
        super(message);
    }
    
}
