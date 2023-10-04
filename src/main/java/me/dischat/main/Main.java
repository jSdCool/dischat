package me.dischat.main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.EnumSet;
import java.util.Scanner;

import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("dischat");
    @Override
    public void onInitialize(){
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("initializing disChat");
        try {
            Initialize_discord_bot();
        } catch (Throwable e) {
            LOGGER.error("an error was encountered while trying to initialize JDA",e);
            //e.printStackTrace();
            //System.out.println("an error was encountered while trying to initialize JDA");
            discordConnected=false;
        }

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            ms=server;
            pm = ms.getPlayerManager();
            if(!discordConnected)
                pm.broadcast(Text.of("failed to connect to discoed"), false);
            });

        CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess,registrationEnvironment) -> {
            dispatcher.register(literal("dischat").requires(source -> source.hasPermissionLevel(3))
                    .then(literal("reload").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.of("reloading dischat"),true);
                        System.out.println("reloading dischat");
                        if(discordConnected)
                            shutDown();
                        discordConnected=false;
                        try {
                            Initialize_discord_bot();
                        } catch (LoginException e) {
                            LOGGER.error("failed to log into discord bot",e);
                            context.getSource().sendError(Text.of("failed to log into discord bot \nreload failed!"));
                            discordConnected=false;
                            return 0;
                        } catch (InterruptedException e) {
                            LOGGER.error("failed to initialise discord bot",e);
                            context.getSource().sendError(Text.of("failed to initialise discord bot \nreload failed!"));
                            discordConnected=false;
                            return 0;
                        } catch (InitializationFailedException e) {
                            context.getSource().sendError(Text.of("failed to initialise discord bot \nreload failed!"));
                            discordConnected=false;
                            return 0;
                        }
                        context.getSource().sendFeedback(() -> Text.of("dischat reloaded!!"),true);
                        discordConnected=true;
                        return 1;
                    }))

            );
        });


    }

    public static  Boolean discordConnected=true;
    public static JDA jda;
    static String botToken="",channelid="";
    static MinecraftServer ms;
    static PlayerManager pm;
    public static TextChannel chatChannel;
    public static final String modVersion ="1.1.0";

    static AuthedUsers discordAdmins;
    static final String authFileName="config/admins.auth";

    @SuppressWarnings("all")
    void Initialize_discord_bot() throws LoginException, InterruptedException, InitializationFailedException {
        // Note: It is important to register your ReadyListener before building
        File config;
        Scanner cfs;
        new File("config/").mkdirs();
        try {
            config = new File("config/dischat.cfg");
            cfs = new Scanner(config);
        }catch(Throwable e){
            try {
                FileWriter mr = new FileWriter("config/dischat.cfg");
                mr.write("#botToken=\n#sendServerId=\n#sendChannelId=");
                mr.close();
                System.out.println("config file created.");

            } catch (IOException ee) {

                LOGGER.error("An error occurred while load disChat config file",ee);
                discordConnected=false;
                return;
            }
            discordConnected=false;
            System.out.println("\n\n\ndischat config file created. populate the fields and then restart this server.\n\n\n");
            return;
        }
        String guildid="";
        while (cfs.hasNextLine()) {
            String line=cfs.nextLine();
            if(line.indexOf("#")==0){
                String pt1=line.substring(1,line.indexOf("="));
                String data=line.substring(line.indexOf("=")+1);
                if(pt1.equals("botToken")){
                    botToken=data;
                }
                if(pt1.equals("sendServerId")){
                    guildid=data;
                }
                if(pt1.equals("sendChannelId")){
                    channelid=data;
                }

            }
        }

        try {
            FileInputStream auths=new FileInputStream(authFileName);
            ObjectInputStream in =new ObjectInputStream(auths);
            discordAdmins=(AuthedUsers)in.readObject();
            in.close();
        }catch(IOException i) {
            discordAdmins=new AuthedUsers();
        }catch (ClassNotFoundException ignored) {}

         jda = JDABuilder.createDefault(botToken)
                 .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new ReadyListener())
                .addEventListeners(new MessageReceived())
                .build();

        // optionally block until JDA is ready
        jda.awaitReady();

        //check for MESSAGE_CONTENT intent. this currently does not work. even if the bot has the intent it will not register as having it

        EnumSet<GatewayIntent> intents =  jda.getGatewayIntents();
        if(!intents.contains(GatewayIntent.MESSAGE_CONTENT)){
            LOGGER.error("\n====================\nDiscord bot does not have MESSAGE_CONTENT intent\nthis intent is required for the operation of this mod\nto change this setting go to https://discord.com/developers/applications\n====================");
            jda.shutdownNow();
            discordConnected=false;
            throw new InitializationFailedException();
        }

        Guild g =jda.getGuildById(guildid);
        if(g == null){
            LOGGER.error("Invalid Discord server information. Check the config file");
            throw new InitializationFailedException();
        }
        chatChannel = g.getTextChannelById(channelid);
        if(chatChannel == null){
            LOGGER.error("Invalid Discord channel information. Check the config file");
            throw new InitializationFailedException();
        }

        Message.suppressContentIntentWarning();//prevent a warning from being sent to std out about message intent
    }



    public static void shutDown(){
        jda.shutdownNow();
        System.out.println("JDA shut down");
    }


    
}
