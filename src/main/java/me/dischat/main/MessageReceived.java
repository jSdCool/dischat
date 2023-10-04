package me.dischat.main;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;

import java.util.List;

public class MessageReceived extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        TextChannel channel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();
        User author = msg.getAuthor();
        String content = msg.getContentRaw();
        //String contentSections[] = content.split(" ");
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
}
