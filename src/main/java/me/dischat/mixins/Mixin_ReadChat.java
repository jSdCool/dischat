package me.dischat.mixins;

import me.dischat.main.Main;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.dischat.main.Main.chatChannel;

@Mixin(PlayerList.class)
public class Mixin_ReadChat {

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",at =@At("HEAD"))
    private void broadcastChatMessage(Component text, boolean overlay, CallbackInfo ci){//detect when a system message is sent
       if(Main.discordConnected) {
           if (text.getString().length() > 9 && text.getString().startsWith("Discord ["))
             return;
           chatChannel.sendMessage(text.getString()).queue();
       }
    }

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",at=@At("HEAD"))
    private void broadcastChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params, CallbackInfo ci){//detetct when a player sends a message
        if(Main.discordConnected) {
            chatChannel.sendMessage("<" + sender.getName().getString() + "> " + message.decoratedContent().getString()).queue();
        }
    }

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/ChatType$Bound;)V",at=@At("HEAD"))
    private void broadcastChatMessage(PlayerChatMessage message, CommandSourceStack source, ChatType.Bound params, CallbackInfo ci){//detect when the /say command is used
        if(Main.discordConnected) {
            String msg = message.decoratedContent().getString();
            chatChannel.sendMessage("[" + source.getTextName() + "] " + msg).queue();
        }
    }
    //PlayerManager#broadcastChatMessage    inject here

}
