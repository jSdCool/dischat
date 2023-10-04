package me.dischat.mixins;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayNetworkHandler.class)
public class Mixin_ReadPlayerChat {
    //@Shadow public ServerPlayerEntity player;
//
//
//
    //@Inject(method = "onGameMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V",at =@At("HEAD"),remap = true )
    //private void onGameMessage(ChatMessageC2SPacket packet, CallbackInfo ci){
    //    //System.out.println("detected by mixin2");
//
    //    if(Main.discordConnected) {
    //        Main.chatChannel.sendMessage("<"+player.getDisplayName().getString()+"> "+packet.getChatMessage()).queue();
    //    }
//
//
    //}
    //PlayerManager#broadcastChatMessage    inject here
}
