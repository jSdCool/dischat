package me.dischat.main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static net.minecraft.server.command.CommandManager.literal;

public class Main extends ListenerAdapter implements ModInitializer {
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
                            e.printStackTrace();
                            context.getSource().sendError(Text.of("failed to log into discord bot \nreload failed!"));
                            discordConnected=false;
                            return 0;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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

    void Initialize_discord_bot() throws LoginException, InterruptedException, InitializationFailedException {
        // Note: It is important to register your ReadyListener before building
        File config;
        Scanner cfs;
        try {
            config = new File("config\\dischat.cgf");
            cfs = new Scanner(config);
        }catch(Throwable e){
            try {
                FileWriter mr = new FileWriter("config\\dischat.cgf");
                mr.write("#botToken=\n#sendServerId=\n#sendChannelId=");
                mr.close();
                System.out.println("config file created.");

            } catch (IOException ee) {
                System.out.println("\n\n\nAn error occurred while creating dischat config file. you may need to make the config folder if it does not already exist\n\n\n");
                ee.printStackTrace();
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

         jda = JDABuilder.createDefault(botToken)
                .addEventListeners(new ReadyListener())
                .addEventListeners(new Main())
                .build();

        // optionally block until JDA is ready
        jda.awaitReady();
        //check for MESSAGE_CONTENT intent. this currently does not work. even if the bot has the intent it will not register as having it
        /*
        EnumSet<GatewayIntent> intents =  jda.getGatewayIntents();
        for(GatewayIntent i : intents){
            LOGGER.info(i.toString());
        }
        if(!intents.contains(GatewayIntent.MESSAGE_CONTENT)){
            LOGGER.error("\n====================\nDiscord bot does not have MESSAGE_CONTENT intent\nthis intent is required for the operation of this mod\nto change this setting go to https://discord.com/developers/applications\n====================");
            jda.shutdownNow();
            discordConnected=false;
            throw new InitializationFailedException();
        }
        */
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

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        TextChannel channel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();
        User author = msg.getAuthor();
        String content = msg.getContentRaw();
        //String contentSections[] = content.split(" ");
        //System.out.println(author + " " + content);
        if(!channel.getId().equals(channelid)){
            return;
        }
        if(author.isBot()) {
            return;
        }
        if(content.equals("/list")){
            List<ServerPlayerEntity> players = pm.getPlayerList();
            if(players.size()==0){
                channel.sendMessage("there are no players online").queue();
                return;
            }
            String playersOut="";
            for(int i=0;i<players.size();i++){
                playersOut+=players.get(i).getName().getString()+" \n";
            }
            channel.sendMessage(playersOut).queue();
            return;
        }
        String name;
        if(event.getMember().getNickname()==null){
            name = author.getName();
        }else{
            name = event.getMember().getNickname();
        }

        Main.pm.broadcast(Text.of("§9Discord §r["+name+"] "+content), false);

    }

    public static void shutDown(){
        jda.shutdownNow();
        System.out.println("JDA shut down");
    }


    
}
