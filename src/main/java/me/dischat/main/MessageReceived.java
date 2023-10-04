package me.dischat.main;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.MinecraftVersion;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class MessageReceived extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        TextChannel channel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();
        User author = msg.getAuthor();
        String content = msg.getContentRaw();
        String contentSections[] = content.split(" ");
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
                double x = 0, y = 0, z = 0;
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
                    channel.sendMessage(contentSections[1]+": "+cords.x+" "+cords.y+" "+cords.z).queue();
                }else{
                    channel.sendMessage("player not found").queue();
                }

                return;
            }else {
                channel.sendMessage("you are not authorized to use this command").queue();
                return;
            }
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
                    player.networkHandler.disconnect(MutableText.of(new LiteralTextContent((finalReason))));
                    channel.sendMessage("kicked "+contentSections[1]+": "+finalReason).queue();
                    System.out.println("kicked "+contentSections[1]+": "+finalReason);
                }else{
                    channel.sendMessage("player not found").queue();
                }

                return;
            }else {
                channel.sendMessage("you are not authorized to use this command").queue();
                return;
            }
        }


        //send message to game chat

        //get the user's display name on the server
        String name;
        if(event.getMember().getNickname()==null){
            name = author.getName();
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
        MutableText chatMessage=MutableText.of(new LiteralTextContent("")) ,discordText =MutableText.of(new LiteralTextContent("Discord "));
        discordText.setStyle(chatMessage.getStyle().withColor(5592575));
        chatMessage.append(discordText);
        MutableText discordName = MutableText.of(new LiteralTextContent(("["+name+"] ")));
        discordName.setStyle(discordName.getStyle().withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,MutableText.of(
                        new LiteralTextContent(("Discord name: "+author.getName()+"\nid: "+author.getId()))))).withColor(roleColor));
        chatMessage.append(discordName);
        chatMessage.append(content);

        //send the message
        Main.pm.broadcast(chatMessage, false);

    }

    boolean hasPlayer(String name){
        List<ServerPlayerEntity> players = Main.pm.getPlayerList();
        for(int i=0;i<players.size();i++){
            if(players.get(i).getName().getString().equals(name)){
                return true;
            }
        }
        return false;
    }
}
