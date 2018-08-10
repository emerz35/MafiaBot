package com.mafia.mafiabot;

import com.vdurmont.emoji.EmojiParser;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.ServerUpdater;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

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
    
    /**
     *Kills a person by setting their role to dead, removing any other Active Player role they have and removing them from the role lists.
     *@param user The user to kill.
     *@param e The message event to get the channel to reply and to get the server for roles.
     * @throws RoleNotHighEnoughException
     * @throws NotInGameException
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void kill(User user, MessageCreateEvent e) throws RoleNotHighEnoughException, NotInGameException, GameDoesntExistException{
        //if(e.getServer().get().getHighestRoleOf(e.getMessage().getUserAuthor().get()).get().getPosition() < e.getServer().get().getRolesByNameIgnoreCase("active moderator").get(0).getPosition()) throw new RoleNotHighEnoughException("You have to be the active moderator or higher to run this command.");
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        inspect(user,e.getServer().get());
        if(!locked){
            if(!saved.contains(user)){
                dead.add(user);
                CompletableFuture<Void> update = new ServerUpdater(getServer()).removeRolesFromUser(user,getServer().getRoles().stream().filter(y -> y.getName().startsWith("Active Player")).collect(Collectors.toList()))
                        .addRolesToUser(user,getServer().getRolesByNameIgnoreCase("Dead")).update();
                doctors.remove(user);
                mafia.remove(user);
                detectives.remove(user);
                villagers.remove(user);
                e.getChannel().sendMessage(user.getName() + " was killed!");
            }
            else e.getChannel().sendMessage(user.getName() + " was saved by a doctor before death.");
            saved.clear();
        }
        else e.getChannel().sendMessage("The doctors haven't all chosen to save someone yet. Please try again later.");
    }
    /**
     * Accuses the user and starts a vote whether to kill them or not
     * @param user The user accused.
     * @param e The message event to get the author and the channel.
     * @throws RoleNotHighEnoughException
     * @throws NotInGameException 
     * @throws com.mafia.mafiabot.GameDoesntExistException 
     */
    public void accuse(User user, MessageCreateEvent e) throws RoleNotHighEnoughException, NotInGameException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        inspect(user,e.getServer().get());
        try {
            Message m = e.getChannel().sendMessage(user.getName() + " has been accused. Are they guilty? React :white_check_mark: for yes and :x: for no.").get();
            m.addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
            m.addReaction(EmojiParser.parseToUnicode(":x:"));
            new Thread(
                () -> {
                    final List<User> playersfor = new LinkedList<>();
                    final List<User> playersagainst = new LinkedList<>();
                    m.addReactionAddListener(x -> {
                        if(x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) playersfor.add(x.getUser());
                        else if(x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":x:"))) playersagainst.add(x.getUser());
                    
                    });
                    m.addReactionRemoveListener(x -> {
                        if(x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) playersfor.remove(x.getUser());
                        else if(x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":x:"))) playersagainst.remove(x.getUser());
                    });
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    m.getReactionAddListeners().forEach(x -> m.removeListener(ReactionAddListener.class, x));
                    m.getReactionRemoveListeners().forEach(x->m.removeListener(ReactionRemoveListener.class, x));
                    
                    try {
                        if(playersfor.size() >= playersagainst.size()) {
                            kill(user,e);
                        }
                        else e.getChannel().sendMessage("There are not enough votes against " + user.getName()+ ". They have been spared.");
                    } catch (RoleNotHighEnoughException | NotInGameException | GameDoesntExistException ex) {
                        Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } 
            ).start();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * Saves a user so they can't be killed and allows a person to be killed if all the doctors have save someone.
     * @param user The user to save.
     * @param server The server the user is in.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void save(User user, Server server) throws GameDoesntExistException{
        saved.add(user);
        if(saved.size() >= doctors.size()) locked = false;
    }
    /**
     * Checks what role the user is in and returns it.
     * @param user The user to inspect.
     * @param server The server the user is in.
     * @return The role that the user has.
     * @throws NotInGameException if the user is not in the game or is dead.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public String inspect(User user, Server server) throws NotInGameException, GameDoesntExistException{
        if(mafia.contains(user)) return "Mafia";
        if(doctors.contains(user)) return "Doctor";
        if(detectives.contains(user)) return "Detective";
        if(villagers.contains(user)) return "Villager";
        throw new NotInGameException("This person is not in the game or dead.");
    }
    /**
     * Adds the users to the mafia list and gives them the mafia role.
     * @param e The message event to get the author and the server.
     * @param users The users to make mafia.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void addMafia(MessageCreateEvent e,List<User>users) throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        mafia.addAll(users);
        users.forEach(x -> getServer().getRolesByNameIgnoreCase("Active Player iill").get(0).addUser(x));
    }
    /**
     * Adds the users to the doctors list and gives them the doctor role.
     * @param e The message event to get the author and the server.
     * @param users The users to make doctors.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void addDoctors(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        doctors.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiii").get(0).addUser(x));
    }
    /**
     * Adds the users to the villagers list and gives them the villager role.
     * @param e The message event to get the author and the server.
     * @param users The users to make villagers.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void addVillagers(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        villagers.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player illl").get(0).addUser(x));
    }
    /**
     * Adds the users to the detectives list and gives them the detective role.
     * @param e The message event to get the author and the server.
     * @param users The users to make detectives.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void addDetectives(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        detectives.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiil").get(0).addUser(x));
    }
    /**
     * Starts the night by muting everyone and sets locked to true if there are doctors still in the game.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void startNight(MessageCreateEvent e)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");       
        //e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.DENIED).build()));
        mafia.forEach(x -> x.mute(server));
        villagers.forEach(x -> x.mute(server));
        detectives.forEach(x -> x.mute(server));
        doctors.forEach(x -> x.mute(server));
        locked = !doctors.isEmpty();
    }
    /**
     * Ends the night by unmuting everyone and sets locked to false.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void endNight(MessageCreateEvent e)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        mafia.forEach(x -> x.unmute(server));
        villagers.forEach(x -> x.unmute(server));
        detectives.forEach(x -> x.unmute(server));
        doctors.forEach(x -> x.unmute(server));
        locked = false;
        saved.clear();
    }
    /**
     * Ends the game by removing everyone in the game from their roles and the role lists.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     * @throws com.mafia.mafiabot.GameDoesntExistException
     */
    public void endGame(MessageCreateEvent e)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getUserAuthor().get().getRoles(server).contains(server.getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        mafia.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player iill").forEach(y -> y.removeUser(x)));
        doctors.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player iiii").forEach(y -> y.removeUser(x)));
        detectives.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player iiil").forEach(y -> y.removeUser(x)));
        villagers.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player illl").forEach(y -> y.removeUser(x)));
        dead.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Dead").forEach(y -> y.removeUser(x)));
        mafia.clear();
        doctors.clear();
        detectives.clear();
        villagers.clear();
        dead.clear();
        saved.clear();
        mod = null;
    }
    
}
