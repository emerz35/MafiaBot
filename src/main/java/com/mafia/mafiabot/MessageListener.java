package com.mafia.mafiabot;

import com.vdurmont.emoji.EmojiParser;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

/**
 * Listens for a new message.
 *
 * @author Charlie Hands
 */
public class MessageListener implements MessageCreateListener {

    public static final List<Game> games = new LinkedList<>();

    /**
     * Runs when a message is created and checks for the different commands.
     *
     * @param e The message event
     */
    @Override
    public void onMessageCreate(MessageCreateEvent e) {
        String message = e.getMessage().getContent();
        try {
            if (!e.getMessage().getAuthor().isYourself()) {
                switch (message.split(" ")[0].toLowerCase()) {
                    case "!creator":
                        e.getChannel().sendMessage("I was made by FlyingLongSword-sama");
                        break;
                    case "!gettoken":
                        e.getChannel().sendMessage("No");
                        break;
                    case "!java":
                        e.getChannel().sendMessage("I was made in Java with the Javacord library.");
                        break;
                    case "!help":
                        e.getChannel().sendMessage(
                                "game:\n!kill @name - Kill someone."
                                + "\n!accuse @name - Accuses someone and starts a vote to see if they will die or not."
                                + "\n!save @name - Save someone."
                                + "\n!inspect @name - Inspect someone. Says if they are mafia, villager, doctor or detective."
                                + "\n!mafia @names - Adds mafia to the game."
                                + "\n!doctors @names - Adds doctors to the game."
                                + "\n!villagers @names - Adds villagers to the game."
                                + "\n!detectives @names - Adds detectives to the game."
                                + "\n!startnight - Starts the night. Everyone is muted in voicechat (hopefully)."
                                + "\n!endnight - Ends the night. Everyone is unmuted."
                                + "\n!startgame - Sends a message for people to join a game. React to join."
                                + "\n!endgame - Removes game roles.");
                        break;
                    case "!startgame":
                        startGame(e);
                        break;
                    case "!addplayer":
                        Main.dbmanager.addPlayer(e.getMessage().getMentionedUsers().get(0));
                        break;
                    case "!getplayercore":
                        e.getChannel().sendMessage(""+Main.dbmanager.getPlayerScore(e.getMessage().getMentionedUsers().get(0)));
                        break;
                    default:

                        Game game = getGame(e.getServer().get());

                        switch (message.split(" ")[0].toLowerCase()) {
                            case "!kill":
                                if (e.getMessage().getMentionedUsers().contains(e.getApi().getYourself())) {
                                    e.getChannel().sendMessage("Aarrrgh!\n*Dies*");
                                }
                                game.kill(e.getMessage().getMentionedUsers().get(0), e);
                                break;
                            case "!save":
                                game.save(e.getMessage().getMentionedUsers().get(0), e.getServer().get());
                                break;
                            case "!inspect":
                                e.getChannel().sendMessage(e.getMessage().getAuthor().getDisplayName() + " is a " + game.inspect(e.getMessage().getMentionedUsers().get(0), e.getServer().get()));
                                break;
                            case "!mafia":
                                game.addMafia(e, e.getMessage().getMentionedUsers());
                                break;
                            case "!doctors":
                                game.addDoctors(e, e.getMessage().getMentionedUsers());
                                break;
                            case "!villagers":
                                game.addVillagers(e, e.getMessage().getMentionedUsers());
                                break;
                            case "!detectives":
                                game.addDetectives(e, e.getMessage().getMentionedUsers());
                                break;
                            case "!startnight":
                                game.startNight(e);
                                break;
                            case "!endnight":
                                game.endNight(e);
                                break;
                            case "!endgame":
                                game.endGame(e);
                                break;
                            case "!accuse":
                                game.accuse(e.getMessage().getMentionedUsers().get(0), e);
                                break;

                            /*case "!test": game.test(e);
                break;*/
                        }
                }
            }
        } catch (RoleNotHighEnoughException | NotInGameException | GameDoesntExistException ex) {
            e.getChannel().sendMessage(ex.getMessage());
        }

    }

    /**
     * Starts the game by sending a message for people to react to within 2
     * minutes, then decides the number of people to assign to each role.
     *
     * @param e The message event to get the server.
     * @throws RoleNotHighEnoughException if the author can't manage roles on
     * the server.
     */
    public void startGame(MessageCreateEvent e) throws RoleNotHighEnoughException {
        if(!e.getMessage().getUserAuthor().get().getRoles(e.getServer().get()).contains(e.getServer().get().getRolesByNameIgnoreCase("active moderator").get(0)))throw new RoleNotHighEnoughException("You have to be the active moderator run this command.");
        try {
            List<User> players = new LinkedList<>();
            Message m = e.getChannel().sendMessage("@here\nNew game started by " + e.getMessage().getAuthor().getName() + ". React to join.").get();
            m.addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
            Game game = new Game(e.getServer().get(), e.getMessage().getUserAuthor().get());
            games.add(game);
            e.getServer().get().getRolesByNameIgnoreCase("active moderator").forEach(x -> e.getMessage().getUserAuthor().get().addRole(x));
            e.getServer().get().getRolesByNameIgnoreCase("trained moderator").forEach(x -> e.getMessage().getUserAuthor().get().removeRole(x));
            new Thread(
                    () -> {
                        m.addReactionAddListener(x -> {
                            if (x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) {
                                players.add(x.getUser());
                            }
                        });
                        m.addReactionRemoveListener(x -> {
                            if (x.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) {
                                players.remove(x.getUser());
                            }
                        });
                        try {
                            Thread.sleep(120000);
                        } catch (InterruptedException ex) {

                        }
                        players.remove(e.getApi().getYourself());
                        players.forEach((user) -> {
                            e.getChannel().sendMessage(user.getName());
                        });
                        m.getReactionAddListeners().forEach(x -> m.removeListener(ReactionAddListener.class, x));
                        m.getReactionRemoveListeners().forEach(x -> m.removeListener(ReactionRemoveListener.class, x));
                        if (players.size() < 5) {
                            e.getChannel().sendMessage("Not enough people to start game. Please try again.");
                        } else {
                            players.stream().forEach(x -> {
                                players.remove(x);
                                players.add(Main.r.nextInt(players.size()), x);
                            });
                            try {
                                game.addMafia(e, players.subList(0, (int) Math.ceil((double) players.size() / 5.0)));
                                game.addDoctors(e, players.subList((int) Math.ceil((double) players.size() / 5.0), (int) Math.ceil((double) (players.size()) / 10.0)));
                                game.addDetectives(e, players.subList((int) Math.ceil((double) (players.size()) / 10.0), (int) Math.ceil((double) (players.size()) / 10.0)));
                                game.addVillagers(e, players.subList((int) Math.ceil((double) (players.size()) / 10.0), players.size()));
                            } catch (RoleNotHighEnoughException | GameDoesntExistException ex) {

                            }
                            e.getChannel().sendMessage("The game has started. Please join voice chat.");
                        }
                    }
            ).start();
        } catch (InterruptedException | ExecutionException ex) {

        }
    }

    /**
     * Gets the game of the server.
     *
     * @param server The server of the game to get from.
     * @return The game of the server.
     * @throws GameDoesntExistException
     */
    public static Game getGame(Server server) throws GameDoesntExistException {
        if (games.isEmpty()) {
            throw new GameDoesntExistException();
        }
        return games.stream().filter(x -> x.getServer().getId() == server.getId()).collect(Collectors.toList()).get(0);
    }
}
