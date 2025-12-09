package me.dischat.main;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.commands.Commands.LEVEL_ADMINS;
import static net.minecraft.commands.Commands.literal;

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
            pm = ms.getPlayerList();
            if(!discordConnected)
                pm.broadcastSystemMessage(Component.nullToEmpty("failed to connect to discoed"), false);
            });

        CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess,registrationEnvironment) -> dispatcher.register(literal("dischat").requires(Commands.hasPermission(LEVEL_ADMINS))
                .then(literal("reload").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.nullToEmpty("reloading dischat"),true);
                    LOGGER.info("reloading dischat");
                    if(discordConnected)
                        shutDown();
                    discordConnected=false;
                    try {
                        Initialize_discord_bot();
                    } catch (LoginException e) {
                        LOGGER.error("failed to log into discord bot",e);
                        context.getSource().sendFailure(Component.nullToEmpty("failed to log into discord bot \nreload failed!"));
                        discordConnected=false;
                        return 0;
                    } catch (InterruptedException e) {
                        LOGGER.error("failed to initialise discord bot",e);
                        context.getSource().sendFailure(Component.nullToEmpty("failed to initialise discord bot \nreload failed!"));
                        discordConnected=false;
                        return 0;
                    } catch (InitializationFailedException e) {
                        context.getSource().sendFailure(Component.nullToEmpty("failed to initialise discord bot \nreload failed!"));
                        discordConnected=false;
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.nullToEmpty("dischat reloaded!!"),true);
                    discordConnected=true;
                    return 1;
                }))
                .then(literal("auth")
                        .then(net.minecraft.commands.Commands.argument("userID", StringArgumentType.greedyString())
                                .executes( (context) -> {
                                    String id= getString(context,"userID");
                                    if(discordConnected) {
                                        if(discordAdmins.ids.contains(id)) {
                                            context.getSource().sendFailure(MutableComponent.create(new LiteralContents("error: that user is already authed")));
                                            return 0;
                                        }
                                        Task<List<Member>> membersTask = discordServer.loadMembers();
                                        membersTask.onSuccess((event) ->{
                                            Member member = event.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);

                                            if(member == null){
                                                context.getSource().sendFailure(MutableComponent.create(new LiteralContents("error: unable to find that user")));
                                                return;
                                            }
                                            discordAdmins.ids.add(id);
                                            saveAuths();
                                            context.getSource().sendSuccess(()-> MutableComponent.create(new LiteralContents("Added "+((member.getNickname()!=null)? member.getNickname()+"("+member.getUser().getName()+")":member.getUser().getName())+" to authorized Discord users")),true);
                                        });



                                    }else{
                                        context.getSource().sendFailure(MutableComponent.create(new LiteralContents("error: not connected to discord")));
                                        return 0;
                                    }

                                    return 1;
                                })))
                .then(literal("unAuth")
                        .then(net.minecraft.commands.Commands.argument("userID", StringArgumentType.greedyString())
                                .executes( (context) -> {
                                    String id= getString(context,"userID");
                                    if(discordAdmins.ids.contains(id)){
                                        discordAdmins.ids.remove(id);
                                        saveAuths();
                                        context.getSource().sendSuccess(() -> Component.nullToEmpty("Successfully removed "+id+" from authed discord users"),true);
                                    }else{
                                        context.getSource().sendFailure(MutableComponent.create(new LiteralContents("no user with that ID was authed")));
                                        return 0;
                                    }

                                    return 1;
                                })))
                .then(literal("status").then(net.minecraft.commands.Commands.argument("status",StringArgumentType.greedyString())
                        .executes((context -> {
                            if(discordConnected) {
                                botStatus= getString(context,"status");
                                jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.of(Activity.ActivityType.CUSTOM_STATUS,botStatus));
                                return 1;
                            }else {
                                context.getSource().sendFailure(MutableComponent.create(new LiteralContents("error: not connected to discord")));
                                return 0;
                            }

                        }))
                ))

        ));


    }

    public static  Boolean discordConnected=true;
    public static JDA jda;
    static String botToken="",channelid="";
    static MinecraftServer ms;
    static PlayerList pm;
    public static TextChannel chatChannel;
    static Guild discordServer;
    public static final String modVersion ="1.2.3";

    static AuthedUsers discordAdmins;
    static final String authFileName="config/admins.auth";
    static String botStatus="";

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
                mr.write("#botToken=\n#sendServerId=\n#sendChannelId=\n#defaultStatus=Connected to Minecraft chat");
                mr.close();
                LOGGER.info("config file created.");

            } catch (IOException ee) {

                LOGGER.error("An error occurred while load disChat config file",ee);
                discordConnected=false;
                return;
            }
            discordConnected=false;
            LOGGER.warn("\n\n\ndischat config file created. populate the fields and then run /dischat reload\n\n\n");
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
                if(pt1.equals("defaultStatus")){
                    botStatus=data;
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
                 .enableIntents(GatewayIntent.MESSAGE_CONTENT,GatewayIntent.GUILD_MEMBERS)
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
        boolean guidMemebrsIntent = intents.contains(GatewayIntent.GUILD_MEMBERS);

        discordServer =jda.getGuildById(guildid);
        if(discordServer == null){
            LOGGER.error("Invalid Discord server information. Check the config file");
            throw new InitializationFailedException();
        }
        chatChannel = discordServer.getTextChannelById(channelid);
        if(chatChannel == null){
            LOGGER.error("Invalid Discord channel information. Check the config file");
            throw new InitializationFailedException();
        }

        if(guidMemebrsIntent) {
            discordServer.loadMembers();
        }else{
            LOGGER.warn("Discord bot does not have the GUILD_MEMBERS intent.\nthis intent is required authorizing users\nto change this setting go to https://discord.com/developers/applications");
        }

        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.of(Activity.ActivityType.CUSTOM_STATUS,botStatus));
        Message.suppressContentIntentWarning();//prevent a warning from being sent to std out about message intent
    }



    public static void shutDown(){
        LOGGER.info("JDA awaiting shut down");
        jda.shutdown();
        try {
            jda.awaitShutdown();
        } catch (InterruptedException e) {
            LOGGER.error("Error shutting down JDA",e);
        }
    }

    public static void saveAuths() {
        try {
            FileOutputStream fileOut =new FileOutputStream(authFileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(discordAdmins);
            out.close();
            fileOut.close();
        }catch(IOException i) {
            LOGGER.error("Exception while saving auth list",i);
        }
    }
    
}
