package me.dischat.main;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.MinecraftVersion;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.PlainTextContent.Literal;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.Optional;

public class MessageReceived extends ListenerAdapter {
    @SuppressWarnings("all")
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        TextChannel channel = event.getChannel().asTextChannel();
        //Guild guild = event.getGuild();
        User author = msg.getAuthor();
        String content = msg.getContentRaw();
        String[] contentSections = content.split(" ");
        //System.out.println(author + " " + content);
        if(!channel.getId().equals(Main.channelid)){
            return;
        }
        if(author.isBot()) {
            return;
        }

        //discord commands
        if(content.equals("/list")){
            List<ServerPlayerEntity> players = Main.pm.getPlayerList();
            if(players.isEmpty()){
                channel.sendMessage("there are no players online").queue();
                return;
            }
            StringBuilder playersOut= new StringBuilder();
            for (ServerPlayerEntity player : players) {
                playersOut.append(player.getName().getString()).append(" \n");
            }
            channel.sendMessage(playersOut.toString()).queue();
            return;
        }
        if(content.equals("/version")){
            channel.sendMessage("mod version: "+Main.modVersion+"\ngame version: "+ MinecraftVersion.CURRENT.getName()).queue();
            return;
        }

        if(contentSections[0].equals("/tp")){
            if(Main.discordAdmins.ids.contains(author.getId())) {
                if (contentSections.length < 5) {
                    channel.sendMessage("missing parameters").queue();
                    return;
                }
                double x , y, z ;
                try {
                    x = Double.parseDouble(contentSections[2]);
                    y = Double.parseDouble(contentSections[3]);
                    z = Double.parseDouble(contentSections[4]);
                } catch (NumberFormatException n) {
                    channel.sendMessage("value entered was not a number").queue();
                    return;
                }
                //teleport the player
                if(Main.pm.getPlayerNames().length >0 && hasPlayer(contentSections[1])) {
                    ServerPlayerEntity player = Main.pm.getPlayer(contentSections[1]);

                    player.teleport(player.getServerWorld(),x, y, z,player.getYaw(),player.getPitch());
                    channel.sendMessage("teleported player").queue();
                }else{
                    channel.sendMessage("player not found").queue();
                }

            }else{
                channel.sendMessage("you are not authorized to use this command").queue();
            }
            return;
        }

        if(contentSections[0].equals("/pos")) {
            if(Main.discordAdmins.ids.contains(author.getId())){
                if(contentSections.length<2) {
                    channel.sendMessage("missing parameters").queue();
                    return;
                }

                if(Main.pm.getPlayerNames().length>0&&hasPlayer(contentSections[1])) {
                    ServerPlayerEntity player = Main.pm.getPlayer(contentSections[1]);
                    Vec3d cords = player.getPos();
                    channel.sendMessage(contentSections[1]+": "+cords.x+" "+cords.y+" "+cords.z+" "+player.getServerWorld().getRegistryKey().getValue()).queue();
                }else{
                    channel.sendMessage("player not found").queue();
                }

            }else {
                channel.sendMessage("you are not authorized to use this command").queue();
            }
            return;
        }

        if(contentSections[0].equals("/kickMC")) {
            if(Main.discordAdmins.ids.contains(author.getId())){
                if(contentSections.length<2) {
                    channel.sendMessage("missing parameters").queue();
                    return;
                }
                StringBuilder reason= new StringBuilder();
                for(int j=2;j<contentSections.length;j++) {
                    reason.append(contentSections[j]).append(" ");
                }

                String finalReason = reason.toString();

                if(Main.pm.getPlayerNames().length>0&&hasPlayer(contentSections[1])) {
                    ServerPlayerEntity player = Main.pm.getPlayer(contentSections[1]);
                    if(finalReason.isEmpty())
                        finalReason="kicked by an operator from discord";
                    player.networkHandler.disconnect(MutableText.of(new Literal((finalReason))));
                    channel.sendMessage("kicked "+contentSections[1]+": "+finalReason).queue();
                    System.out.println("kicked "+contentSections[1]+": "+finalReason);
                }else{
                    channel.sendMessage("player not found").queue();
                }

            }else {
                channel.sendMessage("you are not authorized to use this command").queue();
            }
            return;
        }

        if(contentSections[0].equals("/gamemode")) {
            if(Main.discordAdmins.ids.contains(author.getId())){
                if(contentSections.length<3) {
                    channel.sendMessage("missing parameters").queue();
                    return;
                }

                if(Main.pm.getPlayerNames().length>0&&hasPlayer(contentSections[1])) {
                    ServerPlayerEntity player = Main.pm.getPlayer(contentSections[1]);
                    GameMode mode=GameMode.DEFAULT;
                    switch(contentSections[2]){
                        case "creative":
                            mode=GameMode.CREATIVE;
                            break;
                        case "survival":
                            mode=GameMode.SURVIVAL;
                            break;
                        case "adventure":
                            mode=GameMode.ADVENTURE;
                            break;
                        case "spectator":
                            mode=GameMode.SPECTATOR;
                            break;
                        default:
                            channel.sendMessage("invalid gamemode").queue();

                    }
                    player.changeGameMode(mode);
                    channel.sendMessage("gamemode updated").queue();
                    player.sendMessage(MutableText.of(new Literal("gamemode updated")),false);
                }else{
                    channel.sendMessage("player not found").queue();
                }

            }else {
                channel.sendMessage("you are not authorized to use this command").queue();
            }
            return;
        }

