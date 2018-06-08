package com.mafia.mafiabot;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.ServerUpdater;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

/**
 *
 * @author Charlie Hands
 */
public class Commands {
    private static final List<User> mafia = new LinkedList<User>(), villagers = new LinkedList<User>(),
            saved = new LinkedList<User>(), doctors = new LinkedList<User>(), detectives = new LinkedList<User>(), dead = new LinkedList<User>();
    private static boolean locked = false;
    
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
    
    public static void save(User user){
        saved.add(user);
        if(saved.size() >= doctors.size()) locked = false;
    }
    
    public static String inspect(User user) throws NotInGameException{
        if(mafia.contains(user)) return "Mafia";
        if(doctors.contains(user)) return "Doctor";
        if(detectives.contains(user)) return "Detective";
        if(villagers.contains(user)) return "Villager";
        throw new NotInGameException();
    }
    
    public static void addMafia(MessageCreateEvent e,List<User>users) throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        mafia.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iill").get(0).addUser(x));
    }
    public static void addDoctors(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        doctors.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiii").get(0).addUser(x));
    }
    public static void addVillagers(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        villagers.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player illl").get(0).addUser(x));
    }
    public static void addDetectives(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        detectives.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiil").get(0).addUser(x));
    }
    public static void startNight(MessageCreateEvent e)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.DENIED).build()));
        locked = !doctors.isEmpty();
    }
    public static void endNight(MessageCreateEvent e)throws RoleNotHighEnoughException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.ALLOWED).build()));
        locked = false;
    }
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
