package me.dischat.mixins;

import me.dischat.main.Main;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftDedicatedServer.class)
public class Mixin_ShutDown {
    @Inject(method = "shutdown", at =@At("HEAD"),remap = true )
    private void shutdown(CallbackInfo ci){
        System.out.println("shutting down JDA");
        Main.shutDown();
    }


}
