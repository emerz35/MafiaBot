package com.mafia.mafiabot;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.ServerUpdater;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

/**
 *The commands for the bot, also contains lists for roles in the game.
 * @author Charlie Hands
 */
public class Commands {
    //Lists for users in certain roles.
    private static final List<User> mafia = new LinkedList<User>(), villagers = new LinkedList<User>(),
            saved = new LinkedList<User>(), doctors = new LinkedList<User>(), detectives = new LinkedList<User>(), dead = new LinkedList<User>();
    
    //If anyone can be killed. Is set to true at startnight if there are doctors left alive.
    private static boolean locked = false;
    
    /**
     *Kills a person by setting their role to dead, removing any other Active Player role they have and removing them from the role lists.
     *@param user The user to kill.
     *@param e The message event to get the channel to reply and to get the server for roles.
     */
    public static void kill(User user, MessageCreateEvent e){
        if(!locked){
            if(!saved.contains(user)){
                dead.add(user);
                CompletableFuture<Void> update = new ServerUpdater(e.getServer().get()).removeAllRolesFromUser(user,e.getServer().get().getRoles().stream().filter(y -> y.getName().startsWith("Active Player")).collect(Collectors.toList()))
                        .addAllRolesToUser(user,e.getServer().get().getRolesByNameIgnoreCase("Dead")).update();
                doctors.remove(user);
                mafia.remove(user);
                detectives.remove(user);
                villagers.remove(user);
                e.getChannel().sendMessage(user.getName() + " was killed!");
            }
            else e.getChannel().sendMessage(user.getName() + " was saved by the doctor before death.");
            saved.clear();
        }
        else e.getChannel().sendMessage("The doctors haven't all chosen to save someone yet. Please try again later.");
    }
    /**
     * Saves a user so they can't be killed and allows a person to be killed if all the doctors have save someone.
     * @param user The user to save.
     */
    public static void save(User user){
        saved.add(user);
        if(saved.size() >= doctors.size()) locked = false;
    }
    /**
     * Checks what role the user is in and returns it.
     * @param user The user to inspect.
     * @return The role that the user has.
     * @throws NotInGameException if the user is not in the game or is dead.
     */
    public static String inspect(User user) throws NotInGameException{
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
     */
    public static void addMafia(MessageCreateEvent e,List<User>users) throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        mafia.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iill").get(0).addUser(x));
    }
    /**
     * Adds the users to the doctors list and gives them the doctor role.
     * @param e The message event to get the author and the server.
     * @param users The users to make doctors.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addDoctors(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        doctors.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiii").get(0).addUser(x));
    }
    /**
     * Adds the users to the villagers list and gives them the villager role.
     * @param e The message event to get the author and the server.
     * @param users The users to make villagers.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addVillagers(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        villagers.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player illl").get(0).addUser(x));
    }
    /**
     * Adds the users to the detectives list and gives them the detective role.
     * @param e The message event to get the author and the server.
     * @param users The users to make detectives.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addDetectives(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        detectives.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiil").get(0).addUser(x));
    }
    /**
     * Starts the night by muting everyone and sets locked to true if there are doctors still in the game.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void startNight(MessageCreateEvent e)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.DENIED).build()));
        locked = !doctors.isEmpty();
    }
    /**
     * Ends the night by unmuting everyone and sets locked to false.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void endNight(MessageCreateEvent e)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.ALLOWED).build()));
        locked = false;
    }
    /**
     * Ends the game by removing everyone in the game from 
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void endGame(MessageCreateEvent e)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
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
    }
}
