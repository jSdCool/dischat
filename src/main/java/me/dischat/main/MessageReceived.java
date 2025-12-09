package me.dischat.main;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.DetectedVersion;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            List<ServerPlayer> players = Main.pm.getPlayers();
            if(players.isEmpty()){
                channel.sendMessage("there are no players online").queue();
                return;
            }
            StringBuilder playersOut= new StringBuilder();
            for (ServerPlayer player : players) {
                playersOut.append(player.getName().getString()).append(" \n");
            }
            channel.sendMessage(playersOut.toString()).queue();
            return;
        }
        if(content.equals("/version")){
            channel.sendMessage("mod version: "+Main.modVersion+"\ngame version: "+ DetectedVersion.BUILT_IN.name()).queue();
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
                if(Main.pm.getPlayerNamesArray().length >0 && hasPlayer(contentSections[1])) {
                    ServerPlayer player = Main.pm.getPlayerByName(contentSections[1]);

                    player.teleportTo(player.level(),x, y, z, (Set<Relative>)EnumSet.noneOf(Relative.class),player.getYRot(),player.getXRot(),false);
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

                if(Main.pm.getPlayerNamesArray().length>0&&hasPlayer(contentSections[1])) {
                    ServerPlayer player = Main.pm.getPlayerByName(contentSections[1]);
                    Vec3 cords = player.position();
                    channel.sendMessage(contentSections[1]+": "+cords.x+" "+cords.y+" "+cords.z+" "+player.level().dimension().identifier()).queue();
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

                if(Main.pm.getPlayerNamesArray().length>0&&hasPlayer(contentSections[1])) {
                    ServerPlayer player = Main.pm.getPlayerByName(contentSections[1]);
                    if(finalReason.isEmpty())
                        finalReason="kicked by an operator from discord";
                    player.connection.disconnect(MutableComponent.create(new LiteralContents((finalReason))));
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

                if(Main.pm.getPlayerNamesArray().length>0&&hasPlayer(contentSections[1])) {
                    ServerPlayer player = Main.pm.getPlayerByName(contentSections[1]);
                    GameType mode=GameType.DEFAULT_MODE;
                    switch(contentSections[2]){
                        case "creative":
                            mode=GameType.CREATIVE;
                            break;
                        case "survival":
                            mode=GameType.SURVIVAL;
                            break;
                        case "adventure":
                            mode=GameType.ADVENTURE;
                            break;
                        case "spectator":
                            mode=GameType.SPECTATOR;
                            break;
                        default:
                            channel.sendMessage("invalid gamemode").queue();

                    }
                    player.setGameMode(mode);
                    channel.sendMessage("gamemode updated").queue();
                    player.displayClientMessage(MutableComponent.create(new LiteralContents("gamemode updated")),false);
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
                Optional<GameProfile> profileOptional = Main.ms.services().profileResolver().fetchByName(contentSections[2]);
                //check if the profile was found
                if(profileOptional.isEmpty()){
                    //if not then send the error to the user
                    channel.sendMessage("That player does not exist").queue();
                    return;
                }
                GameProfile gp = profileOptional.get();
                UserWhiteList whitelist = Main.pm.getWhiteList();
                UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(new NameAndId(gp));
                //actualy add the player to the whitlist
                whitelist.add(whitelistEntry);

                Main.LOGGER.info("Added "+ gp.name()+" to the Whitelist from discord");
                channel.sendMessage("Added "+gp.name()+" to the whitelist").queue();

            } else if (contentSections[1].equals("remove")) {
                //attempt to get the profile of the inputed name
                Optional<GameProfile> profileOptional = Main.ms.services().profileResolver().fetchByName(contentSections[2]);
                //check if the profile was found
                if(profileOptional.isEmpty()){
                    //if not then send the error to the user
                    channel.sendMessage("That player does not exist").queue();
                    return;
                }
                GameProfile gp = profileOptional.get();
                UserWhiteList whitelist = Main.pm.getWhiteList();
                if(!whitelist.isWhiteListed(new NameAndId(gp))){
                    channel.sendMessage("That player is not currely whitelisted").queue();
                    return;
                }
                UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(new NameAndId(gp));
                //actualy remove the player to the whitlist
                whitelist.remove(whitelistEntry);

                Main.LOGGER.info("Removed "+ gp.name()+" from the Whitelist from discord");
                channel.sendMessage("Removed "+gp.name()+" from the whitelist").queue();
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
                        PlayerTeam team = scb.getPlayerTeam(lookForTeam);
                        if(team == null){
                            channel.sendMessage("Did not find a team named: "+lookForTeam).queue();
                        }else{
                            String teamMembers = String.join(", ",team.getPlayers());
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
            channel.sendMessage("send messages in this channel to make them appear in Mincreaft\n===COMMANDS===\n" +
                    "/list    list online players\n" +
                    "/team <list> [<team name>]    list teams on the server or list the players on a team\n"+
                    "/version    get the version of this mod and the game\n" +
                    "===MODERATOR COMMANDS===\n" +
                    "/gamemode <player> <mode>    set the gamemode of a player\n" +
                    "/kickMC <player> [<reason>]    kick a player from the server\n" +
                    "/pos <player>    get the position of a player\n" +
                    "/tp <player> <x> <y> <z>    teleport a player to that position\n" +
                    "/whitelist <add | remove> <player>     add or remove a player from the whitelist"
            ).queue();
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
        MutableComponent chatMessage=MutableComponent.create(new LiteralContents("")) ,discordText =MutableComponent.create(new LiteralContents("Discord "));
        discordText.setStyle(chatMessage.getStyle().withColor(5592575));
        chatMessage.append(discordText);
        MutableComponent discordName = MutableComponent.create(new LiteralContents(("["+name+"] ")));
        discordName.setStyle(discordName.getStyle().withHoverEvent(
                new HoverEvent.ShowText(MutableComponent.create(
                        new LiteralContents(("Discord name: "+author.getName()+"\nid: "+author.getId()))))).withColor(roleColor));
        chatMessage.append(discordName);
        chatMessage.append(content);

        //send the message
        Main.pm.broadcastSystemMessage(chatMessage, false);

    }

    boolean hasPlayer(String name){
        List<ServerPlayer> players = Main.pm.getPlayers();
        for (ServerPlayer player : players) {
            if (player.getName().getString().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
