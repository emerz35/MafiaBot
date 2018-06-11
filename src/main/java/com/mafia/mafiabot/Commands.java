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
 *The commands for the bot, also contains lists for roles in the game.
 * @author Charlie Hands
 */
public class Commands {
    
    public static final List<Game> games = new LinkedList<>();
    /**
     *Kills a person by setting their role to dead, removing any other Active Player role they have and removing them from the role lists.
     *@param user The user to kill.
     *@param e The message event to get the channel to reply and to get the server for roles.
     * @throws RoleNotHighEnoughException
     * @throws NotInGameException
     */
    public static void kill(User user, MessageCreateEvent e) throws RoleNotHighEnoughException, NotInGameException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        inspect(user,e.getServer().get());
        if(!game.locked){
            if(!game.saved.contains(user)){
                game.dead.add(user);
                CompletableFuture<Void> update = new ServerUpdater(e.getServer().get()).removeAllRolesFromUser(user,e.getServer().get().getRoles().stream().filter(y -> y.getName().startsWith("Active Player")).collect(Collectors.toList()))
                        .addAllRolesToUser(user,e.getServer().get().getRolesByNameIgnoreCase("Dead")).update();
                game.doctors.remove(user);
                game.mafia.remove(user);
                game.detectives.remove(user);
                game.villagers.remove(user);
                e.getChannel().sendMessage(user.getName() + " was killed!");
            }
            else e.getChannel().sendMessage(user.getName() + " was saved by a doctor before death.");
            game.saved.clear();
        }
        else e.getChannel().sendMessage("The doctors haven't all chosen to save someone yet. Please try again later.");
    }
    /**
     * Accuses the user and starts a vote whether to kill them or not
     * @param user The user accused.
     * @param e The message event to get the author and the channel.
     * @throws RoleNotHighEnoughException
     * @throws NotInGameException 
     */
    public static void accuse(User user, MessageCreateEvent e) throws RoleNotHighEnoughException, NotInGameException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
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
                        Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    m.getReactionAddListeners().forEach(x -> m.removeListener(ReactionAddListener.class, x));
                    m.getReactionRemoveListeners().forEach(x->m.removeListener(ReactionRemoveListener.class, x));
                    
                    try {
                        if(playersfor.size() >= playersagainst.size()) {
                            kill(user,e);
                        }
                        else e.getChannel().sendMessage("There are not enough votes against " + user.getName()+ ". They have been spared.");
                    } catch (RoleNotHighEnoughException | NotInGameException | GameDoesntExistException ex) {
                        Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } 
            ).start();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * Saves a user so they can't be killed and allows a person to be killed if all the doctors have save someone.
     * @param user The user to save.
     * @param server The server the user is in.
     */
    public static void save(User user, Server server) throws GameDoesntExistException{
        Game game = getGame(server);
        game.saved.add(user);
        if(game.saved.size() >= game.doctors.size()) game.locked = false;
    }
    /**
     * Checks what role the user is in and returns it.
     * @param user The user to inspect.
     * @param server The server the user is in.
     * @return The role that the user has.
     * @throws NotInGameException if the user is not in the game or is dead.
     */
    public static String inspect(User user, Server server) throws NotInGameException, GameDoesntExistException{
        Game game = getGame(server);
        if(game.mafia.contains(user)) return "Mafia";
        if(game.doctors.contains(user)) return "Doctor";
        if(game.detectives.contains(user)) return "Detective";
        if(game.villagers.contains(user)) return "Villager";
        throw new NotInGameException("This person is not in the game or dead.");
    }
    /**
     * Adds the users to the mafia list and gives them the mafia role.
     * @param e The message event to get the author and the server.
     * @param users The users to make mafia.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addMafia(MessageCreateEvent e,List<User>users) throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        game.mafia.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iill").get(0).addUser(x));
    }
    /**
     * Adds the users to the doctors list and gives them the doctor role.
     * @param e The message event to get the author and the server.
     * @param users The users to make doctors.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addDoctors(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        game.doctors.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiii").get(0).addUser(x));
    }
    /**
     * Adds the users to the villagers list and gives them the villager role.
     * @param e The message event to get the author and the server.
     * @param users The users to make villagers.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addVillagers(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        game.villagers.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player illl").get(0).addUser(x));
    }
    /**
     * Adds the users to the detectives list and gives them the detective role.
     * @param e The message event to get the author and the server.
     * @param users The users to make detectives.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void addDetectives(MessageCreateEvent e,List<User>users)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        game.detectives.addAll(users);
        users.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active Player iiil").get(0).addUser(x));
    }
    /**
     * Starts the night by muting everyone and sets locked to true if there are doctors still in the game.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void startNight(MessageCreateEvent e)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.DENIED).build()));
        game.locked = !game.doctors.isEmpty();
    }
    /**
     * Ends the night by unmuting everyone and sets locked to false.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void endNight(MessageCreateEvent e)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        e.getServer().get().getRoles().forEach(x->x.updatePermissions(new PermissionsBuilder(x.getPermissions()).setState(PermissionType.VOICE_SPEAK,PermissionState.ALLOWED).build()));
        game.locked = false;
        game.saved.clear();
    }
    /**
     * Ends the game by removing everyone in the game from their roles and the role lists.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void endGame(MessageCreateEvent e)throws RoleNotHighEnoughException, GameDoesntExistException{
        if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
        Game game = getGame(e.getServer().get());
        game.mafia.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player iill").forEach(y -> y.removeUser(x)));
        game.doctors.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player iiii").forEach(y -> y.removeUser(x)));
        game.detectives.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player iiil").forEach(y -> y.removeUser(x)));
        game.villagers.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Active player illl").forEach(y -> y.removeUser(x)));
        game.dead.forEach(x -> e.getServer().get().getRolesByNameIgnoreCase("Dead").forEach(y -> y.removeUser(x)));
        game.mafia.clear();
        game.doctors.clear();
        game.detectives.clear();
        game.villagers.clear();
        game.dead.clear();
        game.saved.clear();
    }
    /**
     * Starts the game by sending a message for people to react to within 2 minutes, then decides the number of people to assign to each role.
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on the server.
     */
    public static void startGame(MessageCreateEvent e) throws RoleNotHighEnoughException{
        try {
            if(!e.getMessage().getAuthor().canManageRolesOnServer()) throw new RoleNotHighEnoughException();
            List<User> players = new LinkedList<>();
            Message m = e.getChannel().sendMessage("@here\nNew game started by " + e.getMessage().getAuthor().getName()+". React to join.").get();
            m.addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
            games.add(new Game(e.getServer().get()));
            new Thread(
                    () -> {
                        System.out.println("Thread running.");
                        m.addReactionAddListener(x -> {if(x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) players.add(x.getUser());});
                        m.addReactionRemoveListener(x -> {if(x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) players.remove(x.getUser());});
                        try {
                            Thread.sleep(120000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        players.remove(e.getApi().getYourself());
                        players.forEach((user) -> {
                            e.getChannel().sendMessage(user.getName());
                        });
                        m.getReactionAddListeners().forEach(x -> m.removeListener(ReactionAddListener.class, x));
                        m.getReactionRemoveListeners().forEach(x->m.removeListener(ReactionRemoveListener.class, x));
                        if(players.size() < 5) e.getChannel().sendMessage("Not enough people to start game. Please try again.");
                        else{
                            players.stream().forEach(x -> {
                                players.remove(x);
                                players.add(Main.r.nextInt(players.size()), x);
                            });
                            try {
                                addMafia(e,players.subList(0, (int)Math.ceil((double)players.size()/5.0)));
                                addDoctors(e,players.subList((int)Math.ceil((double)players.size()/5.0),(int)Math.ceil((double)(players.size())/10.0)));
                                addDetectives(e,players.subList((int)Math.ceil((double)(players.size())/10.0),(int)Math.ceil((double)(players.size())/10.0)));
                                addVillagers(e,players.subList((int)Math.ceil((double)(players.size())/10.0),players.size()));
                            } catch (RoleNotHighEnoughException | GameDoesntExistException ex) {
                                Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            e.getChannel().sendMessage("The game has started. Please join voice chat.");
                        }
                    }
            ).start();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static Game getGame(Server server) throws GameDoesntExistException{
        if(games.isEmpty()) throw new GameDoesntExistException();
        return games.stream().filter(x -> x.getServer().getId() == server.getId()).collect(Collectors.toList()).get(0);
    }
    
    public static void test(MessageCreateEvent e){
        e.getChannel().sendMessage("Yes");
        try {
            Thread.sleep(120000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }
}
