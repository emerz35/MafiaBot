package com.mafia.mafiabot;

import java.util.Random;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;


public class Main {
    private static final String token = "NDU0NDA2NzMzNzE3MTc2MzI0.Dfs-yA._ZTeOX49xTLnQIv4CfXc1YSvdwU";
    public static final Random r = new Random();
    /**
     * The main bot class to run, adds message create listeners
     *
     * @param args The arguments for the program.
     */
    public static void main(String[] args) {

        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

        System.out.println("You can invite me by using the following url: " + api.createBotInvite());
        
        api.addMessageCreateListener(new MessageListener());

        api.addServerJoinListener(event -> System.out.println("Joined server " + event.getServer().getName()));
        api.addServerLeaveListener(event -> System.out.println("Left server " + event.getServer().getName()));
    }
}
