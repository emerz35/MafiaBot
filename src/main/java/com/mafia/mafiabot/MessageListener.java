package com.mafia.mafiabot;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

/**
 *Listens for a new message.
 * @author Charlie Hands
 */
public class MessageListener implements MessageCreateListener{
    /**
     * Runs when a message is created and checks for the different commands.
     * @param e The message event
     */
    @Override
    public void onMessageCreate(MessageCreateEvent e) {
       String message = e.getMessage().getContent();
       try{
        switch(message.split(" ")[0].toLowerCase()){
            case "!kill": 
                if(e.getMessage().getMentionedUsers().contains(e.getApi().getYourself())) e.getChannel().sendMessage("Aarrrgh!\n*Dies*");
                Commands.kill(e.getMessage().getMentionedUsers().get(0), e);
                break;
            case "!save": 
                Commands.save(e.getMessage().getMentionedUsers().get(0));
                break;
            case "!inspect":
                e.getChannel().sendMessage(e.getMessage().getAuthor().getDisplayName() + " is a " + Commands.inspect(e.getMessage().getMentionedUsers().get(0)));
                break;
            case "!mafia":
                Commands.addMafia(e, e.getMessage().getMentionedUsers());
                break;
            case "!doctors": 
                Commands.addDoctors(e, e.getMessage().getMentionedUsers());
                break;
            case "!villagers":
                Commands.addVillagers(e, e.getMessage().getMentionedUsers());
                break;
            case "!detectives": 
                Commands.addDetectives(e, e.getMessage().getMentionedUsers());
                break;
            case "!startnight":
                Commands.startNight(e);
                break;
            case "!endnight": 
                Commands.endNight(e);
                break;
            case "!endgame": 
                Commands.endGame(e);
                break;
            case "!help": e.getChannel().sendMessage(
                    "Commands:\n!kill @name - Kill someone."
                + "\n!save @name - Save someone."
                + "\n!inspect @name - Inspect someone. Says if they are mafia, villager, doctor or detective."
                + "\n!mafia @names - Adds mafia to the game."
                + "\n!doctors @names - Adds doctors to the game."
                + "\n!villagers @names - Adds villagers to the game."
                + "\n!detectives @names - Adds detectives to the game."
                + "\n!startnight - Starts the night. Everyone is muted in voicechat (hopefully)."
                + "\n!endnight - Ends the night. Everyone is unmuted."
                + "\n!endgame - Removes game roles.");
                break;
            case "!startgame":Commands.startGame(e);
                break;
        }    
       }catch(RoleNotHighEnoughException | NotInGameException ex){
           e.getChannel().sendMessage(ex.getMessage());
       
       }
    } 
}
