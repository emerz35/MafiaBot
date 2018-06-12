package com.mafia.mafiabot;

import java.util.LinkedList;
import java.util.List;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

/**
 *
 * @author Charlie Hands
 */
public class Game {
    //Lists for users in certain roles.
    public final List<User> mafia = new LinkedList<User>(), villagers = new LinkedList<User>(),
            saved = new LinkedList<User>(), doctors = new LinkedList<User>(), detectives = new LinkedList<User>(), dead = new LinkedList<User>();
    
    //If anyone can be killed. Is set to true at startnight if there are doctors left alive.
    public boolean locked = false;
    public User mod;
    Server server;
    public Game(Server server, User mod){
        this.server = server;
        this.mod = mod;
    }
    public Server getServer(){
        return server;
    }
}
