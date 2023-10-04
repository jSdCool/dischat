package me.dischat.mixins;

import me.dischat.main.Main;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.dischat.main.Main.chatChannel;

@Mixin(PlayerManager.class)
public class Mixin_ReadChat {

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V",at =@At("HEAD"),remap = true )
    private void broadcastChatMessage(Text text, boolean overlay, CallbackInfo ci){//detect when a system message is sent
       //System.out.println("detected by mixin");
       if(Main.discordConnected) {
           if (text.getString().length() > 9 && text.getString().startsWith("Discord ["))
             return;
            //System.out.println("sending");
           chatChannel.sendMessage(text.getString()).queue();
       }
    }

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",at=@At("HEAD"),remap = true)
    private void broadcastChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci){//detetct when a player sends a message
        if(Main.discordConnected) {
            String msg = message.getContent().getString();
            chatChannel.sendMessage("<" + sender.getName().getString() + "> " + message.getContent().getString()).queue();
        }
    }

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/server/command/ServerCommandSource;Lnet/minecraft/network/message/MessageType$Parameters;)V",at=@At("HEAD"),remap = true)
    private void broadcastChatMessage(SignedMessage message, ServerCommandSource source, MessageType.Parameters params, CallbackInfo ci){//detect when the /say command is used
        if(Main.discordConnected) {
            String msg = message.getContent().getString();
            chatChannel.sendMessage("[" + source.getName() + "] " + msg).queue();
        }
    }
    //PlayerManager#broadcastChatMessage    inject here

}