        if(contentSections[0].equals("/whitelist")){
            if(!Main.discordAdmins.ids.contains(author.getId())){
                channel.sendMessage("you are not authorized to use this command").queue();
                return;
            }
            if(contentSections.length<3) {
                channel.sendMessage("missing parameters").queue();
                return;
            }

            if(contentSections[1].equals("add")){
                //attempt to get the profile of the inputed name
                Optional<GameProfile> profileOptional = Main.ms.getUserCache().findByName(contentSections[2]);
                //check if the profile was found
                if(profileOptional.isEmpty()){
                    //if not then send the error to the user
                    channel.sendMessage("That player does not exist").queue();
                    return;
                }
                GameProfile gp = profileOptional.get();
                Whitelist whitelist = Main.pm.getWhitelist();
                WhitelistEntry whitelistEntry = new WhitelistEntry(gp);
                //actualy add the player to the whitlist
                whitelist.add(whitelistEntry);

                Main.LOGGER.info("Added "+ gp.getName()+" to the Whitelist from discord");
                channel.sendMessage("Added "+gp.getName()+" to the whitelist").queue();

            } else if (contentSections[1].equals("remove")) {
                //attempt to get the profile of the inputed name
                Optional<GameProfile> profileOptional = Main.ms.getUserCache().findByName(contentSections[2]);
                //check if the profile was found
                if(profileOptional.isEmpty()){
                    //if not then send the error to the user
                    channel.sendMessage("That player does not exist").queue();
                    return;
                }
                GameProfile gp = profileOptional.get();
                Whitelist whitelist = Main.pm.getWhitelist();
                if(!whitelist.isAllowed(gp)){
                    channel.sendMessage("That player is not currely whitelisted").queue();
                    return;
                }
                WhitelistEntry whitelistEntry = new WhitelistEntry(gp);
                //actualy remove the player to the whitlist
                whitelist.remove(whitelistEntry);

                Main.LOGGER.info("Removed "+ gp.getName()+" from the Whitelist from discord");
                channel.sendMessage("Removed "+gp.getName()+" from the whitelist").queue();
            }else {
                if(contentSections[1].toLowerCase().equals("@everyone")){
                    channel.sendMessage("@ everyone is not a valid option for this command").queue();
                }else
                channel.sendMessage("Unknown argument: "+contentSections[1]).queue();
            }
            return;
        }

        if(contentSections[0].equals("/team")){
            ServerScoreboard scb = Main.ms.getScoreboard();
            if(contentSections.length == 1){
                channel.sendMessage("This command expreded at leased 1 argument but none were present").queue();
                return;
            }
            if(contentSections[1].equals("list")){
                if(contentSections.length==2) {
                    String teams = String.join("\n", scb.getTeamNames());

                    channel.sendMessage("The server has the folowing teams:\n"+teams).queue();
                }else {
                    for(int i=2;i<contentSections.length;i++){
                        String lookForTeam = contentSections[i];
                        Team team = scb.getTeam(lookForTeam);
                        if(team == null){
                            channel.sendMessage("Did not find a team named: "+lookForTeam).queue();
                        }else{
                            String teamMembers = String.join(", ",team.getPlayerList());
                            channel.sendMessage("The team "+lookForTeam+" has the folowing members: ["+teamMembers+"]").queue();
                        }
                    }
                }
            }else{
                channel.sendMessage("Unknown argument");
            }
            return;
        }

        if(content.equals("/help")) {
            channel.sendMessage("send messages in this channel to make them appear in Mincreaft\n===COMMANDS===\n/list    list online players\n/version    get the version of this mod and the game\n===MODERATOR COMMANDS===\n/tp <player> <x> <y> <z>    teleport a player to that position\n/pos <player>    get the position of a player\n/kickMC <player> [<reason>]    kick a player from the server\n/gamemode <player> <mode>    set the gamemode of a player\n/whitelist <add | remove> <player>     add or remove a player from the whitelist").queue();
            return;
        }



        //send message to game chat

        //get the user's display name on the server
        String name;
        if(event.getMember().getNickname()==null){
            name = author.getEffectiveName();
        }else{
            name = event.getMember().getNickname();
        }
        //get the color of the user's role on the server
        List<Role> roles = event.getMember().getRoles();
        int roleColor=16777215;
        for(int i=roles.size()-1;i>=0;i--) {//go backwards through the roles list to make sure the top role color is applied last
            if(536870911!=roles.get(i).getColorRaw())
                roleColor = roles.get(i).getColorRaw();
        }

        //format the message with all the colors and hover text and stuff
        MutableText chatMessage=MutableText.of(new Literal("")) ,discordText =MutableText.of(new Literal("Discord "));
        discordText.setStyle(chatMessage.getStyle().withColor(5592575));
        chatMessage.append(discordText);
        MutableText discordName = MutableText.of(new Literal(("["+name+"] ")));
        discordName.setStyle(discordName.getStyle().withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,MutableText.of(
                        new Literal(("Discord name: "+author.getName()+"\nid: "+author.getId()))))).withColor(roleColor));
        chatMessage.append(discordName);
        chatMessage.append(content);

        //send the message
        Main.pm.broadcast(chatMessage, false);

    }

    boolean hasPlayer(String name){
        List<ServerPlayerEntity> players = Main.pm.getPlayerList();
        for (ServerPlayerEntity player : players) {
            if (player.getName().getString().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
