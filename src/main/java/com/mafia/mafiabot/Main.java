package com.mafia.mafiabot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;


public class Main {

    /**
     * The entrance point of our program.
     *
     * @param args The arguments for the program. The first element should be the bot's token.
     */
    public static void main(String[] args) {

        DiscordApi api = new DiscordApiBuilder().setToken("NDU0NDA2NzMzNzE3MTc2MzI0.Dfs-yA._ZTeOX49xTLnQIv4CfXc1YSvdwU").login().join();

        System.out.println("You can invite me by using the following url: " + api.createBotInvite());
        
        api.addMessageCreateListener(new MessageListener());

        api.addServerJoinListener(event -> System.out.println("Joined server " + event.getServer().getName()));
        api.addServerLeaveListener(event -> System.out.println("Left server " + event.getServer().getName()));
    }
}
