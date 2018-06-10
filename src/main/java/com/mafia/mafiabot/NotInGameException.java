package com.mafia.mafiabot;

/**
 *
 * @author Charlie Hands
 */
public class NotInGameException extends Exception{
    public NotInGameException(){
        super("This player is not in the game or is dead.");
    }
    public NotInGameException(String message){
        super(message);
    }
    
}
