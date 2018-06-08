package com.mafia.mafiabot;

/**
 *
 * @author Charlie Hands
 */
public class RoleNotHighEnoughException extends Exception{
    public RoleNotHighEnoughException(){
        super("Your role is not high enough to access this command.");
    }
    public RoleNotHighEnoughException(String message){
        super(message);
    }    
}
